package com.hostel.notification.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookingNotificationDTO {

    private Long id;
    private Long userId;
    private Long roomId;
    private String roomNumber;
    private String studentName;
    private String studentEmail;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer occupants;
    private String status;
    private BigDecimal totalPrice;
    private String bookingReference;
    private Boolean lateCheckoutRequested;
    private BigDecimal lateCheckoutFee;
    private String notes;
    private LocalDateTime createdAt;
}
