package com.schoolapp.repository;

import com.schoolapp.model.Room;
import com.schoolapp.model.RoomBooking;
import com.schoolapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface RoomBookingRepository extends JpaRepository<RoomBooking, UUID> {

    List<RoomBooking> findByRoom(Room room);

    List<RoomBooking> findByRoomId(UUID roomId);

    List<RoomBooking> findByBooker(User booker);

    List<RoomBooking> findByBookerId(UUID bookerId);

    List<RoomBooking> findByStatus(RoomBooking.BookingStatus status);

    List<RoomBooking> findByStatusAndRoomId(RoomBooking.BookingStatus status, UUID roomId);

    @Query("SELECT rb FROM RoomBooking rb WHERE rb.room.id = :roomId AND rb.status = 'CONFIRMED' AND rb.startTime < :endTime AND rb.endTime > :startTime ORDER BY rb.startTime")
    List<RoomBooking> findConflictingBookings(@Param("roomId") UUID roomId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT rb FROM RoomBooking rb WHERE rb.booker.id = :bookerId AND rb.status = 'CONFIRMED' ORDER BY rb.startTime")
    List<RoomBooking> findConfirmedBookingsByBooker(@Param("bookerId") UUID bookerId);

    @Query("SELECT rb FROM RoomBooking rb WHERE rb.room.id = :roomId AND rb.status = 'CONFIRMED' AND rb.startTime >= :startTime AND rb.startTime <= :endTime ORDER BY rb.startTime")
    List<RoomBooking> findBookingsInTimeRange(@Param("roomId") UUID roomId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT rb FROM RoomBooking rb WHERE rb.status = 'CONFIRMED' AND rb.startTime >= :startTime ORDER BY rb.startTime")
    List<RoomBooking> findUpcomingBookings(@Param("startTime") LocalDateTime startTime);

    @Query("SELECT rb FROM RoomBooking rb WHERE rb.status = 'CONFIRMED' AND rb.startTime BETWEEN :startDate AND :endDate ORDER BY rb.startTime")
    List<RoomBooking> findBookingsInDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT rb FROM RoomBooking rb WHERE rb.status = 'CONFIRMED' AND rb.startTime >= CURRENT_DATE ORDER BY rb.startTime")
    List<RoomBooking> findUpcomingBookingsFromToday();

    @Query("SELECT rb FROM RoomBooking rb WHERE rb.status = 'CONFIRMED' AND rb.startTime < CURRENT_TIME AND rb.endTime > CURRENT_TIME")
    List<RoomBooking> findCurrentBookings();

    @Query("SELECT COUNT(rb) FROM RoomBooking rb WHERE rb.room.id = :roomId AND rb.status = 'CONFIRMED' AND rb.startTime < :endTime AND rb.endTime > :startTime")
    long countConflictingBookings(@Param("roomId") UUID roomId, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);

    @Query("SELECT rb FROM RoomBooking rb WHERE rb.class.id = :classId ORDER BY rb.startTime DESC")
    List<RoomBooking> findByClassId(@Param("classId") UUID classId);

    @Query("SELECT rb FROM RoomBooking rb WHERE rb.room.id = :roomId AND (rb.title ILIKE %:search% OR rb.notes ILIKE %:search%) ORDER BY rb.startTime")
    List<RoomBooking> searchByRoomId(@Param("roomId") UUID roomId, @Param("search") String search);

    @Query("SELECT rb FROM RoomBooking rb WHERE rb.booker.id = :bookerId AND (rb.title ILIKE %:search% OR rb.notes ILIKE %:search%) ORDER BY rb.startTime")
    List<RoomBooking> searchByBookerId(@Param("bookerId") UUID bookerId, @Param("search") String search);
}