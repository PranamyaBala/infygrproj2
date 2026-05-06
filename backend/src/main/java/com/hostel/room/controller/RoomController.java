package com.hostel.room.controller;

import com.hostel.room.dto.*;
import com.hostel.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

import com.hostel.user.dto.UserDTO;
import com.hostel.user.service.UserService;
import java.security.Principal;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Room Search", description = "APIs for searching and viewing room details")
public class RoomController {

    private final RoomService roomService;
    private final UserService userService;

    @GetMapping("/search")
    @Operation(summary = "Search available rooms with filters (US 01)")
    public ResponseEntity<List<RoomDTO>> searchRooms(
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) Integer floor,
            @RequestParam(required = false) List<String> amenities,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "AVAILABLE") String status,
            Principal principal) {

        RoomSearchCriteria criteria = RoomSearchCriteria.builder()
                .roomType(roomType)
                .floor(floor)
                .amenities(amenities)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .status(status)
                .build();

        if (principal != null) {
            try {
                UserDTO user = userService.getProfile(principal.getName());
                if ("STUDENT".equals(user.getRole()) && user.getGender() != null) {
                    criteria.setGenderPolicy(user.getGender()); // Pass user's gender to criteria
                }
            } catch (Exception e) {
                // Ignore if profile fetch fails or not a user
            }
        }

        List<RoomDTO> rooms = roomService.searchRooms(criteria);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get room details by ID (US 02)")
    public ResponseEntity<RoomDTO> getRoomById(@PathVariable Long id) {
        RoomDTO room = roomService.getRoomById(id);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/number/{roomNumber}")
    @Operation(summary = "Get room details by room number")
    public ResponseEntity<RoomDTO> getRoomByNumber(@PathVariable String roomNumber) {
        RoomDTO room = roomService.getRoomByNumber(roomNumber);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/amenities")
    @Operation(summary = "Get all available amenities")
    public ResponseEntity<List<AmenityDTO>> getAllAmenities() {
        List<AmenityDTO> amenities = roomService.getAllAmenities();
        return ResponseEntity.ok(amenities);
    }

    @GetMapping
    @Operation(summary = "Get all rooms")
    public ResponseEntity<List<RoomDTO>> getAllRooms() {
        List<RoomDTO> rooms = roomService.getAllRooms();
        return ResponseEntity.ok(rooms);
    }
}
