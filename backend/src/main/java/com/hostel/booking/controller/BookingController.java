package com.hostel.booking.controller;

import com.hostel.booking.dto.BookingDTO;
import com.hostel.booking.dto.CreateBookingRequest;
import com.hostel.booking.dto.OccupiedDateRangeDTO;
import com.hostel.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@Tag(name = "Student Bookings", description = "APIs for students to manage their room bookings")
public class BookingController {

    private final BookingService bookingService;
    private final com.hostel.booking.service.ReceiptService receiptService;

    @GetMapping("/{id}/receipt")
    @Operation(summary = "Download booking receipt PDF", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long id, Authentication authentication) {
        byte[] pdfBytes = bookingService.generateBookingReceipt(id, authentication.getName());
        
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=receipt-" + id + ".pdf")
                .body(pdfBytes);
    }


    @PostMapping
    @Operation(summary = "Submit a room booking request (US 03)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<BookingDTO> createBooking(
            Authentication authentication,
            @Valid @RequestBody CreateBookingRequest request) {
        BookingDTO booking = bookingService.createBooking(authentication.getName(), request);
        return new ResponseEntity<>(booking, HttpStatus.CREATED);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my bookings", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<List<BookingDTO>> getMyBookings(Authentication authentication) {
        List<BookingDTO> bookings = bookingService.getBookingsByUserEmail(authentication.getName());
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/room/{roomId}/occupied-dates")
    @Operation(summary = "Get occupied dates for a room")
    public ResponseEntity<List<OccupiedDateRangeDTO>> getOccupiedDates(@PathVariable Long roomId) {
        return ResponseEntity.ok(bookingService.getOccupiedDateRanges(roomId));
    }

    @GetMapping("/room/{roomId}/available-beds")
    public ResponseEntity<Integer> getAvailableBeds(
            @PathVariable Long roomId,
            @RequestParam String startDate,
            @RequestParam String endDate) {
        // Simple helper to bridge to service logic
        return ResponseEntity.ok(bookingService.calculateRemainingCapacity(roomId, 
            LocalDate.parse(startDate), LocalDate.parse(endDate)));
    }


    @GetMapping("/{id}")
    @Operation(summary = "Get booking by ID", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<BookingDTO> getBookingById(@PathVariable Long id) {
        BookingDTO booking = bookingService.getBookingById(id);
        return ResponseEntity.ok(booking);
    }

    @PutMapping("/{id}/late-checkout")
    @Operation(summary = "Request late checkout (US 14)", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<BookingDTO> requestLateCheckout(
            @PathVariable Long id,
            @RequestBody com.hostel.booking.dto.LateCheckoutRequest request) {
        BookingDTO booking = bookingService.handleLateCheckout(id, request);
        return ResponseEntity.ok(booking);
    }
}
