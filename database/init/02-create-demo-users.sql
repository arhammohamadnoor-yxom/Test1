-- Create demo users for testing
-- Password: password (BCrypt encoded)
INSERT INTO users (email, password, role, first_name, last_name, staff_id, student_id, phone_number, is_active) VALUES
('admin@school.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'ADMINISTRATOR', 'System', 'Administrator', 'ADMIN001', NULL, '+60123456789', true),
('teacher@school.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'TEACHER', 'Ahmad', 'Bin Ismail', 'TEACHER001', NULL, '+60123456788', true),
('student@school.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'STUDENT', 'Mohamed', 'Bin Ali', NULL, 'STU2024001', '+60123456787', true);

-- Create demo classes
INSERT INTO classes (name, description, teacher_id, grade_level, subject, schedule, max_students) VALUES
('Science Form 4A', 'Advanced Science Class for Form 4', (SELECT id FROM users WHERE email = 'teacher@school.com'), 4, 'Science', '{"day": "Monday", "time": "08:00-09:30", "room": "Makmal Bio 1"}', 30),
('Mathematics Form 4A', 'Mathematics Class for Form 4', (SELECT id FROM users WHERE email = 'teacher@school.com'), 4, 'Mathematics', '{"day": "Tuesday", "time": "10:00-11:30", "room": "Bilik APD"}', 30);

-- Enroll demo student in classes
INSERT INTO class_enrollments (class_id, student_id, enrollment_date, is_active)
SELECT c.id, u.id, CURRENT_DATE, true
FROM classes c, users u
WHERE u.email = 'student@school.com';

-- Create some sample attendance records
INSERT INTO attendance_records (student_id, class_id, teacher_id, date, status, notes)
SELECT
    u.id as student_id,
    c.id as class_id,
    c.teacher_id as teacher_id,
    CURRENT_DATE - INTERVAL '1 day' as date,
    CASE WHEN random() > 0.2 THEN 'PRESENT' ELSE 'ABSENT' END as status,
    CASE WHEN random() > 0.8 THEN 'Late arrival' ELSE NULL END as notes
FROM users u, classes c
WHERE u.email = 'student@school.com';

-- Create sample room bookings
INSERT INTO room_bookings (room_id, booker_id, class_id, title, start_time, end_time, number_of_participants, status, notes)
SELECT
    r.id as room_id,
    u.id as booker_id,
    c.id as class_id,
    'Science Lab Session' as title,
    CURRENT_TIMESTAMP + INTERVAL '2 hours' as start_time,
    CURRENT_TIMESTAMP + INTERVAL '4 hours' as end_time,
    25 as number_of_participants,
    'CONFIRMED' as status,
    'Regular science practical session' as notes
FROM rooms r, users u, classes c
WHERE u.email = 'teacher@school.com'
  AND c.name = 'Science Form 4A'
  AND r.name = 'Makmal Bio 1'
LIMIT 1;