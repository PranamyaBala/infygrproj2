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
    @DisplayName("CreateBooking - Success: Dormitory Room")
    void createBooking_dormitory_success() {
        testRoom.setRoomType("DORMITORY");
        testRoom.setCapacity(4);
        testRoom.setBasePriceWithAmenities(BigDecimal.valueOf(100));

        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(5));
        request.setOccupants(1);

        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(bookingRepository.findOverlappingBookings(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(roomEventRepository.findOverlappingEvents(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(pricingTierRepository.findActiveByRoomIdAndDate(eq(1L), any()))
                .thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);
        when(userService.getUserById(anyLong())).thenReturn(testUser);

        BookingDTO result = bookingService.createBooking("student@hostel.com", request);

        assertNotNull(result);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("CreateBooking - Success: Private Room")
    void createBooking_privateRoom_success() {
        testRoom.setRoomType("PRIVATE_ROOM");
        testRoom.setCapacity(2);
        testRoom.setBasePriceWithAmenities(BigDecimal.valueOf(300));

        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setOccupants(2);

        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(bookingRepository.findOverlappingBookings(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(roomEventRepository.findOverlappingEvents(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(pricingTierRepository.findActiveByRoomIdAndDate(eq(1L), any()))
                .thenReturn(List.of());
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);
        when(userService.getUserById(anyLong())).thenReturn(testUser);

        BookingDTO result = bookingService.createBooking("student@hostel.com", request);

        assertNotNull(result);
        verify(bookingRepository).save(any(Booking.class));
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
    @DisplayName("UpdateBookingStatus - Check-in: APPROVED to CHECKED_IN")
    void updateBookingStatus_checkin_success() {
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest();
        request.setStatus("CHECKED_IN");
        testBooking.setStatus(BookingStatus.APPROVED);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);

        BookingDTO result = bookingService.updateBookingStatus(1L, request);

        assertNotNull(result);
        verify(roomService).updateRoomStatus(eq(1L), any());
    }

    @Test
    @DisplayName("UpdateBookingStatus - Check-out: CHECKED_IN to CHECKED_OUT")
    void updateBookingStatus_checkout_success() {
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest();
        request.setStatus("CHECKED_OUT");
        testBooking.setStatus(BookingStatus.CHECKED_IN);

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);

        BookingDTO result = bookingService.updateBookingStatus(1L, request);

        assertNotNull(result);
        verify(roomService).updateRoomStatus(eq(1L), any());
    }

    @Test
    @DisplayName("CreateBooking - Conflict: Private Room already booked")
    void createBooking_privateRoomConflict_throwsException() {
        // Reset room to private
        RoomDTO room = RoomDTO.builder()
                .id(1L).roomNumber("101").roomType("PRIVATE_ROOM")
                .capacity(1).status("AVAILABLE").genderPolicy("ANY")
                .build();
        
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setOccupants(1);

        when(userService.getProfile(anyString())).thenReturn(testUser);
        when(roomService.getRoomById(1L)).thenReturn(room);
        when(bookingRepository.findOverlappingBookings(eq(1L), any(), any()))
                .thenReturn(List.of(testBooking));

        assertThrows(BookingConflictException.class,
                () -> bookingService.createBooking("student@hostel.com", request));
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
    @DisplayName("CalculateRemainingCapacity - Dormitory with overlapping bookings")
    void calculateRemainingCapacity_dormitory() {
        RoomDTO dormRoom = RoomDTO.builder()
                .id(2L).roomNumber("108").roomType("DORMITORY")
                .capacity(6).status("AVAILABLE").build();

        Booking existing = Booking.builder()
                .id(10L).roomId(2L).occupants(3)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(4))
                .status(BookingStatus.APPROVED).build();

        when(roomService.getRoomById(2L)).thenReturn(dormRoom);
        when(bookingRepository.findOverlappingBookings(eq(2L), any(), any())).thenReturn(List.of(existing));

        int remaining = bookingService.calculateRemainingCapacity(2L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(4));

        assertEquals(3, remaining);
    }

    @Test
    @DisplayName("CalculateRemainingCapacity - Private room already booked returns 0")
    void calculateRemainingCapacity_privateRoomBooked() {
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(bookingRepository.findOverlappingBookings(eq(1L), any(), any())).thenReturn(List.of(testBooking));

        int remaining = bookingService.calculateRemainingCapacity(1L,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3));

        assertEquals(0, remaining);
    }

    @Test
    @DisplayName("GetBookingsByStatus - Returns filtered list")
    void getBookingsByStatus_success() {
        when(bookingRepository.findByStatus(BookingStatus.PENDING)).thenReturn(List.of(testBooking));
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);

        List<BookingDTO> result = bookingService.getBookingsByStatus("PENDING");

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("GetOccupiedDateRanges - Private room returns blocked ranges")
    void getOccupiedDateRanges_privateRoom() {
        when(roomService.getRoomById(1L)).thenReturn(testRoom); // SINGLE type
        when(bookingRepository.findByRoomId(1L)).thenReturn(List.of(testBooking));

        List<OccupiedDateRangeDTO> result = bookingService.getOccupiedDateRanges(1L);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("GetOccupiedDateRanges - Dormitory returns empty if not full")
    void getOccupiedDateRanges_dormNotFull() {
        RoomDTO dormRoom = RoomDTO.builder()
                .id(2L).roomNumber("108").roomType("DORMITORY")
                .capacity(6).status("AVAILABLE").build();

        Booking b = Booking.builder()
                .id(10L).roomId(2L).occupants(2)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .status(BookingStatus.APPROVED).build();

        when(roomService.getRoomById(2L)).thenReturn(dormRoom);
        when(bookingRepository.findByRoomId(2L)).thenReturn(List.of(b));

        List<OccupiedDateRangeDTO> result = bookingService.getOccupiedDateRanges(2L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("GetOccupiedDateRanges - Dormitory returns full range when at capacity")
    void getOccupiedDateRanges_dormFull() {
        RoomDTO dormRoom = RoomDTO.builder()
                .id(2L).roomNumber("108").roomType("DORMITORY")
                .capacity(4).status("AVAILABLE").build();

        Booking b1 = Booking.builder().id(10L).roomId(2L).occupants(2)
                .startDate(LocalDate.now().plusDays(1)).endDate(LocalDate.now().plusDays(5))
                .status(BookingStatus.APPROVED).build();
        Booking b2 = Booking.builder().id(11L).roomId(2L).occupants(2)
                .startDate(LocalDate.now().plusDays(1)).endDate(LocalDate.now().plusDays(5))
                .status(BookingStatus.APPROVED).build();

        when(roomService.getRoomById(2L)).thenReturn(dormRoom);
        when(bookingRepository.findByRoomId(2L)).thenReturn(List.of(b1, b2));

        List<OccupiedDateRangeDTO> result = bookingService.getOccupiedDateRanges(2L);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("GetOccupiedDateRanges - Dormitory empty bookings returns empty")
    void getOccupiedDateRanges_dormNoBookings() {
        RoomDTO dormRoom = RoomDTO.builder()
                .id(2L).roomType("DORMITORY").capacity(4).build();

        when(roomService.getRoomById(2L)).thenReturn(dormRoom);
        when(bookingRepository.findByRoomId(2L)).thenReturn(Collections.emptyList());

        List<OccupiedDateRangeDTO> result = bookingService.getOccupiedDateRanges(2L);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("GenerateBookingReceipt - Success")
    void generateBookingReceipt_success() {
        testBooking.setStatus(BookingStatus.APPROVED);
        byte[] pdf = "PDF".getBytes();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(receiptService.generateReceipt(any(), any(), any())).thenReturn(pdf);

        byte[] result = bookingService.generateBookingReceipt(1L, "student@hostel.com");

        assertArrayEquals(pdf, result);
    }

    @Test
    @DisplayName("GenerateBookingReceipt - Failure: Unauthorized user")
    void generateBookingReceipt_unauthorized() {
        testBooking.setStatus(BookingStatus.APPROVED);
        UserDTO otherUser = UserDTO.builder().id(99L).email("other@hostel.com").build();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(userService.getProfile("other@hostel.com")).thenReturn(otherUser);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> bookingService.generateBookingReceipt(1L, "other@hostel.com"));
    }

    @Test
    @DisplayName("GenerateBookingReceipt - Failure: Booking is PENDING (no receipt)")
    void generateBookingReceipt_pendingBooking_throwsException() {
        // testBooking is PENDING by default
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);

        assertThrows(IllegalStateException.class,
                () -> bookingService.generateBookingReceipt(1L, "student@hostel.com"));
    }

    @Test
    @DisplayName("GenerateBookingReceipt - Failure: Booking not found")
    void generateBookingReceipt_notFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class,
                () -> bookingService.generateBookingReceipt(99L, "student@hostel.com"));
    }

    @Test
    @DisplayName("UpdateBookingStatus - PENDING to REJECTED")
    void updateBookingStatus_reject_success() {
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest();
        request.setStatus("REJECTED");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);

        BookingDTO result = bookingService.updateBookingStatus(1L, request);

        assertNotNull(result);
    }

    @Test
    @DisplayName("UpdateBookingStatus - PENDING to CANCELLED")
    void updateBookingStatus_cancel_success() {
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest();
        request.setStatus("CANCELLED");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);

        BookingDTO result = bookingService.updateBookingStatus(1L, request);

        assertNotNull(result);
    }

    @Test
    @DisplayName("UpdateBookingStatus - Dorm room checkout makes available")
    void updateBookingStatus_dormCheckout_makesAvailable() {
        RoomDTO dormRoom = RoomDTO.builder()
                .id(1L).roomNumber("108").roomType("DORMITORY")
                .capacity(4).status("OCCUPIED").build();

        testBooking.setStatus(BookingStatus.CHECKED_IN);
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest();
        request.setStatus("CHECKED_OUT");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(bookingRepository.findByRoomId(1L)).thenReturn(Collections.emptyList());
        when(roomService.getRoomById(1L)).thenReturn(dormRoom);
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);

        BookingDTO result = bookingService.updateBookingStatus(1L, request);

        assertNotNull(result);
        verify(roomService).updateRoomStatus(eq(1L), any());
    }

    @Test
    @DisplayName("HandleLateCheckout - Success: With custom fee")
    void handleLateCheckout_withCustomFee() {
        Booking checkedIn = Booking.builder()
                .id(1L).userId(1L).roomId(1L)
                .status(BookingStatus.CHECKED_IN)
                .totalPrice(BigDecimal.valueOf(420))
                .bookingReference("BK-2026-LATE")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(1))
                .occupants(1).build();

        LateCheckoutRequest request = new LateCheckoutRequest();
        request.setLateCheckoutFee(BigDecimal.valueOf(50));
        request.setNotes("Late due to travel");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(checkedIn));
        when(bookingRepository.save(any(Booking.class))).thenReturn(checkedIn);
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(userService.getUserById(1L)).thenReturn(testUser);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);

        BookingDTO result = bookingService.handleLateCheckout(1L, request);

        assertNotNull(result);
        verify(bookingRepository).save(argThat(b ->
                b.getLateCheckoutFee().compareTo(BigDecimal.valueOf(50)) == 0));
    }

    @Test
    @DisplayName("GetOccupancyReport - Failure: Start after end")
    void getOccupancyReport_invalidDates_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> bookingService.getOccupancyReport(
                        LocalDate.now().plusDays(5), LocalDate.now()));
    }

    @Test
    @DisplayName("ExportBookingsToCsv - Handles missing user/room gracefully")
    void exportBookingsToCsv_withMissingUserRoom() {
        when(bookingRepository.findAll()).thenReturn(List.of(testBooking));
        when(userService.getUserById(1L)).thenThrow(new RuntimeException("User not found"));
        when(roomService.getRoomById(1L)).thenThrow(new RuntimeException("Room not found"));

        byte[] csv = bookingService.exportBookingsToCsv();

        assertNotNull(csv);
        String csvStr = new String(csv);
        assertTrue(csvStr.contains("Booking Reference"));
        assertTrue(csvStr.contains("N/A"));
    }

    @Test
    @DisplayName("CreateBooking - Success: With active pricing tier (seasonal multiplier)")
    void createBooking_withPricingTier_success() {
        com.hostel.room.entity.PricingTier tier = com.hostel.room.entity.PricingTier.builder()
                .id(1L).seasonName("Summer")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(2))
                .priceMultiplier(new java.math.BigDecimal("1.5"))
                .build();

        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setOccupants(1);

        when(userService.getProfile("student@hostel.com")).thenReturn(testUser);
        when(roomService.getRoomById(1L)).thenReturn(testRoom);
        when(bookingRepository.findOverlappingBookings(eq(1L), any(), any())).thenReturn(Collections.emptyList());
        when(roomEventRepository.findOverlappingEvents(eq(1L), any(), any())).thenReturn(Collections.emptyList());
        when(pricingTierRepository.findActiveByRoomIdAndDate(eq(1L), any())).thenReturn(List.of(tier));
        when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);
        when(modelMapper.map(any(Booking.class), eq(BookingDTO.class))).thenReturn(testBookingDTO);
        when(userService.getUserById(1L)).thenReturn(testUser);

        BookingDTO result = bookingService.createBooking("student@hostel.com", request);

        assertNotNull(result);
        verify(bookingRepository).save(any(Booking.class));
    }

    @Test
    @DisplayName("CreateBooking - Gender policy MALE_ONLY blocks female user")
    void createBooking_maleOnlyRoom_blocksFemalUser() {
        UserDTO femaleUser = UserDTO.builder()
                .id(2L).email("female@hostel.com").gender("FEMALE").build();

        RoomDTO maleRoom = RoomDTO.builder()
                .id(1L).roomNumber("M01").roomType("SINGLE")
                .capacity(1).status("AVAILABLE")
                .genderPolicy("MALE_ONLY").build();

        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setOccupants(1);

        when(userService.getProfile("female@hostel.com")).thenReturn(femaleUser);
        when(roomService.getRoomById(1L)).thenReturn(maleRoom);

        assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking("female@hostel.com", request));
    }
}
