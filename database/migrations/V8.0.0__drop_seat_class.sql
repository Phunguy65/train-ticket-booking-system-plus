-- Drop seat_class column from seats table and seat_class_at_booking column from booking_seats.
-- Pricing is now unified: all seats on a route share route.base_price (no multiplier).
ALTER TABLE seats
DROP COLUMN seat_class;

ALTER TABLE booking_seats
DROP COLUMN seat_class_at_booking;
