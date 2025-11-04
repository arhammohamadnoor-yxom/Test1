package com.schoolapp.service;

import com.schoolapp.dto.RoomBookingRequest;
import com.schoolapp.model.Room;
import com.schoolapp.model.RoomBooking;
import com.schoolapp.model.Class;
import com.schoolapp.model.User;
import com.schoolapp.repository.RoomBookingRepository;
import com.schoolapp.repository.RoomRepository;
import com.schoolapp.repository.ClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RoomBookingService {

    private final RoomBookingRepository bookingRepository;
    private final RoomRepository roomRepository;
    private final ClassRepository classRepository;
    private final AuthService authService;

    @Transactional
    public RoomBooking createBooking(RoomBookingRequest request) {
        User currentUser = authService.getCurrentUser();
        if (currentUser.getRole() != User.UserRole.TEACHER) {
            throw new RuntimeException("Only teachers can book rooms");
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Room not found: " + request.getRoomId()));

        // Check for booking conflicts
        List<RoomBooking> conflictingBookings = bookingRepository.findConflictingBookings(
                room.getId(), request.getStartTime(), request.getEndTime());

        if (!conflictingBookings.isEmpty()) {
            throw new RuntimeException("Room is already booked during this time");
        }

        // Validate booking time
        if (request.getEndTime().isBefore(request.getStartTime()) ||
            request.getEndTime().isEqual(request.getStartTime())) {
            throw new RuntimeException("End time must be after start time");
        }

        // Validate booking is not too far in the future (e.g., max 3 months)
        if (request.getStartTime().isAfter(LocalDateTime.now().plusMonths(3))) {
            throw new RuntimeException("Cannot book rooms more than 3 months in advance");
        }

        // Validate booking is not in the past
        if (request.getStartTime().isBefore(LocalDateTime.now().minusHours(1))) {
            throw new RuntimeException("Cannot book rooms for past times");
        }

        // Validate participant count doesn't exceed room capacity
        if (request.getNumberOfParticipants() > room.getCapacity()) {
            throw new RuntimeException("Number of participants exceeds room capacity");
        }

        Class classEntity = null;
        if (request.getClassId() != null) {
            classEntity = classRepository.findById(request.getClassId())
                    .orElseThrow(() -> new RuntimeException("Class not found: " + request.getClassId()));

            // Verify teacher owns this class
            if (!classEntity.getTeacher().getId().equals(currentUser.getId())) {
                throw new RuntimeException("You can only book rooms for your own classes");
            }
        }

        RoomBooking booking = RoomBooking.builder()
                .room(room)
                .booker(currentUser)
                .class_(classEntity)
                .title(request.getTitle())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .numberOfParticipants(request.getNumberOfParticipants())
                .notes(request.getNotes())
                .status(RoomBooking.BookingStatus.CONFIRMED)
                .build();

        return bookingRepository.save(booking);
    }

    @Transactional
    public RoomBooking updateBooking(UUID bookingId, RoomBookingRequest request) {
        User currentUser = authService.getCurrentUser();
        if (currentUser.getRole() != User.UserRole.TEACHER) {
            throw new RuntimeException("Only teachers can update bookings");
        }

        RoomBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        // Verify teacher owns this booking
        if (!booking.getBooker().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only update your own bookings");
        }

        // Cannot update bookings that are in the past
        if (booking.getStartTime().isBefore(LocalDateTime.now().minusHours(1))) {
            throw new RuntimeException("Cannot update past bookings");
        }

        Room room = booking.getRoom();
        if (!request.getRoomId().equals(room.getId())) {
            room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new RuntimeException("Room not found: " + request.getRoomId()));
        }

        // Check for conflicts (exclude current booking)
        List<RoomBooking> conflictingBookings = bookingRepository.findConflictingBookings(
                room.getId(), request.getStartTime(), request.getEndTime())
                .stream()
                .filter(b -> !b.getId().equals(bookingId))
                .toList();

        if (!conflictingBookings.isEmpty()) {
            throw new RuntimeException("Room is already booked during this time");
        }

        // Update booking details
        booking.setRoom(room);
        booking.setTitle(request.getTitle());
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setNumberOfParticipants(request.getNumberOfParticipants());
        booking.setNotes(request.getNotes());

        // Update class if provided
        if (request.getClassId() != null) {
            Class classEntity = classRepository.findById(request.getClassId())
                    .orElseThrow(() -> new RuntimeException("Class not found: " + request.getClassId()));

            if (!classEntity.getTeacher().getId().equals(currentUser.getId())) {
                throw new RuntimeException("You can only book rooms for your own classes");
            }
            booking.setClass_(classEntity);
        }

        return bookingRepository.save(booking);
    }

    @Transactional
    public void cancelBooking(UUID bookingId) {
        User currentUser = authService.getCurrentUser();
        if (currentUser.getRole() != User.UserRole.TEACHER) {
            throw new RuntimeException("Only teachers can cancel bookings");
        }

        RoomBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found: " + bookingId));

        // Verify teacher owns this booking
        if (!booking.getBooker().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You can only cancel your own bookings");
        }

        booking.setStatus(RoomBooking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    public List<RoomBooking> getBookingsForTeacher(UUID teacherId, LocalDateTime startDate, LocalDateTime endDate) {
        return bookingRepository.findBookingsInDateRange(startDate, endDate)
                .stream()
                .filter(booking -> booking.getBooker().getId().equals(teacherId))
                .toList();
    }

    public List<RoomBooking> getBookingsForRoom(UUID roomId, LocalDateTime startDate, LocalDateTime endDate) {
        return bookingRepository.findBookingsInTimeRange(roomId, startDate, endDate);
    }

    public List<RoomBooking> getUpcomingBookingsForTeacher(UUID teacherId) {
        return bookingRepository.findConfirmedBookingsByBooker(teacherId);
    }

    public List<Room> getAvailableRooms(LocalDateTime startTime, LocalDateTime endTime) {
        List<Room> allRooms = roomRepository.findActiveRooms();
        List<Room> unavailableRooms = new ArrayList<>();

        for (Room room : allRooms) {
            List<RoomBooking> conflicts = bookingRepository.findConflictingBookings(room.getId(), startTime, endTime);
            if (!conflicts.isEmpty()) {
                unavailableRooms.add(room);
            }
        }

        allRooms.removeAll(unavailableRooms);
        return allRooms;
    }

    public boolean isRoomAvailable(UUID roomId, LocalDateTime startTime, LocalDateTime endTime) {
        List<RoomBooking> conflicts = bookingRepository.findConflictingBookings(roomId, startTime, endTime);
        return conflicts.isEmpty();
    }

    public Map<LocalDate, List<RoomBooking>> getBookingsByDate(UUID roomId, LocalDate startDate, LocalDate endDate) {
        List<RoomBooking> bookings = bookingRepository.findBookingsInDateRange(
                roomId, startDate.atStartOfDay(), endDate.atTime(23, 59, 59));

        Map<LocalDate, List<RoomBooking>> bookingsByDate = new HashMap<>();
        for (RoomBooking booking : bookings) {
            LocalDate date = booking.getStartTime().toLocalDate();
            bookingsByDate.computeIfAbsent(date, k -> new ArrayList<>()).add(booking);
        }

        return bookingsByDate;
    }

    public List<RoomBooking> getCurrentBookings() {
        return bookingRepository.findCurrentBookings();
    }

    public List<RoomBooking> getBookingsForClass(UUID classId) {
        return bookingRepository.findByClassId(classId);
    }

    public Map<String, Long> getRoomUsageStats(UUID roomId, LocalDateTime startDate, LocalDateTime endDate) {
        List<RoomBooking> bookings = bookingRepository.findBookingsInDateRange(roomId, startDate, endDate);

        Map<String, Long> stats = new HashMap<>();
        stats.put("totalBookings", (long) bookings.size());
        stats.put("confirmedBookings", bookings.stream().filter(b -> b.isConfirmed()).count());
        stats.put("cancelledBookings", bookings.stream().filter(b -> b.isCancelled()).count());

        return stats;
    }
}