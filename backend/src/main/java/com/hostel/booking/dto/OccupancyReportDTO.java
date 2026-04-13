package com.hostel.booking.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OccupancyReportDTO {

    private Long roomId;
    private String roomNumber;
    private String roomType;
    private long totalDays;
    private long occupiedDays;
    private double occupancyRate;
    private BigDecimal revenue;
}
