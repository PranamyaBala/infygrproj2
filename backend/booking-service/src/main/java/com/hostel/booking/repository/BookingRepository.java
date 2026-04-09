package com.hostel.booking.repository;

import com.hostel.booking.entity.Booking;
import com.hostel.booking.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserId(Long userId);

    List<Booking> findByRoomId(Long roomId);

    List<Booking> findByStatus(BookingStatus status);

    Optional<Booking> findByBookingReference(String bookingReference);

    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId " +
           "AND b.status IN ('PENDING', 'APPROVED', 'CHECKED_IN') " +
           "AND b.startDate <= :endDate AND b.endDate >= :startDate")
    List<Booking> findOverlappingBookings(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT b FROM Booking b WHERE b.roomId = :roomId " +
           "AND b.status IN ('APPROVED', 'CHECKED_IN', 'CHECKED_OUT') " +
           "AND b.startDate <= :endDate AND b.endDate >= :startDate")
    List<Booking> findBookingsForRoomInDateRange(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT DISTINCT b.roomId FROM Booking b WHERE " +
           "b.status IN ('APPROVED', 'CHECKED_IN', 'CHECKED_OUT') " +
           "AND b.startDate <= :endDate AND b.endDate >= :startDate")
    List<Long> findDistinctRoomIdsWithBookingsInRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
