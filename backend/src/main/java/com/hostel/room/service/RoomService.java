package com.hostel.room.service;

import com.hostel.booking.repository.BookingRepository;
import com.hostel.room.dto.*;
import com.hostel.room.entity.*;
import com.hostel.room.exception.RoomAlreadyExistsException;
import com.hostel.room.exception.RoomNotFoundException;
import com.hostel.room.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final AmenityRepository amenityRepository;
    private final PricingTierRepository pricingTierRepository;
    private final RoomEventRepository roomEventRepository;
    private final BookingRepository bookingRepository;
    private final ModelMapper modelMapper;

    // ==================== ROOM SEARCH (US 01) ====================

    @Transactional(readOnly = true)
    public List<RoomDTO> searchRooms(RoomSearchCriteria criteria) {
        RoomType roomType = criteria.getRoomType() != null
                ? RoomType.valueOf(criteria.getRoomType().toUpperCase()) : null;
        RoomStatus status = criteria.getStatus() != null
                ? RoomStatus.valueOf(criteria.getStatus().toUpperCase()) : null;

        List<Room> rooms = roomRepository.searchRooms(
                roomType, criteria.getFloor(), status,
                criteria.getMinCapacity(),
                criteria.getMinPrice(), criteria.getMaxPrice());

        // Filter by amenities using streams
        if (criteria.getAmenities() != null && !criteria.getAmenities().isEmpty()) {
            rooms = rooms.stream()
                    .filter(room -> {
                        Set<String> roomAmenityNames = room.getAmenities().stream()
                                .map(Amenity::getName)
                                .collect(Collectors.toSet());
                        return roomAmenityNames.containsAll(criteria.getAmenities());
                    })
                    .collect(Collectors.toList());
        }

        log.info("Room search returned {} results for criteria: {}", rooms.size(), criteria);

        return rooms.stream()
                .map(this::mapToDTO)
                .filter(dto -> {
                    // If user searched for AVAILABLE rooms, hide rooms that are dynamically marked OCCUPIED (due to events)
                    if (status == RoomStatus.AVAILABLE && RoomStatus.OCCUPIED.name().equals(dto.getStatus())) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    // ==================== ROOM DETAIL (US 02) ====================

    @Transactional(readOnly = true)
    public RoomDTO getRoomById(Long id) {
        Room room = roomRepository.findByIdWithAmenities(id)
                .orElseThrow(() -> new RoomNotFoundException(id));
        return mapToDTO(room);
    }

    @Transactional(readOnly = true)
    public RoomDTO getRoomByNumber(String roomNumber) {
        Room room = roomRepository.findByRoomNumber(roomNumber)
                .orElseThrow(() -> new RoomNotFoundException("Room not found with number: " + roomNumber));
        return mapToDTO(room);
    }

    // ==================== ROOM CRUD (Admin) ====================

    @Transactional
    public RoomDTO createRoom(CreateRoomRequest request) {
        validateRoomNumberRange(request.getRoomNumber());
        if (roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new RoomAlreadyExistsException(request.getRoomNumber());
        }

        Room room = Room.builder()
                .roomNumber(request.getRoomNumber())
                .roomType(RoomType.valueOf(request.getRoomType().toUpperCase()))
                .floor(request.getFloor())
                .capacity(request.getCapacity())
                .pricePerNight(request.getPricePerNight())
                .status(RoomStatus.AVAILABLE)
                .description(request.getDescription())
                .imagePath(request.getImagePath())
                .floorPlanPath(request.getFloorPlanPath())
                .build();

        // Associate amenities using streams
        if (request.getAmenityIds() != null && !request.getAmenityIds().isEmpty()) {
            Set<Amenity> amenities = new HashSet<>(amenityRepository.findByIdIn(request.getAmenityIds()));
            room.setAmenities(amenities);
        }

        Room savedRoom = roomRepository.save(room);
        log.info("Room created: {}", savedRoom.getRoomNumber());

        return mapToDTO(savedRoom);
    }

    @Transactional
    public RoomDTO updateRoom(Long id, CreateRoomRequest request) {
        validateRoomNumberRange(request.getRoomNumber());
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException(id));

        room.setRoomNumber(request.getRoomNumber());
        room.setRoomType(RoomType.valueOf(request.getRoomType().toUpperCase()));
        room.setFloor(request.getFloor());
        room.setCapacity(request.getCapacity());
        room.setPricePerNight(request.getPricePerNight());
        room.setDescription(request.getDescription());
        room.setImagePath(request.getImagePath());
        room.setFloorPlanPath(request.getFloorPlanPath());

        if (request.getAmenityIds() != null) {
            Set<Amenity> amenities = new HashSet<>(amenityRepository.findByIdIn(request.getAmenityIds()));
            room.setAmenities(amenities);
        }

        Room updatedRoom = roomRepository.save(room);
        log.info("Room updated: {}", updatedRoom.getRoomNumber());

        return mapToDTO(updatedRoom);
    }

    // ==================== ROOM STATUS (US 04) ====================

    @Transactional
    public RoomDTO updateRoomStatus(Long id, UpdateRoomStatusRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException(id));

        RoomStatus newStatus = RoomStatus.valueOf(request.getStatus().toUpperCase());
        room.setStatus(newStatus);

        if (newStatus == RoomStatus.MAINTENANCE) {
            room.setMaintenanceStartDate(request.getMaintenanceStartDate());
            room.setMaintenanceEndDate(request.getMaintenanceEndDate());
            room.setOccupiedStartDate(null);
            room.setOccupiedEndDate(null);
        } else if (newStatus == RoomStatus.OCCUPIED) {
            room.setOccupiedStartDate(request.getOccupiedStartDate());
            room.setOccupiedEndDate(request.getOccupiedEndDate());
            room.setMaintenanceStartDate(null);
            room.setMaintenanceEndDate(null);
        } else {
            room.setMaintenanceStartDate(null);
            room.setMaintenanceEndDate(null);
            room.setOccupiedStartDate(null);
            room.setOccupiedEndDate(null);
        }

        Room updatedRoom = roomRepository.save(room);
        log.info("Room {} status changed to: {}", room.getRoomNumber(), newStatus);

        return mapToDTO(updatedRoom);
    }

    // ==================== ALL ROOMS (Admin) ====================

    @Transactional(readOnly = true)
    public List<RoomDTO> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoomDTO> getRoomsByStatus(String status) {
        RoomStatus roomStatus = RoomStatus.valueOf(status.toUpperCase());
        return roomRepository.findByStatus(roomStatus).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ==================== AMENITIES ====================

    @Transactional(readOnly = true)
    public List<AmenityDTO> getAllAmenities() {
        return amenityRepository.findAll().stream()
                .map(amenity -> modelMapper.map(amenity, AmenityDTO.class))
                .collect(Collectors.toList());
    }

    // ==================== PRICING TIERS (US 13) ====================

    @Transactional
    public PricingTierDTO addPricingTier(Long roomId, PricingTierDTO dto) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        PricingTier tier = PricingTier.builder()
                .room(room)
                .seasonName(dto.getSeasonName())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .priceMultiplier(dto.getPriceMultiplier())
                .build();

        PricingTier saved = pricingTierRepository.save(tier);
        log.info("Pricing tier '{}' added for room {}", dto.getSeasonName(), room.getRoomNumber());

        dto.setId(saved.getId());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<PricingTierDTO> getPricingTiers(Long roomId) {
        return pricingTierRepository.findByRoomId(roomId).stream()
                .map(tier -> modelMapper.map(tier, PricingTierDTO.class))
                .collect(Collectors.toList());
    }

    // ==================== ROOM EVENTS (US 09) ====================

    @Transactional
    public RoomEventDTO assignRoomToEvent(Long roomId, RoomEventDTO dto) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomNotFoundException(roomId));

        if (dto.getStartDate().isAfter(dto.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        // 1. Check for overlapping events
        List<RoomEvent> overlapping = roomEventRepository.findOverlappingEvents(
                roomId, dto.getStartDate(), dto.getEndDate());

        if (!overlapping.isEmpty()) {
            String conflictingEvents = overlapping.stream()
                    .map(RoomEvent::getEventName)
                    .collect(Collectors.joining(", "));
            throw new IllegalArgumentException(
                    "Room has overlapping events: " + conflictingEvents);
        }

        // 2. Build and Save the event
        RoomEvent event = RoomEvent.builder()
                .room(room)
                .eventName(dto.getEventName())
                .groupName(dto.getGroupName())
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .build();

        RoomEvent saved = roomEventRepository.save(event);
        
        // 3. Manual Control Implementation: Persist OCCUPIED status in DB
        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);
        
        log.info("Room {} assigned to event '{}' ({} - {})",
                room.getRoomNumber(), event.getEventName(), event.getStartDate(), event.getEndDate());

        dto.setId(saved.getId());
        return dto;
    }

    @Transactional(readOnly = true)
    public List<RoomEventDTO> getRoomEvents(Long roomId) {
        return roomEventRepository.findByRoomId(roomId).stream()
                .map(event -> modelMapper.map(event, RoomEventDTO.class))
                .collect(Collectors.toList());
    }

    // ==================== HELPER ====================

    private void validateRoomNumberRange(String roomNumber) {
        try {
            // Extract the last two digits (sequence)
            int seq = Integer.parseInt(roomNumber.substring(roomNumber.length() - 2));
            if (seq < 1 || seq > 18) {
                throw new IllegalArgumentException("Room number must end with a sequence between 01 and 18 (e.g., 101 - 118).");
            }
            
            // Basic check to ensure starting digit is roughly aligned with floor (optional but good practice)
            // if (roomNumber.length() >= 3) {
            //     int floorDigit = Character.getNumericValue(roomNumber.charAt(0));
            //     // Additional checks could go here if needed
            // }
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) throw (IllegalArgumentException) e;
            throw new IllegalArgumentException("Invalid room number format. Must end with a numerical sequence.");
        }
    }

    private RoomDTO mapToDTO(Room room) {
        RoomDTO dto = modelMapper.map(room, RoomDTO.class);
        if (room.getAmenities() != null) {
            List<AmenityDTO> amenityDTOs = room.getAmenities().stream()
                    .map(a -> modelMapper.map(a, AmenityDTO.class))
                    .collect(Collectors.toList());
            dto.setAmenities(amenityDTOs);
        }

        // Populate Pricing Tiers (US 13)
        List<PricingTier> tiers = pricingTierRepository.findByRoomId(room.getId());
        
        // Cumulative Pricing: Base Room Price + Sum(Amenity Prices)
        java.math.BigDecimal combinedBasePrice = room.getPricePerNight();
        if (room.getAmenities() != null) {
            for (Amenity a : room.getAmenities()) {
                combinedBasePrice = combinedBasePrice.add(a.getPrice() != null ? a.getPrice() : java.math.BigDecimal.ZERO);
            }
        }
        
        java.time.LocalDate today = java.time.LocalDate.now();
        final java.math.BigDecimal finalBasePrice = combinedBasePrice;

        if (tiers != null && !tiers.isEmpty()) {
            List<PricingTierDTO> tierDTOs = tiers.stream()
                    .map(t -> {
                        if (!today.isBefore(t.getStartDate()) && !today.isAfter(t.getEndDate())) {
                            dto.setCurrentPrice(finalBasePrice.multiply(t.getPriceMultiplier()));
                        }
                        return modelMapper.map(t, PricingTierDTO.class);
                    })
                    .collect(Collectors.toList());
            dto.setPricingTiers(tierDTOs);
        }

        dto.setBasePriceWithAmenities(finalBasePrice);

        if (dto.getCurrentPrice() == null) {
            dto.setCurrentPrice(finalBasePrice);
        }

        // Calculate available beds: capacity - active occupants
        int activeOccupants = bookingRepository.sumActiveOccupants(room.getId(), today);
        dto.setAvailableBeds(Math.max(0, room.getCapacity() - activeOccupants));

        return dto;
    }
}
