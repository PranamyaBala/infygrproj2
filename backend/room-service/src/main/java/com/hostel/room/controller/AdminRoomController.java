package com.hostel.room.controller;

import com.hostel.room.dto.*;
import com.hostel.room.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/rooms")
@RequiredArgsConstructor
@Tag(name = "Admin Room Management", description = "Admin APIs for managing rooms, pricing, and events")
public class AdminRoomController {

    private final RoomService roomService;

    @PostMapping
    @Operation(summary = "Create a new room")
    public ResponseEntity<RoomDTO> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        RoomDTO room = roomService.createRoom(request);
        return new ResponseEntity<>(room, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing room")
    public ResponseEntity<RoomDTO> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody CreateRoomRequest request) {
        RoomDTO room = roomService.updateRoom(id, request);
        return ResponseEntity.ok(room);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update room status (US 04)")
    public ResponseEntity<RoomDTO> updateRoomStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoomStatusRequest request) {
        RoomDTO room = roomService.updateRoomStatus(id, request);
        return ResponseEntity.ok(room);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get rooms by status")
    public ResponseEntity<List<RoomDTO>> getRoomsByStatus(@PathVariable String status) {
        List<RoomDTO> rooms = roomService.getRoomsByStatus(status);
        return ResponseEntity.ok(rooms);
    }

    // ==================== PRICING TIERS ====================

    @PostMapping("/{id}/pricing")
    @Operation(summary = "Add pricing tier to room (US 13)")
    public ResponseEntity<PricingTierDTO> addPricingTier(
            @PathVariable Long id,
            @Valid @RequestBody PricingTierDTO request) {
        PricingTierDTO tier = roomService.addPricingTier(id, request);
        return new ResponseEntity<>(tier, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/pricing")
    @Operation(summary = "Get pricing tiers for a room")
    public ResponseEntity<List<PricingTierDTO>> getPricingTiers(@PathVariable Long id) {
        List<PricingTierDTO> tiers = roomService.getPricingTiers(id);
        return ResponseEntity.ok(tiers);
    }

    // ==================== ROOM EVENTS ====================

    @PostMapping("/{id}/events")
    @Operation(summary = "Assign room to event/group (US 09)")
    public ResponseEntity<RoomEventDTO> assignRoomToEvent(
            @PathVariable Long id,
            @Valid @RequestBody RoomEventDTO request) {
        RoomEventDTO event = roomService.assignRoomToEvent(id, request);
        return new ResponseEntity<>(event, HttpStatus.CREATED);
    }

    @GetMapping("/{id}/events")
    @Operation(summary = "Get events for a room")
    public ResponseEntity<List<RoomEventDTO>> getRoomEvents(@PathVariable Long id) {
        List<RoomEventDTO> events = roomService.getRoomEvents(id);
        return ResponseEntity.ok(events);
    }
}
