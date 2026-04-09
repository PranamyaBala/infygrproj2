package com.hostel.booking.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LateCheckoutRequest {

    private BigDecimal lateCheckoutFee;
    private String notes;
}
