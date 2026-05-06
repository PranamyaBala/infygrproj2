package com.hostel.booking.service;

import com.hostel.booking.dto.*;
import com.hostel.booking.entity.Booking;
import com.hostel.booking.entity.BookingStatus;
import com.hostel.booking.exception.BookingConflictException;
import com.hostel.booking.exception.BookingNotFoundException;
import com.hostel.booking.repository.BookingRepository;
import com.hostel.notification.service.EmailService;
import com.hostel.room.dto.RoomDTO;
import com.hostel.room.repository.PricingTierRepository;
import com.hostel.room.repository.RoomEventRepository;
import com.hostel.room.service.RoomService;
import com.hostel.user.dto.UserDTO;
import com.hostel.user.service.UserService;
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
    @Mock private PricingTierRepository pricingTierRepository;
    @Mock private RoomEventRepository roomEventRepository;
    @Mock private RoomService roomService;
    @Mock private UserService userService;
    @Mock private EmailService emailService;
    @Mock private ModelMapper modelMapper;
    @Mock private ReceiptService receiptService;

    @InjectMocks
    private BookingService bookingService;

    private UserDTO testUser;
    private RoomDTO testRoom;
    private Booking testBooking;
    private BookingDTO testBookingDTO;

    @BeforeEach
    void setUp() {
        testUser = UserDTO.builder()
                .id(1L).email("student@hostel.com")
                .firstName("John").lastName("Doe").build();

        testRoom = RoomDTO.builder()
                .id(1L).roomNumber("101").roomType("SINGLE")
                .capacity(1).status("AVAILABLE")
                .basePriceWithAmenities(BigDecimal.valueOf(210))
                .currentPrice(BigDecimal.valueOf(210))
                .build();

        testBooking = Booking.builder()
                .id(1L).userId(1L).roomId(1L)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .occupants(1)
                .status(BookingStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(420))
                .bookingReference("BK-2026-ABCD1234")
                .build();

        testBookingDTO = BookingDTO.builder()
                .id(1L).userId(1L).roomId(1L)
                .roomNumber("101")
                .studentName("John Doe").studentEmail("student@hostel.com")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .occupants(1).status("PENDING")
                .totalPrice(BigDecimal.valueOf(420))
                .bookingReference("BK-2026-ABCD1234")
                .build();
    }

    @Test
    @DisplayName("CreateBooking - Success: Single room booking")
    void createBooking_success() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setOccupants(1);

        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(bookingRepository.findOverlappingBookings(eq(1L), any(), any())).thenReturn(Collections.emptyList());
        when(roomEventRepository.findOverlappingEvents(eq(1L), any(), any())).thenReturn(Collections.emptyList());
        when(pricingTierRepository.findActiveByRoomIdAndDate(eq(1L), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);
        when(userService.getUserById(1L)).thenReturn(testUser);

        BookingDTO result = bookingService.createBooking("student@hostel.com", request);

        assertNotNull(result);
        assertEquals("BK-2026-ABCD1234", result.getBookingReference());
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("CreateBooking - Failure: Start date in the past")
    void createBooking_pastDate_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().minusDays(1));
        request.setEndDate(LocalDate.now().plusDays(1));
        request.setOccupants(1);

        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking("student@hostel.com", request));
    }

    @Test
    @DisplayName("CreateBooking - Failure: End date before start date")
    void createBooking_endBeforeStart_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(5));
        request.setEndDate(LocalDate.now().plusDays(2));
        request.setOccupants(1);

        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking("student@hostel.com", request));
    }

    @Test
    @DisplayName("CreateBooking - Failure: Room not available")
    void createBooking_roomUnavailable_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setOccupants(1);

        RoomDTO occupiedRoom = RoomDTO.builder()
                .id(1L).status("OCCUPIED").capacity(1).roomType("SINGLE").build();

        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);
        when(roomService.getRoomById(1L)).thenReturn(occupiedRoom);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking("student@hostel.com", request));
    }

    @Test
    @DisplayName("CreateBooking - Failure: Overlapping booking for private room")
    void createBooking_privateRoomOverlap_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setOccupants(1);

        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(bookingRepository.findOverlappingBookings(eq(1L), any(), any()))
                .thenReturn(List.of(testBooking));

        assertThrows(BookingConflictException.class,
                () -> bookingService.createBooking("student@hostel.com", request));
    }

    @Test
    @DisplayName("CreateBooking - Failure: Dormitory exceeds capacity")
    void createBooking_dormExceedsCapacity_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(2L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setOccupants(5);

        RoomDTO dormRoom = RoomDTO.builder()
                .id(2L).roomNumber("108").roomType("DORMITORY")
                .capacity(6).status("AVAILABLE")
                .basePriceWithAmenities(BigDecimal.valueOf(310))
                .build();

        Booking existingBooking = Booking.builder()
                .id(10L).roomId(2L).occupants(3)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .status(BookingStatus.APPROVED)
                .build();

        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);
        when(roomService.getRoomById(2L)).thenReturn(dormRoom);
        when(bookingRepository.findOverlappingBookings(eq(2L), any(), any()))
                .thenReturn(List.of(existingBooking));

        assertThrows(BookingConflictException.class,
                () -> bookingService.createBooking("student@hostel.com", request));
    }

    @Test
    @DisplayName("CreateBooking - Failure: Room reserved for event")
    void createBooking_eventConflict_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setOccupants(1);

        com.hostel.room.entity.RoomEvent event = com.hostel.room.entity.RoomEvent.builder()
                .id(1L).eventName("Tech Fest").build();

        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(bookingRepository.findOverlappingBookings(eq(1L), any(), any())).thenReturn(Collections.emptyList());
        when(roomEventRepository.findOverlappingEvents(eq(1L), any(), any())).thenReturn(List.of(event));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking("student@hostel.com", request));
    }

    @Test
    @DisplayName("CreateBooking - Failure: Gender Policy Conflict")
    void createBooking_genderPolicyConflict_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setOccupants(1);

        UserDTO maleUser = UserDTO.builder()
                .id(1L).email("male@hostel.com")
                .firstName("John").lastName("Doe")
                .gender("MALE").build();

        RoomDTO femaleRoom = RoomDTO.builder()
                .id(1L).roomNumber("101").roomType("SINGLE")
                .capacity(1).status("AVAILABLE")
                .genderPolicy("FEMALE_ONLY")
                .build();

        when(userService.getProfile("male@hostel.com")).thenReturn(maleUser);
        when(roomService.getRoomById(1L)).thenReturn(femaleRoom);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking("male@hostel.com", request),
                "This room is designated for females only.");
    }

    @Test
    @DisplayName("CreateBooking - Failure: End Date Before Start Date")
    void createBooking_endDateBeforeStartDate_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(3));
        request.setEndDate(LocalDate.now().plusDays(1));
        request.setOccupants(1);

        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking("student@hostel.com", request));
    }

    @Test
    @DisplayName("CreateBooking - Failure: Room Not Available")
    void createBooking_roomNotAvailable_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setOccupants(1);

        RoomDTO unavailableRoom = RoomDTO.builder().status("MAINTENANCE").build();

        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);
        when(roomService.getRoomById(1L)).thenReturn(unavailableRoom);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking("student@hostel.com", request));
    }

    @Test
    @DisplayName("CreateBooking - Failure: Occupants Exceed Capacity")
    void createBooking_occupantsExceedCapacity_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setOccupants(5); // Capacity is 1

        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);
        when(roomService.getRoomById(1L)).thenReturn(testRoom);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking("student@hostel.com", request));
    }

    @Test
    @DisplayName("CreateBooking - Failure: Dormitory Full")
    void createBooking_dormitoryFull_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setOccupants(2);

        RoomDTO dormRoom = RoomDTO.builder()
                .id(1L).roomNumber("101").roomType("DORMITORY")
                .capacity(4).status("AVAILABLE")
                .basePriceWithAmenities(BigDecimal.valueOf(200))
                .genderPolicy("COED").build();

        Booking existing = Booking.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .occupants(3).status(BookingStatus.APPROVED).build();

        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);
        when(roomService.getRoomById(1L)).thenReturn(dormRoom);
        when(bookingRepository.findOverlappingBookings(any(), any(), any()))
                .thenReturn(List.of(existing));

        assertThrows(BookingConflictException.class,
                () -> bookingService.createBooking("student@hostel.com", request));
    }

    @Test
    @DisplayName("CreateBooking - Failure: Private Room Booked")
    void createBooking_privateRoomBooked_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setOccupants(1);

        Booking existing = Booking.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .occupants(1).status(BookingStatus.APPROVED).build();

        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(bookingRepository.findOverlappingBookings(any(), any(), any()))
                .thenReturn(List.of(existing));

        assertThrows(BookingConflictException.class,
                () -> bookingService.createBooking("student@hostel.com", request));
    }

    @Test
    @DisplayName("CreateBooking - Failure: Room Event Conflict")
    void createBooking_roomEventConflict_throwsException() {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setOccupants(1);

        com.hostel.room.entity.RoomEvent event = com.hostel.room.entity.RoomEvent.builder()
                .eventName("Maintenance").build();

        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(bookingRepository.findOverlappingBookings(any(), any(), any())).thenReturn(java.util.Collections.emptyList());
        when(roomEventRepository.findOverlappingEvents(any(), any(), any()))
                .thenReturn(List.of(event));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking("student@hostel.com", request));
    }

    @Test
    @DisplayName("GetBookingsByUserEmail - Success")
    void getBookingsByUserEmail_success() {
        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);
        when(bookingRepository.findByUserId(1L)).thenReturn(List.of(testBooking));
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);

        List<BookingDTO> result = bookingService.getBookingsByUserEmail("student@hostel.com");

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("GetAllBookings - Success")
    void getAllBookings_success() {
        when(bookingRepository.findAll()).thenReturn(List.of(testBooking));
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);

        List<BookingDTO> result = bookingService.getAllBookings();

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("GetBookingById - Success")
    void getBookingById_success() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);

        BookingDTO result = bookingService.getBookingById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("GetBookingById - Failure: Not found")
    void getBookingById_notFound_throwsException() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class, () -> bookingService.getBookingById(99L));
    }

    @Test
    @DisplayName("UpdateBookingStatus - Approve: PENDING to APPROVED")
    void updateBookingStatus_approve_success() {
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest();
        request.setStatus("APPROVED");
        request.setNotes("Room allocated");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);

        BookingDTO result = bookingService.updateBookingStatus(1L, request);

        assertNotNull(result);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("UpdateBookingStatus - Failure: Invalid transition PENDING to CHECKED_IN")
    void updateBookingStatus_invalidTransition_throwsException() {
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest();
        request.setStatus("CHECKED_IN");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.updateBookingStatus(1L, request));
    }

    @Test
    @DisplayName("HandleLateCheckout - Success: Applies 15% default fee")
    void handleLateCheckout_success() {
        Booking checkedIn = Booking.builder()
                .id(1L).userId(1L).roomId(1L)
                .status(BookingStatus.CHECKED_IN)
                .totalPrice(BigDecimal.valueOf(420))
                .bookingReference("BK-2026-LATE")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .occupants(1)
                .build();

        LateCheckoutRequest request = new LateCheckoutRequest();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(checkedIn));
        when(bookingRepository.save(any(Booking.class))).thenReturn(checkedIn);
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);

        BookingDTO result = bookingService.handleLateCheckout(1L, request);

        assertNotNull(result);
        verify(bookingRepository).save(argThat(b -> b.getLateCheckoutRequested()));
    }

    @Test
    @DisplayName("HandleLateCheckout - Failure: Not checked in")
    void handleLateCheckout_notCheckedIn_throwsException() {
        LateCheckoutRequest request = new LateCheckoutRequest();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.handleLateCheckout(1L, request));
    }

    @Test
    @DisplayName("GetOccupancyReport - Success")
    void getOccupancyReport_success() {
        LocalDate start = LocalDate.of(2026, 4, 1);
        LocalDate end = LocalDate.of(2026, 4, 30);

        when(roomService.getAllRooms()).thenReturn(List.of(testRoom));
        when(bookingRepository.findBookingsForRoomInDateRange(eq(1L), any(), any()))
                .thenReturn(List.of(testBooking));

        List<OccupancyReportDTO> result = bookingService.getOccupancyReport(start, end);

        assertEquals(1, result.size());
        assertNotNull(result.get(0).getRoomNumber());
    }

    @Test
    @DisplayName("ExportBookingsToCsv - Success")
    void exportBookingsToCsv_success() {
        when(bookingRepository.findAll()).thenReturn(List.of(testBooking));
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(roomService.getRoomById(1L)).thenReturn(testRoom);

        byte[] csv = bookingService.exportBookingsToCsv();

        assertNotNull(csv);
        String csvStr = new String(csv);
        assertTrue(csvStr.contains("Booking Reference"));
        assertTrue(csvStr.contains("BK-2026-ABCD1234"));
    }

    @Test
    @DisplayName("CalculateRemainingCapacity - Private room with no bookings")
    void calculateRemainingCapacity_privateRoom() {
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(bookingRepository.findOverlappingBookings(eq(1L), any(), any())).thenReturn(Collections.emptyList());

        int remaining = bookingService.calculateRemainingCapacity(1L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        assertEquals(1, remaining);
    }
}
