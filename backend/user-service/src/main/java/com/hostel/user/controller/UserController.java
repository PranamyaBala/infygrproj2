package com.hostel.user.controller;

import com.hostel.user.dto.*;
import com.hostel.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for user registration, authentication, and profile management")
public class UserController {

    private final UserService userService;

    // ==================== AUTHENTICATION ====================

    @PostMapping("/register")
    @Operation(summary = "Register a new student account")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and receive JWT token")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    // ==================== PROFILE ====================

    @GetMapping("/profile")
    @Operation(summary = "Get current user's profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDTO> getProfile(Authentication authentication) {
        UserDTO profile = userService.getProfile(authentication.getName());
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update current user's profile", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDTO> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserDTO updatedProfile = userService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(updatedProfile);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping
    @Operation(summary = "Get all users (Admin only)", security = @SecurityRequirement(name = "bearerAuth"))
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    // ==================== PROFILE PICTURE ====================

    @PostMapping(value = "/profile/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload profile picture", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDTO> uploadProfilePicture(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) throws IOException {
        UserDTO updatedProfile = userService.uploadProfilePicture(authentication.getName(), file);
        return ResponseEntity.ok(updatedProfile);
    }

    @DeleteMapping("/profile/picture")
    @Operation(summary = "Delete profile picture", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserDTO> deleteProfilePicture(Authentication authentication) throws IOException {
        UserDTO updatedProfile = userService.deleteProfilePicture(authentication.getName());
        return ResponseEntity.ok(updatedProfile);
    }

    // ==================== PREFERENCES ====================

    @GetMapping("/preferences")
    @Operation(summary = "Get saved room search preferences", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserPreferenceDTO> getPreferences(Authentication authentication) {
        UserPreferenceDTO preferences = userService.getPreferences(authentication.getName());
        return ResponseEntity.ok(preferences);
    }

    @PutMapping("/preferences")
    @Operation(summary = "Save room search preferences", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserPreferenceDTO> savePreferences(
            Authentication authentication,
            @Valid @RequestBody UserPreferenceDTO preferences) {
        UserPreferenceDTO saved = userService.savePreferences(authentication.getName(), preferences);
        return ResponseEntity.ok(saved);
    }
}
