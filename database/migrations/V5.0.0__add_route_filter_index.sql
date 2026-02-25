-- V5.0.0 — Add composite index to support route filter queries
--
-- Supports the most common query pattern: search by origin + destination + departure date range.
-- Used by GET /routes with optional filter parameters.
CREATE INDEX idx_routes_origin_dest_departure ON routes (
    origin_station_id,
    destination_station_id,
    departure_time
);
