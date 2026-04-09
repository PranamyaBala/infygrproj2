package com.hostel.booking.client;

import com.hostel.booking.dto.BookingDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service", url = "${services.notification-service.url:http://localhost:8087}",
             fallback = NotificationServiceClientFallback.class)
public interface NotificationServiceClient {

    @PostMapping("/api/notifications/booking-submitted")
    void notifyBookingSubmitted(@RequestBody BookingDTO booking);

    @PostMapping("/api/notifications/booking-approved")
    void notifyBookingApproved(@RequestBody BookingDTO booking);

    @PostMapping("/api/notifications/booking-rejected")
    void notifyBookingRejected(@RequestBody BookingDTO booking);

    @PostMapping("/api/notifications/booking-confirmed")
    void notifyBookingConfirmed(@RequestBody BookingDTO booking);
}
