package com.hostel.room.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "amenities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 50)
    private String icon;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private java.math.BigDecimal price = java.math.BigDecimal.ZERO;
}
