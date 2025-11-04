-- Create Class Enrollments Table (Join Table)
CREATE TABLE class_enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    class_id UUID NOT NULL REFERENCES classes(id) ON DELETE CASCADE,
    student_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    enrollment_date DATE DEFAULT CURRENT_DATE,
    is_active BOOLEAN DEFAULT true,

    -- Ensure a student can only be enrolled once in a class
    UNIQUE(class_id, student_id)
);

-- Create indexes for enrollment lookups
CREATE INDEX idx_class_enrollments_class ON class_enrollments(class_id);
CREATE INDEX idx_class_enrollments_student ON class_enrollments(student_id);
CREATE INDEX idx_class_enrollments_active ON class_enrollments(is_active);