package com.schoolapp.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RoomBookingRequest {
    @NotNull(message = "Room is required")
    private UUID roomId;

    private UUID classId;

    @NotBlank(message = "Title is required")
    private String title;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    @Future(message = "End time must be in the future")
    private LocalDateTime endTime;

    @NotNull(message = "Number of participants is required")
    @Positive(message = "Number of participants must be positive")
    private Integer numberOfParticipants;

    private String notes;
}