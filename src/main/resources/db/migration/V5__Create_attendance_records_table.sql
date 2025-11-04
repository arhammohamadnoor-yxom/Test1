-- Create Attendance Records Table
CREATE TABLE attendance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    date DATE NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PRESENT', 'ABSENT')),
    notes TEXT,
    marked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Ensure unique attendance record per student per class per day
    UNIQUE(student_id, class_id, date)
);

-- Create indexes for attendance lookups
CREATE INDEX idx_attendance_student ON attendance_records(student_id);
CREATE INDEX idx_attendance_class ON attendance_records(class_id);
CREATE INDEX idx_attendance_teacher ON attendance_records(teacher_id);
CREATE INDEX idx_attendance_date ON attendance_records(date);
CREATE INDEX idx_attendance_status ON attendance_records(status);

-- Composite index for common queries
CREATE INDEX idx_attendance_class_date ON attendance_records(class_id, date);

-- Trigger for updated_at
CREATE TRIGGER update_attendance_records_updated_at
    BEFORE UPDATE ON attendance_records
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();