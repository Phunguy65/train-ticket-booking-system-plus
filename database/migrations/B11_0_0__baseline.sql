-- ============================================================
-- V11.0.0 — B2.0.0 Baseline Schema
--
-- Complete consolidated schema representing the final state after
-- all migrations V1.0.0 through V12.0.0. This is the authoritative
-- starting point for new database instances.
--
-- Key design decisions captured here:
--   - UUID primary keys using uuidv7() (monotonic ordering for B-tree perf)
--   - All timestamps are TIMESTAMPTZ (timezone-aware, UTC)
--   - Per-route seat availability (not global seat status)
--   - Two-phase booking: HELD → CONFIRMED (no PENDING status)
--   - Multi-seat bookings via booking_seats join table
--   - Unified pricing: all seats share route.base_price (no seat class multiplier)
--   - JWT refresh token revocation tracking
--   - Soft delete via deleted_at TIMESTAMPTZ (NULL = active) on all
--     master-data and operational tables; follows refresh_tokens.revoked_at
--     pattern — manual WHERE deleted_at IS NULL filtering in application queries
--
-- For existing databases (already at V11.0.0):
--   Run: flyway migrate  (applies V12.0.0 automatically)
-- ============================================================
-- ============================================================
-- TABLE: users
-- ============================================================
CREATE TABLE users (
    id UUID NOT NULL DEFAULT uuidv7(),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

COMMENT ON COLUMN users.created_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN users.updated_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN users.deleted_at IS 'Soft delete timestamp (UTC); NULL = active';

-- ============================================================
-- TABLE: stations
-- ============================================================
CREATE TABLE stations (
    id UUID NOT NULL DEFAULT uuidv7(),
    code VARCHAR(10) NOT NULL,
    name VARCHAR(255) NOT NULL,
    city VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT pk_stations PRIMARY KEY (id)
);

COMMENT ON COLUMN stations.created_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN stations.deleted_at IS 'Soft delete timestamp (UTC); NULL = active';

-- ============================================================
-- TABLE: trains
-- ============================================================
CREATE TABLE trains (
    id UUID NOT NULL DEFAULT uuidv7(),
    train_number VARCHAR(20) NOT NULL,
    name VARCHAR(255) NOT NULL,
    total_seats INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT pk_trains PRIMARY KEY (id)
);

COMMENT ON COLUMN trains.created_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN trains.deleted_at IS 'Soft delete timestamp (UTC); NULL = active';

-- ============================================================
-- TABLE: routes
-- ============================================================
CREATE TABLE routes (
    id UUID NOT NULL DEFAULT uuidv7(),
    train_id UUID NOT NULL,
    origin_station_id UUID NOT NULL,
    destination_station_id UUID NOT NULL,
    departure_time TIMESTAMPTZ NOT NULL,
    arrival_time TIMESTAMPTZ NOT NULL,
    base_price DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT pk_routes PRIMARY KEY (id),
    CONSTRAINT routes_train_id_fkey FOREIGN KEY (train_id) REFERENCES trains (id),
    CONSTRAINT routes_origin_station_id_fkey FOREIGN KEY (origin_station_id) REFERENCES stations (id),
    CONSTRAINT routes_destination_station_id_fkey FOREIGN KEY (destination_station_id) REFERENCES stations (id)
);

COMMENT ON COLUMN routes.departure_time IS 'Departure time with timezone (UTC)';

COMMENT ON COLUMN routes.arrival_time IS 'Arrival time with timezone (UTC)';

COMMENT ON COLUMN routes.created_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN routes.deleted_at IS 'Soft delete timestamp (UTC); NULL = active';

-- ============================================================
-- TABLE: seats
-- Dropped columns (not present in final state):
--   - status    (V1.0.0) → dropped V4.0.0: moved to route_seat_availability
--   - seat_class (V1.0.0) → dropped V8.1.0: unified pricing, no multiplier
-- ============================================================
CREATE TABLE seats (
    id UUID NOT NULL DEFAULT uuidv7(),
    train_id UUID NOT NULL,
    seat_number VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT pk_seats PRIMARY KEY (id),
    CONSTRAINT seats_train_id_fkey FOREIGN KEY (train_id) REFERENCES trains (id)
);

COMMENT ON COLUMN seats.created_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN seats.deleted_at IS 'Soft delete timestamp (UTC); NULL = active';

-- ============================================================
-- TABLE: bookings
-- Dropped columns (not present in final state):
--   - seat_id (V1.0.0) → dropped V7.0.0: replaced by booking_seats join table
-- ============================================================
CREATE TABLE bookings (
    id UUID NOT NULL DEFAULT uuidv7(),
    user_id UUID NOT NULL,
    route_id UUID NOT NULL,
    booking_reference VARCHAR(50) NOT NULL,
    passenger_name VARCHAR(255) NOT NULL,
    passenger_email VARCHAR(255) NOT NULL,
    passenger_phone VARCHAR(20),
    total_price DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    idempotency_key VARCHAR(255),
    payment_deadline TIMESTAMPTZ,
    payment_code VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT pk_bookings PRIMARY KEY (id),
    CONSTRAINT uq_bookings_reference UNIQUE (booking_reference),
    CONSTRAINT uq_bookings_idempotency UNIQUE (idempotency_key),
    CONSTRAINT bookings_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT bookings_route_id_fkey FOREIGN KEY (route_id) REFERENCES routes (id),
    CONSTRAINT chk_booking_status CHECK (status IN ('HELD', 'CONFIRMED', 'CANCELLED'))
);

COMMENT ON COLUMN bookings.payment_deadline IS 'Payment deadline with timezone (UTC)';

COMMENT ON COLUMN bookings.created_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN bookings.updated_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN bookings.deleted_at IS 'Soft delete timestamp (UTC); NULL = active';

-- ============================================================
-- TABLE: booking_seats
-- Join table for multi-seat bookings with price snapshot.
-- Dropped columns (not present in final state):
--   - seat_class_at_booking (V7.0.0) → dropped V8.1.0: unified pricing
-- ============================================================
CREATE TABLE booking_seats (
    booking_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    price_at_booking DECIMAL(10, 2) NOT NULL,
    CONSTRAINT pk_booking_seats PRIMARY KEY (booking_id, seat_id),
    CONSTRAINT fk_bs_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT fk_bs_seat FOREIGN KEY (seat_id) REFERENCES seats (id)
);

-- ============================================================
-- TABLE: transactions
-- ============================================================
CREATE TABLE transactions (
    id UUID NOT NULL DEFAULT uuidv7(),
    booking_id UUID,
    gateway_transaction_id VARCHAR(255),
    amount DECIMAL(10, 2) NOT NULL,
    description TEXT,
    transaction_date TIMESTAMPTZ,
    gateway VARCHAR(50) DEFAULT 'SEPAY',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_transactions PRIMARY KEY (id),
    CONSTRAINT transactions_booking_id_fkey FOREIGN KEY (booking_id) REFERENCES bookings (id)
);

COMMENT ON COLUMN transactions.transaction_date IS 'Transaction date with timezone (UTC)';

COMMENT ON COLUMN transactions.created_at IS 'Timestamp with timezone (UTC)';

-- ============================================================
-- TABLE: route_seat_availability
-- Per-route seat availability tracking (replaces global seats.status).
-- Dropped columns (not present in final state):
--   - version (V4.0.0) → dropped V10.0.0: pessimistic locking made
--     optimistic version check unreachable dead code
-- ============================================================
CREATE TABLE route_seat_availability (
    route_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    CONSTRAINT pk_route_seat_availability PRIMARY KEY (route_id, seat_id),
    CONSTRAINT fk_rsa_route FOREIGN KEY (route_id) REFERENCES routes (id),
    CONSTRAINT fk_rsa_seat FOREIGN KEY (seat_id) REFERENCES seats (id),
    CONSTRAINT chk_rsa_status CHECK (
        status IN ('AVAILABLE', 'HELD', 'BOOKED', 'CANCELLED')
    )
);

-- ============================================================
-- TABLE: refresh_tokens
-- JWT refresh token management with revocation tracking.
-- ============================================================
CREATE TABLE refresh_tokens (
    id UUID NOT NULL DEFAULT uuidv7(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT refresh_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id)
);

COMMENT ON TABLE refresh_tokens IS 'Stores hashed JWT refresh tokens with expiration and revocation tracking';

COMMENT ON COLUMN refresh_tokens.id IS 'Primary key (UUIDv7 for monotonic ordering)';

COMMENT ON COLUMN refresh_tokens.user_id IS 'Foreign key to users table';

COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 hash of raw refresh token (64 hex chars), never store plain token';

COMMENT ON COLUMN refresh_tokens.expires_at IS 'Token expiration timestamp (UTC)';

COMMENT ON COLUMN refresh_tokens.revoked_at IS 'Token revocation timestamp (UTC); NULL means token is still active';

COMMENT ON COLUMN refresh_tokens.created_at IS 'Token creation timestamp (UTC)';

-- ============================================================
-- INDEXES
-- ============================================================
-- users: active e-mail uniqueness (soft-delete-aware)
CREATE UNIQUE INDEX uq_users_email_active ON users (email)
WHERE
    deleted_at IS NULL;

-- users: filter active rows quickly
CREATE INDEX idx_users_deleted_at ON users (deleted_at)
WHERE
    deleted_at IS NULL;

-- stations: active code uniqueness (soft-delete-aware)
CREATE UNIQUE INDEX uq_stations_code_active ON stations (code)
WHERE
    deleted_at IS NULL;

-- stations: filter active rows quickly
CREATE INDEX idx_stations_deleted_at ON stations (deleted_at)
WHERE
    deleted_at IS NULL;

-- trains: active train_number uniqueness (soft-delete-aware)
CREATE UNIQUE INDEX uq_trains_train_number_active ON trains (train_number)
WHERE
    deleted_at IS NULL;

-- trains: filter active rows quickly
CREATE INDEX idx_trains_deleted_at ON trains (deleted_at)
WHERE
    deleted_at IS NULL;

-- routes: filter active rows quickly
CREATE INDEX idx_routes_deleted_at ON routes (deleted_at)
WHERE
    deleted_at IS NULL;

-- seats: active (train_id, seat_number) uniqueness (soft-delete-aware)
CREATE UNIQUE INDEX uq_seats_train_seat_active ON seats (train_id, seat_number)
WHERE
    deleted_at IS NULL;

-- seats: filter active rows quickly
CREATE INDEX idx_seats_deleted_at ON seats (deleted_at)
WHERE
    deleted_at IS NULL;

-- bookings: user lookup
CREATE INDEX idx_bookings_user ON bookings (user_id);

-- bookings: hold expiry polling (status + deadline)
CREATE INDEX idx_bookings_status_deadline ON bookings (status, payment_deadline);

-- bookings: one active hold per user per route (business rule enforcement)
-- Excludes soft-deleted rows so a deleted HELD booking cannot block a new hold.
CREATE UNIQUE INDEX idx_one_active_hold_per_user_route ON bookings (user_id, route_id)
WHERE
    status = 'HELD'
    AND deleted_at IS NULL;

-- bookings: filter active rows quickly
CREATE INDEX idx_bookings_deleted_at ON bookings (deleted_at)
WHERE
    deleted_at IS NULL;

-- routes: train lookup
CREATE INDEX idx_routes_train ON routes (train_id);

-- routes: departure time range queries
CREATE INDEX idx_routes_departure ON routes (departure_time);

-- routes: composite index for search by origin + destination + departure date range
CREATE INDEX idx_routes_origin_dest_departure ON routes (
    origin_station_id,
    destination_station_id,
    departure_time
);

-- route_seat_availability: find available seats for a route
CREATE INDEX idx_route_seat_status ON route_seat_availability (route_id, status);

-- refresh_tokens: fast token lookup by hash (primary query pattern)
CREATE UNIQUE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);

-- refresh_tokens: revoke all tokens for a user
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- refresh_tokens: active token queries (WHERE user_id = ? AND revoked_at IS NULL)
CREATE INDEX idx_refresh_tokens_user_revoked ON refresh_tokens (user_id, revoked_at);
