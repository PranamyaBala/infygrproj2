package com.hostel.booking.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomInfoDTO {

    private Long id;
    private String roomNumber;
    private String roomType;
    private Integer floor;
    private Integer capacity;
    private BigDecimal pricePerNight;
    private String status;
    private List<AmenityInfo> amenities;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AmenityInfo {
        private Long id;
        private String name;
    }
}
