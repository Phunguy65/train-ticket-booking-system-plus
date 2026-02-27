-- V6.0.0 — Add HELD status to route_seat_availability
--
-- Updates the CHECK constraint on route_seat_availability.status to include 'HELD',
-- enabling the two-phase seat hold flow.
ALTER TABLE route_seat_availability
DROP CONSTRAINT IF EXISTS chk_rsa_status;

ALTER TABLE route_seat_availability
ADD CONSTRAINT chk_rsa_status CHECK (
    status IN ('AVAILABLE', 'HELD', 'BOOKED', 'CANCELLED')
);
