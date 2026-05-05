package com.hostel.booking.controller;

import com.hostel.booking.dto.*;
import com.hostel.booking.service.BookingService;
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
class AdminBookingControllerTest {

    @Mock private BookingService bookingService;

    @InjectMocks
    private AdminBookingController adminBookingController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private BookingDTO testBookingDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminBookingController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testBookingDTO = BookingDTO.builder()
                .id(1L).userId(1L).roomId(1L)
                .roomNumber("101")
                .studentName("John Doe").studentEmail("student@hostel.com")
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .occupants(1).status("PENDING")
                .totalPrice(BigDecimal.valueOf(420))
                .bookingReference("BK-2026-TEST01")
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/bookings - Get all bookings 200")
    void getAllBookings_200() throws Exception {
        when(bookingService.getAllBookings()).thenReturn(List.of(testBookingDTO));

        mockMvc.perform(get("/api/admin/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingReference").value("BK-2026-TEST01"));
    }

    @Test
    @DisplayName("GET /api/admin/bookings/status/{status} - Get by status 200")
    void getBookingsByStatus_200() throws Exception {
        when(bookingService.getBookingsByStatus("PENDING")).thenReturn(List.of(testBookingDTO));

        mockMvc.perform(get("/api/admin/bookings/status/PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("PUT /api/admin/bookings/{id}/status - Approve booking 200")
    void updateBookingStatus_200() throws Exception {
        UpdateBookingStatusRequest request = new UpdateBookingStatusRequest();
        request.setStatus("APPROVED");
        request.setNotes("Room allocated");

        BookingDTO approvedBooking = BookingDTO.builder()
                .id(1L).status("APPROVED").bookingReference("BK-2026-TEST01").build();

        when(bookingService.updateBookingStatus(eq(1L), any(UpdateBookingStatusRequest.class)))
                .thenReturn(approvedBooking);

        mockMvc.perform(put("/api/admin/bookings/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("PUT /api/admin/bookings/{id}/late-checkout - Process late checkout 200")
    void handleLateCheckout_200() throws Exception {
        LateCheckoutRequest request = new LateCheckoutRequest();
        request.setLateCheckoutFee(BigDecimal.valueOf(50));

        when(bookingService.handleLateCheckout(eq(1L), any(LateCheckoutRequest.class)))
                .thenReturn(testBookingDTO);

        mockMvc.perform(put("/api/admin/bookings/1/late-checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/admin/bookings/reports/occupancy - Get report 200")
    void getOccupancyReport_200() throws Exception {
        OccupancyReportDTO report = OccupancyReportDTO.builder()
                .roomId(1L).roomNumber("101").roomType("SINGLE")
                .totalDays(30).occupiedDays(20).occupancyRate(66.7)
                .revenue(BigDecimal.valueOf(4200))
                .build();

        when(bookingService.getOccupancyReport(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of(report));

        mockMvc.perform(get("/api/admin/bookings/reports/occupancy")
                        .param("startDate", "2026-04-01")
                        .param("endDate", "2026-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roomNumber").value("101"))
                .andExpect(jsonPath("$[0].occupancyRate").value(66.7));
    }

    @Test
    @DisplayName("GET /api/admin/bookings/export/csv - Export CSV 200")
    void exportBookingsCsv_200() throws Exception {
        byte[] csvData = "Booking Reference,Student Name\nBK-2026-TEST01,John Doe".getBytes();
        when(bookingService.exportBookingsToCsv()).thenReturn(csvData);

        mockMvc.perform(get("/api/admin/bookings/export/csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=bookings_export.csv"));
    }
}
