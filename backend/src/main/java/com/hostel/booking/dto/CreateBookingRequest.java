package com.hostel.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBookingRequest {

    @NotNull(message = "Please provide a valid room ID")
    private Long roomId;

    @NotNull(message = "Please provide a valid start date")
    private LocalDate startDate;

    @NotNull(message = "Please provide a valid end date")
    private LocalDate endDate;

    @NotNull(message = "Please provide a valid number of occupants")
    @Min(value = 1, message = "Number of occupants must be at least 1")
    private Integer occupants;

    private String notes;
}
