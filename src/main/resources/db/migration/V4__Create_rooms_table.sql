-- Create Rooms Table
CREATE TABLE rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(255) NOT NULL,
    capacity INTEGER DEFAULT 1 CHECK (capacity > 0),
    equipment TEXT, -- JSON array of available equipment
    location VARCHAR(255),
    is_active BOOLEAN DEFAULT true,
    booking_rules TEXT, -- JSON for specific booking restrictions
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for room lookups
CREATE INDEX idx_rooms_type ON rooms(type);
CREATE INDEX idx_rooms_active ON rooms(is_active);
CREATE INDEX idx_rooms_capacity ON rooms(capacity);

-- Trigger for updated_at
CREATE TRIGGER update_rooms_updated_at
    BEFORE UPDATE ON rooms
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();