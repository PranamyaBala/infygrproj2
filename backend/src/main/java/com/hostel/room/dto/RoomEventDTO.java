package com.hostel.room.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomEventDTO {

    private Long id;

    @NotBlank(message = "Please provide a valid event name")
    private String eventName;

    private String groupName;

    @NotNull(message = "Please provide a valid start date")
    private LocalDate startDate;

    @NotNull(message = "Please provide a valid end date")
    private LocalDate endDate;
}
