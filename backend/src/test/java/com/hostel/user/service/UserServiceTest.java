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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserPreferenceRepository preferenceRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private ModelMapper modelMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("student@hostel.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .phone("9876543210")
                .role(Role.STUDENT)
                .build();

        testUserDTO = UserDTO.builder()
                .id(1L)
                .email("student@hostel.com")
                .firstName("John")
                .lastName("Doe")
                .phone("9876543210")
                .role("STUDENT")
                .build();
    }

    @Test
    @DisplayName("Register - Success: New user registers and gets JWT token")
    void register_success() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@hostel.com");
        request.setPassword("Test@1234");
        request.setFirstName("New");
        request.setLastName("User");

        when(userRepository.existsByEmail("new@hostel.com")).thenReturn(false);
        when(passwordEncoder.encode("Test@1234")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(
                User.builder().id(2L).email("new@hostel.com").firstName("New").lastName("User").role(Role.STUDENT).build()
        );
        when(jwtTokenProvider.generateTokenFromEmail("new@hostel.com")).thenReturn("jwt-token");

        AuthResponse response = userService.register(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("new@hostel.com", response.getEmail());
        assertEquals("STUDENT", response.getRole());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Register - Failure: Duplicate email throws UserAlreadyExistsException")
    void register_duplicateEmail_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("student@hostel.com");
        request.setPassword("Test@1234");
        request.setFirstName("John");
        request.setLastName("Doe");

        when(userRepository.existsByEmail("student@hostel.com")).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Login - Success: Valid credentials return JWT token")
    void login_success() {
        LoginRequest request = new LoginRequest();
        request.setEmail("student@hostel.com");
        request.setPassword("Test@1234");

        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtTokenProvider.generateToken(auth)).thenReturn("jwt-token");
        when(userRepository.findByEmail("student@hostel.com")).thenReturn(Optional.of(testUser));

        AuthResponse response = userService.login(request);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("student@hostel.com", response.getEmail());
    }

    @Test
    @DisplayName("Login - Failure: Invalid credentials throw InvalidCredentialsException")
    void login_invalidCredentials_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("bad@hostel.com");
        request.setPassword("wrong");

        when(authenticationManager.authenticate(any())).thenThrow(new RuntimeException("Bad credentials"));

        assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
    }

    @Test
    @DisplayName("GetProfile - Success: Returns user profile DTO")
    void getProfile_success() {
        when(userRepository.findByEmail("student@hostel.com")).thenReturn(Optional.of(testUser));
        when(modelMapper.map(testUser, UserDTO.class)).thenReturn(testUserDTO);

        UserDTO result = userService.getProfile("student@hostel.com");

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        verify(userRepository).findByEmail("student@hostel.com");
    }

    @Test
    @DisplayName("GetProfile - Failure: Non-existent email throws UserNotFoundException")
    void getProfile_notFound_throwsException() {
        when(userRepository.findByEmail("unknown@hostel.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getProfile("unknown@hostel.com"));
    }

    @Test
    @DisplayName("GetUserById - Success: Returns user by ID")
    void getUserById_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(modelMapper.map(testUser, UserDTO.class)).thenReturn(testUserDTO);

        UserDTO result = userService.getUserById(1L);

        assertEquals(1L, result.getId());
    }

    @Test
    @DisplayName("GetUserById - Failure: Non-existent ID throws UserNotFoundException")
    void getUserById_notFound_throwsException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getUserById(99L));
    }

    @Test
    @DisplayName("UpdateProfile - Success: Updates user fields")
    void updateProfile_success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");
        request.setPhone("1111111111");

        when(userRepository.findByEmail("student@hostel.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(modelMapper.map(any(User.class), eq(UserDTO.class))).thenReturn(testUserDTO);

        UserDTO result = userService.updateProfile("student@hostel.com", request);

        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("UpdateProfile - Failure: Email conflict throws UserAlreadyExistsException")
    void updateProfile_emailConflict_throwsException() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("taken@hostel.com");

        User otherUser = User.builder().id(99L).email("taken@hostel.com").build();

        when(userRepository.findByEmail("student@hostel.com")).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("taken@hostel.com")).thenReturn(Optional.of(otherUser));

        assertThrows(UserAlreadyExistsException.class,
                () -> userService.updateProfile("student@hostel.com", request));
    }

    @Test
    @DisplayName("GetAllUsers - Success: Returns list of all users")
    void getAllUsers_success() {
        when(userRepository.findAll()).thenReturn(List.of(testUser));
        when(modelMapper.map(testUser, UserDTO.class)).thenReturn(testUserDTO);

        List<UserDTO> result = userService.getAllUsers();

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("UploadProfilePicture - Failure: Invalid file type throws IllegalArgumentException")
    void uploadProfilePicture_invalidType_throwsException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("application/pdf");
        when(userRepository.findByEmail("student@hostel.com")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class,
                () -> userService.uploadProfilePicture("student@hostel.com", file));
    }

    @Test
    @DisplayName("UploadProfilePicture - Failure: File too large throws IllegalArgumentException")
    void uploadProfilePicture_tooLarge_throwsException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getSize()).thenReturn(10L * 1024 * 1024);
        when(userRepository.findByEmail("student@hostel.com")).thenReturn(Optional.of(testUser));

        assertThrows(IllegalArgumentException.class,
                () -> userService.uploadProfilePicture("student@hostel.com", file));
    }

    @Test
    @DisplayName("GetPreferences - Success: Returns user preferences")
    void getPreferences_success() {
        UserPreference pref = UserPreference.builder()
                .id(1L).user(testUser)
                .preferredRoomType("SINGLE")
                .preferredFloor(2)
                .preferredAmenities("[\"WiFi\",\"AC\"]")
                .build();

        when(userRepository.findByEmail("student@hostel.com")).thenReturn(Optional.of(testUser));
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(pref));

        UserPreferenceDTO result = userService.getPreferences("student@hostel.com");

        assertNotNull(result);
        assertEquals("SINGLE", result.getPreferredRoomType());
        assertEquals(2, result.getPreferredFloor());
        assertEquals(2, result.getPreferredAmenities().size());
    }

    @Test
    @DisplayName("SavePreferences - Success: Saves and returns preferences")
    void savePreferences_success() {
        UserPreferenceDTO dto = new UserPreferenceDTO();
        dto.setPreferredRoomType("DOUBLE");
        dto.setPreferredFloor(3);
        dto.setPreferredAmenities(Arrays.asList("WiFi", "Balcony"));

        UserPreference pref = UserPreference.builder().id(1L).user(testUser).build();

        when(userRepository.findByEmail("student@hostel.com")).thenReturn(Optional.of(testUser));
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(pref));
        when(preferenceRepository.save(any(UserPreference.class))).thenReturn(pref);

        UserPreferenceDTO result = userService.savePreferences("student@hostel.com", dto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(preferenceRepository).save(any(UserPreference.class));
    }
    @Test
    @DisplayName("DeleteProfilePicture - Success")
    void deleteProfilePicture_success() throws java.io.IOException {
        testUser.setProfilePicturePath("uploads/profiles/test.jpg");
        when(userRepository.findByEmail("student@hostel.com")).thenReturn(Optional.of(testUser));
        when(modelMapper.map(any(User.class), eq(UserDTO.class))).thenReturn(testUserDTO);

        UserDTO result = userService.deleteProfilePicture("student@hostel.com");

        assertNotNull(result);
        assertNull(testUser.getProfilePicturePath());
        verify(userRepository).save(any(User.class));
    }
}
