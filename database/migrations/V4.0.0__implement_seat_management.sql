-- ============================================================
-- V4.0.0 – Implement seat management
--
-- 1. Drop the stale idx_seats_train_status index (depends on status column)
-- 2. Drop the unused seats.status column
-- 3. Create route_seat_availability table with optimistic locking
-- ============================================================
-- STEP 1: Drop index that depends on seats.status
DROP INDEX IF EXISTS idx_seats_train_status;

-- STEP 2: Drop the unused global-status column
ALTER TABLE seats
DROP COLUMN IF EXISTS status;

-- STEP 3: Create per-route seat availability table
CREATE TABLE route_seat_availability (
    route_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    version INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pk_route_seat_availability PRIMARY KEY (route_id, seat_id),
    CONSTRAINT fk_rsa_route FOREIGN KEY (route_id) REFERENCES routes (id),
    CONSTRAINT fk_rsa_seat FOREIGN KEY (seat_id) REFERENCES seats (id),
    CONSTRAINT chk_rsa_status CHECK (status IN ('AVAILABLE', 'BOOKED', 'CANCELLED'))
);

-- STEP 4: Performance index for "find available seats for a route" query
CREATE INDEX idx_route_seat_status ON route_seat_availability (route_id, status);
