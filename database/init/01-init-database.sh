#!/bin/bash
set -e

# Create additional indexes for performance
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    -- Add performance indexes
    CREATE INDEX IF NOT EXISTS idx_attendance_composite ON attendance_records(class_id, date, status);
    CREATE INDEX IF NOT EXISTS idx_room_bookings_composite ON room_bookings(room_id, start_time, end_time, status);
    CREATE INDEX IF NOT EXISTS idx_class_enrollments_composite ON class_enrollments(class_id, student_id, is_active);

    -- Create full-text search indexes
    CREATE INDEX IF NOT EXISTS idx_users_search ON users USING gin(to_tsvector('english', first_name || ' ' || last_name || ' ' || email));
    CREATE INDEX IF NOT EXISTS idx_classes_search ON classes USING gin(to_tsvector('english', name || ' ' || description));

    -- Create materialized view for attendance statistics
    CREATE OR REPLACE VIEW attendance_stats AS
    SELECT
        c.id as class_id,
        c.name as class_name,
        ar.date,
        COUNT(*) as total_students,
        COUNT(CASE WHEN ar.status = 'PRESENT' THEN 1 END) as present_count,
        COUNT(CASE WHEN ar.status = 'ABSENT' THEN 1 END) as absent_count,
        ROUND(
            (COUNT(CASE WHEN ar.status = 'PRESENT' THEN 1 END) * 100.0 / COUNT(*)), 2
        ) as attendance_percentage
    FROM attendance_records ar
    JOIN classes c ON ar.class_id = c.id
    GROUP BY c.id, c.name, ar.date;

    -- Create function to update attendance stats
    CREATE OR REPLACE FUNCTION refresh_attendance_stats()
    RETURNS void AS \$\$
    BEGIN
        REFRESH MATERIALIZED VIEW CONCURRENTLY attendance_stats;
    END;
    \$\$ LANGUAGE plpgsql;
EOSQL

echo "Database initialization completed successfully!"