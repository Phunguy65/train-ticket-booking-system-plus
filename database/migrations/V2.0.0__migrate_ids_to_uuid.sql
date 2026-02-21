-- ============================================================
-- V2.0.0 — Migrate all primary keys and foreign keys from
-- BIGSERIAL/BIGINT to UUID (using uuidv7() for monotonic ordering).
--
-- Strategy: clean drop-and-recreate (database is empty in dev).
-- Migration order follows FK dependency chain:
--   Round 1: users, stations, trains   (no FK dependencies)
--   Round 2: routes, seats             (FK → Round 1)
--   Round 3: bookings                  (FK → Round 2)
--   Round 4: transactions              (FK → Round 3)
-- ============================================================
-- ============================================================
-- STEP 1: Drop all FK constraints
-- (must be done before altering referenced columns)
-- ============================================================
-- routes FK constraints (inline-defined in V1, must drop by generated name)
ALTER TABLE routes
DROP CONSTRAINT IF EXISTS routes_train_id_fkey;

ALTER TABLE routes
DROP CONSTRAINT IF EXISTS routes_origin_station_id_fkey;

ALTER TABLE routes
DROP CONSTRAINT IF EXISTS routes_destination_station_id_fkey;

-- seats FK constraints
ALTER TABLE seats
DROP CONSTRAINT IF EXISTS seats_train_id_fkey;

-- bookings FK constraints
ALTER TABLE bookings
DROP CONSTRAINT IF EXISTS bookings_user_id_fkey;

ALTER TABLE bookings
DROP CONSTRAINT IF EXISTS bookings_route_id_fkey;

ALTER TABLE bookings
DROP CONSTRAINT IF EXISTS bookings_seat_id_fkey;

-- transactions FK constraints
ALTER TABLE transactions
DROP CONSTRAINT IF EXISTS transactions_booking_id_fkey;

-- ============================================================
-- STEP 2: Drop all related indexes
-- ============================================================
DROP INDEX IF EXISTS idx_seats_train_status;

DROP INDEX IF EXISTS idx_bookings_user;

DROP INDEX IF EXISTS idx_bookings_route;

DROP INDEX IF EXISTS idx_routes_train;

DROP INDEX IF EXISTS idx_routes_departure;

DROP INDEX IF EXISTS idx_unique_active_booking;

-- ============================================================
-- STEP 3: Round 1 — Migrate independent tables
-- users, stations, trains (no FK columns)
-- ============================================================
-- users
ALTER TABLE users
ALTER COLUMN id
DROP DEFAULT,
ALTER COLUMN id TYPE UUID USING gen_random_uuid(),
ALTER COLUMN id
SET DEFAULT uuidv7();

ALTER TABLE users
DROP CONSTRAINT IF EXISTS users_pkey;

ALTER TABLE users
ADD PRIMARY KEY (id);

-- stations
ALTER TABLE stations
ALTER COLUMN id
DROP DEFAULT,
ALTER COLUMN id TYPE UUID USING gen_random_uuid(),
ALTER COLUMN id
SET DEFAULT uuidv7();

ALTER TABLE stations
DROP CONSTRAINT IF EXISTS stations_pkey;

ALTER TABLE stations
ADD PRIMARY KEY (id);

-- trains
ALTER TABLE trains
ALTER COLUMN id
DROP DEFAULT,
ALTER COLUMN id TYPE UUID USING gen_random_uuid(),
ALTER COLUMN id
SET DEFAULT uuidv7();

ALTER TABLE trains
DROP CONSTRAINT IF EXISTS trains_pkey;

ALTER TABLE trains
ADD PRIMARY KEY (id);

-- ============================================================
-- STEP 4: Round 2 — Migrate routes and seats
-- (FK columns: train_id, origin_station_id, destination_station_id)
-- ============================================================
-- routes: PK
ALTER TABLE routes
ALTER COLUMN id
DROP DEFAULT,
ALTER COLUMN id TYPE UUID USING gen_random_uuid(),
ALTER COLUMN id
SET DEFAULT uuidv7();

ALTER TABLE routes
DROP CONSTRAINT IF EXISTS routes_pkey;

