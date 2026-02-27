-- V8.0.0 — Add partial unique index for one active hold per user per route
--
-- Enforces the business rule: a user may not have more than one HELD booking
-- for the same route at the same time.
-- The partial index only applies to rows where status = 'HELD'.
CREATE UNIQUE INDEX idx_one_active_hold_per_user_route ON bookings (user_id, route_id)
WHERE
    status = 'HELD';
