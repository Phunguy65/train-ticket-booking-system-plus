# Context

The `implement-coach-domain` change delivered the full Coach persistence infrastructure: `Coach` aggregate, `CoachId`, `CoachRepository` interface, `CoachEntity`, `CoachJpaRepository`, `CoachEntityMapper`, and `CoachRepositoryAdapter`. The database table `coaches` already exists (migration V12). What is missing is the application and web layers that make the Coach resource accessible via REST.

The `train` module follows a Vertical Slice / Clean Architecture pattern. Each operation is a dedicated `@Service @Transactional` use case class. Controllers are package-private `@RestController` classes in `train/infrastructure/web/` returning `JsendResponse<?>`. Business errors are typed sealed interfaces returned via `Result<T, E>` — never thrown as exceptions.

The `SeatController` is the canonical reference for nested-resource endpoints (`/{version}/trains/{trainId}/seats`). Coach follows the same structural pattern.

## Goals / Non-Goals

**Goals:**

- Expose three REST endpoints for Coach: Create, Get by ID, Get All (by train)
- Validate that the `trainId` path parameter refers to an existing, non-deleted train on all write and read-by-ID operations
- Return typed business errors via `JsendResponse` fail responses with machine-readable `ErrorCode` values
- Secure the create endpoint to `ADMIN` role; leave read endpoints public
- Add `COACH_NOT_FOUND`, `COACH_CAR_NUMBER_ALREADY_EXISTS`, `COACH_TRAIN_NOT_FOUND` to the shared `ErrorCode` enum

**Non-Goals:**

- No update (PATCH) or delete (DELETE) endpoints for Coach — those are a separate change
- No pagination for the Get All endpoint — a train has at most ~20 coaches (follows `SeatController` pattern of returning a plain list)
- No changes to the existing `Coach` domain model, `CoachRepository`, or persistence layer
- No database migrations
- No frontend changes

## Decisions

### Decision 1: Nested routes under `/{version}/trains/{trainId}/coaches`

**Choice:** All three endpoints are nested under the train path, following the `SeatController` pattern (`/{version}/trains/{trainId}/seats`).

**Rationale:** Coaches have no independent existence outside a train. Nesting makes the ownership explicit in the URL, mirrors the existing seat endpoints, and provides a natural `trainId` context for all operations without requiring it in the request body.

**Alternative considered:** Flat routes (`/{version}/coaches`) with `trainId` as a query parameter for list and a path param for get/create. Rejected because it breaks the established nested-resource pattern, requires extra validation logic to ensure consistency between body and path, and is harder to reason about for operators.

### Decision 2: Validate `trainId` in application layer, not only via FK constraint

**Choice:** `CreateCoachUseCase` explicitly calls `trainRepository.findById(trainId)` and returns `CoachError.TrainNotFound` if empty.

**Rationale:** Letting a DB `DataIntegrityViolationException` escape from a missing FK would bypass the `Result<T, E>` contract and require catching a technical exception in the controller — the exact anti-pattern the codebase avoids. `CreateSeatUseCase` does this same check for its parent Coach. Explicit validation gives a clean, typed `404 Not Found` response.

**Alternative considered:** Rely on PostgreSQL FK constraint violation. Rejected because it leaks infrastructure concerns into the controller layer and violates the Result monad pattern used throughout.

### Decision 3: `GetCoachByIdUseCase` validates coach belongs to the given `trainId`

**Choice:** When handling `GET /trains/{trainId}/coaches/{id}`, the use case checks that `coach.getTrainId().equals(trainId)` after fetching by ID. If the coach exists but belongs to a different train, return `CoachError.CoachNotFound` (same as if the coach didn't exist at all).

**Rationale:** Without this check, a client could enumerate coach IDs across trains by guessing UUIDs — a form of insecure direct object reference. Returning `404` (rather than `403`) avoids leaking whether a coach with that ID exists at all on another train, consistent with OWASP IDOR guidance. `GetCoachByIdUseCase` needs both `coachId` and `trainId` as parameters.

**Alternative considered:** Ignore `trainId` in GetById — just fetch by `coachId` alone. Rejected because it weakens the ownership semantics of the nested URL and creates an IDOR surface.

### Decision 4: Get All returns `List<CoachDto>` (no pagination)

**Choice:** `GetCoachesByTrainUseCase` returns `List<CoachDto>`. The controller wraps it in `JsendResponse.success(list)` directly.

**Rationale:** A single train has at most ~20 coaches in practice. The `CoachJpaRepository.findAllActiveByTrainId` query already orders by `carNumber ASC`. Pagination overhead (page/size params, `SliceHttpResponse` wrapper, sort validation) is not justified. This matches how `GetSeatsByTrainUseCase` / `SeatController` handles the equivalent list endpoint.

**Alternative considered:** Paginated response using `PageResult<T>` and `SliceHttpResponse` (Train and Station pattern). Rejected because the dataset is too small to benefit, and the simpler list response is already established for child-resource lists.

### Decision 5: `CoachController` follows the full-path-on-method pattern (like `SeatController`)

**Choice:** `CoachController` is a plain `@RestController` with no class-level `@RequestMapping`. Each method carries the full path: `@PostMapping(value = "/{version}/trains/{trainId}/coaches", version = "1.0")`.

**Rationale:** This is the exact pattern used by `SeatController` for its nested endpoints. `TrainController` uses a class-level `@RequestMapping("/{version}/trains")` because all its methods share the same prefix — coaches live under a different prefix than trains, so the full-path approach avoids any accidental path collision.

## Risks / Trade-offs

- **`trainId` ownership check adds an extra DB read per GetById call** → Acceptable: it is a primary-key lookup (indexed UUID), so the cost is negligible. Trade-off of correctness over micro-optimization.
- **`CoachError.TrainNotFound` name may be confusing** (it lives in `CoachError` but refers to the parent Train) → Consistent with how `SeatError.TrainNotFound` names the same concept. Controllers map it to HTTP 404 and `ErrorCode.COACH_TRAIN_NOT_FOUND` making the semantic clear at the API level.
- **No update/delete endpoints** → Operators who need to correct a mis-entered coach must wait for the follow-up change. Accepted as an explicit Non-Goal.

## Open Questions

- *(none — all design decisions have been made based on existing patterns in the codebase)*
