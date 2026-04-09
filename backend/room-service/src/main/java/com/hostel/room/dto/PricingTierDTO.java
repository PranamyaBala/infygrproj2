package com.hostel.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PricingTierDTO {

    private Long id;

    @NotBlank(message = "Please provide a valid season name")
    private String seasonName;

    @NotNull(message = "Please provide a valid start date")
    private LocalDate startDate;

    @NotNull(message = "Please provide a valid end date")
    private LocalDate endDate;

    @NotNull(message = "Please provide a valid price multiplier")
    private BigDecimal priceMultiplier;
}
