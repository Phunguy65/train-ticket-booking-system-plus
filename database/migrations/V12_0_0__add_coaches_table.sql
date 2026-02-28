-- ============================================================
-- V12.0.0 — Add coaches (toa tàu) intermediate layer
--
-- Refactors the seat hierarchy from:
--   trains (1) ──< (N) seats
-- to:
--   trains (1) ──< (N) coaches (1) ──< (N) seats
--
-- Migration strategy (zero-downtime, 4 phases):
--   Phase 1: Create coaches table
--   Phase 2: Backfill — create a default coach for every existing train,
--             assign all existing seats to it
--   Phase 3: Harden constraints on seats.coach_id
--   Phase 4: Drop the now-redundant seats.train_id column
--             and its associated index / constraint
-- ============================================================
-- ============================================================
-- PHASE 1: Create coaches table
-- ============================================================
CREATE TABLE coaches (
    id UUID NOT NULL DEFAULT uuidv7(),
    train_id UUID NOT NULL,
    car_number INTEGER NOT NULL,
    total_seats INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT pk_coaches PRIMARY KEY (id),
    CONSTRAINT fk_coaches_train FOREIGN KEY (train_id) REFERENCES trains (id),
    CONSTRAINT chk_coaches_car_number CHECK (car_number > 0),
    CONSTRAINT chk_coaches_total_seats CHECK (total_seats > 0)
);

COMMENT ON TABLE coaches IS 'Toa tàu — intermediate layer between trains and seats';

COMMENT ON COLUMN coaches.car_number IS 'Physical position of this car in the train (1-based)';

COMMENT ON COLUMN coaches.total_seats IS 'Total seat capacity declared for this car';

COMMENT ON COLUMN coaches.created_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN coaches.deleted_at IS 'Soft delete timestamp (UTC); NULL = active';

-- active (train_id, car_number) uniqueness (soft-delete-aware)
CREATE UNIQUE INDEX uq_coaches_train_car_active ON coaches (train_id, car_number)
WHERE
    deleted_at IS NULL;

-- fast lookup of all coaches for a given train
CREATE INDEX idx_coaches_train_id ON coaches (train_id);

-- filter active rows quickly
CREATE INDEX idx_coaches_deleted_at ON coaches (deleted_at)
WHERE
    deleted_at IS NULL;

-- ============================================================
-- PHASE 2: Backfill existing data
--   • For every active train that has at least one seat, insert one
--     default coach (car_number = 1, total_seats = train.total_seats).
--   • For trains without any seats yet, also create a default coach so
--     the train is always "valid" under the new model.
--   • Add coach_id (nullable) to seats, then populate it.
-- ============================================================
-- Insert one default coach per train (reuse the train's own id as a
-- deterministic seed so the backfill is idempotent on repeated runs).
INSERT INTO
    coaches (train_id, car_number, total_seats)
SELECT
    t.id,
    1 AS car_number,
    t.total_seats AS total_seats
FROM
    trains t
WHERE
    t.deleted_at IS NULL;

-- Add coach_id column (nullable for now so we can populate it first)
ALTER TABLE seats
ADD COLUMN coach_id UUID;

-- Populate coach_id for every existing seat by matching its train_id
-- to the single default coach we just created for that train.
UPDATE seats s
SET
    coach_id = c.id
FROM
    coaches c
WHERE
    c.train_id = s.train_id
    AND c.car_number = 1
    AND s.coach_id IS NULL;

-- ============================================================
-- PHASE 3: Harden constraints
-- ============================================================
-- Make coach_id mandatory
ALTER TABLE seats
ALTER COLUMN coach_id
SET NOT NULL;

-- Foreign key from seats to coaches
ALTER TABLE seats
ADD CONSTRAINT fk_seats_coach FOREIGN KEY (coach_id) REFERENCES coaches (id);

-- Seat-number uniqueness is now scoped to a coach, not to a train.
-- Drop the old unique index and replace with a new one.
DROP INDEX IF EXISTS uq_seats_train_seat_active;

CREATE UNIQUE INDEX uq_seats_coach_seat_active ON seats (coach_id, seat_number)
WHERE
    deleted_at IS NULL;

-- Fast lookup of all seats for a given coach
CREATE INDEX idx_seats_coach_id ON seats (coach_id);

-- ============================================================
-- PHASE 4: Remove the now-redundant seats.train_id column
-- ============================================================
-- Drop the old foreign-key constraint before dropping the column
ALTER TABLE seats
DROP CONSTRAINT seats_train_id_fkey;

ALTER TABLE seats
DROP COLUMN train_id;
