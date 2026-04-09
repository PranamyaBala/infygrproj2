package com.hostel.room.exception;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String message) {
        super(message);
    }
    public RoomNotFoundException(Long id) {
        super("Room not found with id: " + id);
    }
}
