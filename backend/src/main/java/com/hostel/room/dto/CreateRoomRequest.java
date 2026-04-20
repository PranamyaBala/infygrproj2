package com.hostel.room.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRoomRequest {

    @NotBlank(message = "Please provide a valid room number")
    private String roomNumber;

    @NotBlank(message = "Please provide a valid room type")
    private String roomType;

    @NotNull(message = "Please provide a valid floor")
    @Min(value = 1, message = "Floor must be at least 1")
    @Max(value = 4, message = "Maximum floor allowed is 4")
    private Integer floor;

    @NotNull(message = "Please provide a valid capacity")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 10, message = "Capacity cannot exceed 10")
    private Integer capacity;

    @NotNull(message = "Please provide a valid price per night")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal pricePerNight;

    private String description;
    private String imagePath;
    private String floorPlanPath;
    @NotNull(message = "Please select at least 3 amenities")
    @Size(min = 3, message = "At least 3 amenities must be selected")
    private List<Long> amenityIds;
}
