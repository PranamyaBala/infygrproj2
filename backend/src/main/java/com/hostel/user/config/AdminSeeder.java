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
        seedRoomPrices();
        seedRoomImages();
        seedFloorPlans();
        seedSampleBookings();
        fixExistingBookingPrices();
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
                Amenity.builder().name("WiFi").icon("wifi").description("High-speed wireless internet").price(new java.math.BigDecimal("50.00")).build(),
                Amenity.builder().name("Air Conditioning").icon("ac").description("Central air conditioning").price(new java.math.BigDecimal("200.00")).build(),
                Amenity.builder().name("Study Desk").icon("desk").description("Dedicated study desk with chair").price(new java.math.BigDecimal("30.00")).build(),
                Amenity.builder().name("Wardrobe").icon("wardrobe").description("Built-in wardrobe").price(new java.math.BigDecimal("30.00")).build(),
                Amenity.builder().name("Ensuite Bathroom").icon("bath").description("Private attached bathroom").price(new java.math.BigDecimal("150.00")).build(),
                Amenity.builder().name("Laundry Access").icon("laundry").description("In-room laundry machine access").price(new java.math.BigDecimal("50.00")).build(),
                Amenity.builder().name("Kitchenette").icon("kitchen").description("Small kitchen with microwave and fridge").price(new java.math.BigDecimal("100.00")).build(),
                Amenity.builder().name("Balcony").icon("balcony").description("Private balcony with view").price(new java.math.BigDecimal("80.00")).build()
            );
            amenityRepository.saveAll(defaultAmenities);
            log.info("===> DEFAULT AMENITIES SEEDED <===");
        } else {
            // Force update prices for existing amenities that have 0 or null price (US Update)
            List<Amenity> existing = amenityRepository.findAll();
            boolean changed = false;
            for (Amenity a : existing) {
                if (a.getPrice() == null || a.getPrice().compareTo(java.math.BigDecimal.ZERO) == 0) {
                    if (a.getName().equals("WiFi")) a.setPrice(new java.math.BigDecimal("50.00"));
                    else if (a.getName().equals("Air Conditioning")) a.setPrice(new java.math.BigDecimal("200.00"));
                    else if (a.getName().equals("Ensuite Bathroom")) a.setPrice(new java.math.BigDecimal("150.00"));
                    else if (a.getName().equals("Kitchenette")) a.setPrice(new java.math.BigDecimal("100.00"));
                    else if (a.getName().equals("Balcony")) a.setPrice(new java.math.BigDecimal("80.00"));
                    else if (a.getName().equals("Laundry Access")) a.setPrice(new java.math.BigDecimal("50.00"));
                    else a.setPrice(new java.math.BigDecimal("30.00"));
                    changed = true;
                }
            }
            if (changed) {
                amenityRepository.saveAll(existing);
                log.info("===> EXISTING AMENITY PRICES UPDATED <===");
            }
        }
    }
    private void seedRoomPrices() {
        List<Room> rooms = roomRepository.findAll();
        boolean changed = false;
        for (Room room : rooms) {
            BigDecimal newPrice = null;
            String type = room.getRoomType().toString();
            
            if (type.equals("SINGLE")) newPrice = new BigDecimal("100.00");
            else if (type.equals("DOUBLE")) newPrice = new BigDecimal("200.00");
            else if (type.equals("TRIPLE")) newPrice = new BigDecimal("300.00");
            else if (type.equals("SUITE")) newPrice = new BigDecimal("1000.00");
            else if (type.equals("DORMITORY")) newPrice = new BigDecimal("50.00");
            
            if (newPrice != null && (room.getPricePerNight() == null || room.getPricePerNight().compareTo(newPrice) != 0)) {
                room.setPricePerNight(newPrice);
                changed = true;
            }
        }
        if (changed) {
            roomRepository.saveAll(rooms);
            log.info("===> ROOM BASE PRICES UPDATED: Single: 100, Double: 200, Triple: 300, Suite: 1000, Dorm: 50 <===");
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

    private void seedFloorPlans() {
        List<Room> rooms = roomRepository.findAll();
        boolean updated = false;
        for (Room room : rooms) {
            if (room.getFloorPlanPath() == null || room.getFloorPlanPath().isEmpty() || room.getFloorPlanPath().contains(".jpg")) {
                int floor = room.getFloor();
                String suffix = (floor == 1) ? "st" : (floor == 2) ? "nd" : (floor == 3) ? "rd" : "th";
                String path = "/images/floors/" + floor + suffix + "_floor.png";
                room.setFloorPlanPath(path);
                updated = true;
            }
        }
        if (updated) {
            roomRepository.saveAll(rooms);
            log.info("===> ROOM FLOOR PLAN PATHS UPDATED (PNG) <===");
        }
    }

    private void seedSampleBookings() {
        if (bookingRepository.count() == 0) {
            User student = userRepository.findByEmail("student@hostel.com").orElse(null);
            List<Room> rooms = roomRepository.findAllWithAmenities();

            if (student != null && !rooms.isEmpty()) {
                Booking b1 = Booking.builder()
                        .userId(student.getId())
                        .roomId(rooms.get(0).getId())
                        .startDate(LocalDate.now().plusDays(2))
                        .endDate(LocalDate.now().plusDays(10))
                        .occupants(1)
                        .status(BookingStatus.PENDING)
                        .totalPrice(calculateBasePriceWithAmenities(rooms.get(0)).multiply(new BigDecimal("8")))
                        .bookingReference("BK-" + LocalDate.now().getYear() + "-SAMP01")
                        .build();

                Booking b2 = Booking.builder()
                        .userId(student.getId())
                        .roomId(rooms.get(1).getId())
                        .startDate(LocalDate.now().minusDays(5))
                        .endDate(LocalDate.now().plusDays(5))
                        .occupants(1)
                        .status(BookingStatus.APPROVED)
                        .totalPrice(calculateBasePriceWithAmenities(rooms.get(1)).multiply(new BigDecimal("10")))
                        .bookingReference("BK-" + LocalDate.now().getYear() + "-SAMP02")
                        .build();

                Booking b3 = Booking.builder()
                        .userId(student.getId())
                        .roomId(rooms.get(2).getId())
                        .startDate(LocalDate.now().minusDays(10))
                        .endDate(LocalDate.now().minusDays(2))
                        .occupants(2)
                        .status(BookingStatus.CHECKED_IN)
                        .totalPrice(calculateBasePriceWithAmenities(rooms.get(2)).multiply(new BigDecimal("8")))
                        .bookingReference("BK-" + LocalDate.now().getYear() + "-SAMP03")
                        .build();

                bookingRepository.saveAll(Arrays.asList(b1, b2, b3));
                log.info("===> SAMPLE BOOKINGS SEEDED (with amenity-inclusive prices) <===");
            }
        }
    }

    /**
     * Recalculate totalPrice for ALL existing bookings using basePriceWithAmenities × nights.
     * This fixes bookings that were created with the old pricePerNight (without amenities).
     */
    private void fixExistingBookingPrices() {
        List<Booking> allBookings = bookingRepository.findAll();
        boolean changed = false;

        for (Booking booking : allBookings) {
            Room room = roomRepository.findByIdWithAmenities(booking.getRoomId()).orElse(null);
            if (room == null) continue;

            BigDecimal basePriceWithAmenities = calculateBasePriceWithAmenities(room);
            long nights = java.time.temporal.ChronoUnit.DAYS.between(booking.getStartDate(), booking.getEndDate());
            if (nights <= 0) nights = 1;

            BigDecimal correctTotal = basePriceWithAmenities.multiply(new BigDecimal(nights));

            if (booking.getTotalPrice().compareTo(correctTotal) != 0) {
                log.info("Fixing booking {} price: {} -> {} (basePriceWithAmenities={} × {} nights)",
                        booking.getBookingReference(), booking.getTotalPrice(), correctTotal,
                        basePriceWithAmenities, nights);
                booking.setTotalPrice(correctTotal);
                changed = true;
            }
        }

        if (changed) {
            bookingRepository.saveAll(allBookings);
            log.info("===> EXISTING BOOKING PRICES FIXED (recalculated with amenity-inclusive rates) <===");
        }
    }

    /**
     * Calculate basePriceWithAmenities for a room: pricePerNight + sum of amenity prices.
     */
    private BigDecimal calculateBasePriceWithAmenities(Room room) {
        BigDecimal basePrice = room.getPricePerNight() != null ? room.getPricePerNight() : BigDecimal.ZERO;
        if (room.getAmenities() != null) {
            for (Amenity amenity : room.getAmenities()) {
                basePrice = basePrice.add(amenity.getPrice() != null ? amenity.getPrice() : BigDecimal.ZERO);
            }
        }
        return basePrice;
    }
}
