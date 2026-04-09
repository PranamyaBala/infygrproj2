package com.hostel.booking.controller;

import com.hostel.booking.dto.*;
import com.hostel.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/bookings")
@RequiredArgsConstructor
@Tag(name = "Admin Booking Management", description = "Admin APIs for managing bookings, reports, and exports")
public class AdminBookingController {

    private final BookingService bookingService;

    @GetMapping
    @Operation(summary = "Get all booking requests (US 05)")
    public ResponseEntity<List<BookingDTO>> getAllBookings() {
        List<BookingDTO> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get bookings by status")
    public ResponseEntity<List<BookingDTO>> getBookingsByStatus(@PathVariable String status) {
        List<BookingDTO> bookings = bookingService.getBookingsByStatus(status);
        return ResponseEntity.ok(bookings);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Approve or reject a booking request (US 05)")
    public ResponseEntity<BookingDTO> updateBookingStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBookingStatusRequest request) {
        BookingDTO booking = bookingService.updateBookingStatus(id, request);
        return ResponseEntity.ok(booking);
    }

    @PutMapping("/{id}/late-checkout")
    @Operation(summary = "Handle late checkout request (US 14)")
    public ResponseEntity<BookingDTO> handleLateCheckout(
            @PathVariable Long id,
            @RequestBody LateCheckoutRequest request) {
        BookingDTO booking = bookingService.handleLateCheckout(id, request);
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/reports/occupancy")
    @Operation(summary = "Generate occupancy report for date range (US 07)")
    public ResponseEntity<List<OccupancyReportDTO>> getOccupancyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<OccupancyReportDTO> report = bookingService.getOccupancyReport(startDate, endDate);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Export all bookings to CSV file (US 11)")
    public ResponseEntity<byte[]> exportBookingsCsv() {
        byte[] csvData = bookingService.exportBookingsToCsv();
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=bookings_export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvData);
    }
}
