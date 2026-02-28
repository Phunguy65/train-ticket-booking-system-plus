# Tasks

## 1. Shared — Error Codes

- [x] 1.1 Add `COACH_NOT_FOUND`, `COACH_CAR_NUMBER_ALREADY_EXISTS`, and `COACH_TRAIN_NOT_FOUND` to the `ErrorCode` enum in `shared/infrastructure/web/ErrorCode.java` under the Train module section

## 2. Domain Layer — Typed Business Errors

- [x] 2.1 Create `CoachError.java` sealed interface in `train/domain/errors/`

## 3. Application Layer — Commands and DTOs

- [x] 3.1 Create `CreateCoachCommand.java` record in `train/application/command/` with fields: `UUID trainId`, `int carNumber`, `int totalSeats`
- [x] 3.2 Create `CoachDto.java` record in `train/application/dto/` with fields: `UUID id`, `UUID trainId`, `int carNumber`, `int totalSeats`, `Instant createdAt`

## 4. Application Layer — Use Cases

- [x] 4.1 Create `CreateCoachUseCase.java` (`@Service @Transactional`) in `train/application/usecase/`
- [x] 4.2 Create `GetCoachByIdUseCase.java` (`@Service @Transactional(readOnly = true)`) in `train/application/usecase/`
- [x] 4.3 Create `GetCoachesByTrainUseCase.java` (`@Service @Transactional(readOnly = true)`) in `train/application/usecase/`

## 5. Web Layer — HTTP DTOs and Mapper

- [x] 5.1 Create `CreateCoachHttpRequest.java` record in `train/infrastructure/web/`
- [x] 5.2 Create `CoachHttpResponse.java` record in `train/infrastructure/web/`
- [x] 5.3 Create `CoachRequestMapper.java` `@Component` in `train/infrastructure/web/`

## 6. Web Layer — Controller

- [x] 6.1 Create `CoachController.java` `@RestController` in `train/infrastructure/web/`
  - `@PostMapping(value = "/{version}/trains/{trainId}/coaches", version = "1.0")` with `@PreAuthorize("hasRole('ADMIN')")` — accept `@PathVariable UUID trainId` and `@Valid @RequestBody CreateCoachHttpRequest`; call `createCoachUseCase.execute(mapper.toCommand(trainId, request))`; on success return `201 Created` with `Location` header and `JsendResponse.success(mapper.toResponse(dto))`; on failure delegate to `coachErrorResponse(error)`
  - `@GetMapping(value = "/{version}/trains/{trainId}/coaches", version = "1.0")` — accept `@PathVariable UUID trainId`; call `getCoachesByTrainUseCase.execute(TrainId.of(trainId))`; return `200 OK` with `JsendResponse.success(list)`
  - `@GetMapping(value = "/{version}/trains/{trainId}/coaches/{id}", version = "1.0")` — accept `@PathVariable UUID trainId` and `@PathVariable UUID id`; call `getCoachByIdUseCase.execute(CoachId.of(id), TrainId.of(trainId))`; on success return `200 OK`; on failure delegate to `coachErrorResponse(error)`
  - Private `coachErrorResponse(CoachError error)` method: switch on error type — `CoachNotFound` → `404 NOT_FOUND` + `ErrorCode.COACH_NOT_FOUND`; `CarNumberAlreadyExists` → `409 CONFLICT` + `ErrorCode.COACH_CAR_NUMBER_ALREADY_EXISTS`; `TrainNotFound` → `404 NOT_FOUND` + `ErrorCode.COACH_TRAIN_NOT_FOUND`

## 7. Tests — Use Cases

- [x] 7.1 Create `CreateCoachUseCaseTest.java` (`@ExtendWith(MockitoExtension.class)`) in `train/application/usecase/`
- [x] 7.2 Create `GetCoachByIdUseCaseTest.java` (`@ExtendWith(MockitoExtension.class)`) in `train/application/usecase/`
- [x] 7.3 Create `GetCoachesByTrainUseCaseTest.java` (`@ExtendWith(MockitoExtension.class)`) in `train/application/usecase/`

## 8. Tests — Controller

- [x] 8.1 Create `CoachControllerTest.java` (`@WebMvcTest(CoachController.class)`) in `train/infrastructure/web/`
  - `POST` with valid body + ADMIN role → `201 Created` with `Location` header
  - `POST` with `carNumber: 0` → `400 Bad Request` with `VALIDATION_ERROR`
  - `POST` without ADMIN role → `403 Forbidden`
  - `POST` with non-existent train → `404` with `COACH_TRAIN_NOT_FOUND`
  - `POST` with duplicate `carNumber` → `409` with `COACH_CAR_NUMBER_ALREADY_EXISTS`
  - `GET` list → `200 OK` with array
  - `GET` by ID found → `200 OK` with coach data
  - `GET` by ID not found → `404` with `COACH_NOT_FOUND`