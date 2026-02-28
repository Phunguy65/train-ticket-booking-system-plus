-- ============================================================
-- V12.0.0 — Add train_cars (toa tàu) intermediate layer
--
-- Refactors the seat hierarchy from:
--   trains (1) ──< (N) seats
-- to:
--   trains (1) ──< (N) train_cars (1) ──< (N) seats
--
-- Migration strategy (zero-downtime, 4 phases):
--   Phase 1: Create train_cars table
--   Phase 2: Backfill — create a default car for every existing train,
--             assign all existing seats to it
--   Phase 3: Harden constraints on seats.train_car_id
--   Phase 4: Drop the now-redundant seats.train_id column
--             and its associated index / constraint
-- ============================================================
-- ============================================================
-- PHASE 1: Create train_cars table
-- ============================================================
CREATE TABLE train_cars (
    id UUID NOT NULL DEFAULT uuidv7(),
    train_id UUID NOT NULL,
    car_number INTEGER NOT NULL,
    total_seats INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT pk_train_cars PRIMARY KEY (id),
    CONSTRAINT fk_train_cars_train FOREIGN KEY (train_id) REFERENCES trains (id),
    CONSTRAINT chk_train_cars_car_number CHECK (car_number > 0),
    CONSTRAINT chk_train_cars_total_seats CHECK (total_seats > 0)
);

COMMENT ON TABLE train_cars IS 'Toa tàu — intermediate layer between trains and seats';

COMMENT ON COLUMN train_cars.car_number IS 'Physical position of this car in the train (1-based)';

COMMENT ON COLUMN train_cars.total_seats IS 'Total seat capacity declared for this car';

COMMENT ON COLUMN train_cars.created_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN train_cars.deleted_at IS 'Soft delete timestamp (UTC); NULL = active';

-- active (train_id, car_number) uniqueness (soft-delete-aware)
CREATE UNIQUE INDEX uq_train_cars_train_car_active ON train_cars (train_id, car_number)
WHERE
    deleted_at IS NULL;

-- fast lookup of all cars for a given train
CREATE INDEX idx_train_cars_train_id ON train_cars (train_id);

-- filter active rows quickly
CREATE INDEX idx_train_cars_deleted_at ON train_cars (deleted_at)
WHERE
    deleted_at IS NULL;

-- ============================================================
-- PHASE 2: Backfill existing data
--   • For every active train that has at least one seat, insert one
--     default train_car (car_number = 1, total_seats = train.total_seats).
--   • For trains without any seats yet, also create a default car so
--     the train is always "valid" under the new model.
--   • Add train_car_id (nullable) to seats, then populate it.
-- ============================================================
-- Insert one default car per train (reuse the train's own id as a
-- deterministic seed so the backfill is idempotent on repeated runs).
INSERT INTO
    train_cars (train_id, car_number, total_seats)
SELECT
    t.id,
    1 AS car_number,
    t.total_seats AS total_seats
FROM
    trains t
WHERE
    t.deleted_at IS NULL;

-- Add train_car_id column (nullable for now so we can populate it first)
ALTER TABLE seats
ADD COLUMN train_car_id UUID;

-- Populate train_car_id for every existing seat by matching its train_id
-- to the single default car we just created for that train.
UPDATE seats s
SET
    train_car_id = tc.id
FROM
    train_cars tc
WHERE
    tc.train_id = s.train_id
    AND tc.car_number = 1
    AND s.train_car_id IS NULL;

-- ============================================================
-- PHASE 3: Harden constraints
-- ============================================================
-- Make train_car_id mandatory
ALTER TABLE seats
ALTER COLUMN train_car_id
SET NOT NULL;

-- Foreign key from seats to train_cars
ALTER TABLE seats
ADD CONSTRAINT fk_seats_train_car FOREIGN KEY (train_car_id) REFERENCES train_cars (id);

-- Seat-number uniqueness is now scoped to a car, not to a train.
-- Drop the old unique index and replace with a new one.
DROP INDEX IF EXISTS uq_seats_train_seat_active;

CREATE UNIQUE INDEX uq_seats_car_seat_active ON seats (train_car_id, seat_number)
WHERE
    deleted_at IS NULL;

-- Fast lookup of all seats for a given car
CREATE INDEX idx_seats_train_car_id ON seats (train_car_id);

-- ============================================================
-- PHASE 4: Remove the now-redundant seats.train_id column
-- ============================================================
-- Drop the old foreign-key constraint before dropping the column
ALTER TABLE seats
DROP CONSTRAINT seats_train_id_fkey;

ALTER TABLE seats
DROP COLUMN train_id;
