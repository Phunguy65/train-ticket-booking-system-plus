CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    booking_id BIGINT REFERENCES bookings (id),
    gateway_transaction_id VARCHAR(255),
    amount DECIMAL(10, 2) NOT NULL,
    description TEXT,
    transaction_date TIMESTAMP,
    gateway VARCHAR(50) DEFAULT 'SEPAY',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE bookings
ADD COLUMN payment_deadline TIMESTAMP,
ADD COLUMN payment_code VARCHAR(50);

DROP INDEX IF EXISTS idx_bookings_route;

CREATE UNIQUE INDEX idx_unique_active_booking ON bookings (route_id, seat_id)
WHERE
    status IN ('PENDING', 'PAID', 'CONFIRMED');
