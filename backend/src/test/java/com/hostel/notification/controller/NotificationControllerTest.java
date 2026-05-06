package com.hostel.notification.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.notification.dto.BookingNotificationDTO;
import com.hostel.notification.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = NotificationController.class, 
    excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.hostel.user.security.JwtAuthenticationFilter.class)
)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EmailService emailService;

    @Autowired
    private ObjectMapper objectMapper;

    private BookingNotificationDTO dto;

    @BeforeEach
    void setUp() {
        dto = new BookingNotificationDTO();
        dto.setBookingReference("BK-123");
        dto.setStudentEmail("test@example.com");
    }

    @Test
    void notifyBookingSubmitted_ShouldReturnOk() throws Exception {
        doNothing().when(emailService).sendBookingSubmittedEmail(any());
        
        mockMvc.perform(post("/api/notifications/booking-submitted")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void notifyBookingApproved_ShouldReturnOk() throws Exception {
        doNothing().when(emailService).sendBookingApprovedEmail(any());

        mockMvc.perform(post("/api/notifications/booking-approved")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void notifyBookingRejected_ShouldReturnOk() throws Exception {
        doNothing().when(emailService).sendBookingRejectedEmail(any());

        mockMvc.perform(post("/api/notifications/booking-rejected")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void notifyBookingConfirmed_ShouldReturnOk() throws Exception {
        doNothing().when(emailService).sendBookingConfirmedEmail(any());

        mockMvc.perform(post("/api/notifications/booking-confirmed")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }
}
