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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferenceRepository preferenceRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserDTO testUserDTO;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .email("test@hostel.com")
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .role(Role.STUDENT)
                .build();

        testUserDTO = UserDTO.builder()
                .id(1L)
                .email("test@hostel.com")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .role("STUDENT")
                .build();

        registerRequest = RegisterRequest.builder()
                .email("test@hostel.com")
                .password("Test@1234")
                .firstName("John")
                .lastName("Doe")
                .phone("1234567890")
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@hostel.com")
                .password("Test@1234")
                .build();
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("Should register a new user successfully")
    void register_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateTokenFromEmail(anyString())).thenReturn("jwt-token");

        AuthResponse response = userService.register(registerRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("test@hostel.com", response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("STUDENT", response.getRole());

        verify(userRepository).existsByEmail("test@hostel.com");
        verify(userRepository).save(any(User.class));
        verify(jwtTokenProvider).generateTokenFromEmail("test@hostel.com");
    }

    @Test
    @DisplayName("Should throw UserAlreadyExistsException when email exists")
    void register_EmailExists_ThrowsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThrows(UserAlreadyExistsException.class,
                () -> userService.register(registerRequest));

        verify(userRepository).existsByEmail("test@hostel.com");
        verify(userRepository, never()).save(any(User.class));
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void login_Success() {
        Authentication authentication = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateToken(authentication)).thenReturn("jwt-token");
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        AuthResponse response = userService.login(loginRequest);

        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("test@hostel.com", response.getEmail());
    }

    @Test
    @DisplayName("Should throw InvalidCredentialsException for wrong password")
    void login_WrongPassword_ThrowsException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(InvalidCredentialsException.class,
                () -> userService.login(loginRequest));
    }

    // ==================== GET PROFILE TESTS ====================

    @Test
    @DisplayName("Should get user profile successfully")
    void getProfile_Success() {
        when(userRepository.findByEmail("test@hostel.com")).thenReturn(Optional.of(testUser));
        when(modelMapper.map(testUser, UserDTO.class)).thenReturn(testUserDTO);

        UserDTO result = userService.getProfile("test@hostel.com");

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        assertEquals("test@hostel.com", result.getEmail());
        verify(userRepository).findByEmail("test@hostel.com");
    }

    @Test
    @DisplayName("Should throw UserNotFoundException for non-existent user")
    void getProfile_NotFound_ThrowsException() {
        when(userRepository.findByEmail("unknown@hostel.com")).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.getProfile("unknown@hostel.com"));
    }

    // ==================== UPDATE PROFILE TESTS ====================

    @Test
    @DisplayName("Should update user profile successfully")
    void updateProfile_Success() {
        UpdateProfileRequest updateRequest = UpdateProfileRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .phone("0987654321")
                .build();

        User updatedUser = User.builder()
                .id(1L)
                .email("test@hostel.com")
                .firstName("Jane")
                .lastName("Smith")
                .phone("0987654321")
                .role(Role.STUDENT)
                .build();

        UserDTO updatedDTO = UserDTO.builder()
                .id(1L)
                .email("test@hostel.com")
                .firstName("Jane")
                .lastName("Smith")
                .phone("0987654321")
                .role("STUDENT")
                .build();

        when(userRepository.findByEmail("test@hostel.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(updatedUser);
        when(modelMapper.map(updatedUser, UserDTO.class)).thenReturn(updatedDTO);

        UserDTO result = userService.updateProfile("test@hostel.com", updateRequest);

        assertNotNull(result);
        assertEquals("Jane", result.getFirstName());
        assertEquals("Smith", result.getLastName());
        verify(userRepository).save(any(User.class));
    }

    // ==================== GET ALL USERS TEST ====================

    @Test
    @DisplayName("Should return all users for admin")
    void getAllUsers_Success() {
        User user2 = User.builder()
                .id(2L).email("user2@hostel.com")
                .firstName("Jane").lastName("Smith")
                .role(Role.STUDENT).build();

        UserDTO userDTO2 = UserDTO.builder()
                .id(2L).email("user2@hostel.com")
                .firstName("Jane").lastName("Smith")
                .role("STUDENT").build();

        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));
        when(modelMapper.map(testUser, UserDTO.class)).thenReturn(testUserDTO);
        when(modelMapper.map(user2, UserDTO.class)).thenReturn(userDTO2);

        List<UserDTO> result = userService.getAllUsers();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userRepository).findAll();
    }

    // ==================== PREFERENCES TESTS ====================

    @Test
    @DisplayName("Should save user preferences successfully")
    void savePreferences_Success() {
        UserPreferenceDTO prefDTO = UserPreferenceDTO.builder()
                .preferredRoomType("SINGLE")
                .preferredFloor(2)
                .preferredAmenities(Arrays.asList("WiFi", "AC"))
                .build();

        UserPreference savedPref = UserPreference.builder()
                .id(1L)
                .user(testUser)
                .preferredRoomType("SINGLE")
                .preferredFloor(2)
                .preferredAmenities("[\"WiFi\",\"AC\"]")
                .build();

        when(userRepository.findByEmail("test@hostel.com")).thenReturn(Optional.of(testUser));
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(UserPreference.class))).thenReturn(savedPref);

        UserPreferenceDTO result = userService.savePreferences("test@hostel.com", prefDTO);

        assertNotNull(result);
        assertEquals("SINGLE", result.getPreferredRoomType());
        assertEquals(2, result.getPreferredFloor());
        verify(preferenceRepository).save(any(UserPreference.class));
    }

    @Test
    @DisplayName("Should get user preferences successfully")
    void getPreferences_Success() {
        UserPreference pref = UserPreference.builder()
                .id(1L)
                .user(testUser)
                .preferredRoomType("DOUBLE")
                .preferredFloor(3)
                .preferredAmenities("[\"WiFi\",\"Balcony\"]")
                .build();

        when(userRepository.findByEmail("test@hostel.com")).thenReturn(Optional.of(testUser));
        when(preferenceRepository.findByUserId(1L)).thenReturn(Optional.of(pref));

        UserPreferenceDTO result = userService.getPreferences("test@hostel.com");

        assertNotNull(result);
        assertEquals("DOUBLE", result.getPreferredRoomType());
        assertEquals(3, result.getPreferredFloor());
        assertEquals(2, result.getPreferredAmenities().size());
        assertTrue(result.getPreferredAmenities().contains("WiFi"));
    }

    // ==================== GET USER BY ID TEST ====================

    @Test
    @DisplayName("Should get user by ID successfully")
    void getUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(modelMapper.map(testUser, UserDTO.class)).thenReturn(testUserDTO);

        UserDTO result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw UserNotFoundException for invalid ID")
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.getUserById(999L));
    }
}
