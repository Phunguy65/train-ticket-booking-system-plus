CREATE INDEX IF NOT EXISTS idx_trip_seat_availability_trip_status_booking ON trip_seat_availability (scheduled_trip_id, status, booking_id);
