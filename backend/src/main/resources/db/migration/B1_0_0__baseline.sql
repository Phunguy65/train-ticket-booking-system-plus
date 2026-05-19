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
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT chk_users_role CHECK (role IN ('CUSTOMER', 'ADMIN'))
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
    total_seats INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT pk_trains PRIMARY KEY (id)
);

COMMENT ON COLUMN trains.created_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN trains.deleted_at IS 'Soft delete timestamp (UTC); NULL = active';

-- ============================================================
-- TABLE: coaches
-- ============================================================
CREATE TABLE coaches (
    id UUID NOT NULL DEFAULT uuidv7(),
    train_id UUID NOT NULL,
    car_number INTEGER NOT NULL,
    total_seats INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT pk_coaches PRIMARY KEY (id),
    CONSTRAINT fk_coaches_train FOREIGN KEY (train_id) REFERENCES trains (id),
    CONSTRAINT chk_coaches_car_number CHECK (car_number > 0)
);

COMMENT ON TABLE coaches IS 'Toa tàu — intermediate layer between trains and seats';

COMMENT ON COLUMN coaches.car_number IS 'Physical position of this car in the train (1-based)';

COMMENT ON COLUMN coaches.total_seats IS 'Computed seat count — auto-updated by event listener when seats are created/deleted';

COMMENT ON COLUMN coaches.created_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN coaches.deleted_at IS 'Soft delete timestamp (UTC); NULL = active';

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
    base_price BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SCHEDULED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT pk_routes PRIMARY KEY (id),
    CONSTRAINT routes_train_id_fkey FOREIGN KEY (train_id) REFERENCES trains (id),
    CONSTRAINT routes_origin_station_id_fkey FOREIGN KEY (origin_station_id) REFERENCES stations (id),
    CONSTRAINT routes_destination_station_id_fkey FOREIGN KEY (destination_station_id) REFERENCES stations (id),
    CONSTRAINT chk_routes_status CHECK (
        status IN (
            'SCHEDULED',
            'BOARDING',
            'DEPARTED',
            'ARRIVED',
            'CANCELLED'
        )
    ),
    CONSTRAINT chk_routes_times CHECK (arrival_time > departure_time)
);

COMMENT ON COLUMN routes.departure_time IS 'Departure time with timezone (UTC)';

COMMENT ON COLUMN routes.arrival_time IS 'Arrival time with timezone (UTC)';

COMMENT ON COLUMN routes.created_at IS 'Timestamp with timezone (UTC)';

COMMENT ON COLUMN routes.deleted_at IS 'Soft delete timestamp (UTC); NULL = active';

COMMENT ON COLUMN routes.base_price IS 'Stored in lowest denomination (e.g., pure VND or USD cents)';

-- ============================================================
-- TABLE: seats
-- ============================================================
CREATE TABLE seats (
    id UUID NOT NULL DEFAULT uuidv7(),
    coach_id UUID NOT NULL,
    seat_number VARCHAR(10) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT pk_seats PRIMARY KEY (id),
    CONSTRAINT fk_seats_coach FOREIGN KEY (coach_id) REFERENCES coaches (id)
);

-- ============================================================
-- TABLE: bookings
-- ============================================================
CREATE TABLE bookings (
    id UUID NOT NULL DEFAULT uuidv7(),
    user_id UUID NOT NULL,
    route_id UUID NOT NULL,
    passenger_name VARCHAR(255) NOT NULL,
    passenger_email VARCHAR(255) NOT NULL,
    passenger_phone VARCHAR(20),
    total_price BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL,
    idempotency_key VARCHAR(255),
    payment_deadline TIMESTAMPTZ,
    payment_reference VARCHAR(255),
    checkout_session_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_bookings PRIMARY KEY (id),
    CONSTRAINT uq_bookings_idempotency UNIQUE (idempotency_key),
    CONSTRAINT bookings_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT bookings_route_id_fkey FOREIGN KEY (route_id) REFERENCES routes (id),
    CONSTRAINT chk_booking_status CHECK (status IN ('HELD', 'CONFIRMED', 'CANCELLED'))
);

COMMENT ON COLUMN bookings.payment_deadline IS 'Payment deadline with timezone (UTC)';

COMMENT ON COLUMN bookings.created_at IS 'Timestamp with timezone (UTC)';

-- ============================================================
-- TABLE: route_seat_availability
-- Core table for concurrency, locking, and seating assignment
-- ============================================================
CREATE TABLE route_seat_availability (
    route_id UUID NOT NULL,
    seat_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    booking_id UUID,
    price_at_booking BIGINT,
    version INTEGER NOT NULL DEFAULT 1,
    CONSTRAINT pk_route_seat_availability PRIMARY KEY (route_id, seat_id),
    CONSTRAINT fk_rsa_route FOREIGN KEY (route_id) REFERENCES routes (id),
    CONSTRAINT fk_rsa_seat FOREIGN KEY (seat_id) REFERENCES seats (id),
    CONSTRAINT fk_rsa_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT chk_rsa_status CHECK (
        status IN ('AVAILABLE', 'HELD', 'BOOKED', 'CANCELLED')
    )
);

