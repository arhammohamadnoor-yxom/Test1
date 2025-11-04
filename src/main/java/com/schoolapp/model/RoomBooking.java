package com.schoolapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "room_bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "UUID DEFAULT gen_random_uuid()")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false, foreignKey = @ForeignKey(name = "fk_booking_room"))
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booker_id", nullable = false, foreignKey = @ForeignKey(name = "fk_booking_booker"))
    private User booker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", foreignKey = @ForeignKey(name = "fk_booking_class"))
    private Class class_;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "number_of_participants", nullable = false)
    private Integer numberOfParticipants = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status;

    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = BookingStatus.CONFIRMED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum BookingStatus {
        CONFIRMED, CANCELLED
    }

    public boolean isConfirmed() {
        return status == BookingStatus.CONFIRMED;
    }

    public boolean isCancelled() {
        return status == BookingStatus.CANCELLED;
    }

    public String getStatusDisplay() {
        return status.name().charAt(0) + status.name().substring(1).toLowerCase();
    }

    public String getDurationDisplay() {
        if (startTime != null && endTime != null) {
            long hours = java.time.Duration.between(startTime, endTime).toHours();
            long minutes = java.time.Duration.between(startTime, endTime).toMinutesPart();

            if (hours > 0) {
                return hours + "h " + minutes + "m";
            } else {
                return minutes + "m";
            }
        }
        return "Unknown";
    }

    public String getTimeRangeDisplay() {
        if (startTime != null && endTime != null) {
            return startTime.toLocalTime() + " - " + endTime.toLocalTime();
        }
        return "Unknown time";
    }

    public String getDateDisplay() {
        if (startTime != null) {
            return startTime.toLocalDate().toString();
        }
        return "Unknown date";
    }
}