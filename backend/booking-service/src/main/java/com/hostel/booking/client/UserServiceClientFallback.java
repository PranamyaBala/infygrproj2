package com.hostel.booking.client;

import com.hostel.booking.dto.UserInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserServiceClientFallback implements UserServiceClient {

    @Override
    public UserInfoDTO getUserById(Long id) {
        log.warn("Circuit breaker OPEN: User Service unavailable. Returning fallback for user ID: {}", id);
        return UserInfoDTO.builder()
                .id(id)
                .firstName("Unavailable")
                .lastName("")
                .email("unavailable@hostel.com")
                .build();
    }
}
