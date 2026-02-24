# Why

The `train/` module currently exists as an empty package with no implementation, despite the `trains` table already being fully defined in the database schema (including UUID primary keys after the V2.0.0 migration). The backend lacks any API to create or retrieve train records, which blocks downstream features like route creation and seat assignment that already reference `train_id` as a foreign key. This change implements the foundational Train CRUD endpoints so the system can manage train data end-to-end.

## What Changes

- Implement the `train/` Spring Modulith module following the existing vertical slice architecture (domain → application → infrastructure)
- Add `POST /api/v1.0/trains` endpoint to create a new train
- Add `GET /api/v1.0/trains` endpoint to list trains with pagination
- Add `GET /api/v1.0/trains/{id}` endpoint to retrieve a single train by UUID
- Expose `TrainId` as a named interface so other modules (`booking`, `route`, `seat`) can reference it safely

## Capabilities

### New Capabilities

- `train-management`: Core Train aggregate with create and read operations — `POST /api/v1.0/trains`, `GET /api/v1.0/trains`, `GET /api/v1.0/trains/{id}`

### Modified Capabilities

<!-- No existing specs require behavioral changes. -->

## Impact

- **New module**: `backend/src/main/java/io/github/phunguy65/ttbs/backend/train/` (all layers)
- **New tests**: unit, persistence integration, and web layer tests under `backend/src/test/java/.../train/`
- **Security config**: `SecurityConfig` needs a new `requestMatchers` rule for train endpoints (ADMIN-only write, authenticated read)
- **No database migrations needed**: `trains` table with UUID PK already exists in `V2.0.0`
- **No breaking changes**: no existing code depends on the train module
