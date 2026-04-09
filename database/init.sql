-- =============================================================
-- Student Hostel & Room Allocation Management System
-- Database DDL Script — Single MySQL DB, separate schemas
-- =============================================================

-- ===================== USER SCHEMA ===========================
CREATE DATABASE IF NOT EXISTS hostel_user_db;
USE hostel_user_db;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role ENUM('STUDENT', 'ADMIN') NOT NULL DEFAULT 'STUDENT',
    profile_picture_path VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    preferred_room_type VARCHAR(50),
    preferred_floor INT,
    preferred_amenities JSON,
    preferred_min_price DECIMAL(10,2),
    preferred_max_price DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===================== ROOM SCHEMA ===========================
CREATE DATABASE IF NOT EXISTS hostel_room_db;
USE hostel_room_db;

CREATE TABLE rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(20) NOT NULL UNIQUE,
    room_type ENUM('SINGLE', 'DOUBLE', 'TRIPLE', 'SUITE', 'DORMITORY') NOT NULL,
    floor INT NOT NULL,
    capacity INT NOT NULL,
    price_per_night DECIMAL(10,2) NOT NULL,
    status ENUM('AVAILABLE', 'OCCUPIED', 'MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    description TEXT,
    image_path VARCHAR(500),
    floor_plan_path VARCHAR(500),
    maintenance_start_date DATE,
    maintenance_end_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_rooms_status (status),
    INDEX idx_rooms_type (room_type),
    INDEX idx_rooms_floor (floor),
    INDEX idx_rooms_price (price_per_night)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE amenities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    icon VARCHAR(50),
    description VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE room_amenities (
    room_id BIGINT NOT NULL,
    amenity_id BIGINT NOT NULL,
    PRIMARY KEY (room_id, amenity_id),
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (amenity_id) REFERENCES amenities(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE pricing_tiers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    season_name VARCHAR(100) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    price_multiplier DECIMAL(4,2) NOT NULL DEFAULT 1.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    INDEX idx_pricing_dates (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE room_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    event_name VARCHAR(200) NOT NULL,
    group_name VARCHAR(200),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    INDEX idx_event_dates (start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===================== BOOKING SCHEMA ========================
CREATE DATABASE IF NOT EXISTS hostel_booking_db;
USE hostel_booking_db;

CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    occupants INT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    total_price DECIMAL(10,2) NOT NULL,
    booking_reference VARCHAR(50) NOT NULL UNIQUE,
    late_checkout_requested BOOLEAN DEFAULT FALSE,
    late_checkout_fee DECIMAL(10,2) DEFAULT 0.00,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_bookings_user (user_id),
    INDEX idx_bookings_room (room_id),
    INDEX idx_bookings_status (status),
    INDEX idx_bookings_dates (start_date, end_date),
    INDEX idx_bookings_reference (booking_reference)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===================== NOTIFICATION SCHEMA ====================
CREATE DATABASE IF NOT EXISTS hostel_notification_db;
USE hostel_notification_db;

CREATE TABLE notification_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_email VARCHAR(255) NOT NULL,
    subject VARCHAR(500) NOT NULL,
    notification_type ENUM('BOOKING_SUBMITTED', 'BOOKING_APPROVED', 'BOOKING_REJECTED', 'BOOKING_CONFIRMED') NOT NULL,
    status ENUM('SENT', 'FAILED', 'PENDING') NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notification_type (notification_type),
    INDEX idx_notification_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ===================== SEED DATA =============================
USE hostel_room_db;

-- Seed amenities
INSERT INTO amenities (name, icon, description) VALUES
('Ensuite Bathroom', 'bath', 'Private attached bathroom'),
('WiFi', 'wifi', 'High-speed wireless internet'),
('Kitchenette', 'kitchen', 'Small kitchen with microwave and fridge'),
('Balcony', 'balcony', 'Private balcony with view'),
('Air Conditioning', 'ac', 'Central air conditioning'),
('Study Desk', 'desk', 'Dedicated study desk with chair'),
('Wardrobe', 'wardrobe', 'Built-in wardrobe'),
('Laundry Access', 'laundry', 'In-room laundry machine access');

-- Seed sample rooms
INSERT INTO rooms (room_number, room_type, floor, capacity, price_per_night, status, description, image_path) VALUES
('101', 'SINGLE', 1, 1, 50.00, 'AVAILABLE', 'Cozy single room with a view of the garden. Perfect for students who prefer privacy and a quiet environment.', '/images/rooms/room-single-1.jpg'),
('102', 'SINGLE', 1, 1, 50.00, 'AVAILABLE', 'Bright single room with large windows. South-facing for natural sunlight throughout the day.', '/images/rooms/room-single-2.jpg'),
('103', 'DOUBLE', 1, 2, 80.00, 'AVAILABLE', 'Spacious double room ideal for two students. Includes two beds, two desks, and shared bathroom.', '/images/rooms/room-double-1.jpg'),
('201', 'DOUBLE', 2, 2, 85.00, 'AVAILABLE', 'Second floor double room with balcony access. Beautifully furnished with modern decor.', '/images/rooms/room-double-2.jpg'),
('202', 'TRIPLE', 2, 3, 110.00, 'AVAILABLE', 'Large triple room perfect for groups. Features three single beds and a common study area.', '/images/rooms/room-triple-1.jpg'),
('203', 'SUITE', 2, 2, 150.00, 'AVAILABLE', 'Premium suite with separate living area. Includes ensuite bathroom and kitchenette.', '/images/rooms/room-suite-1.jpg'),
('301', 'SUITE', 3, 2, 160.00, 'OCCUPIED', 'Top floor premium suite with panoramic campus view. Luxury furnishings and private balcony.', '/images/rooms/room-suite-2.jpg'),
('302', 'DORMITORY', 3, 6, 30.00, 'AVAILABLE', 'Economical dormitory-style room with six beds. Shared facilities with common lounge access.', '/images/rooms/room-dorm-1.jpg'),
('303', 'DORMITORY', 3, 6, 30.00, 'MAINTENANCE', 'Dormitory room currently undergoing renovation. Expected to be available next semester.', '/images/rooms/room-dorm-2.jpg'),
('104', 'SINGLE', 1, 1, 55.00, 'AVAILABLE', 'Corner single room with extra space and two windows. Quiet location at the end of the corridor.', '/images/rooms/room-single-3.jpg'),
('204', 'DOUBLE', 2, 2, 90.00, 'AVAILABLE', 'Premium double room with ensuite bathroom and modern furnishing. Includes mini fridge.', '/images/rooms/room-double-3.jpg'),
('304', 'TRIPLE', 3, 3, 120.00, 'AVAILABLE', 'Top floor triple room with skylight. Spacious layout with individual study areas.', '/images/rooms/room-triple-2.jpg');

-- Associate amenities with rooms
INSERT INTO room_amenities (room_id, amenity_id) VALUES
(1, 2), (1, 6), (1, 7),                 -- Room 101: WiFi, Study Desk, Wardrobe
(2, 2), (2, 6), (2, 7),                 -- Room 102: WiFi, Study Desk, Wardrobe
(3, 1), (3, 2), (3, 6), (3, 7),         -- Room 103: Ensuite, WiFi, Desk, Wardrobe
(4, 1), (4, 2), (4, 4), (4, 6), (4, 7), -- Room 201: Ensuite, WiFi, Balcony, Desk, Wardrobe
(5, 2), (5, 6), (5, 7),                 -- Room 202: WiFi, Study Desk, Wardrobe
(6, 1), (6, 2), (6, 3), (6, 4), (6, 5), (6, 6), (6, 7), -- Room 203: All amenities
(7, 1), (7, 2), (7, 3), (7, 4), (7, 5), (7, 6), (7, 7), -- Room 301: All amenities
(8, 2), (8, 8),                         -- Room 302: WiFi, Laundry
(9, 2), (9, 8),                         -- Room 303: WiFi, Laundry
(10, 1), (10, 2), (10, 5), (10, 6), (10, 7), -- Room 104: Ensuite, WiFi, AC, Desk, Wardrobe
(11, 1), (11, 2), (11, 3), (11, 5), (11, 6), (11, 7), -- Room 204: Ensuite, WiFi, Kitchen, AC, Desk, Wardrobe
(12, 2), (12, 5), (12, 6), (12, 7);     -- Room 304: WiFi, AC, Desk, Wardrobe

-- Seed admin user (password: Admin@123 — BCrypt hashed)
USE hostel_user_db;
INSERT INTO users (email, password, first_name, last_name, phone, role) VALUES
('admin@hostel.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System', 'Administrator', '9999999999', 'ADMIN'),
('student@hostel.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'John', 'Doe', '8888888888', 'STUDENT');
