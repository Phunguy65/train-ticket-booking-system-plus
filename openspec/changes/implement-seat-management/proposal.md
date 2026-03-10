# Why

The `seats` table exists in the database schema but has no domain layer — no aggregate, no repository, no use cases, and no API endpoints. As a result, the booking flow cannot validate seat existence or availability before creating a booking, and the `seats.status` column (designed for global per-seat availability) is unused and represents the wrong abstraction. Seat availability must be tracked per route, not globally, or the same seat cannot be independently booked on different routes.

## What Changes

- **NEW**: `Seat` vertical slice in the `train` module — domain model, repository, use cases, REST endpoints following the existing clean architecture pattern
- **NEW**: `route_seat_availability` table to track seat availability per route (replaces the wrong global `seats.status`)
- **REMOVED**: `seats.status` column and its index `idx_seats_train_status` — the column is unused, carries wrong semantics, and is replaced by per-route availability
- **NEW**: `GetAvailableSeatsForRouteUseCase` — query available seats for a given route
- **NEW**: Seat availability pre-population when a new route is created (via domain event: `RouteCreated` → seed `route_seat_availability` rows)
- **UPDATED**: `CreateBookingUseCase` — must validate that the requested seat exists and is AVAILABLE for the requested route before creating a booking (currently performs no validation)
- **NEW**: Flyway migration `V4.0.0` — drops `seats.status`, drops stale index, adds `route_seat_availability` table with optimistic locking (`version` column)

## Capabilities

### New Capabilities

- `backend-seat-slice`: Full vertical slice for seat management within the `train` module — `Seat` aggregate, `SeatRepository`, CRUD use cases (`CreateSeatUseCase`, `GetSeatsByTrainUseCase`), and REST endpoints (`POST /api/v1/trains/{trainId}/seats`, `GET /api/v1/trains/{trainId}/seats`)
- `backend-seat-availability`: Per-route seat availability tracking — `RouteSeatAvailability` domain model, `RouteSeatAvailabilityRepository`, `GetAvailableSeatsForRouteUseCase`, and REST endpoint (`GET /api/v1/routes/{routeId}/seats/available`); includes availability pre-population on route creation and optimistic locking for concurrent booking protection
- `seat-management-schema`: Flyway migration `V4.0.0` — removes `seats.status` column and its index, adds `route_seat_availability` table

### Modified Capabilities

- `database-schema`: Index requirement `seats(train_id, status)` is removed; `route_seat_availability` table is added
- `backend-booking-slice`: `CreateBookingUseCase` gains a seat availability validation step before persisting a booking

## Impact

- **Database**: New migration `V4.0.0` — drops `seats.status` column + index; adds `route_seat_availability` table
- **Backend `train` module**: New `Seat` vertical slice files (domain, application, infrastructure layers)
- **Backend `booking` module**: `CreateBookingUseCase` updated to validate seat availability; cross-module read of `RouteSeatAvailability` via a domain service or application port
- **API**: Two new endpoint groups — `/api/v1/trains/{trainId}/seats` (seat CRUD) and `/api/v1/routes/{routeId}/seats/available` (availability query)
- **No frontend changes required** for this change
