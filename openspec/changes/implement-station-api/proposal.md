# Why

The system manages train routes that reference origin and destination stations, but no Station management API exists in the backend. Without the ability to create and query stations, operators cannot populate station reference data, and downstream features (routes, schedules, bookings) cannot reference valid stations. This change introduces the three foundational station endpoints needed before route management can proceed.

## What Changes

- Add `station` vertical slice module under the backend with full Hexagonal/Clean Architecture layers
- Implement `POST /api/v1/stations` — create a new station (Admin only)
- Implement `GET /api/v1/stations/{id}` — retrieve a station by UUID
- Implement `GET /api/v1/stations` — paginated list of stations with sorting
- Move `StationId` value object from `train.domain.model` to `station.domain.model` and update cross-module references
- Register `STATION_NOT_FOUND` and `STATION_CODE_ALREADY_EXISTS` error codes in the shared `ErrorCode` enum
- Add Spring Security rules for station endpoints in `SecurityConfig`

## Capabilities

### New Capabilities

- `station-management`: CRUD API for railway stations — create, get by ID, and list stations with pagination/sorting

### Modified Capabilities

- *(none — no existing spec-level behavior changes)*

## Impact

- **New module**: `backend/src/main/java/.../station/` (domain + application + infrastructure layers)
- **Shared module**: `ErrorCode` enum gains two new values
- **Train module**: `StationId` import updated to reference `station.domain.model`
- **Security config**: New access rules for `/api/v1/stations/**`
- **Database**: `stations` table already exists (schema migration V1 + V2); no new migration required
- **API surface**: Three new REST endpoints under `/api/v1/stations`
