-- V7.0.0 — Create booking_seats table and migrate seat data
--
-- 1. Create booking_seats join table with composite PK and price snapshot columns
-- 2. Add an index on bookings(status, payment_deadline) for expiry polling
-- 3. Migrate existing bookings.seat_id rows into booking_seats
--    (sets price_at_booking = bookings.total_price, seat_class_at_booking from seats table)
-- 4. Drop bookings.seat_id column
-- STEP 1: Create booking_seats table
CREATE TABLE booking_seats (
    booking_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    price_at_booking DECIMAL(10, 2) NOT NULL,
    seat_class_at_booking VARCHAR(20) NOT NULL,
    CONSTRAINT pk_booking_seats PRIMARY KEY (booking_id, seat_id),
    CONSTRAINT fk_bs_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_bs_seat FOREIGN KEY (seat_id) REFERENCES seats (id)
);

-- STEP 2: Add index for hold expiry polling
CREATE INDEX idx_bookings_status_deadline ON bookings (status, payment_deadline);

-- STEP 3: Migrate existing bookings.seat_id rows into booking_seats
-- Uses the seat's class from the seats table; falls back to 'ECONOMY' if not found
INSERT INTO
    booking_seats (
        booking_id,
        seat_id,
        price_at_booking,
        seat_class_at_booking
    )
SELECT
    b.id AS booking_id,
    b.seat_id AS seat_id,
    b.total_price AS price_at_booking,
    COALESCE(s.seat_class, 'ECONOMY') AS seat_class_at_booking
FROM
    bookings b
    LEFT JOIN seats s ON s.id = b.seat_id
WHERE
    b.seat_id IS NOT NULL;

-- STEP 4: Drop bookings.seat_id column (after migrating data)
ALTER TABLE bookings
DROP CONSTRAINT IF EXISTS bookings_seat_id_fkey;

ALTER TABLE bookings
DROP COLUMN IF EXISTS seat_id;
