-- V9.0.0 — Update bookings.status CHECK constraint: replace PENDING with HELD
--
-- Replaces the old constraint that allowed 'PENDING' with a new one that requires 'HELD'.
-- Also migrates any existing PENDING rows to HELD.
-- STEP 1: Update existing PENDING rows to HELD (before changing constraint)
UPDATE bookings
SET
    status = 'HELD'
WHERE
    status = 'PENDING';

-- STEP 2: Drop the old unique active booking index that referenced PENDING/PAID/CONFIRMED
DROP INDEX IF EXISTS idx_unique_active_booking;

-- STEP 3: Drop the old status CHECK constraint (if it exists with a known name)
ALTER TABLE bookings
DROP CONSTRAINT IF EXISTS chk_booking_status;

-- Also drop unnamed constraints by column — Postgres won't let us reference unnamed ones directly.
-- We use DO block to find and drop any existing CHECK on status column.
DO $$
    DECLARE
        v_constraint_name TEXT;
    BEGIN
        SELECT con.conname
        INTO v_constraint_name
        FROM pg_constraint con
                 JOIN pg_class rel ON rel.oid = con.conrelid
                 JOIN pg_namespace nsp ON nsp.oid = rel.relnamespace
        WHERE nsp.nspname = 'public'
          AND rel.relname = 'bookings'
          AND con.contype = 'c'
          AND pg_get_constraintdef(con.oid) LIKE '%status%';

        IF v_constraint_name IS NOT NULL THEN
            EXECUTE format('ALTER TABLE bookings DROP CONSTRAINT %I', v_constraint_name);
        END IF;
    END
$$;

-- STEP 4: Add new status CHECK constraint with HELD instead of PENDING
ALTER TABLE bookings
ADD CONSTRAINT chk_booking_status CHECK (status IN ('HELD', 'CONFIRMED', 'CANCELLED'));