ALTER TABLE routes
ADD PRIMARY KEY (id);

-- routes: FK columns
ALTER TABLE routes
ALTER COLUMN train_id TYPE UUID USING NULL::UUID,
ALTER COLUMN origin_station_id TYPE UUID USING NULL::UUID,
ALTER COLUMN destination_station_id TYPE UUID USING NULL::UUID;

-- seats: PK
ALTER TABLE seats
ALTER COLUMN id
DROP DEFAULT,
ALTER COLUMN id TYPE UUID USING gen_random_uuid(),
ALTER COLUMN id
SET DEFAULT uuidv7();

ALTER TABLE seats
DROP CONSTRAINT IF EXISTS seats_pkey;

ALTER TABLE seats
ADD PRIMARY KEY (id);

-- seats: FK column
ALTER TABLE seats
ALTER COLUMN train_id TYPE UUID USING NULL::UUID;

-- ============================================================
-- STEP 5: Round 3 — Migrate bookings
-- (FK columns: user_id, route_id, seat_id)
-- ============================================================
-- bookings: PK
ALTER TABLE bookings
ALTER COLUMN id
DROP DEFAULT,
ALTER COLUMN id TYPE UUID USING gen_random_uuid(),
ALTER COLUMN id
SET DEFAULT uuidv7();

ALTER TABLE bookings
DROP CONSTRAINT IF EXISTS bookings_pkey;

ALTER TABLE bookings
ADD PRIMARY KEY (id);

-- bookings: FK columns
ALTER TABLE bookings
ALTER COLUMN user_id TYPE UUID USING NULL::UUID,
ALTER COLUMN route_id TYPE UUID USING NULL::UUID,
ALTER COLUMN seat_id TYPE UUID USING NULL::UUID;

-- ============================================================
-- STEP 6: Round 4 — Migrate transactions
-- (FK column: booking_id)
-- ============================================================
-- transactions: PK
ALTER TABLE transactions
ALTER COLUMN id
DROP DEFAULT,
ALTER COLUMN id TYPE UUID USING gen_random_uuid(),
ALTER COLUMN id
SET DEFAULT uuidv7();

ALTER TABLE transactions
DROP CONSTRAINT IF EXISTS transactions_pkey;

ALTER TABLE transactions
ADD PRIMARY KEY (id);

-- transactions: FK column
ALTER TABLE transactions
ALTER COLUMN booking_id TYPE UUID USING NULL::UUID;

-- ============================================================
-- STEP 7: Recreate all FK constraints (now UUID → UUID)
-- ============================================================
ALTER TABLE routes
ADD CONSTRAINT routes_train_id_fkey FOREIGN KEY (train_id) REFERENCES trains (id),
ADD CONSTRAINT routes_origin_station_id_fkey FOREIGN KEY (origin_station_id) REFERENCES stations (id),
ADD CONSTRAINT routes_destination_station_id_fkey FOREIGN KEY (destination_station_id) REFERENCES stations (id);

ALTER TABLE seats
ADD CONSTRAINT seats_train_id_fkey FOREIGN KEY (train_id) REFERENCES trains (id);

ALTER TABLE bookings
ADD CONSTRAINT bookings_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id),
ADD CONSTRAINT bookings_route_id_fkey FOREIGN KEY (route_id) REFERENCES routes (id),
ADD CONSTRAINT bookings_seat_id_fkey FOREIGN KEY (seat_id) REFERENCES seats (id);

ALTER TABLE transactions
ADD CONSTRAINT transactions_booking_id_fkey FOREIGN KEY (booking_id) REFERENCES bookings (id);

-- ============================================================
-- STEP 8: Recreate all indexes on UUID columns
-- ============================================================
CREATE INDEX idx_seats_train_status ON seats (train_id, status);

CREATE INDEX idx_bookings_user ON bookings (user_id);

CREATE INDEX idx_routes_train ON routes (train_id);

CREATE INDEX idx_routes_departure ON routes (departure_time);

CREATE UNIQUE INDEX idx_unique_active_booking ON bookings (route_id, seat_id)
WHERE
    status IN ('PENDING', 'PAID', 'CONFIRMED');
