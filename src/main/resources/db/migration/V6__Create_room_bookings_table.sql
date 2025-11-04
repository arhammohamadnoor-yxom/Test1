-- Create Room Bookings Table
CREATE TABLE room_bookings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    room_id UUID NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    booker_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    class_id UUID REFERENCES classes(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    number_of_participants INTEGER DEFAULT 1 CHECK (number_of_participants > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN ('CONFIRMED', 'CANCELLED')),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Ensure end_time is after start_time
    CONSTRAINT check_time_order CHECK (end_time > start_time),

    -- Prevent booking conflicts in the same room during overlapping times
    EXCLUDE USING GIST (
        room_id WITH =,
        tsrange(start_time, end_time) WITH &&
    )
);

-- Create indexes for booking lookups
CREATE INDEX idx_room_bookings_room ON room_bookings(room_id);
CREATE INDEX idx_room_bookings_booker ON room_bookings(booker_id);
CREATE INDEX idx_room_bookings_class ON room_bookings(class_id);
CREATE INDEX idx_room_bookings_status ON room_bookings(status);
CREATE INDEX idx_room_bookings_time_range ON room_bookings(room_id, start_time, end_time);

-- Index for finding bookings in a time range
CREATE INDEX idx_room_bookings_start_time ON room_bookings(start_time);
CREATE INDEX idx_room_bookings_end_time ON room_bookings(end_time);

-- Trigger for updated_at
CREATE TRIGGER update_room_bookings_updated_at
    BEFORE UPDATE ON room_bookings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();