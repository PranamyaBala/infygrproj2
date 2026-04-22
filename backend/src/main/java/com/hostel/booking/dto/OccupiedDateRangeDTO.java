package com.hostel.booking.dto;

import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OccupiedDateRangeDTO {
    private LocalDate startDate;
    private LocalDate endDate;
}
