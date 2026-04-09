package com.hostel.booking.client;

import com.hostel.booking.dto.BookingDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationServiceClientFallback implements NotificationServiceClient {

    @Override
    public void notifyBookingSubmitted(BookingDTO booking) {
        log.warn("Notification Service unavailable. Could not send submission notification for booking: {}",
                booking.getBookingReference());
    }

    @Override
    public void notifyBookingApproved(BookingDTO booking) {
        log.warn("Notification Service unavailable. Could not send approval notification for booking: {}",
                booking.getBookingReference());
    }

    @Override
    public void notifyBookingRejected(BookingDTO booking) {
        log.warn("Notification Service unavailable. Could not send rejection notification for booking: {}",
                booking.getBookingReference());
    }

    @Override
    public void notifyBookingConfirmed(BookingDTO booking) {
        log.warn("Notification Service unavailable. Could not send confirmation notification for booking: {}",
                booking.getBookingReference());
    }
}
