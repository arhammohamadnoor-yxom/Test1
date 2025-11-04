package com.schoolapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "rooms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "UUID DEFAULT gen_random_uuid()")
    private UUID id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String type;

    @Column(nullable = false)
    private Integer capacity = 1;

    @Column(columnDefinition = "TEXT")
    private String equipment; // JSON array of available equipment

    private String location;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "booking_rules", columnDefinition = "TEXT")
    private String bookingRules; // JSON for specific booking restrictions

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomBooking> bookings;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getDisplayName() {
        return name + " (Capacity: " + capacity + ")";
    }

    public String getFullDisplayName() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (location != null && !location.trim().isEmpty()) {
            sb.append(" - ").append(location);
        }
        sb.append(" (Capacity: ").append(capacity).append(")");
        return sb.toString();
    }
}