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
@Table(name = "classes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Class {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "UUID DEFAULT gen_random_uuid()")
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false, foreignKey = @ForeignKey(name = "fk_class_teacher"))
    private User teacher;

    @Column(name = "grade_level", nullable = false)
    private Integer gradeLevel;

    @Column(nullable = false, length = 100)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String schedule; // JSON format for recurring schedule

    @Column(name = "max_students", nullable = false)
    private Integer maxStudents = 30;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "class", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClassEnrollment> enrollments;

    @OneToMany(mappedBy = "class", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AttendanceRecord> attendanceRecords;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public int getCurrentEnrollmentCount() {
        return enrollments != null ?
            (int) enrollments.stream().filter(ClassEnrollment::getIsActive).count() : 0;
    }

    public boolean hasSpaceAvailable() {
        return getCurrentEnrollmentCount() < maxStudents;
    }

    public String getDisplayName() {
        return name + " - " + subject + " (Grade " + gradeLevel + ")";
    }
}