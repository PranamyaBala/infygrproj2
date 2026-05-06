package com.hostel.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hostel.user.dto.AuthResponse;
import com.hostel.user.dto.LoginRequest;
import com.hostel.user.dto.RegisterRequest;
import com.hostel.user.dto.UpdateProfileRequest;
import com.hostel.user.dto.UserDTO;
import com.hostel.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    controllers = UserController.class, 
    excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.hostel.user.security.JwtAuthenticationFilter.class)
)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        userDTO = UserDTO.builder().id(1L).email("test@example.com").build();
    }

    @Test
    void login_ShouldReturnOk() throws Exception {
        LoginRequest req = new LoginRequest("test@example.com", "pass");
        AuthResponse authResponse = AuthResponse.builder().token("token").userId(1L).email("test@example.com").build();
        when(userService.login(any())).thenReturn(authResponse);

        mockMvc.perform(post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void register_ShouldReturnOk() throws Exception {
        RegisterRequest req = new RegisterRequest("first", "last", "test@example.com", "Password@123", "1234567890", "MALE");
        AuthResponse authResponse = AuthResponse.builder().token("token").userId(1L).email("test@example.com").build();
        when(userService.register(any())).thenReturn(authResponse);

        mockMvc.perform(post("/api/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    void getProfile_ShouldReturnOk() throws Exception {
        when(userService.getProfile(anyString())).thenReturn(userDTO);

        mockMvc.perform(get("/api/users/profile")
                .principal(mockAuth("test@example.com")))
                .andExpect(status().isOk());
    }

    @Test
    void updateProfile_ShouldReturnOk() throws Exception {
        UpdateProfileRequest req = new UpdateProfileRequest("first", "last", "test@example.com", "1234567890", "MALE");
        when(userService.updateProfile(anyString(), any())).thenReturn(userDTO);

        mockMvc.perform(put("/api/users/profile")
                .principal(mockAuth("test@example.com"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void getAllUsers_ShouldReturnOk() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(userDTO));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk());
    }

    private org.springframework.security.core.Authentication mockAuth(String email) {
        org.springframework.security.core.Authentication auth = org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        when(auth.getName()).thenReturn(email);
        return auth;
    }
}
