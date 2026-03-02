-- ============================================================
-- V13.0.0 — Add soft-delete support to bookings table
--
-- Adds deleted_at column to bookings to enable soft-delete cascade
-- when a route is soft-deleted (via RouteDeleted domain event).
-- ============================================================
ALTER TABLE bookings
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

COMMENT ON COLUMN bookings.deleted_at IS 'Soft delete timestamp (UTC); NULL = active';

-- Fast filter of active (non-deleted) bookings
CREATE INDEX idx_bookings_deleted_at ON bookings (deleted_at)
WHERE
    deleted_at IS NULL;
