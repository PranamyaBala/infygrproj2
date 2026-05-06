package com.hostel.booking.controller;

import com.hostel.booking.dto.*;
import com.hostel.booking.service.BookingService;
import com.hostel.booking.service.ReceiptService;
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
import org.springframework.security.core.Authentication;
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
class BookingControllerTest {

    @Mock private BookingService bookingService;
    @Mock private ReceiptService receiptService;

    @InjectMocks
    private BookingController bookingController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private BookingDTO testBookingDTO;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bookingController).build();
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
    @DisplayName("POST /api/bookings - Create booking 201")
    void createBooking_201() throws Exception {
        CreateBookingRequest request = new CreateBookingRequest();
        request.setRoomId(1L);
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setOccupants(1);

        when(bookingService.createBooking(eq("student@hostel.com"), any(CreateBookingRequest.class)))
                .thenReturn(testBookingDTO);

        mockMvc.perform(post("/api/bookings")
                        .principal(mockAuth("student@hostel.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bookingReference").value("BK-2026-TEST01"));
    }

    @Test
    @DisplayName("GET /api/bookings/my - Get my bookings 200")
    void getMyBookings_200() throws Exception {
        when(bookingService.getBookingsByUserEmail("student@hostel.com"))
                .thenReturn(List.of(testBookingDTO));

        mockMvc.perform(get("/api/bookings/my")
                        .principal(mockAuth("student@hostel.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].bookingReference").value("BK-2026-TEST01"));
    }

    @Test
    @DisplayName("GET /api/bookings/{id} - Get booking by ID 200")
    void getBookingById_200() throws Exception {
        when(bookingService.getBookingById(1L)).thenReturn(testBookingDTO);

        mockMvc.perform(get("/api/bookings/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("GET /api/bookings/room/{id}/occupied-dates - Get occupied dates 200")
    void getOccupiedDates_200() throws Exception {
        OccupiedDateRangeDTO range = OccupiedDateRangeDTO.builder()
                .startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(3)).build();

        when(bookingService.getOccupiedDateRanges(1L)).thenReturn(List.of(range));

        mockMvc.perform(get("/api/bookings/room/1/occupied-dates"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("GET /api/bookings/room/{id}/available-beds - Returns count 200")
    void getAvailableBeds_200() throws Exception {
        when(bookingService.calculateRemainingCapacity(eq(1L), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(4);

        mockMvc.perform(get("/api/bookings/room/1/available-beds")
                        .param("startDate", LocalDate.now().plusDays(1).toString())
                        .param("endDate", LocalDate.now().plusDays(3).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(4));
    }

    @Test
    @DisplayName("GET /api/bookings/{id}/receipt - Download receipt 200")
    void downloadReceipt_200() throws Exception {
        byte[] pdf = "test-pdf".getBytes();
        when(bookingService.generateBookingReceipt(eq(1L), anyString())).thenReturn(pdf);

        mockMvc.perform(get("/api/bookings/1/receipt")
                        .principal(mockAuth("student@hostel.com")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(content().bytes(pdf));
    }

    @Test
    @DisplayName("PUT /api/bookings/{id}/late-checkout - Request late checkout 200")
    void requestLateCheckout_200() throws Exception {
        LateCheckoutRequest request = new LateCheckoutRequest();
        request.setLateCheckoutFee(BigDecimal.valueOf(50));
        when(bookingService.handleLateCheckout(eq(1L), any())).thenReturn(testBookingDTO);

        mockMvc.perform(put("/api/bookings/1/late-checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private Authentication mockAuth(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        return auth;
    }
}
