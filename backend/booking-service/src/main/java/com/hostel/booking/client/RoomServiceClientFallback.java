package com.hostel.booking.client;

import com.hostel.booking.dto.RoomInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class RoomServiceClientFallback implements RoomServiceClient {

    @Override
    public RoomInfoDTO getRoomById(Long id) {
        log.warn("Circuit breaker OPEN: Room Service unavailable. Returning fallback for room ID: {}", id);
        return RoomInfoDTO.builder()
                .id(id)
                .roomNumber("Unavailable")
                .roomType("UNKNOWN")
                .capacity(0)
                .pricePerNight(BigDecimal.ZERO)
                .status("UNAVAILABLE")
                .build();
    }

    @Override
    public List<RoomInfoDTO> getAllRooms() {
        log.warn("Circuit breaker OPEN: Room Service unavailable. Returning empty room list.");
        return Collections.emptyList();
    }
}
