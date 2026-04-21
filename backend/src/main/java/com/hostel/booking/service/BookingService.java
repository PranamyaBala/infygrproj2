package com.hostel.booking.service;

import com.hostel.booking.dto.*;
import com.hostel.booking.entity.Booking;
import com.hostel.booking.entity.BookingStatus;
import com.hostel.booking.exception.BookingConflictException;
import com.hostel.booking.exception.BookingNotFoundException;
import com.hostel.booking.repository.BookingRepository;
import com.hostel.notification.dto.BookingNotificationDTO;
import com.hostel.notification.service.EmailService;
import com.hostel.room.dto.RoomDTO;
import com.hostel.room.dto.UpdateRoomStatusRequest;
import com.hostel.room.service.RoomService;
import com.hostel.user.dto.UserDTO;
import com.hostel.user.service.UserService;
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
    private final com.hostel.room.repository.PricingTierRepository pricingTierRepository;
    private final com.hostel.room.repository.RoomEventRepository roomEventRepository;
    private final RoomService roomService;
    private final UserService userService;
    private final EmailService emailService;
    private final ModelMapper modelMapper;

    // ==================== CREATE BOOKING (US 03) ====================

    @Transactional
    public BookingDTO createBooking(String email, CreateBookingRequest request) {
        UserDTO currentUser = userService.getProfile(email);
        Long userId = currentUser.getId();
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        RoomDTO roomInfo = roomService.getRoomById(request.getRoomId());
        if (!"AVAILABLE".equals(roomInfo.getStatus())) {
            throw new IllegalArgumentException("Room is not available for booking");
        }

        if (request.getOccupants() > roomInfo.getCapacity()) {
            throw new IllegalArgumentException(
                    "Number of occupants (" + request.getOccupants() +
                    ") exceeds room capacity (" + roomInfo.getCapacity() + ")");
        }

        List<Booking> overlapping = bookingRepository.findOverlappingBookings(
                request.getRoomId(), request.getStartDate(), request.getEndDate());
        if (!overlapping.isEmpty()) {
            throw new BookingConflictException(
                    "Room " + roomInfo.getRoomNumber() + " already has bookings during the requested period");
        }

        // Check for Room Events (Group Assignment - US 09)
        List<com.hostel.room.entity.RoomEvent> events = roomEventRepository.findOverlappingEvents(
                request.getRoomId(), request.getStartDate(), request.getEndDate());
        if (!events.isEmpty()) {
            throw new IllegalArgumentException(
                    "This room is reserved for a special event: " + events.get(0).getEventName());
        }

        long numberOfNights = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        
        // Dynamic Pricing Logic (Season/Demand - US 13)
        BigDecimal totalPrice = BigDecimal.ZERO;
        LocalDate currentDate = request.getStartDate();
        while (currentDate.isBefore(request.getEndDate())) {
            BigDecimal dayPrice = roomInfo.getBasePriceWithAmenities();
            
            // Check for active pricing tiers on this specific date
            List<com.hostel.room.entity.PricingTier> tiers = pricingTierRepository
                    .findActiveByRoomIdAndDate(request.getRoomId(), currentDate);
            
            if (!tiers.isEmpty()) {
                // Apply the first active multiplier
                dayPrice = dayPrice.multiply(tiers.get(0).getPriceMultiplier());
            }
            
            totalPrice = totalPrice.add(dayPrice);
            currentDate = currentDate.plusDays(1);
        }

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

        try {
            emailService.sendBookingSubmittedEmail(mapToNotification(dto));
        } catch (Exception e) {
            log.warn("Could not send booking submission notification: {}", e.getMessage());
        }

        return dto;
    }

    // ==================== GET BOOKINGS ====================

    @Transactional(readOnly = true)
    public List<BookingDTO> getBookingsByUserEmail(String email) {
        UserDTO currentUser = userService.getProfile(email);
        return bookingRepository.findByUserId(currentUser.getId()).stream()
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

        validateStatusTransition(oldStatus, newStatus);

        booking.setStatus(newStatus);
        if (request.getNotes() != null) {
            booking.setNotes(request.getNotes());
        }

        Booking updated = bookingRepository.save(booking);
        log.info("Booking {} status changed: {} -> {}", booking.getBookingReference(), oldStatus, newStatus);

        BookingDTO dto = enrichBookingDTO(updated);

        // US 04/US 05 Sync: Automate Room Status based on Booking Status
        try {
            if (newStatus == BookingStatus.CHECKED_IN) {
                roomService.updateRoomStatus(booking.getRoomId(), UpdateRoomStatusRequest.builder()
                        .status("OCCUPIED")
                        .occupiedStartDate(booking.getStartDate())
                        .occupiedEndDate(booking.getEndDate())
                        .build());
                log.info("Auto-sync: Room {} marked as OCCUPIED due to check-in", booking.getRoomId());
            } else if (newStatus == BookingStatus.CHECKED_OUT) {
                roomService.updateRoomStatus(booking.getRoomId(), UpdateRoomStatusRequest.builder()
                        .status("AVAILABLE")
                        .build());
                log.info("Auto-sync: Room {} marked as AVAILABLE due to check-out", booking.getRoomId());
            }
        } catch (Exception e) {
            log.error("Failed to auto-sync room status for room {}: {}", booking.getRoomId(), e.getMessage());
        }

        try {
            BookingNotificationDTO notifDto = mapToNotification(dto);
            switch (newStatus) {
                case APPROVED -> emailService.sendBookingApprovedEmail(notifDto);
                case REJECTED -> emailService.sendBookingRejectedEmail(notifDto);
                case CHECKED_IN -> emailService.sendBookingConfirmedEmail(notifDto);
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
                : booking.getTotalPrice().multiply(new BigDecimal("0.15")));

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
    public List<OccupancyReportDTO> getOccupancyReport(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        // Ensure totalDaysInRange is at least 1 to avoid division by zero
        long totalDaysInRange = Math.max(1, ChronoUnit.DAYS.between(startDate, endDate));
        List<RoomDTO> allRooms = roomService.getAllRooms();

        return allRooms.stream()
                .map(room -> {
                    List<Booking> bookings = bookingRepository.findBookingsForRoomInDateRange(
                            room.getId(), startDate, endDate);

                    long occupiedDays = bookings.stream()
                            .mapToLong(booking -> {
                                LocalDate effectiveStart = booking.getStartDate().isBefore(startDate)
                                        ? startDate : booking.getStartDate();
                                LocalDate effectiveEnd = booking.getEndDate().isAfter(endDate)
                                        ? endDate : booking.getEndDate();
                                // Ensure at least 1 day if start and end are same
                                long days = ChronoUnit.DAYS.between(effectiveStart, effectiveEnd);
                                return Math.max(0, days);
                            })
                            .sum();

                    double occupancyRate = (double) occupiedDays / totalDaysInRange * 100;

                    BigDecimal revenue = bookings.stream()
                            .map(Booking::getTotalPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    return OccupancyReportDTO.builder()
                            .roomId(room.getId())
                            .roomNumber(room.getRoomNumber())
                            .roomType(room.getRoomType())
                            .totalDays(totalDaysInRange)
                            .occupiedDays(occupiedDays)
                            .occupancyRate(BigDecimal.valueOf(Math.min(100.0, occupancyRate))
                                    .setScale(1, RoundingMode.HALF_UP).doubleValue())
                            .revenue(revenue)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ==================== CSV EXPORT (US 11) ====================

    @Transactional(readOnly = true)
    public byte[] exportBookingsToCsv() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        String header = "Booking Reference,Student Name,Student Email,Room Number," +
                "Check-in,Check-out,Occupants,Status,Total Price,Late Checkout Fee\n";

        String csvBody = bookingRepository.findAll().stream()
                .map(booking -> {
                    UserDTO user;
                    RoomDTO room;
                    try {
                        user = userService.getUserById(booking.getUserId());
                    } catch (Exception e) {
                        user = UserDTO.builder().firstName("N/A").lastName("").email("N/A").build();
                    }
                    try {
                        room = roomService.getRoomById(booking.getRoomId());
                    } catch (Exception e) {
                        room = RoomDTO.builder().roomNumber("N/A").build();
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
        return csvContent.getBytes(StandardCharsets.UTF_8);
    }

    // ==================== HELPERS ====================

    private void validateStatusTransition(BookingStatus from, BookingStatus to) {
        boolean valid = switch (from) {
            case PENDING -> to == BookingStatus.APPROVED || to == BookingStatus.REJECTED || to == BookingStatus.CANCELLED;
            case APPROVED -> to == BookingStatus.CHECKED_IN || to == BookingStatus.CANCELLED;
            case CHECKED_IN -> to == BookingStatus.CHECKED_OUT;
            default -> false;
        };
        if (!valid) {
            throw new IllegalArgumentException("Invalid status transition: " + from + " -> " + to);
        }
    }

    private BookingDTO enrichBookingDTO(Booking booking) {
        RoomDTO room;
        UserDTO user;
        try {
            room = roomService.getRoomById(booking.getRoomId());
        } catch (Exception e) {
            room = RoomDTO.builder().roomNumber("N/A").build();
        }
        try {
            user = userService.getUserById(booking.getUserId());
        } catch (Exception e) {
            user = UserDTO.builder().firstName("N/A").lastName("").email("N/A").build();
        }
        return enrichBookingDTO(booking, room);
    }

    private BookingDTO enrichBookingDTO(Booking booking, RoomDTO room) {
        UserDTO user;
        try {
            user = userService.getUserById(booking.getUserId());
        } catch (Exception e) {
            user = UserDTO.builder().firstName("N/A").lastName("").email("N/A").build();
        }

        BookingDTO dto = modelMapper.map(booking, BookingDTO.class);
        dto.setRoomNumber(room.getRoomNumber());
        dto.setStudentName(user.getFirstName() + " " + user.getLastName());
        dto.setStudentEmail(user.getEmail());
        return dto;
    }

    private BookingNotificationDTO mapToNotification(BookingDTO dto) {
        BookingNotificationDTO notif = new BookingNotificationDTO();
        notif.setId(dto.getId());
        notif.setUserId(dto.getUserId());
        notif.setRoomId(dto.getRoomId());
        notif.setRoomNumber(dto.getRoomNumber());
        notif.setStudentName(dto.getStudentName());
        notif.setStudentEmail(dto.getStudentEmail());
        notif.setStartDate(dto.getStartDate());
        notif.setEndDate(dto.getEndDate());
        notif.setOccupants(dto.getOccupants());
        notif.setStatus(dto.getStatus());
        notif.setTotalPrice(dto.getTotalPrice());
        notif.setBookingReference(dto.getBookingReference());
        notif.setLateCheckoutRequested(dto.getLateCheckoutRequested());
        notif.setLateCheckoutFee(dto.getLateCheckoutFee());
        notif.setNotes(dto.getNotes());
        notif.setCreatedAt(dto.getCreatedAt());
        return notif;
    }
}
