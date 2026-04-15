package com.hostel.user.config;

import com.hostel.user.entity.Role;
import com.hostel.user.entity.User;
import com.hostel.user.repository.UserRepository;
import com.hostel.room.entity.Amenity;
import com.hostel.room.entity.Room;
import com.hostel.room.repository.AmenityRepository;
import com.hostel.room.repository.RoomRepository;
import com.hostel.booking.entity.Booking;
import com.hostel.booking.entity.BookingStatus;
import com.hostel.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final AmenityRepository amenityRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdminUser();
        seedStudentUser();
        seedAmenities();
        seedRoomImages();
        seedSampleBookings();
    }

    private void seedAdminUser() {
        if (!userRepository.existsByEmail("admin@hostel.com")) {
            User admin = User.builder()
                    .email("admin@hostel.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .firstName("System")
                    .lastName("Administrator")
                    .phone("9999999999")
                    .role(Role.ADMIN)
                    .build();
            userRepository.save(admin);
            log.info("===> ADMIN ACCOUNT SEEDED: admin@hostel.com / Admin@123 <===");
        }
    }

    private void seedStudentUser() {
        if (!userRepository.existsByEmail("student@hostel.com")) {
            User student = User.builder()
                    .email("student@hostel.com")
                    .password(passwordEncoder.encode("Student@123"))
                    .firstName("John")
                    .lastName("Doe")
                    .phone("8888888888")
                    .role(Role.STUDENT)
                    .build();
            userRepository.save(student);
            log.info("===> STUDENT ACCOUNT SEEDED: student@hostel.com / Student@123 <===");
        }
    }

    private void seedAmenities() {
        if (amenityRepository.count() == 0) {
            List<Amenity> defaultAmenities = Arrays.asList(
                Amenity.builder().name("WiFi").icon("wifi").description("High-speed wireless internet").build(),
                Amenity.builder().name("Air Conditioning").icon("ac").description("Central air conditioning").build(),
                Amenity.builder().name("Study Desk").icon("desk").description("Dedicated study desk with chair").build(),
                Amenity.builder().name("Wardrobe").icon("wardrobe").description("Built-in wardrobe").build(),
                Amenity.builder().name("Ensuite Bathroom").icon("bath").description("Private attached bathroom").build(),
                Amenity.builder().name("Laundry Access").icon("laundry").description("In-room laundry machine access").build(),
                Amenity.builder().name("Kitchenette").icon("kitchen").description("Small kitchen with microwave and fridge").build(),
                Amenity.builder().name("Balcony").icon("balcony").description("Private balcony with view").build()
            );
            amenityRepository.saveAll(defaultAmenities);
            log.info("===> DEFAULT AMENITIES SEEDED <===");
        }
    }

    private void seedRoomImages() {
        List<Room> rooms = roomRepository.findAll();
        boolean updated = false;
        for (Room room : rooms) {
            if (room.getImagePath() == null || room.getImagePath().isEmpty()) {
                String type = room.getRoomType().toString();
                String path = "/images/rooms/" + type.toLowerCase() + "_bed.jpg";
                room.setImagePath(path);
                updated = true;
            }
        }
        if (updated) {
            roomRepository.saveAll(rooms);
            log.info("===> ROOM IMAGE PATHS UPDATED TO DEFAULTS <===");
        }
    }

    private void seedSampleBookings() {
        if (bookingRepository.count() == 0) {
            User student = userRepository.findByEmail("student@hostel.com").orElse(null);
            List<Room> rooms = roomRepository.findAll();

            if (student != null && !rooms.isEmpty()) {
                Booking b1 = Booking.builder()
                        .userId(student.getId())
                        .roomId(rooms.get(0).getId())
                        .startDate(LocalDate.now().plusDays(2))
                        .endDate(LocalDate.now().plusDays(10))
                        .occupants(1)
                        .status(BookingStatus.PENDING)
                        .totalPrice(new BigDecimal("400.00"))
                        .bookingReference("BK-" + LocalDate.now().getYear() + "-SAMP01")
                        .build();

                Booking b2 = Booking.builder()
                        .userId(student.getId())
                        .roomId(rooms.get(1).getId())
                        .startDate(LocalDate.now().minusDays(5))
                        .endDate(LocalDate.now().plusDays(5))
                        .occupants(1)
                        .status(BookingStatus.APPROVED)
                        .totalPrice(new BigDecimal("500.00"))
                        .bookingReference("BK-" + LocalDate.now().getYear() + "-SAMP02")
                        .build();

                Booking b3 = Booking.builder()
                        .userId(student.getId())
                        .roomId(rooms.get(2).getId())
                        .startDate(LocalDate.now().minusDays(10))
                        .endDate(LocalDate.now().minusDays(2))
                        .occupants(2)
                        .status(BookingStatus.CHECKED_IN)
                        .totalPrice(new BigDecimal("640.00"))
                        .bookingReference("BK-" + LocalDate.now().getYear() + "-SAMP03")
                        .build();

                bookingRepository.saveAll(Arrays.asList(b1, b2, b3));
                log.info("===> SAMPLE BOOKINGS SEEDED <===");
            }
        }
    }
}
