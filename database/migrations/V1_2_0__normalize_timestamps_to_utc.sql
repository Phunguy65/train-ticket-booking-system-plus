-- Normalize all timestamp columns to TIMESTAMPTZ (UTC)
-- This migration converts all TIMESTAMP columns to TIMESTAMPTZ to ensure timezone-aware storage
-- Users table
ALTER TABLE users
ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
ALTER COLUMN created_at
SET DEFAULT CURRENT_TIMESTAMP,
ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC',
ALTER COLUMN updated_at
SET DEFAULT CURRENT_TIMESTAMP;

-- Stations table
ALTER TABLE stations
ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
ALTER COLUMN created_at
SET DEFAULT CURRENT_TIMESTAMP;

-- Trains table
ALTER TABLE trains
ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
ALTER COLUMN created_at
SET DEFAULT CURRENT_TIMESTAMP;

-- Routes table
ALTER TABLE routes
ALTER COLUMN departure_time TYPE TIMESTAMPTZ USING departure_time AT TIME ZONE 'UTC',
ALTER COLUMN arrival_time TYPE TIMESTAMPTZ USING arrival_time AT TIME ZONE 'UTC',
ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
ALTER COLUMN created_at
SET DEFAULT CURRENT_TIMESTAMP;

-- Seats table
ALTER TABLE seats
ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
ALTER COLUMN created_at
SET DEFAULT CURRENT_TIMESTAMP;

-- Bookings table
ALTER TABLE bookings
ALTER COLUMN payment_deadline TYPE TIMESTAMPTZ USING payment_deadline AT TIME ZONE 'UTC',
ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
ALTER COLUMN created_at
SET DEFAULT CURRENT_TIMESTAMP,
ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC',
ALTER COLUMN updated_at
SET DEFAULT CURRENT_TIMESTAMP;

-- Transactions table
ALTER TABLE transactions
ALTER COLUMN transaction_date TYPE TIMESTAMPTZ USING transaction_date AT TIME ZONE 'UTC',
ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
ALTER COLUMN created_at
SET DEFAULT CURRENT_TIMESTAMP;

-- Comment to document the change
COMMENT ON COLUMN users.created_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN users.updated_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN stations.created_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN trains.created_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN routes.departure_time IS 'Departure time with timezone (UTC)';

COMMENT ON COLUMN routes.arrival_time IS 'Arrival time with timezone (UTC)';

COMMENT ON COLUMN routes.created_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN seats.created_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN bookings.payment_deadline IS 'Payment deadline with timezone (UTC)';

COMMENT ON COLUMN bookings.created_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN bookings.updated_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN transactions.transaction_date IS 'Transaction date with timezone (UTC)';

COMMENT ON COLUMN transactions.created_at IS 'Timestamp with timezone (UTC)';
