package com.hostel.room.service;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock private RoomRepository roomRepository;
    @Mock private AmenityRepository amenityRepository;
    @Mock private PricingTierRepository pricingTierRepository;
    @Mock private RoomEventRepository roomEventRepository;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private RoomService roomService;

    private Room testRoom;
    private RoomDTO testRoomDTO;
    private Amenity wifiAmenity;

    @BeforeEach
    void setUp() {
        wifiAmenity = Amenity.builder().id(1L).name("WiFi").icon("wifi").build();

        testRoom = Room.builder()
                .id(1L).roomNumber("101").roomType(RoomType.SINGLE)
                .floor(1).capacity(1).pricePerNight(new BigDecimal("50.00"))
                .status(RoomStatus.AVAILABLE).description("Test room")
                .amenities(new HashSet<>(Set.of(wifiAmenity)))
                .pricingTiers(new HashSet<>()).events(new HashSet<>())
                .build();

        AmenityDTO wifiDTO = AmenityDTO.builder().id(1L).name("WiFi").icon("wifi").build();
        testRoomDTO = RoomDTO.builder()
                .id(1L).roomNumber("101").roomType("SINGLE")
                .floor(1).capacity(1).pricePerNight(new BigDecimal("50.00"))
                .status("AVAILABLE").amenities(List.of(wifiDTO))
                .build();
    }

    @Test
    @DisplayName("Should search rooms with criteria")
    void searchRooms_Success() {
        RoomSearchCriteria criteria = RoomSearchCriteria.builder()
                .roomType("SINGLE").floor(1).status("AVAILABLE").build();

        when(roomRepository.searchRooms(RoomType.SINGLE, 1, RoomStatus.AVAILABLE, null, null))
                .thenReturn(List.of(testRoom));
        when(modelMapper.map(testRoom, RoomDTO.class)).thenReturn(testRoomDTO);
        when(modelMapper.map(wifiAmenity, AmenityDTO.class))
                .thenReturn(AmenityDTO.builder().id(1L).name("WiFi").build());

        List<RoomDTO> results = roomService.searchRooms(criteria);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("101", results.get(0).getRoomNumber());
    }

    @Test
    @DisplayName("Should filter rooms by amenities")
    void searchRooms_FilterByAmenities() {
        RoomSearchCriteria criteria = RoomSearchCriteria.builder()
                .amenities(List.of("WiFi")).build();

        when(roomRepository.searchRooms(null, null, null, null, null))
                .thenReturn(List.of(testRoom));
        when(modelMapper.map(testRoom, RoomDTO.class)).thenReturn(testRoomDTO);
        when(modelMapper.map(wifiAmenity, AmenityDTO.class))
                .thenReturn(AmenityDTO.builder().id(1L).name("WiFi").build());

        List<RoomDTO> results = roomService.searchRooms(criteria);

        assertEquals(1, results.size());
    }

    @Test
    @DisplayName("Should get room by ID")
    void getRoomById_Success() {
        when(roomRepository.findByIdWithAmenities(1L)).thenReturn(Optional.of(testRoom));
        when(modelMapper.map(testRoom, RoomDTO.class)).thenReturn(testRoomDTO);
        when(modelMapper.map(wifiAmenity, AmenityDTO.class))
                .thenReturn(AmenityDTO.builder().id(1L).name("WiFi").build());

        RoomDTO result = roomService.getRoomById(1L);

        assertNotNull(result);
        assertEquals("101", result.getRoomNumber());
    }

    @Test
    @DisplayName("Should throw RoomNotFoundException for invalid ID")
    void getRoomById_NotFound() {
        when(roomRepository.findByIdWithAmenities(999L)).thenReturn(Optional.empty());
        assertThrows(RoomNotFoundException.class, () -> roomService.getRoomById(999L));
    }

    @Test
    @DisplayName("Should create a new room")
    void createRoom_Success() {
        CreateRoomRequest request = CreateRoomRequest.builder()
                .roomNumber("102").roomType("DOUBLE").floor(1)
                .capacity(2).pricePerNight(new BigDecimal("80.00"))
                .amenityIds(List.of(1L)).build();

        when(roomRepository.existsByRoomNumber("102")).thenReturn(false);
        when(amenityRepository.findByIdIn(List.of(1L))).thenReturn(List.of(wifiAmenity));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(modelMapper.map(any(Room.class), eq(RoomDTO.class))).thenReturn(testRoomDTO);
        when(modelMapper.map(wifiAmenity, AmenityDTO.class))
                .thenReturn(AmenityDTO.builder().id(1L).name("WiFi").build());

        RoomDTO result = roomService.createRoom(request);

        assertNotNull(result);
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    @DisplayName("Should throw exception for duplicate room number")
    void createRoom_Duplicate() {
        CreateRoomRequest request = CreateRoomRequest.builder().roomNumber("101").roomType("SINGLE")
                .floor(1).capacity(1).pricePerNight(new BigDecimal("50")).build();

        when(roomRepository.existsByRoomNumber("101")).thenReturn(true);
        assertThrows(RoomAlreadyExistsException.class, () -> roomService.createRoom(request));
    }

    @Test
    @DisplayName("Should update room status to MAINTENANCE")
    void updateRoomStatus_Maintenance() {
        UpdateRoomStatusRequest request = UpdateRoomStatusRequest.builder()
                .status("MAINTENANCE")
                .maintenanceStartDate(LocalDate.now())
                .maintenanceEndDate(LocalDate.now().plusDays(7))
                .build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);
        when(modelMapper.map(any(Room.class), eq(RoomDTO.class))).thenReturn(testRoomDTO);
        when(modelMapper.map(wifiAmenity, AmenityDTO.class))
                .thenReturn(AmenityDTO.builder().id(1L).name("WiFi").build());

        RoomDTO result = roomService.updateRoomStatus(1L, request);

        assertNotNull(result);
        verify(roomRepository).save(any(Room.class));
    }

    @Test
    @DisplayName("Should get all amenities")
    void getAllAmenities_Success() {
        AmenityDTO dto = AmenityDTO.builder().id(1L).name("WiFi").build();
        when(amenityRepository.findAll()).thenReturn(List.of(wifiAmenity));
        when(modelMapper.map(wifiAmenity, AmenityDTO.class)).thenReturn(dto);

        List<AmenityDTO> result = roomService.getAllAmenities();

        assertEquals(1, result.size());
        assertEquals("WiFi", result.get(0).getName());
    }

    @Test
    @DisplayName("Should add pricing tier to room")
    void addPricingTier_Success() {
        PricingTierDTO dto = PricingTierDTO.builder()
                .seasonName("Summer").startDate(LocalDate.of(2025, 6, 1))
                .endDate(LocalDate.of(2025, 8, 31))
                .priceMultiplier(new BigDecimal("1.5")).build();

        PricingTier saved = PricingTier.builder().id(1L).room(testRoom)
                .seasonName("Summer").priceMultiplier(new BigDecimal("1.5")).build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(pricingTierRepository.save(any(PricingTier.class))).thenReturn(saved);

        PricingTierDTO result = roomService.addPricingTier(1L, dto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("Should assign room to event")
    void assignRoomToEvent_Success() {
        RoomEventDTO dto = RoomEventDTO.builder()
                .eventName("Orientation Week").groupName("Freshmen")
                .startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2025, 9, 7)).build();

        RoomEvent saved = RoomEvent.builder().id(1L).room(testRoom)
                .eventName("Orientation Week").build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomEventRepository.findOverlappingEvents(eq(1L), any(), any()))
                .thenReturn(Collections.emptyList());
        when(roomEventRepository.save(any(RoomEvent.class))).thenReturn(saved);

        RoomEventDTO result = roomService.assignRoomToEvent(1L, dto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("Should reject overlapping event assignment")
    void assignRoomToEvent_Overlap() {
        RoomEventDTO dto = RoomEventDTO.builder()
                .eventName("New Event").startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2025, 9, 7)).build();

        RoomEvent existing = RoomEvent.builder().id(1L).eventName("Existing Event").build();

        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(roomEventRepository.findOverlappingEvents(eq(1L), any(), any()))
                .thenReturn(List.of(existing));

        assertThrows(IllegalArgumentException.class,
                () -> roomService.assignRoomToEvent(1L, dto));
    }

    @Test
    @DisplayName("Should get all rooms")
    void getAllRooms_Success() {
        when(roomRepository.findAll()).thenReturn(List.of(testRoom));
        when(modelMapper.map(testRoom, RoomDTO.class)).thenReturn(testRoomDTO);
        when(modelMapper.map(wifiAmenity, AmenityDTO.class))
                .thenReturn(AmenityDTO.builder().id(1L).name("WiFi").build());

        List<RoomDTO> result = roomService.getAllRooms();

        assertEquals(1, result.size());
    }
}
