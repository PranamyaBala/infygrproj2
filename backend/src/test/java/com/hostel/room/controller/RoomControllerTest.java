package com.hostel.room.controller;

import com.hostel.room.dto.*;
import com.hostel.room.service.RoomService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RoomControllerTest {

    @Mock private RoomService roomService;

    @InjectMocks
    private RoomController roomController;

    private MockMvc mockMvc;
    private RoomDTO testRoomDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roomController).build();

        testRoomDTO = RoomDTO.builder()
                .id(1L).roomNumber("101").roomType("SINGLE")
                .floor(1).capacity(1)
                .pricePerNight(BigDecimal.valueOf(200))
                .status("AVAILABLE")
                .build();
    }

    @Test
    @DisplayName("GET /api/rooms/search - Returns rooms list 200")
    void searchRooms_200() throws Exception {
        when(roomService.searchRooms(any(RoomSearchCriteria.class))).thenReturn(List.of(testRoomDTO));

        mockMvc.perform(get("/api/rooms/search").param("status", "AVAILABLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomNumber").value("101"));
    }

    @Test
    @DisplayName("GET /api/rooms/{id} - Returns room details 200")
    void getRoomById_200() throws Exception {
        when(roomService.getRoomById(1L)).thenReturn(testRoomDTO);

        mockMvc.perform(get("/api/rooms/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomNumber").value("101"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    @DisplayName("GET /api/rooms/number/{roomNumber} - Returns room by number 200")
    void getRoomByNumber_200() throws Exception {
        when(roomService.getRoomByNumber("101")).thenReturn(testRoomDTO);

        mockMvc.perform(get("/api/rooms/number/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomNumber").value("101"));
    }

    @Test
    @DisplayName("GET /api/rooms/amenities - Returns amenities list 200")
    void getAllAmenities_200() throws Exception {
        AmenityDTO amenity = new AmenityDTO();
        amenity.setId(1L);
        amenity.setName("WiFi");
        amenity.setPrice(BigDecimal.valueOf(10));

        when(roomService.getAllAmenities()).thenReturn(List.of(amenity));

        mockMvc.perform(get("/api/rooms/amenities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("WiFi"));
    }

    @Test
    @DisplayName("GET /api/rooms - Returns all rooms 200")
    void getAllRooms_200() throws Exception {
        when(roomService.getAllRooms()).thenReturn(List.of(testRoomDTO));

        mockMvc.perform(get("/api/rooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
