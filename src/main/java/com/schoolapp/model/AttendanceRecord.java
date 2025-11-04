package com.schoolapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "attendance_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "UUID DEFAULT gen_random_uuid()")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attendance_student"))
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attendance_class"))
    private Class class_;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false, foreignKey = @ForeignKey(name = "fk_attendance_teacher"))
    private User teacher;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AttendanceStatus status;

    private String notes;

    @Column(name = "marked_at", nullable = false)
    private LocalDateTime markedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        markedAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AttendanceStatus {
        PRESENT, ABSENT
    }

    public boolean isPresent() {
        return status == AttendanceStatus.PRESENT;
    }

    public boolean isAbsent() {
        return status == AttendanceStatus.ABSENT;
    }

    public String getStatusDisplay() {
        return status.name().charAt(0) + status.name().substring(1).toLowerCase();
    }
}