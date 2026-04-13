package com.hostel.user.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferenceDTO {

    private Long id;

    private String preferredRoomType;

    private Integer preferredFloor;

    private List<String> preferredAmenities;

    @DecimalMin(value = "0.0", message = "Minimum price cannot be negative")
    private BigDecimal preferredMinPrice;

    @DecimalMin(value = "0.0", message = "Maximum price cannot be negative")
    private BigDecimal preferredMaxPrice;
}
