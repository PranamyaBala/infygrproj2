package com.hostel.room.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmenityDTO {

    private Long id;
    private String name;
    private String icon;
    private String description;
    private java.math.BigDecimal price;
}
