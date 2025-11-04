package com.schoolapp.repository;

import com.schoolapp.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    List<Room> findByIsActive(Boolean isActive);

    List<Room> findByType(String type);

    List<Room> findByTypeAndIsActive(String type, Boolean isActive);

    Optional<Room> findByName(String name);

    @Query("SELECT r FROM Room r WHERE r.isActive = true ORDER BY r.type, r.name")
    List<Room> findActiveRooms();

    @Query("SELECT r FROM Room r WHERE r.capacity >= :minCapacity AND r.isActive = true ORDER BY r.capacity, r.name")
    List<Room> findActiveRoomsByMinCapacity(@Param("minCapacity") Integer minCapacity);

    @Query("SELECT r FROM Room r WHERE r.type ILIKE %:type% OR r.name ILIKE %:type% AND r.isActive = true ORDER BY r.type, r.name")
    List<Room> searchActiveRooms(@Param("type") String type);

    @Query("SELECT DISTINCT r.type FROM Room r WHERE r.isActive = true ORDER BY r.type")
    List<String> findActiveRoomTypes();

    @Query("SELECT COUNT(r) FROM Room r WHERE r.type = :type AND r.isActive = true")
    long countActiveRoomsByType(@Param("type") String type);

    @Query("SELECT r FROM Room r WHERE r.capacity >= :minCapacity AND r.capacity <= :maxCapacity AND r.isActive = true ORDER BY r.capacity")
    List<Room> findActiveRoomsByCapacityRange(@Param("minCapacity") Integer minCapacity, @Param("maxCapacity") Integer maxCapacity);
}