-- ============================================================
-- TABLE: refresh_tokens
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

-- ============================================================
-- TABLE: payments
-- ============================================================
CREATE TABLE payments (
    id UUID NOT NULL DEFAULT uuidv7(),
    booking_id UUID NOT NULL,
    checkout_session_id VARCHAR(255) NOT NULL,
    stripe_event_id VARCHAR(255),
    amount BIGINT NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'VND',
    status VARCHAR(20) NOT NULL,
    stripe_payment_intent_id VARCHAR(255),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT uq_payments_checkout_session_id UNIQUE (checkout_session_id),
    CONSTRAINT uq_payments_stripe_event_id UNIQUE (stripe_event_id),
    CONSTRAINT uq_payments_stripe_payment_intent_id UNIQUE (stripe_payment_intent_id),
    CONSTRAINT chk_payments_status CHECK (
        status IN (
            'PENDING',
            'PAID',
            'CANCELLED',
            'FAILED',
            'REFUNDED'
        )
    )
);

COMMENT ON TABLE payments IS 'Stripe Checkout Session payment records';

COMMENT ON COLUMN payments.checkout_session_id IS 'Stripe Checkout Session ID (cs_...)';

COMMENT ON COLUMN payments.stripe_event_id IS 'Stripe webhook event ID for idempotency';

COMMENT ON COLUMN payments.status IS 'Payment lifecycle: PENDING → PAID or CANCELLED';

-- ============================================================
-- INDEXES
-- ============================================================
-- Users
CREATE UNIQUE INDEX uq_users_email_active ON users (email)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_users_deleted_at ON users (deleted_at)
WHERE
    deleted_at IS NULL;

-- Stations
CREATE UNIQUE INDEX uq_stations_code_active ON stations (code)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_stations_deleted_at ON stations (deleted_at)
WHERE
    deleted_at IS NULL;

-- Trains
CREATE UNIQUE INDEX uq_trains_train_number_active ON trains (train_number)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_trains_deleted_at ON trains (deleted_at)
WHERE
    deleted_at IS NULL;

-- Coaches
CREATE UNIQUE INDEX uq_coaches_train_car_active ON coaches (train_id, car_number)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_coaches_train_id ON coaches (train_id);

CREATE INDEX idx_coaches_deleted_at ON coaches (deleted_at)
WHERE
    deleted_at IS NULL;

-- Routes
CREATE INDEX idx_routes_deleted_at ON routes (deleted_at)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_routes_train ON routes (train_id);

CREATE INDEX idx_routes_departure ON routes (departure_time);

CREATE INDEX idx_routes_origin_dest_departure ON routes (
    origin_station_id,
    destination_station_id,
    departure_time
);

CREATE INDEX idx_routes_origin_station ON routes (origin_station_id);

CREATE INDEX idx_routes_destination_station ON routes (destination_station_id);

-- Seats
CREATE UNIQUE INDEX uq_seats_coach_seat_active ON seats (coach_id, seat_number)
WHERE
    deleted_at IS NULL;

CREATE INDEX idx_seats_coach_id ON seats (coach_id);

CREATE INDEX idx_seats_deleted_at ON seats (deleted_at)
WHERE
    deleted_at IS NULL;

-- Bookings
CREATE INDEX idx_bookings_user ON bookings (user_id);

CREATE INDEX idx_bookings_route ON bookings (route_id);

CREATE INDEX idx_bookings_status_deadline ON bookings (status, payment_deadline);

CREATE UNIQUE INDEX idx_one_active_hold_per_user_route ON bookings (user_id, route_id)
WHERE
    status = 'HELD';

CREATE INDEX idx_bookings_checkout_session_id ON bookings (checkout_session_id)
WHERE
    checkout_session_id IS NOT NULL;

-- Route Seat Availability
CREATE INDEX idx_route_seat_status ON route_seat_availability (route_id, status);

CREATE INDEX idx_route_seat_seat_id ON route_seat_availability (seat_id);

CREATE INDEX idx_route_seat_booking_id ON route_seat_availability (booking_id)
WHERE
    booking_id IS NOT NULL;

-- Refresh Tokens
CREATE UNIQUE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

CREATE INDEX idx_refresh_tokens_user_revoked ON refresh_tokens (user_id, revoked_at);

-- Payments
CREATE INDEX idx_payments_checkout_session_id ON payments (checkout_session_id);

CREATE INDEX idx_payments_booking_id ON payments (booking_id);
