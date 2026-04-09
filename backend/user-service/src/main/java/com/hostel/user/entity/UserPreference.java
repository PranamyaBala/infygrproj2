package com.hostel.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "preferred_room_type", length = 50)
    private String preferredRoomType;

    @Column(name = "preferred_floor")
    private Integer preferredFloor;

    @Column(name = "preferred_amenities", columnDefinition = "JSON")
    private String preferredAmenities;

    @Column(name = "preferred_min_price", precision = 10, scale = 2)
    private BigDecimal preferredMinPrice;

    @Column(name = "preferred_max_price", precision = 10, scale = 2)
    private BigDecimal preferredMaxPrice;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
