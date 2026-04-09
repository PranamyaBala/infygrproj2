package com.hostel.booking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBookingStatusRequest {

    @NotBlank(message = "Please provide a valid status")
    private String status;

    private String notes;
}
