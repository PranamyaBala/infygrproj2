package com.hostel.notification.service;

import com.hostel.notification.dto.BookingNotificationDTO;
import com.hostel.notification.entity.NotificationLog;
import com.hostel.notification.entity.NotificationStatus;
import com.hostel.notification.repository.NotificationLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private TemplateEngine templateEngine;
    @Mock private NotificationLogRepository notificationLogRepository;

    @InjectMocks
    private EmailService emailService;

    private BookingNotificationDTO testBooking;

    @BeforeEach
    void setUp() {
        // Dev mode: notifications disabled (logs instead of sending)
        ReflectionTestUtils.setField(emailService, "notificationsEnabled", false);
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@hostel.com");

        testBooking = BookingNotificationDTO.builder()
                .id(1L).bookingReference("BK-2025-TEST1234")
                .studentName("John Doe").studentEmail("john@hostel.com")
                .roomNumber("101").startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2025, 12, 15))
                .occupants(1).totalPrice(new BigDecimal("5000"))
                .status("PENDING").build();
    }

    @Test
    @DisplayName("Should log booking submitted email in dev mode")
    void sendBookingSubmitted_DevMode() {
        when(notificationLogRepository.save(any(NotificationLog.class)))
                .thenAnswer(i -> i.getArgument(0));

        emailService.sendBookingSubmittedEmail(testBooking);

        verify(notificationLogRepository).save(any(NotificationLog.class));
        verify(mailSender, never()).send(any(jakarta.mail.internet.MimeMessage.class));
    }

    @Test
    @DisplayName("Should log booking approved email in dev mode")
    void sendBookingApproved_DevMode() {
        when(notificationLogRepository.save(any(NotificationLog.class)))
                .thenAnswer(i -> i.getArgument(0));

        emailService.sendBookingApprovedEmail(testBooking);

        verify(notificationLogRepository).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("Should log booking rejected email in dev mode")
    void sendBookingRejected_DevMode() {
        when(notificationLogRepository.save(any(NotificationLog.class)))
                .thenAnswer(i -> i.getArgument(0));

        emailService.sendBookingRejectedEmail(testBooking);

        verify(notificationLogRepository).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("Should log booking confirmed email in dev mode")
    void sendBookingConfirmed_DevMode() {
        when(notificationLogRepository.save(any(NotificationLog.class)))
                .thenAnswer(i -> i.getArgument(0));

        emailService.sendBookingConfirmedEmail(testBooking);

        verify(notificationLogRepository).save(any(NotificationLog.class));
    }
}
