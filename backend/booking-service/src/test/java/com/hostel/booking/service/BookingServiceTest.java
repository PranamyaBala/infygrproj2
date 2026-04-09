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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private RoomServiceClient roomServiceClient;
    @Mock private UserServiceClient userServiceClient;
    @Mock private NotificationServiceClient notificationServiceClient;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private BookingService bookingService;

    private Booking testBooking;
    private BookingDTO testBookingDTO;
    private RoomInfoDTO testRoomInfo;
    private UserInfoDTO testUserInfo;

    @BeforeEach
    void setUp() {
        testRoomInfo = RoomInfoDTO.builder()
                .id(1L).roomNumber("101").roomType("SINGLE")
                .capacity(2).pricePerNight(new BigDecimal("50.00"))
                .status("AVAILABLE").build();

        testUserInfo = UserInfoDTO.builder()
                .id(1L).firstName("John").lastName("Doe")
                .email("john@hostel.com").build();

        testBooking = Booking.builder()
                .id(1L).userId(1L).roomId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .occupants(1).status(BookingStatus.PENDING)
                .totalPrice(new BigDecimal("200.00"))
                .bookingReference("BK-2025-TEST1234")
                .lateCheckoutFee(BigDecimal.ZERO)
                .build();

        testBookingDTO = BookingDTO.builder()
                .id(1L).userId(1L).roomId(1L).roomNumber("101")
                .studentName("John Doe").studentEmail("john@hostel.com")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .occupants(1).status("PENDING")
                .totalPrice(new BigDecimal("200.00"))
                .bookingReference("BK-2025-TEST1234").build();
    }

    // ==================== CREATE BOOKING TESTS ====================

    @Test
    @DisplayName("Should create booking successfully")
    void createBooking_Success() {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .occupants(1).build();

        when(roomServiceClient.getRoomById(1L)).thenReturn(testRoomInfo);
        when(bookingRepository.findOverlappingBookings(eq(1L), any(), any()))
                .thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);
        when(userServiceClient.getUserById(1L)).thenReturn(testUserInfo);

        BookingDTO result = bookingService.createBooking(1L, request);

        assertNotNull(result);
        assertEquals("101", result.getRoomNumber());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should reject booking when dates overlap")
    void createBooking_OverlappingDates() {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .occupants(1).build();

        when(roomServiceClient.getRoomById(1L)).thenReturn(testRoomInfo);
        when(bookingRepository.findOverlappingBookings(eq(1L), any(), any()))
                .thenReturn(List.of(testBooking));

        assertThrows(BookingConflictException.class,
                () -> bookingService.createBooking(1L, request));
    }

    @Test
    @DisplayName("Should reject booking when occupants exceed capacity")
    void createBooking_ExceedsCapacity() {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .occupants(5).build();

        when(roomServiceClient.getRoomById(1L)).thenReturn(testRoomInfo);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(1L, request));
    }

    @Test
    @DisplayName("Should reject booking with past start date")
    void createBooking_PastDate() {
        CreateBookingRequest request = CreateBookingRequest.builder()
                .roomId(1L)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .occupants(1).build();

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(1L, request));
    }

    // ==================== STATUS UPDATE TESTS ====================

    @Test
    @DisplayName("Should approve a pending booking")
    void updateStatus_Approve() {
        UpdateBookingStatusRequest request = UpdateBookingStatusRequest.builder()
                .status("APPROVED").build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);
        when(userServiceClient.getUserById(1L)).thenReturn(testUserInfo);
        when(roomServiceClient.getRoomById(anyLong())).thenReturn(testRoomInfo);

        BookingDTO result = bookingService.updateBookingStatus(1L, request);

        assertNotNull(result);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("Should reject invalid status transition")
    void updateStatus_InvalidTransition() {
        testBooking.setStatus(BookingStatus.CHECKED_OUT);
        UpdateBookingStatusRequest request = UpdateBookingStatusRequest.builder()
                .status("APPROVED").build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.updateBookingStatus(1L, request));
    }

    @Test
    @DisplayName("Should throw BookingNotFoundException for invalid ID")
    void updateStatus_NotFound() {
        when(bookingRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class,
                () -> bookingService.updateBookingStatus(999L,
                        UpdateBookingStatusRequest.builder().status("APPROVED").build()));
    }

    // ==================== LATE CHECKOUT TEST ====================

    @Test
    @DisplayName("Should process late checkout for checked-in booking")
    void handleLateCheckout_Success() {
        testBooking.setStatus(BookingStatus.CHECKED_IN);
        LateCheckoutRequest request = LateCheckoutRequest.builder()
                .lateCheckoutFee(new BigDecimal("30.00")).build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);
        when(userServiceClient.getUserById(1L)).thenReturn(testUserInfo);
        when(roomServiceClient.getRoomById(anyLong())).thenReturn(testRoomInfo);

        BookingDTO result = bookingService.handleLateCheckout(1L, request);

        assertNotNull(result);
        verify(bookingRepository).save(any(Booking.class));
    }

    // ==================== GET BOOKINGS TESTS ====================

    @Test
    @DisplayName("Should get bookings by user ID")
    void getBookingsByUserId_Success() {
        when(bookingRepository.findByUserId(1L)).thenReturn(List.of(testBooking));
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);
        when(roomServiceClient.getRoomById(1L)).thenReturn(testRoomInfo);
        when(userServiceClient.getUserById(1L)).thenReturn(testUserInfo);

        List<BookingDTO> result = bookingService.getBookingsByUserId(1L);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should get all bookings for admin")
    void getAllBookings_Success() {
        when(bookingRepository.findAll()).thenReturn(List.of(testBooking));
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);
        when(roomServiceClient.getRoomById(1L)).thenReturn(testRoomInfo);
        when(userServiceClient.getUserById(1L)).thenReturn(testUserInfo);

        List<BookingDTO> result = bookingService.getAllBookings();

        assertEquals(1, result.size());
    }

    // ==================== CSV EXPORT TEST ====================

    @Test
    @DisplayName("Should export bookings to CSV")
    void exportBookingsToCsv_Success() {
        when(bookingRepository.findAll()).thenReturn(List.of(testBooking));
        when(bookingRepository.count()).thenReturn(1L);
        when(roomServiceClient.getRoomById(1L)).thenReturn(testRoomInfo);
        when(userServiceClient.getUserById(1L)).thenReturn(testUserInfo);

        byte[] csv = bookingService.exportBookingsToCsv();

        assertNotNull(csv);
        String csvContent = new String(csv);
        assertTrue(csvContent.contains("Booking Reference"));
        assertTrue(csvContent.contains("BK-2025-TEST1234"));
        assertTrue(csvContent.contains("101"));
    }

    // ==================== OCCUPANCY REPORT TEST ====================

    @Test
    @DisplayName("Should generate occupancy report")
    void getOccupancyReport_Success() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 31);

        when(roomServiceClient.getAllRooms()).thenReturn(List.of(testRoomInfo));
        when(bookingRepository.findBookingsForRoomInDateRange(eq(1L), any(), any()))
                .thenReturn(List.of(testBooking));

        List<OccupancyReportDTO> report = bookingService.getOccupancyReport(start, end);

        assertNotNull(report);
        assertEquals(1, report.size());
        assertEquals("101", report.get(0).getRoomNumber());
    }
}
