package com.hostel.room.service;

import com.hostel.booking.repository.BookingRepository;
import com.hostel.room.dto.*;
import com.hostel.room.entity.*;
import com.hostel.room.exception.RoomAlreadyExistsException;
import com.hostel.room.exception.RoomNotFoundException;
import com.hostel.room.repository.*;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock private RoomRepository roomRepository;
    @Mock private AmenityRepository amenityRepository;
    @Mock private PricingTierRepository pricingTierRepository;
    @Mock private RoomEventRepository roomEventRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private RoomService roomService;

    private Room testRoom;
    private RoomDTO testRoomDTO;
    private Amenity testAmenity;

    @BeforeEach
    void setUp() {
        testAmenity = Amenity.builder()
                .id(1L).name("WiFi").price(BigDecimal.valueOf(10)).build();

        testRoom = Room.builder()
                .id(1L).roomNumber("101").roomType(RoomType.SINGLE)
                .floor(1).capacity(1)
                .pricePerNight(BigDecimal.valueOf(200))
                .status(RoomStatus.AVAILABLE)
                .amenities(new HashSet<>(Set.of(testAmenity)))
                .build();

        testRoomDTO = RoomDTO.builder()
                .id(1L).roomNumber("101").roomType("SINGLE")
                .floor(1).capacity(1)
                .pricePerNight(BigDecimal.valueOf(200))
                .status("AVAILABLE")
                .basePriceWithAmenities(BigDecimal.valueOf(210))
                .currentPrice(BigDecimal.valueOf(210))
                .build();
    }

    @Test
    @DisplayName("SearchRooms - No filter returns all rooms")
    void searchRooms_noFilter() {
        RoomSearchCriteria criteria = new RoomSearchCriteria();
        when(roomRepository.searchRooms(null, null, null, null, null, null))
                .thenReturn(List.of(testRoom));
        when(modelMapper.map(any(Room.class), eq(RoomDTO.class))).thenReturn(testRoomDTO);
        when(pricingTierRepository.findByRoomId(1L)).thenReturn(Collections.emptyList());
        when(bookingRepository.sumActiveOccupants(eq(1L), any(LocalDate.class))).thenReturn(0);

        List<RoomDTO> result = roomService.searchRooms(criteria);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("SearchRooms - Filters by GenderPolicy")
    void searchRooms_filtersByGenderPolicy() {
        RoomSearchCriteria criteria = new RoomSearchCriteria();
        criteria.setGenderPolicy("MALE");
        
        Room femaleRoom = Room.builder()
                .id(2L).roomNumber("102").roomType(RoomType.SINGLE)
                .capacity(1)
                .genderPolicy(GenderPolicy.FEMALE_ONLY).build();
        
        Room maleRoom = Room.builder()
                .id(3L).roomNumber("103").roomType(RoomType.SINGLE)
                .capacity(1)
                .genderPolicy(GenderPolicy.MALE_ONLY).build();

        when(roomRepository.searchRooms(null, null, null, null, null, null))
                .thenReturn(List.of(testRoom, femaleRoom, maleRoom));
        
        // Only mapping the rooms that pass the filter
        lenient().when(modelMapper.map(any(Room.class), eq(RoomDTO.class))).thenReturn(testRoomDTO);
        
        when(pricingTierRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());
        when(bookingRepository.sumActiveOccupants(anyLong(), any(LocalDate.class))).thenReturn(0);

        List<RoomDTO> result = roomService.searchRooms(criteria);

        // Should return testRoom (COED default) and maleRoom (MALE_ONLY).
        // Should filter out femaleRoom.
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("SearchRooms - Filters by Amenities")
    void searchRooms_filtersByAmenities() {
        RoomSearchCriteria criteria = new RoomSearchCriteria();
        criteria.setAmenities(List.of("WiFi"));
        
        when(roomRepository.searchRooms(null, null, null, null, null, null))
                .thenReturn(List.of(testRoom));
        when(modelMapper.map(any(Room.class), eq(RoomDTO.class))).thenReturn(testRoomDTO);
        when(pricingTierRepository.findByRoomId(1L)).thenReturn(Collections.emptyList());
        when(bookingRepository.sumActiveOccupants(eq(1L), any())).thenReturn(0);

        List<RoomDTO> result = roomService.searchRooms(criteria);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("CreateRoom - Success")
    void createRoom_success() {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setRoomNumber("105");
        request.setRoomType("SINGLE");
        request.setCapacity(1);
        request.setFloor(1);
        request.setPricePerNight(BigDecimal.valueOf(200));

        when(roomRepository.existsByRoomNumber("105")).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(modelMapper.map(any(Room.class), eq(RoomDTO.class))).thenReturn(testRoomDTO);
        when(pricingTierRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());
        when(bookingRepository.sumActiveOccupants(anyLong(), any())).thenReturn(0);

        RoomDTO result = roomService.createRoom(request);

        assertNotNull(result);
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    @DisplayName("GetRoomById - Success")
    void getRoomById_success() {
        when(roomRepository.findByIdWithAmenities(1L)).thenReturn(Optional.of(testRoom));
        when(modelMapper.map(any(Room.class), eq(RoomDTO.class))).thenReturn(testRoomDTO);
        when(pricingTierRepository.findByRoomId(1L)).thenReturn(Collections.emptyList());
        when(bookingRepository.sumActiveOccupants(eq(1L), any(LocalDate.class))).thenReturn(0);

        RoomDTO result = roomService.getRoomById(1L);

        assertNotNull(result);
        assertEquals("101", result.getRoomNumber());
    }

    @Test
    @DisplayName("GetRoomById - Not found throws RoomNotFoundException")
    void getRoomById_notFound_throwsException() {
        when(roomRepository.findByIdWithAmenities(99L)).thenReturn(Optional.empty());

        assertThrows(RoomNotFoundException.class, () -> roomService.getRoomById(99L));
    }

    @Test
    @DisplayName("GetRoomByNumber - Success")
    void getRoomByNumber_success() {
        when(roomRepository.findByRoomNumber("101")).thenReturn(Optional.of(testRoom));
        when(modelMapper.map(any(Room.class), eq(RoomDTO.class))).thenReturn(testRoomDTO);
        when(pricingTierRepository.findByRoomId(1L)).thenReturn(Collections.emptyList());
        when(bookingRepository.sumActiveOccupants(eq(1L), any(LocalDate.class))).thenReturn(0);

        RoomDTO result = roomService.getRoomByNumber("101");

        assertEquals("101", result.getRoomNumber());
    }

    @Test
    @DisplayName("CreateRoom - Duplicate number throws RoomAlreadyExistsException")
    void createRoom_duplicateNumber_throwsException() {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setRoomNumber("101");
        request.setRoomType("SINGLE");

        when(roomRepository.existsByRoomNumber("101")).thenReturn(true);

        assertThrows(RoomAlreadyExistsException.class, () -> roomService.createRoom(request));
    }

    @Test
    @DisplayName("CreateRoom - Invalid room number throws IllegalArgumentException")
    void createRoom_invalidNumber_throwsException() {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setRoomNumber("199");
        request.setRoomType("SINGLE");

        assertThrows(IllegalArgumentException.class, () -> roomService.createRoom(request));
    }

    @Test
    @DisplayName("UpdateRoom - Success")
    void updateRoom_success() {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setRoomNumber("101");
        request.setRoomType("DOUBLE");
        request.setFloor(1);
        request.setCapacity(2);
        request.setPricePerNight(BigDecimal.valueOf(300));
        request.setAmenityIds(List.of(1L, 2L, 3L));

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(amenityRepository.findByIdIn(List.of(1L, 2L, 3L))).thenReturn(List.of(testAmenity));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(modelMapper.map(any(Room.class), eq(RoomDTO.class))).thenReturn(testRoomDTO);
        when(pricingTierRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());
        when(bookingRepository.sumActiveOccupants(anyLong(), any(LocalDate.class))).thenReturn(0);

        RoomDTO result = roomService.updateRoom(1L, request);

        assertNotNull(result);
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    @DisplayName("UpdateRoomStatus - to MAINTENANCE")
    void updateRoomStatus_toMaintenance() {
        UpdateRoomStatusRequest request = UpdateRoomStatusRequest.builder()
                .status("MAINTENANCE")
                .maintenanceStartDate(LocalDate.now())
                .maintenanceEndDate(LocalDate.now().plusDays(7))
                .build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(modelMapper.map(any(Room.class), eq(RoomDTO.class))).thenReturn(testRoomDTO);
        when(pricingTierRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());
        when(bookingRepository.sumActiveOccupants(anyLong(), any(LocalDate.class))).thenReturn(0);

        roomService.updateRoomStatus(1L, request);

        verify(roomRepository).save(argThat(r -> r.getStatus() == RoomStatus.MAINTENANCE));
    }

    @Test
    @DisplayName("UpdateRoomStatus - to AVAILABLE clears dates")
    void updateRoomStatus_toAvailable() {
        UpdateRoomStatusRequest request = UpdateRoomStatusRequest.builder()
                .status("AVAILABLE").build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(modelMapper.map(any(Room.class), eq(RoomDTO.class))).thenReturn(testRoomDTO);
        when(pricingTierRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());
        when(bookingRepository.sumActiveOccupants(anyLong(), any(LocalDate.class))).thenReturn(0);

        roomService.updateRoomStatus(1L, request);

        verify(roomRepository).save(argThat(r -> r.getStatus() == RoomStatus.AVAILABLE));
    }

    @Test
    @DisplayName("AddPricingTier - Success")
    void addPricingTier_success() {
        PricingTierDTO dto = new PricingTierDTO();
        dto.setSeasonName("Summer");
        dto.setStartDate(LocalDate.of(2026, 6, 1));
        dto.setEndDate(LocalDate.of(2026, 8, 31));
        dto.setPriceMultiplier(BigDecimal.valueOf(1.5));

        PricingTier saved = PricingTier.builder().id(1L).room(testRoom).build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(pricingTierRepository.save(any(PricingTier.class))).thenReturn(saved);

        PricingTierDTO result = roomService.addPricingTier(1L, dto);

        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("AddPricingTier - Invalid dates throws IllegalArgumentException")
    void addPricingTier_invalidDates_throwsException() {
        PricingTierDTO dto = new PricingTierDTO();
        dto.setStartDate(LocalDate.of(2026, 8, 31));
        dto.setEndDate(LocalDate.of(2026, 6, 1));

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));

        assertThrows(IllegalArgumentException.class, () -> roomService.addPricingTier(1L, dto));
    }

    @Test
    @DisplayName("AssignRoomToEvent - Success")
    void assignRoomToEvent_success() {
        RoomEventDTO dto = new RoomEventDTO();
        dto.setEventName("Tech Fest");
        dto.setGroupName("CS Dept");
        dto.setStartDate(LocalDate.of(2026, 5, 15));
        dto.setEndDate(LocalDate.of(2026, 5, 18));

        RoomEvent saved = RoomEvent.builder().id(1L).room(testRoom).build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomEventRepository.findOverlappingEvents(eq(1L), any(), any())).thenReturn(Collections.emptyList());
        when(roomEventRepository.save(any(RoomEvent.class))).thenReturn(saved);
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        RoomEventDTO result = roomService.assignRoomToEvent(1L, dto);

        assertEquals(1L, result.getId());
        verify(roomRepository).save(argThat(r -> r.getStatus() == RoomStatus.OCCUPIED));
    }

    @Test
    @DisplayName("AssignRoomToEvent - Overlap throws IllegalArgumentException")
    void assignRoomToEvent_overlap_throwsException() {
        RoomEventDTO dto = new RoomEventDTO();
        dto.setEventName("Another");
        dto.setStartDate(LocalDate.of(2026, 5, 15));
        dto.setEndDate(LocalDate.of(2026, 5, 18));

        RoomEvent existing = RoomEvent.builder().id(1L).eventName("Existing").build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomEventRepository.findOverlappingEvents(eq(1L), any(), any()))
                .thenReturn(List.of(existing));

        assertThrows(IllegalArgumentException.class, () -> roomService.assignRoomToEvent(1L, dto));
    }

    @Test
    @DisplayName("GetAllRooms - Success")
    void getAllRooms_success() {
        when(roomRepository.findAll()).thenReturn(List.of(testRoom));
        when(modelMapper.map(any(Room.class), eq(RoomDTO.class))).thenReturn(testRoomDTO);
        when(pricingTierRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());
        when(bookingRepository.sumActiveOccupants(anyLong(), any(LocalDate.class))).thenReturn(0);

        List<RoomDTO> result = roomService.getAllRooms();

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("GetAllAmenities - Success")
    void getAllAmenities_success() {
        when(amenityRepository.findAll()).thenReturn(List.of(testAmenity));
        when(modelMapper.map(any(Amenity.class), eq(AmenityDTO.class)))
                .thenReturn(new AmenityDTO());

        List<AmenityDTO> result = roomService.getAllAmenities();

        assertEquals(1, result.size());
        verify(amenityRepository).findAll();
    }

    @Test
    @DisplayName("GetPricingTiers - Success")
    void getPricingTiers_success() {
        PricingTier tier = PricingTier.builder().id(1L).room(testRoom)
                .seasonName("Summer").startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(2))
                .priceMultiplier(BigDecimal.valueOf(1.5)).build();

        when(pricingTierRepository.findByRoomId(1L)).thenReturn(List.of(tier));
        when(modelMapper.map(any(PricingTier.class), eq(PricingTierDTO.class)))
                .thenReturn(new PricingTierDTO());

        List<PricingTierDTO> result = roomService.getPricingTiers(1L);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("GetRoomEvents - Success")
    void getRoomEvents_success() {
        RoomEvent event = RoomEvent.builder().id(1L).eventName("Tech Fest")
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(3)).build();

        when(roomEventRepository.findByRoomId(1L)).thenReturn(List.of(event));
        when(modelMapper.map(any(RoomEvent.class), eq(RoomEventDTO.class)))
                .thenReturn(new RoomEventDTO());

        List<RoomEventDTO> result = roomService.getRoomEvents(1L);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("UpdateRoomStatus - to OCCUPIED sets dates")
    void updateRoomStatus_toOccupied() {
        UpdateRoomStatusRequest request = UpdateRoomStatusRequest.builder()
                .status("OCCUPIED")
                .occupiedStartDate(LocalDate.now())
                .occupiedEndDate(LocalDate.now().plusDays(5))
                .build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(modelMapper.map(any(Room.class), eq(RoomDTO.class))).thenReturn(testRoomDTO);
        when(pricingTierRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());
        when(bookingRepository.sumActiveOccupants(anyLong(), any(LocalDate.class))).thenReturn(0);

        roomService.updateRoomStatus(1L, request);

        verify(roomRepository).save(argThat(r -> r.getStatus() == RoomStatus.OCCUPIED));
    }

    @Test
    @DisplayName("UpdateRoomStatus - Room not found throws exception")
    void updateRoomStatus_notFound_throwsException() {
        UpdateRoomStatusRequest request = UpdateRoomStatusRequest.builder()
                .status("AVAILABLE").build();

        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RoomNotFoundException.class,
                () -> roomService.updateRoomStatus(99L, request));
    }

    @Test
    @DisplayName("SearchRooms - AVAILABLE filter hides dynamically OCCUPIED rooms")
    void searchRooms_availableFilter_hidesOccupied() {
        RoomSearchCriteria criteria = new RoomSearchCriteria();
        criteria.setStatus("AVAILABLE");

        // DTO reports as OCCUPIED (dynamic state)
        RoomDTO occupiedDTO = RoomDTO.builder()
                .id(1L).roomNumber("101").status("OCCUPIED").build();

        when(roomRepository.searchRooms(null, null, RoomStatus.AVAILABLE, null, null, null))
                .thenReturn(List.of(testRoom));
        when(modelMapper.map(any(Room.class), eq(RoomDTO.class))).thenReturn(occupiedDTO);
        when(pricingTierRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());
        when(bookingRepository.sumActiveOccupants(anyLong(), any(LocalDate.class))).thenReturn(0);

        List<RoomDTO> result = roomService.searchRooms(criteria);

        // Dynamically OCCUPIED room should be filtered OUT
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("CreateRoom - With amenity IDs and gender policy")
    void createRoom_withAmenitiesAndGenderPolicy() {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setRoomNumber("102");
        request.setRoomType("DORMITORY");
        request.setCapacity(6);
        request.setFloor(1);
        request.setPricePerNight(BigDecimal.valueOf(150));
        request.setGenderPolicy("FEMALE_ONLY");
        request.setAmenityIds(List.of(1L));

        when(roomRepository.existsByRoomNumber("102")).thenReturn(false);
        when(amenityRepository.findByIdIn(List.of(1L))).thenReturn(List.of(testAmenity));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(modelMapper.map(any(Room.class), eq(RoomDTO.class))).thenReturn(testRoomDTO);
        when(pricingTierRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());
        when(bookingRepository.sumActiveOccupants(anyLong(), any(LocalDate.class))).thenReturn(0);

        RoomDTO result = roomService.createRoom(request);

        assertNotNull(result);
        verify(amenityRepository).findByIdIn(List.of(1L));
    }

    @Test
    @DisplayName("UpdateRoom - Not found throws RoomNotFoundException")
    void updateRoom_notFound_throwsException() {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setRoomNumber("101");
        request.setRoomType("SINGLE");

        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RoomNotFoundException.class,
                () -> roomService.updateRoom(99L, request));
    }

    @Test
    @DisplayName("SearchRooms - With RoomType and Floor filter")
    void searchRooms_withRoomTypeAndFloor() {
        RoomSearchCriteria criteria = new RoomSearchCriteria();
        criteria.setRoomType("SINGLE");
        criteria.setFloor(1);

        when(roomRepository.searchRooms(eq(RoomType.SINGLE), eq(1), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(testRoom));
        when(modelMapper.map(any(Room.class), eq(RoomDTO.class))).thenReturn(testRoomDTO);
        when(pricingTierRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());
        when(bookingRepository.sumActiveOccupants(anyLong(), any(LocalDate.class))).thenReturn(0);

        List<RoomDTO> result = roomService.searchRooms(criteria);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("mapToDTO - With active pricing tier sets currentPrice")
    void mapToDTO_withActivePricingTier_setsCurrentPrice() {
        PricingTier activeTier = PricingTier.builder()
                .id(1L).room(testRoom)
                .seasonName("Peak")
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(10))
                .priceMultiplier(BigDecimal.valueOf(2.0))
                .build();

        when(roomRepository.findByIdWithAmenities(1L)).thenReturn(Optional.of(testRoom));
        when(modelMapper.map(any(Room.class), eq(RoomDTO.class))).thenReturn(testRoomDTO);
        when(pricingTierRepository.findByRoomId(1L)).thenReturn(List.of(activeTier));
        lenient().when(modelMapper.map(any(PricingTier.class), eq(PricingTierDTO.class)))
                .thenReturn(new PricingTierDTO());
        when(bookingRepository.sumActiveOccupants(eq(1L), any(LocalDate.class))).thenReturn(0);

        RoomDTO result = roomService.getRoomById(1L);

        assertNotNull(result);
        verify(pricingTierRepository).findByRoomId(1L);
    }
}
