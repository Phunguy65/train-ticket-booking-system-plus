-- Migration: Drop the redundant `version` column from `route_seat_availability`.
--
-- Background: The `version` column was introduced in V4.0.0 to support JPA optimistic locking
-- (@Version). Pessimistic locking (SELECT ... FOR UPDATE NOWAIT) was later added and fully
-- serialises all concurrent writers, making the optimistic version check unreachable dead code.
-- The @Version annotation and field have been removed from the Java model; this migration
-- removes the column from the database schema to complete the cleanup.
ALTER TABLE route_seat_availability
DROP COLUMN version;
