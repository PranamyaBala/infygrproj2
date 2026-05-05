package com.hostel.room.controller;

import com.hostel.room.dto.*;
import com.hostel.room.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminRoomControllerTest {

    @Mock private RoomService roomService;

    @InjectMocks
    private AdminRoomController adminRoomController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private RoomDTO testRoomDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminRoomController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testRoomDTO = RoomDTO.builder()
                .id(1L).roomNumber("101").roomType("SINGLE")
                .floor(1).capacity(1)
                .pricePerNight(BigDecimal.valueOf(200))
                .status("AVAILABLE")
                .build();
    }

    @Test
    @DisplayName("POST /api/admin/rooms - Create room 201")
    void createRoom_201() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setRoomNumber("201");
        request.setRoomType("SINGLE");
        request.setFloor(2);
        request.setCapacity(1);
        request.setPricePerNight(BigDecimal.valueOf(200));
        request.setAmenityIds(List.of(1L, 2L, 3L));

        when(roomService.createRoom(any(CreateRoomRequest.class))).thenReturn(testRoomDTO);

        mockMvc.perform(post("/api/admin/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomNumber").value("101"));
    }

    @Test
    @DisplayName("PUT /api/admin/rooms/{id} - Update room 200")
    void updateRoom_200() throws Exception {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setRoomNumber("101");
        request.setRoomType("DOUBLE");
        request.setFloor(1);
        request.setCapacity(2);
        request.setPricePerNight(BigDecimal.valueOf(300));
        request.setAmenityIds(List.of(1L, 2L, 3L));

        when(roomService.updateRoom(eq(1L), any(CreateRoomRequest.class))).thenReturn(testRoomDTO);

        mockMvc.perform(put("/api/admin/rooms/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/admin/rooms/{id}/status - Update status 200")
    void updateRoomStatus_200() throws Exception {
        UpdateRoomStatusRequest request = UpdateRoomStatusRequest.builder()
                .status("MAINTENANCE").build();

        when(roomService.updateRoomStatus(eq(1L), any(UpdateRoomStatusRequest.class))).thenReturn(testRoomDTO);

        mockMvc.perform(put("/api/admin/rooms/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/admin/rooms/status/{status} - Get by status 200")
    void getRoomsByStatus_200() throws Exception {
        when(roomService.getRoomsByStatus("AVAILABLE")).thenReturn(List.of(testRoomDTO));

        mockMvc.perform(get("/api/admin/rooms/status/AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomNumber").value("101"));
    }

    @Test
    @DisplayName("POST /api/admin/rooms/{id}/pricing - Add pricing tier 201")
    void addPricingTier_201() throws Exception {
        PricingTierDTO dto = new PricingTierDTO();
        dto.setSeasonName("Summer");
        dto.setStartDate(LocalDate.of(2026, 6, 1));
        dto.setEndDate(LocalDate.of(2026, 8, 31));
        dto.setPriceMultiplier(BigDecimal.valueOf(1.5));

        PricingTierDTO saved = new PricingTierDTO();
        saved.setId(1L);
        saved.setSeasonName("Summer");

        when(roomService.addPricingTier(eq(1L), any(PricingTierDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/api/admin/rooms/1/pricing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seasonName").value("Summer"));
    }

    @Test
    @DisplayName("POST /api/admin/rooms/{id}/events - Assign event 201")
    void assignRoomToEvent_201() throws Exception {
        RoomEventDTO dto = new RoomEventDTO();
        dto.setEventName("Tech Fest");
        dto.setGroupName("CS Dept");
        dto.setStartDate(LocalDate.of(2026, 5, 15));
        dto.setEndDate(LocalDate.of(2026, 5, 18));

        RoomEventDTO saved = new RoomEventDTO();
        saved.setId(1L);
        saved.setEventName("Tech Fest");

        when(roomService.assignRoomToEvent(eq(1L), any(RoomEventDTO.class))).thenReturn(saved);

        mockMvc.perform(post("/api/admin/rooms/1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventName").value("Tech Fest"));
    }

    @Test
    @DisplayName("GET /api/admin/rooms/{id}/events - Get room events 200")
    void getRoomEvents_200() throws Exception {
        RoomEventDTO event = new RoomEventDTO();
        event.setId(1L);
        event.setEventName("Tech Fest");

        when(roomService.getRoomEvents(1L)).thenReturn(List.of(event));

        mockMvc.perform(get("/api/admin/rooms/1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventName").value("Tech Fest"));
    }
}
