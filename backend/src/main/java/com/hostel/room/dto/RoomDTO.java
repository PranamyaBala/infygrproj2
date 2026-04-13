package com.hostel.room.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDTO {

    private Long id;
    private String roomNumber;
    private String roomType;
    private Integer floor;
    private Integer capacity;
    private BigDecimal pricePerNight;
    private String status;
    private String description;
    private String imagePath;
    private String floorPlanPath;
    private List<AmenityDTO> amenities;
    private java.time.LocalDate maintenanceStartDate;
    private java.time.LocalDate maintenanceEndDate;
    private java.time.LocalDate occupiedStartDate;
    private java.time.LocalDate occupiedEndDate;
}
