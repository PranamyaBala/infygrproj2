package com.hostel.room.repository;

import com.hostel.room.entity.PricingTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PricingTierRepository extends JpaRepository<PricingTier, Long> {

    List<PricingTier> findByRoomId(Long roomId);

    @Query("SELECT p FROM PricingTier p WHERE p.room.id = :roomId " +
           "AND p.startDate <= :date AND p.endDate >= :date")
    List<PricingTier> findActiveByRoomIdAndDate(
            @Param("roomId") Long roomId,
            @Param("date") LocalDate date);
}
