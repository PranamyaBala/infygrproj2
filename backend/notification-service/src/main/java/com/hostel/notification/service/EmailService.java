package com.hostel.notification.service;

import com.hostel.notification.dto.BookingNotificationDTO;
import com.hostel.notification.entity.NotificationLog;
import com.hostel.notification.entity.NotificationStatus;
import com.hostel.notification.entity.NotificationType;
import com.hostel.notification.repository.NotificationLogRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final NotificationLogRepository notificationLogRepository;

    @Value("${spring.mail.username:noreply@hostel.com}")
    private String fromEmail;

    @Value("${app.notifications.enabled:false}")
    private boolean notificationsEnabled;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public void sendBookingSubmittedEmail(BookingNotificationDTO booking) {
        String subject = "Booking Request Submitted - " + booking.getBookingReference();
        sendBookingEmail(booking, subject, NotificationType.BOOKING_SUBMITTED, "booking-submitted");
    }

    public void sendBookingApprovedEmail(BookingNotificationDTO booking) {
        String subject = "Booking Approved! - " + booking.getBookingReference();
        sendBookingEmail(booking, subject, NotificationType.BOOKING_APPROVED, "booking-approved");
    }

    public void sendBookingRejectedEmail(BookingNotificationDTO booking) {
        String subject = "Booking Rejected - " + booking.getBookingReference();
        sendBookingEmail(booking, subject, NotificationType.BOOKING_REJECTED, "booking-rejected");
    }

    public void sendBookingConfirmedEmail(BookingNotificationDTO booking) {
        String subject = "Booking Confirmation - " + booking.getBookingReference();
        sendBookingEmail(booking, subject, NotificationType.BOOKING_CONFIRMED, "booking-confirmed");
    }

    private void sendBookingEmail(BookingNotificationDTO booking, String subject,
                                   NotificationType type, String templateName) {
        NotificationLog logEntry = NotificationLog.builder()
                .recipientEmail(booking.getStudentEmail())
                .subject(subject)
                .notificationType(type)
                .status(NotificationStatus.PENDING)
                .build();

        try {
            if (!notificationsEnabled) {
                // Development mode: log instead of sending
                log.info("📧 [DEV MODE] Email would be sent to: {}", booking.getStudentEmail());
                log.info("   Subject: {}", subject);
                log.info("   Booking: {} | Room: {} | Dates: {} to {}",
                        booking.getBookingReference(), booking.getRoomNumber(),
                        booking.getStartDate().format(DATE_FORMAT),
                        booking.getEndDate().format(DATE_FORMAT));
                log.info("   Total Price: ₹{}", booking.getTotalPrice());
                logEntry.setStatus(NotificationStatus.SENT);
                notificationLogRepository.save(logEntry);
                return;
            }

            // Production mode: send actual email
            Context context = new Context();
            context.setVariable("studentName", booking.getStudentName());
            context.setVariable("bookingReference", booking.getBookingReference());
            context.setVariable("roomNumber", booking.getRoomNumber());
            context.setVariable("checkIn", booking.getStartDate().format(DATE_FORMAT));
            context.setVariable("checkOut", booking.getEndDate().format(DATE_FORMAT));
            context.setVariable("occupants", booking.getOccupants());
            context.setVariable("totalPrice", booking.getTotalPrice());
            context.setVariable("status", booking.getStatus());

            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(booking.getStudentEmail());
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {} for booking: {}",
                    booking.getStudentEmail(), booking.getBookingReference());

            logEntry.setStatus(NotificationStatus.SENT);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", booking.getStudentEmail(), e.getMessage());
            logEntry.setStatus(NotificationStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending email: {}", e.getMessage());
            logEntry.setStatus(NotificationStatus.FAILED);
            logEntry.setErrorMessage(e.getMessage());
        }

        notificationLogRepository.save(logEntry);
    }
}
