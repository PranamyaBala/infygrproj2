package com.hostel.booking.controller;

import com.hostel.booking.dto.BookingDTO;
import com.hostel.booking.dto.CreateBookingRequest;
import com.hostel.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Student Bookings", description = "APIs for students to manage their room bookings")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @Operation(summary = "Submit a room booking request (US 03)")
    public ResponseEntity<BookingDTO> createBooking(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingDTO booking = bookingService.createBooking(userId, request);
        return new ResponseEntity<>(booking, HttpStatus.CREATED);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my bookings")
    public ResponseEntity<List<BookingDTO>> getMyBookings(
            @RequestHeader("X-User-Id") Long userId) {
        List<BookingDTO> bookings = bookingService.getBookingsByUserId(userId);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID")
    public ResponseEntity<BookingDTO> getBookingById(@PathVariable Long id) {
        BookingDTO booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(booking);
    }
}
