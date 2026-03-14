# Tasks

## 1. Shared infrastructure — PageRequest DTO

- [x] 1.1 Create `shared.infrastructure.web.request.PageRequest` record with
      `page` (default 0, `@Min(0)`) and `size` (default 20, `@Min(1) @Max(100)`)
      fields

## 2. Per-module Query objects

- [x] 2.1 Create
      `station.application.query.GetStationsQuery(int page, int size)`
- [x] 2.2 Create `user.application.query.GetUsersQuery(int page, int size)`
- [x] 2.3 Create `train.application.query.GetTrainsQuery(int page, int size)`
- [x] 2.4 Create `train.application.query.GetRoutesQuery(int page, int size)`
- [x] 2.5 Create
      `train.application.query.GetCoachesQuery(int page, int size, UUID trainId)`
- [x] 2.6 Create
      `train.application.query.GetSeatsQuery(int page, int size, UUID trainId)`
- [x] 2.7 Create
      `train.application.query.GetAvailableSeatsQuery(int page, int size, UUID routeId)`

## 3. Domain repository port updates

- [x] 3.1 Update `StationRepository.findAll` signature to
      `findAll(int page, int size, Sort sort)`
- [x] 3.2 Update `UserRepository.findAll` signature to
      `findAll(int page, int size, Sort sort)`
- [x] 3.3 Update `TrainRepository.findAll` signature to
      `findAll(int page, int size, Sort sort)`
- [x] 3.4 Update `RouteRepository.findAll` signature to
      `findAll(int page, int size, Sort sort)` — remove `RouteFilter` and
      `SortDirection` params
- [x] 3.5 Add
      `CoachRepository.findAll(int page, int size, Sort sort, TrainId trainId)`
      — paginated list by train
- [x] 3.6 Add
      `SeatRepository.findAll(int page, int size, Sort sort, TrainId trainId)` —
      paginated list by train (join via Coach)
- [x] 3.7 Add
      `SeatRepository.findAllAvailable(int page, int size, Sort sort, RouteId routeId)`
      — paginated available seats by route

## 4. Use case updates

- [x] 4.1 Update `GetStationsUseCase.execute` to accept `GetStationsQuery`;
      build `Sort.by("code").ascending().and(Sort.by("id").ascending())`
      internally
- [x] 4.2 Update `GetUsersUseCase.execute` to accept `GetUsersQuery`; build
      `Sort.by("createdAt").descending().and(Sort.by("id").ascending())`
      internally
- [x] 4.3 Update `GetTrainsUseCase.execute` to accept `GetTrainsQuery`; build
      `Sort.by("trainNumber").ascending().and(Sort.by("id").ascending())`
      internally
- [x] 4.4 Update `GetRoutesUseCase.execute` to accept `GetRoutesQuery`; build
      `Sort.by("departureTime").ascending().and(Sort.by("id").ascending())`
      internally; remove `RouteFilter` usage
- [x] 4.5 Update `GetCoachesByTrainUseCase.execute` to accept `GetCoachesQuery`;
      return `PageResult<CoachResponse>`; build
      `Sort.by("carNumber").ascending().and(Sort.by("id").ascending())`
- [x] 4.6 Update `GetSeatsByTrainUseCase.execute` to accept `GetSeatsQuery`;
      return `PageResult<SeatResponse>`; build
      `Sort.by("seatNumber").ascending().and(Sort.by("id").ascending())`
- [x] 4.7 Update `GetAvailableSeatsForRouteUseCase.execute` to accept
      `GetAvailableSeatsQuery`; return `PageResult<SeatResponse>`; build
      `Sort.by("seatNumber").ascending().and(Sort.by("id").ascending())`

## 5. Infrastructure adapter + JPA updates

- [x] 5.1 Update `StationRepositoryAdapter.findAll` to accept `Sort`; remove
      `SortDirection` conversion logic
- [x] 5.2 Update `UserRepositoryAdapter.findAll` to accept `Sort`; remove
      `SortDirection` conversion logic
- [x] 5.3 Update `TrainRepositoryAdapter.findAll` to accept `Sort`; remove
      `SortDirection` conversion logic
- [x] 5.4 Update `RouteRepositoryAdapter.findAll` to accept `Sort`; remove
      filter params; update call to `RouteJpaRepository`
- [x] 5.5 Update `RouteJpaRepository.findAllWithFilter` → rename to
      `findAllActive(Pageable pageable)` with simple `WHERE deletedAt IS NULL`
      query
- [x] 5.6 Add `CoachRepositoryAdapter.findAll(page, size, sort, TrainId)`
      delegating to new JPA method
- [x] 5.7 Add
      `CoachJpaRepository.findAllActiveByTrainId(UUID trainId, Pageable pageable)`
      — `WHERE trainId = :trainId AND deletedAt IS NULL`
- [x] 5.8 Add `SeatRepositoryAdapter.findAll(page, size, sort, TrainId)`
      delegating to new JPA join query
- [x] 5.9 Add
      `SeatJpaRepository.findAllActiveByTrainId(UUID trainId, Pageable pageable)`
      — join `SeatEntity` → `CoachEntity` on
      `coachId = coach.id WHERE coach.trainId = :trainId AND seat.deletedAt IS NULL`
- [x] 5.10 Add
      `SeatRepositoryAdapter.findAllAvailable(page, size, sort, RouteId)` —
      query `RouteSeatAvailabilityRepository` then paginate via
      `SeatJpaRepository`
- [x] 5.11 Add
      `SeatJpaRepository.findAllAvailableByRouteId(UUID routeId, Pageable pageable)`
      — join `SeatEntity` → `RouteSeatAvailabilityEntity` on
      `seatId WHERE routeId = :routeId AND status = AVAILABLE AND seat.deletedAt IS NULL`

## 6. Controller updates

- [x] 6.1 Update `StationController.list` — replace 3 `@RequestParam` with
      `@ModelAttribute @Valid PageRequest`; call
      `new GetStationsQuery(request.page(), request.size())`; remove
      `ALLOWED_SORT_FIELDS`, validation block, sort-parse block
- [x] 6.2 Update `UserController.list` — same pattern as 6.1; `GetUsersQuery`
- [x] 6.3 Update `TrainController.list` — same pattern as 6.1; `GetTrainsQuery`
- [x] 6.4 Update `RouteController.list` — replace 7 `@RequestParam` with
      `@ModelAttribute @Valid PageRequest`; call
      `new GetRoutesQuery(request.page(), request.size())`; remove filter
      params, `ALLOWED_SORT_FIELDS`, validation block, sort-parse block
- [x] 6.5 Update `CoachController.getCoachesByTrain` — add
      `@ModelAttribute @Valid PageRequest`; call
      `new GetCoachesQuery(request.page(), request.size(), trainId)`; wrap
      result in `SliceHttpResponse`
- [x] 6.6 Update `SeatController.getSeatsByTrain` — add
      `@ModelAttribute @Valid PageRequest`; call
      `new GetSeatsQuery(request.page(), request.size(), trainId)`; wrap result
      in `SliceHttpResponse`
- [x] 6.7 Update `SeatController.getAvailableSeats` — add
      `@ModelAttribute @Valid PageRequest`; call
      `new GetAvailableSeatsQuery(request.page(), request.size(), routeId)`;
      wrap result in `SliceHttpResponse`

## 7. Cleanup

- [x] 7.1 Delete `train.domain.model.RouteFilter`
- [x] 7.2 Delete `shared.domain.SortDirection` (verify no remaining references
      first)
- [x] 7.3 Run `./gradlew build` — confirm zero compilation errors
