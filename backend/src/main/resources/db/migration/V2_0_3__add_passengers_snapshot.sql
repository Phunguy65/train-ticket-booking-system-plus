-- Add passengers_snapshot JSONB column to bookings table
-- This column stores passenger data for multi-passenger bookings
-- Nullable for backward compatibility with existing bookings
ALTER TABLE bookings
ADD COLUMN passengers_snapshot JSONB;

COMMENT ON COLUMN bookings.passengers_snapshot IS 'JSON array of passenger snapshots with seatId, fullName, idDocumentNumber, dateOfBirth, gender. Null for legacy bookings.';
