package com.hostel.notification.controller;

import com.hostel.notification.dto.BookingNotificationDTO;
import com.hostel.notification.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Internal API for sending email notifications")
public class NotificationController {

    private final EmailService emailService;

    @PostMapping("/booking-submitted")
    @Operation(summary = "Send booking submitted notification (US 06)")
    public ResponseEntity<String> notifyBookingSubmitted(@RequestBody BookingNotificationDTO booking) {
        emailService.sendBookingSubmittedEmail(booking);
        return ResponseEntity.ok("Booking submission notification sent");
    }

    @PostMapping("/booking-approved")
    @Operation(summary = "Send booking approved notification (US 06)")
    public ResponseEntity<String> notifyBookingApproved(@RequestBody BookingNotificationDTO booking) {
        emailService.sendBookingApprovedEmail(booking);
        return ResponseEntity.ok("Booking approval notification sent");
    }

    @PostMapping("/booking-rejected")
    @Operation(summary = "Send booking rejected notification (US 06)")
    public ResponseEntity<String> notifyBookingRejected(@RequestBody BookingNotificationDTO booking) {
        emailService.sendBookingRejectedEmail(booking);
        return ResponseEntity.ok("Booking rejection notification sent");
    }

    @PostMapping("/booking-confirmed")
    @Operation(summary = "Send booking confirmation email (US 15)")
    public ResponseEntity<String> notifyBookingConfirmed(@RequestBody BookingNotificationDTO booking) {
        emailService.sendBookingConfirmedEmail(booking);
        return ResponseEntity.ok("Booking confirmation notification sent");
    }
}
