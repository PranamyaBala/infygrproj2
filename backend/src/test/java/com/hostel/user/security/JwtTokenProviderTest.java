package com.hostel.user.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret",
                "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 86400000L);
    }

    @Test
    @DisplayName("GenerateToken - From Authentication returns valid JWT")
    void generateToken_success() {
        UserDetails userDetails = new User("student@hostel.com", "password", Collections.emptyList());
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, Collections.emptyList());

        String token = jwtTokenProvider.generateToken(auth);

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("GenerateTokenFromEmail - Returns valid JWT")
    void generateTokenFromEmail_success() {
        String token = jwtTokenProvider.generateTokenFromEmail("student@hostel.com");

        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("GetEmailFromToken - Extracts correct email")
    void getEmailFromToken_success() {
        String token = jwtTokenProvider.generateTokenFromEmail("student@hostel.com");
        String email = jwtTokenProvider.getEmailFromToken(token);

        assertEquals("student@hostel.com", email);
    }

    @Test
    @DisplayName("ValidateToken - Valid token returns true")
    void validateToken_valid() {
        String token = jwtTokenProvider.generateTokenFromEmail("student@hostel.com");

        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("ValidateToken - Expired token returns false")
    void validateToken_expired() {
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", -1000L);
        String token = jwtTokenProvider.generateTokenFromEmail("student@hostel.com");

        assertFalse(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("ValidateToken - Malformed token returns false")
    void validateToken_malformed() {
        assertFalse(jwtTokenProvider.validateToken("invalid.token.here"));
    }
}
