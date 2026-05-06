package com.hostel.user.config;

import com.hostel.booking.entity.Booking;
import com.hostel.booking.repository.BookingRepository;
import com.hostel.room.entity.Amenity;
import com.hostel.room.entity.Room;
import com.hostel.room.entity.RoomType;
import com.hostel.room.repository.AmenityRepository;
import com.hostel.room.repository.RoomRepository;
import com.hostel.user.entity.User;
import com.hostel.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AmenityRepository amenityRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminSeeder adminSeeder;

    @BeforeEach
    void setUp() {
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
    }

    @Test
    void testRun_WhenDbIsEmpty_ShouldSeedEverything() {
        // Arrange empty DB scenarios
        when(userRepository.existsByEmail("admin@hostel.com")).thenReturn(false);
        when(userRepository.existsByEmail("student@hostel.com")).thenReturn(false);
        when(amenityRepository.count()).thenReturn(0L);
        when(bookingRepository.count()).thenReturn(0L);

        // Mock return for seedSampleBookings
        User mockStudent = new User();
        mockStudent.setId(1L);
        when(userRepository.findByEmail("student@hostel.com")).thenReturn(Optional.of(mockStudent));
        
        Room mockRoom1 = new Room(); mockRoom1.setId(1L); mockRoom1.setRoomType(RoomType.SINGLE); mockRoom1.setFloor(1);
        Room mockRoom2 = new Room(); mockRoom2.setId(2L); mockRoom2.setRoomType(RoomType.DOUBLE); mockRoom2.setFloor(2);
        Room mockRoom3 = new Room(); mockRoom3.setId(3L); mockRoom3.setRoomType(RoomType.TRIPLE); mockRoom3.setFloor(3);
        when(roomRepository.findAllWithAmenities()).thenReturn(Arrays.asList(mockRoom1, mockRoom2, mockRoom3));

        // For seedRoomPrices, seedRoomImages, seedFloorPlans
        when(roomRepository.findAll()).thenReturn(Arrays.asList(mockRoom1, mockRoom2, mockRoom3));
        
        // For fixExistingBookingPrices
        when(bookingRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        adminSeeder.run();

        // Assert
        verify(userRepository, times(2)).save(any(User.class)); // 1 admin + 1 student
        verify(amenityRepository, times(1)).saveAll(anyList()); // 8 default amenities
        verify(roomRepository, atLeastOnce()).saveAll(anyList()); // update prices, images, floor plans
        verify(bookingRepository, times(1)).saveAll(anyList()); // sample bookings
    }

    @Test
    void testRun_WhenDbIsAlreadySeeded_ShouldOnlyUpdateNeeded() {
        // Arrange populated DB scenarios
        when(userRepository.existsByEmail("admin@hostel.com")).thenReturn(true);
        when(userRepository.existsByEmail("student@hostel.com")).thenReturn(true);
        when(amenityRepository.count()).thenReturn(8L);
        
        Amenity existingAmenity = Amenity.builder().name("WiFi").price(BigDecimal.ZERO).build();
        when(amenityRepository.findAll()).thenReturn(Collections.singletonList(existingAmenity));

        when(bookingRepository.count()).thenReturn(3L);

        Room mockRoom = new Room();
        mockRoom.setId(1L);
        mockRoom.setRoomType(RoomType.SUITE);
        mockRoom.setPricePerNight(new BigDecimal("1000.00")); // correct price
        mockRoom.setImagePath("/images/rooms/suite_bed.jpg"); // correct path
        mockRoom.setFloor(1);
        mockRoom.setFloorPlanPath("/images/floors/1st_floor.png"); // correct path
        
        when(roomRepository.findAll()).thenReturn(Collections.singletonList(mockRoom));

        Booking existingBooking = Booking.builder()
                .id(1L).roomId(1L).startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(2))
                .totalPrice(BigDecimal.ZERO).build(); // zero price will force update
                
        when(bookingRepository.findAll()).thenReturn(Collections.singletonList(existingBooking));
        when(roomRepository.findByIdWithAmenities(1L)).thenReturn(Optional.of(mockRoom));

        // Act
        adminSeeder.run();

        // Assert
        verify(userRepository, never()).save(any(User.class)); // Already exists
        verify(amenityRepository, times(1)).saveAll(anyList()); // To update zero price amenity
        verify(roomRepository, never()).saveAll(anyList()); // All room data was already correct
        verify(bookingRepository, times(1)).saveAll(anyList()); // To fix the zero price booking
    }
}
