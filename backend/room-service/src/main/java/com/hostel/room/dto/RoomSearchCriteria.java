package com.hostel.room.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomSearchCriteria {

    private String roomType;
    private Integer floor;
    private List<String> amenities;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String status;
    private Integer page;
    private Integer size;
}
