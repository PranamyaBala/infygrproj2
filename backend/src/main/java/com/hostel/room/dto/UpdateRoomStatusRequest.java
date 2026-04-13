package com.hostel.room.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRoomStatusRequest {

    @NotBlank(message = "Please provide a valid status")
    private String status;

    private LocalDate maintenanceStartDate;
    private LocalDate maintenanceEndDate;
    private LocalDate occupiedStartDate;
    private LocalDate occupiedEndDate;
}
