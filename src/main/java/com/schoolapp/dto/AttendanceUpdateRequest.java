package com.schoolapp.dto;

import com.schoolapp.model.AttendanceRecord;
import lombok.Data;

@Data
public class AttendanceUpdateRequest {
    private AttendanceRecord.AttendanceStatus status;
    private String notes;
}