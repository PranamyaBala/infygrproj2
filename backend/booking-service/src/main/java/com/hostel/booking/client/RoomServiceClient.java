package com.hostel.booking.client;

import com.hostel.booking.dto.RoomInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "room-service", url = "${services.room-service.url:http://localhost:8083}",
             fallback = RoomServiceClientFallback.class)
public interface RoomServiceClient {

    @GetMapping("/api/rooms/{id}")
    RoomInfoDTO getRoomById(@PathVariable("id") Long id);

    @GetMapping("/api/rooms")
    List<RoomInfoDTO> getAllRooms();
}
