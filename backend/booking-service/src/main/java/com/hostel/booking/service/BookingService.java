package com.hostel.booking.service;

import com.hostel.booking.client.NotificationServiceClient;
import com.hostel.booking.client.RoomServiceClient;
import com.hostel.booking.client.UserServiceClient;
import com.hostel.booking.dto.*;
import com.hostel.booking.entity.Booking;
import com.hostel.booking.entity.BookingStatus;
import com.hostel.booking.exception.BookingConflictException;
import com.hostel.booking.exception.BookingNotFoundException;
import com.hostel.booking.repository.BookingRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final RoomServiceClient roomServiceClient;
    private final UserServiceClient userServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final ModelMapper modelMapper;

    // ==================== CREATE BOOKING (US 03) ====================

    @Transactional
    public BookingDTO createBooking(Long userId, CreateBookingRequest request) {
        // Validate dates
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        // Get room info from Room Service
        RoomInfoDTO roomInfo = roomServiceClient.getRoomById(request.getRoomId());
        if ("UNAVAILABLE".equals(roomInfo.getStatus())) {
            throw new IllegalArgumentException("Room service is currently unavailable. Please try again later.");
        }
        if (!"AVAILABLE".equals(roomInfo.getStatus())) {
            throw new IllegalArgumentException("Room is not available for booking");
        }

        // Check occupancy against room capacity
        if (request.getOccupants() > roomInfo.getCapacity()) {
            throw new IllegalArgumentException(
                    "Number of occupants (" + request.getOccupants() +
                    ") exceeds room capacity (" + roomInfo.getCapacity() + ")");
        }

        // Check for overlapping bookings
        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                request.getRoomId(), request.getStartDate(), request.getEndDate());
        if (!overlapping.isEmpty()) {
            throw new BookingConflictException(
                    "Room " + roomInfo.getRoomNumber() + " already has bookings during the requested period");
        }

        // Calculate total price using Streams
        long numberOfNights = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        BigDecimal totalPrice = roomInfo.getPricePerNight()
                .multiply(BigDecimal.valueOf(numberOfNights));

        // Generate unique booking reference
        String bookingRef = "BK-" + LocalDate.now().getYear() + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Booking booking = Booking.builder()
                .userId(userId)
                .roomId(request.getRoomId())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .occupants(request.getOccupants())
                .status(BookingStatus.PENDING)
                .totalPrice(totalPrice)
                .bookingReference(bookingRef)
                .notes(request.getNotes())
                .build();

        Booking saved = bookingRepository.save(booking);
        log.info("Booking created: {} for room {} by user {}", bookingRef, roomInfo.getRoomNumber(), userId);

        BookingDTO dto = enrichBookingDTO(saved, roomInfo);

        // Notify student (async, non-blocking via fallback)
        try {
            notificationServiceClient.notifyBookingSubmitted(dto);
        } catch (Exception e) {
            log.warn("Could not send booking submission notification: {}", e.getMessage());
        }

        return dto;
    }

    // ==================== GET BOOKINGS ====================

    @Transactional(readOnly = true)
    public List<BookingDTO> getBookingsByUserId(Long userId) {
        return bookingRepository.findByUserId(userId).stream()
                .map(this::enrichBookingDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingDTO> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(this::enrichBookingDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BookingDTO> getBookingsByStatus(String status) {
        BookingStatus bookingStatus = BookingStatus.valueOf(status.toUpperCase());
        return bookingRepository.findByStatus(bookingStatus).stream()
                .map(this::enrichBookingDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BookingDTO getBookingById(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));
        return enrichBookingDTO(booking);
    }

    // ==================== UPDATE STATUS (US 05) ====================

    @Transactional
    public BookingDTO updateBookingStatus(Long id, UpdateBookingStatusRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        BookingStatus newStatus = BookingStatus.valueOf(request.getStatus().toUpperCase());
        BookingStatus oldStatus = booking.getStatus();

        // Validate status transitions
        validateStatusTransition(oldStatus, newStatus);

        booking.setStatus(newStatus);
        if (request.getNotes() != null) {
            booking.setNotes(request.getNotes());
        }

        Booking updated = bookingRepository.save(booking);
        log.info("Booking {} status changed: {} -> {}", booking.getBookingReference(), oldStatus, newStatus);

        BookingDTO dto = enrichBookingDTO(updated);

        // Send notifications based on status change
        try {
            switch (newStatus) {
                case APPROVED -> notificationServiceClient.notifyBookingApproved(dto);
                case REJECTED -> notificationServiceClient.notifyBookingRejected(dto);
                case CHECKED_IN -> notificationServiceClient.notifyBookingConfirmed(dto);
                default -> { /* no notification for other statuses */ }
            }
        } catch (Exception e) {
            log.warn("Could not send status change notification: {}", e.getMessage());
        }

        return dto;
    }

    // ==================== LATE CHECKOUT (US 14) ====================

    @Transactional
    public BookingDTO handleLateCheckout(Long id, LateCheckoutRequest request) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new BookingNotFoundException(id));

        if (booking.getStatus() != BookingStatus.CHECKED_IN) {
            throw new IllegalArgumentException("Late checkout can only be requested for checked-in bookings");
        }

        booking.setLateCheckoutRequested(true);
        booking.setLateCheckoutFee(request.getLateCheckoutFee() != null
                ? request.getLateCheckoutFee()
                : booking.getTotalPrice().multiply(new BigDecimal("0.15"))); // 15% fee default

        if (request.getNotes() != null) {
            booking.setNotes(request.getNotes());
        }

        Booking updated = bookingRepository.save(booking);
        log.info("Late checkout processed for booking {}: fee = {}",
                booking.getBookingReference(), updated.getLateCheckoutFee());

        return enrichBookingDTO(updated);
    }

    // ==================== OCCUPANCY REPORT (US 07) ====================

    @Transactional(readOnly = true)
    @CircuitBreaker(name = "roomService", fallbackMethod = "getOccupancyReportFallback")
    public List<OccupancyReportDTO> getOccupancyReport(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        long totalDaysInRange = ChronoUnit.DAYS.between(startDate, endDate);
        List<RoomInfoDTO> allRooms = roomServiceClient.getAllRooms();

        return allRooms.stream()
                .map(room -> {
                    List<Booking> bookings = bookingRepository.findBookingsForRoomInDateRange(
                            room.getId(), startDate, endDate);

                    // Calculate occupied days using streams
                    long occupiedDays = bookings.stream()
                            .mapToLong(booking -> {
                                LocalDate effectiveStart = booking.getStartDate().isBefore(startDate)
                                        ? startDate : booking.getStartDate();
                                LocalDate effectiveEnd = booking.getEndDate().isAfter(endDate)
                                        ? endDate : booking.getEndDate();
                                return ChronoUnit.DAYS.between(effectiveStart, effectiveEnd);
                            })
                            .sum();

                    double occupancyRate = totalDaysInRange > 0
                            ? (double) occupiedDays / totalDaysInRange * 100 : 0;

                    BigDecimal revenue = bookings.stream()
                            .map(Booking::getTotalPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return OccupancyReportDTO.builder()
                            .roomId(room.getId())
                            .roomNumber(room.getRoomNumber())
                            .roomType(room.getRoomType())
                            .totalDays(totalDaysInRange)
                            .occupiedDays(occupiedDays)
                            .occupancyRate(BigDecimal.valueOf(occupancyRate)
                                    .setScale(1, RoundingMode.HALF_UP).doubleValue())
                            .revenue(revenue)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<OccupancyReportDTO> getOccupancyReportFallback(
            LocalDate startDate, LocalDate endDate, Exception ex) {
        log.error("Cannot generate occupancy report — Room Service unavailable: {}", ex.getMessage());
        throw new IllegalArgumentException(
                "Cannot generate report: Room Service is temporarily unavailable");
    }

    // ==================== CSV EXPORT (US 11) ====================

    @Transactional(readOnly = true)
    public byte[] exportBookingsToCsv() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String header = "Booking Reference,Student Name,Student Email,Room Number," +
                "Check-in,Check-out,Occupants,Status,Total Price,Late Checkout Fee\n";

        String csvBody = bookingRepository.findAll().stream()
                .map(booking -> {
                    // Enrich with user and room info
                    UserInfoDTO user;
                    RoomInfoDTO room;
                    try {
                        user = userServiceClient.getUserById(booking.getUserId());
                    } catch (Exception e) {
                        user = UserInfoDTO.builder().firstName("N/A").lastName("").email("N/A").build();
                    }
                    try {
                        room = roomServiceClient.getRoomById(booking.getRoomId());
                    } catch (Exception e) {
                        room = RoomInfoDTO.builder().roomNumber("N/A").build();
                    }

                    return String.join(",",
                            booking.getBookingReference(),
                            "\"" + user.getFirstName() + " " + user.getLastName() + "\"",
                            user.getEmail(),
                            room.getRoomNumber(),
                            booking.getStartDate().format(formatter),
                            booking.getEndDate().format(formatter),
                            String.valueOf(booking.getOccupants()),
                            booking.getStatus().name(),
                            booking.getTotalPrice().toString(),
                            booking.getLateCheckoutFee() != null ? booking.getLateCheckoutFee().toString() : "0.00"
                    );
                })
                .collect(Collectors.joining("\n"));

        String csvContent = header + csvBody;
        log.info("CSV export generated with {} booking records", bookingRepository.count());

        return csvContent.getBytes(StandardCharsets.UTF_8);
    }

    // ==================== HELPERS ====================

    private void validateStatusTransition(BookingStatus from, BookingStatus to) {
        boolean valid = switch (from) {
            case PENDING -> to == BookingStatus.APPROVED || to == BookingStatus.REJECTED
                            || to == BookingStatus.CANCELLED;
            case APPROVED -> to == BookingStatus.CHECKED_IN || to == BookingStatus.CANCELLED;
            case CHECKED_IN -> to == BookingStatus.CHECKED_OUT;
            default -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException(
                    "Invalid status transition: " + from + " -> " + to);
        }
    }

    private BookingDTO enrichBookingDTO(Booking booking) {
        RoomInfoDTO room;
        UserInfoDTO user;
        try {
            room = roomServiceClient.getRoomById(booking.getRoomId());
        } catch (Exception e) {
            room = RoomInfoDTO.builder().roomNumber("N/A").build();
        }
        try {
            user = userServiceClient.getUserById(booking.getUserId());
        } catch (Exception e) {
            user = UserInfoDTO.builder().firstName("N/A").lastName("").email("N/A").build();
        }
        return enrichBookingDTO(booking, room);
    }

    private BookingDTO enrichBookingDTO(Booking booking, RoomInfoDTO room) {
        UserInfoDTO user;
        try {
            user = userServiceClient.getUserById(booking.getUserId());
        } catch (Exception e) {
            user = UserInfoDTO.builder().firstName("N/A").lastName("").email("N/A").build();
        }

        BookingDTO dto = modelMapper.map(booking, BookingDTO.class);
        dto.setRoomNumber(room.getRoomNumber());
        dto.setStudentName(user.getFirstName() + " " + user.getLastName());
        dto.setStudentEmail(user.getEmail());
        return dto;
    }
}
