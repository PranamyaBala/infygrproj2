package com.hostel.user.controller;

import com.hostel.user.dto.*;
import com.hostel.user.exception.InvalidCredentialsException;
import com.hostel.user.exception.UserAlreadyExistsException;
import com.hostel.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new com.hostel.user.exception.GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/users/register - Success 201")
    void register_success_201() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@hostel.com");
        request.setPassword("Test@1234");
        request.setFirstName("New");
        request.setLastName("User");

        AuthResponse response = new AuthResponse("jwt-token", 1L, "new@hostel.com", "New", "User", "STUDENT");
        when(userService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("new@hostel.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"));

        verify(userService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /api/users/register - Duplicate email 409")
    void register_duplicateEmail_409() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("dup@hostel.com");
        request.setPassword("Test@1234");
        request.setFirstName("Dup");
        request.setLastName("User");

        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new UserAlreadyExistsException("email", "dup@hostel.com"));

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST /api/users/login - Success 200")
    void login_success_200() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@hostel.com");
        request.setPassword("Test@1234");

        AuthResponse response = new AuthResponse("jwt-token", 1L, "student@hostel.com", "John", "Doe", "STUDENT");
        when(userService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    @DisplayName("POST /api/users/login - Bad credentials 401")
    void login_badCredentials_401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("bad@hostel.com");
        request.setPassword("wrong");

        when(userService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/users/profile - Authenticated 200")
    void getProfile_authenticated_200() throws Exception {
        UserDTO profile = UserDTO.builder()
                .id(1L).email("student@hostel.com")
                .firstName("John").lastName("Doe").build();

        when(userService.getProfile("student@hostel.com")).thenReturn(profile);

        mockMvc.perform(get("/api/users/profile")
                        .principal(mockAuthentication("student@hostel.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("student@hostel.com"));
    }

    @Test
    @DisplayName("PUT /api/users/profile - Update success 200")
    void updateProfile_success_200() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Updated");

        UserDTO updatedProfile = UserDTO.builder()
                .id(1L).firstName("Updated").lastName("Doe").email("student@hostel.com").build();

        when(userService.updateProfile(eq("student@hostel.com"), any(UpdateProfileRequest.class)))
                .thenReturn(updatedProfile);

        mockMvc.perform(put("/api/users/profile")
                        .principal(mockAuthentication("student@hostel.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    @DisplayName("GET /api/users - Get all users 200")
    void getAllUsers_200() throws Exception {
        UserDTO user = UserDTO.builder().id(1L).email("student@hostel.com").firstName("John").lastName("Doe").build();
        when(userService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/users")
                        .principal(mockAuthentication("admin@hostel.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("student@hostel.com"));
    }

    @Test
    @DisplayName("GET /api/users/{id} - Get user by ID 200")
    void getUserById_200() throws Exception {
        UserDTO user = UserDTO.builder().id(1L).email("student@hostel.com").firstName("John").lastName("Doe").build();
        when(userService.getUserById(1L)).thenReturn(user);

        mockMvc.perform(get("/api/users/1")
                        .principal(mockAuthentication("admin@hostel.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    // Helper to create a mock Authentication principal
    private Authentication mockAuthentication(String email) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(email);
        return auth;
    }
}
