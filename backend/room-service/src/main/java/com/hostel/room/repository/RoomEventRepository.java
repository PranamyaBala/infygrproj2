package com.hostel.room.repository;

import com.hostel.room.entity.RoomEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomEventRepository extends JpaRepository<RoomEvent, Long> {

    List<RoomEvent> findByRoomId(Long roomId);

    @Query("SELECT e FROM RoomEvent e WHERE e.room.id = :roomId " +
           "AND e.startDate <= :endDate AND e.endDate >= :startDate")
    List<RoomEvent> findOverlappingEvents(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
