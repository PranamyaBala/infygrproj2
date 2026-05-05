package com.hostel.notification.service;

import com.hostel.notification.dto.BookingNotificationDTO;
import com.hostel.notification.entity.NotificationLog;
import com.hostel.notification.entity.NotificationStatus;
import com.hostel.notification.repository.NotificationLogRepository;
import jakarta.mail.internet.MimeMessage;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @Mock private TemplateEngine templateEngine;
    @Mock private NotificationLogRepository notificationLogRepository;

    @InjectMocks
    private EmailService emailService;

    private BookingNotificationDTO testNotification;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@hostel.com");
        ReflectionTestUtils.setField(emailService, "notificationsEnabled", false);

        testNotification = new BookingNotificationDTO();
        testNotification.setId(1L);
        testNotification.setStudentEmail("student@hostel.com");
        testNotification.setStudentName("John Doe");
        testNotification.setBookingReference("BK-2026-TEST01");
        testNotification.setRoomNumber("101");
        testNotification.setStartDate(LocalDate.now().plusDays(1));
        testNotification.setEndDate(LocalDate.now().plusDays(3));
        testNotification.setOccupants(1);
        testNotification.setStatus("PENDING");
        testNotification.setTotalPrice(BigDecimal.valueOf(420));
    }

    @Test
    @DisplayName("SendBookingSubmittedEmail - Dev mode logs instead of sending")
    void sendBookingSubmittedEmail_devMode() {
        emailService.sendBookingSubmittedEmail(testNotification);

        verify(notificationLogRepository).save(argThat(log ->
                log.getStatus() == NotificationStatus.SENT));
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("SendBookingApprovedEmail - Dev mode logs successfully")
    void sendBookingApprovedEmail_devMode() {
        emailService.sendBookingApprovedEmail(testNotification);

        verify(notificationLogRepository).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("SendBookingRejectedEmail - Dev mode logs successfully")
    void sendBookingRejectedEmail_devMode() {
        emailService.sendBookingRejectedEmail(testNotification);

        verify(notificationLogRepository).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("SendBookingConfirmedEmail - Dev mode logs successfully")
    void sendBookingConfirmedEmail_devMode() {
        emailService.sendBookingConfirmedEmail(testNotification);

        verify(notificationLogRepository).save(any(NotificationLog.class));
    }

    @Test
    @DisplayName("SendEmail - Production mode sends via JavaMailSender")
    void sendEmail_productionMode_success() throws Exception {
        ReflectionTestUtils.setField(emailService, "notificationsEnabled", true);

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(templateEngine.process(anyString(), any())).thenReturn("<html>Test</html>");

        emailService.sendBookingSubmittedEmail(testNotification);

        verify(mailSender).send(any(MimeMessage.class));
        verify(notificationLogRepository).save(argThat(log ->
                log.getStatus() == NotificationStatus.SENT));
    }

    @Test
    @DisplayName("SendEmail - Production mode failure logs as FAILED")
    void sendEmail_productionMode_failure() throws Exception {
        ReflectionTestUtils.setField(emailService, "notificationsEnabled", true);

        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(templateEngine.process(anyString(), any())).thenThrow(new RuntimeException("Template error"));

        emailService.sendBookingSubmittedEmail(testNotification);

        verify(notificationLogRepository).save(argThat(log ->
                log.getStatus() == NotificationStatus.FAILED));
    }
}
