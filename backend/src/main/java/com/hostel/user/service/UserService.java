package com.hostel.user.service;

import com.hostel.user.dto.*;
import com.hostel.user.entity.Role;
import com.hostel.user.entity.User;
import com.hostel.user.entity.UserPreference;
import com.hostel.user.exception.InvalidCredentialsException;
import com.hostel.user.exception.UserAlreadyExistsException;
import com.hostel.user.exception.UserNotFoundException;
import com.hostel.user.repository.UserPreferenceRepository;
import com.hostel.user.repository.UserRepository;
import com.hostel.user.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository preferenceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final ModelMapper modelMapper;

    private static final String UPLOAD_DIR = "uploads/profiles";

    // ==================== AUTHENTICATION ====================

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("email", request.getEmail());
        }

        // Build user entity using Lombok builder
        User user = User.builder()
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .phone(request.getPhone())
                .role(Role.STUDENT)
                .build();

        User savedUser = userRepository.save(user);
        log.info("New user registered successfully: {}", savedUser.getEmail());

        // Generate JWT token
        String token = jwtTokenProvider.generateTokenFromEmail(savedUser.getEmail());

        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getRole().name()
        );
    }

    public AuthResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail().toLowerCase().trim(),
                            request.getPassword()
                    )
            );

            String token = jwtTokenProvider.generateToken(authentication);

            User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                    .orElseThrow(() -> new InvalidCredentialsException());

            log.info("User logged in successfully: {}", user.getEmail());

            return new AuthResponse(
                    token,
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole().name()
            );
        } catch (Exception e) {
            log.error("Login failed for email: {}", request.getEmail());
            throw new InvalidCredentialsException();
        }
    }

    // ==================== PROFILE MANAGEMENT ====================

    @Transactional(readOnly = true)
    public UserDTO getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
        return modelMapper.map(user, UserDTO.class);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        return modelMapper.map(user, UserDTO.class);
    }

    @Transactional
    public UserDTO updateProfile(String email, UpdateProfileRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        // Update only non-null fields using streams for cleanliness
        if (request.getFirstName() != null && !request.getFirstName().isBlank()) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null && !request.getLastName().isBlank()) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            // Check if new email is already taken by another user
            userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                    .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                    .ifPresent(existingUser -> {
                        throw new UserAlreadyExistsException("email", request.getEmail());
                    });
            user.setEmail(request.getEmail().toLowerCase().trim());
        }

        User updatedUser = userRepository.save(user);
        log.info("Profile updated for user: {}", updatedUser.getEmail());

        return modelMapper.map(updatedUser, UserDTO.class);
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> modelMapper.map(user, UserDTO.class))
                .collect(Collectors.toList());
    }

    // ==================== PROFILE PICTURE ====================

    @Transactional
    public UserDTO uploadProfilePicture(String email, MultipartFile file) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        // Validate file type using streams
        List<String> allowedTypes = Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp");
        boolean isValidType = allowedTypes.stream()
                .anyMatch(type -> type.equals(file.getContentType()));

        if (!isValidType) {
            throw new IllegalArgumentException("Invalid file type. Allowed: JPEG, PNG, GIF, WebP");
        }

        if (file.getSize() > 5 * 1024 * 1024) { // 5MB limit
            throw new IllegalArgumentException("File size must be less than 5MB");
        }

        // Create upload directory if not exists
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String filename = UUID.randomUUID() + extension;

        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Delete old picture if exists
        if (user.getProfilePicturePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(user.getProfilePicturePath()));
            } catch (IOException e) {
                log.warn("Could not delete old profile picture: {}", e.getMessage());
            }
        }

        // Store URL-friendly path (relative to the backend origin)
        String webPath = "/uploads/profiles/" + filename;
        user.setProfilePicturePath(webPath);
        User updatedUser = userRepository.save(user);
        log.info("Profile picture uploaded for user: {}", email);

        return modelMapper.map(updatedUser, UserDTO.class);
    }

    @Transactional
    public UserDTO deleteProfilePicture(String email) throws IOException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        if (user.getProfilePicturePath() != null) {
            Files.deleteIfExists(Paths.get(user.getProfilePicturePath()));
            user.setProfilePicturePath(null);
            userRepository.save(user);
            log.info("Profile picture deleted for user: {}", email);
        }

        return modelMapper.map(user, UserDTO.class);
    }

    // ==================== PREFERENCES ====================

    @Transactional(readOnly = true)
    public UserPreferenceDTO getPreferences(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        UserPreference preference = preferenceRepository.findByUserId(user.getId())
                .orElse(UserPreference.builder().user(user).build());

        UserPreferenceDTO dto = new UserPreferenceDTO();
        dto.setId(preference.getId());
        dto.setPreferredRoomType(preference.getPreferredRoomType());
        dto.setPreferredFloor(preference.getPreferredFloor());
        dto.setPreferredMinPrice(preference.getPreferredMinPrice());
        dto.setPreferredMaxPrice(preference.getPreferredMaxPrice());

        // Parse JSON amenities string to list
        if (preference.getPreferredAmenities() != null && !preference.getPreferredAmenities().isBlank()) {
            List<String> amenities = Arrays.stream(
                    preference.getPreferredAmenities()
                            .replace("[", "").replace("]", "").replace("\"", "")
                            .split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            dto.setPreferredAmenities(amenities);
        }

        return dto;
    }

    @Transactional
    public UserPreferenceDTO savePreferences(String email, UserPreferenceDTO preferencesDto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        UserPreference preference = preferenceRepository.findByUserId(user.getId())
                .orElse(UserPreference.builder().user(user).build());

        preference.setPreferredRoomType(preferencesDto.getPreferredRoomType());
        preference.setPreferredFloor(preferencesDto.getPreferredFloor());
        preference.setPreferredMinPrice(preferencesDto.getPreferredMinPrice());
        preference.setPreferredMaxPrice(preferencesDto.getPreferredMaxPrice());

        // Convert list to JSON string using streams
        if (preferencesDto.getPreferredAmenities() != null) {
            String amenitiesJson = preferencesDto.getPreferredAmenities().stream()
                    .map(a -> "\"" + a + "\"")
                    .collect(Collectors.joining(",", "[", "]"));
            preference.setPreferredAmenities(amenitiesJson);
        }

        UserPreference saved = preferenceRepository.save(preference);
        log.info("Preferences saved for user: {}", email);

        preferencesDto.setId(saved.getId());
        return preferencesDto;
    }
}
