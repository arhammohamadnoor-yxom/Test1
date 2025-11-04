-- Create Classes Table
CREATE TABLE classes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    teacher_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    grade_level INTEGER NOT NULL CHECK (grade_level BETWEEN 1 AND 13),
    subject VARCHAR(100) NOT NULL,
    schedule TEXT, -- JSON format for recurring schedule
    max_students INTEGER DEFAULT 30 CHECK (max_students > 0),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for class lookups
CREATE INDEX idx_classes_teacher ON classes(teacher_id);
CREATE INDEX idx_classes_grade_level ON classes(grade_level);
CREATE INDEX idx_classes_subject ON classes(subject);

-- Trigger for updated_at
CREATE TRIGGER update_classes_updated_at
    BEFORE UPDATE ON classes
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();