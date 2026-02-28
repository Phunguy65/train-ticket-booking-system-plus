# Why

The `implement-coach-domain` change delivered the Coach aggregate, repository port, and full persistence infrastructure (entity, JPA repository, adapter, mapper). The API surface — application use cases, REST endpoints — was explicitly out of scope for that change. Without these endpoints, operators cannot manage coaches (create, inspect) via the API, blocking downstream seat management which requires a valid `coachId`.

## What Changes

- **New domain errors** (`CoachError`) covering the three business failure cases: coach not found, car number uniqueness violation within a train, and parent train not found.
- **Three application use cases**: `CreateCoachUseCase`, `GetCoachByIdUseCase`, `GetCoachesByTrainUseCase` — orchestrating the full create and read flows.
- **REST endpoints** under `/{version}/trains/{trainId}/coaches` following the same nested-resource pattern used by seats (`SeatController`):
  - `POST /{version}/trains/{trainId}/coaches` — create a coach (ADMIN only)
  - `GET  /{version}/trains/{trainId}/coaches` — list all coaches for a train (public)
  - `GET  /{version}/trains/{trainId}/coaches/{id}` — get a single coach by ID, validating it belongs to the given `trainId` (public)
- **ErrorCode entries** for coach-specific failure codes added to the shared enum.

## Capabilities

### New Capabilities

- `coach-api`: REST API for coach management — create a coach under a train, retrieve a single coach (with `trainId` ownership validation), and list all coaches for a given train.

### Modified Capabilities

- *(none — no existing spec-level behavior changes)*

## Impact

- **Backend `train` module**: New files in `domain/errors/`, `application/command/`, `application/dto/`, `application/usecase/`, `infrastructure/web/`. No changes to existing domain model, repository interfaces, or persistence layer.
- **Shared module**: `ErrorCode` enum gains `COACH_NOT_FOUND`, `COACH_CAR_NUMBER_ALREADY_EXISTS`, `COACH_TRAIN_NOT_FOUND`.
- **API surface**: Three new REST endpoints under `/{version}/trains/{trainId}/coaches`.
- **Security**: Create endpoint requires `ADMIN` role; GET endpoints are publicly accessible (consistent with Train, Station, Route patterns).
- **No database migration required** — `coaches` table already exists (V12 migration).
- **No frontend changes required** for this change.
