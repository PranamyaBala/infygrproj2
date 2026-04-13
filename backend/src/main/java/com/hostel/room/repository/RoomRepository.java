package com.hostel.room.repository;

import com.hostel.room.entity.Room;
import com.hostel.room.entity.RoomStatus;
import com.hostel.room.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByRoomNumber(String roomNumber);

    boolean existsByRoomNumber(String roomNumber);

    List<Room> findByStatus(RoomStatus status);

    List<Room> findByRoomType(RoomType roomType);

    List<Room> findByFloor(Integer floor);

    @Query("SELECT DISTINCT r FROM Room r LEFT JOIN FETCH r.amenities " +
           "WHERE (:roomType IS NULL OR r.roomType = :roomType) " +
           "AND (:floor IS NULL OR r.floor = :floor) " +
           "AND (:status IS NULL OR r.status = :status) " +
           "AND (:minPrice IS NULL OR r.pricePerNight >= :minPrice) " +
           "AND (:maxPrice IS NULL OR r.pricePerNight <= :maxPrice)")
    List<Room> searchRooms(
            @Param("roomType") RoomType roomType,
            @Param("floor") Integer floor,
            @Param("status") RoomStatus status,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice);

    @Query("SELECT r FROM Room r LEFT JOIN FETCH r.amenities WHERE r.id = :id")
    Optional<Room> findByIdWithAmenities(@Param("id") Long id);

    @Query("SELECT r FROM Room r LEFT JOIN FETCH r.amenities LEFT JOIN FETCH r.pricingTiers WHERE r.id = :id")
    Optional<Room> findByIdWithDetails(@Param("id") Long id);
}
