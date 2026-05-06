package com.hostel.booking.service;

import com.hostel.booking.entity.Booking;
import com.hostel.room.dto.RoomDTO;
import com.hostel.user.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ReceiptServiceTest {

    @InjectMocks
    private ReceiptService receiptService;

    private Booking testBooking;
    private RoomDTO testRoom;
    private UserDTO testUser;

    @BeforeEach
    void setUp() {
        testBooking = Booking.builder()
                .id(1L)
                .bookingReference("BK-TEST-123")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(2))
                .occupants(1)
                .totalPrice(new BigDecimal("500.00"))
                .lateCheckoutFee(new BigDecimal("50.00"))
                .build();

        testRoom = RoomDTO.builder()
                .roomNumber("101")
                .roomType("SINGLE")
                .build();

        testUser = UserDTO.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();
    }

    @Test
    void generateReceipt_ShouldReturnPdfBytes() {
        // Act
        byte[] pdfBytes = receiptService.generateReceipt(testBooking, testRoom, testUser);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0, "PDF bytes should not be empty");
    }

    @Test
    void generateReceipt_WithoutLateCheckoutFee_ShouldReturnPdfBytes() {
        // Arrange
        testBooking.setLateCheckoutFee(null);

        // Act
        byte[] pdfBytes = receiptService.generateReceipt(testBooking, testRoom, testUser);

        // Assert
        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0, "PDF bytes should not be empty");
    }
}
