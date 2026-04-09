package com.hostel.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/user-service")
    public ResponseEntity<Map<String, Object>> userServiceFallback() {
        return buildFallbackResponse("User Service");
    }

    @GetMapping("/room-service")
    public ResponseEntity<Map<String, Object>> roomServiceFallback() {
        return buildFallbackResponse("Room Service");
    }

    @GetMapping("/booking-service")
    public ResponseEntity<Map<String, Object>> bookingServiceFallback() {
        return buildFallbackResponse("Booking Service");
    }

    private ResponseEntity<Map<String, Object>> buildFallbackResponse(String serviceName) {
        Map<String, Object> response = Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", HttpStatus.SERVICE_UNAVAILABLE.value(),
                "error", "Service Unavailable",
                "message", serviceName + " is temporarily unavailable. Please try again later.",
                "service", serviceName
        );
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
