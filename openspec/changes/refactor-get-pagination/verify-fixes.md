# Verify Fixes Log

## 2026-03-14 Round 1 (from opsx-apply auto-verify)

### opsx-test-verifier

- Fixed: Updated GetStationsUseCaseTest to use GetStationsQuery and Sort.class
  matcher instead of deleted SortDirection
- Fixed: Updated ListUsersUseCaseTest to use GetUsersQuery and Sort.class
  matcher instead of deleted SortDirection
- Fixed: Updated GetTrainsUseCaseTest to use GetTrainsQuery and Sort.class
  matcher instead of deleted SortDirection
- Fixed: Updated GetRoutesUseCaseTest to use GetRoutesQuery and Sort.class
  matcher; removed deleted RouteFilter and SortDirection references
- Fixed: Rewrote GetCoachesByTrainUseCaseTest to use GetCoachesQuery and return
  PageResult<CoachResponse>
- Fixed: Rewrote GetSeatsByTrainUseCaseTest to use GetSeatsQuery and return
  PageResult<SeatResponse>
- Fixed: Updated StationRepositoryAdapterTest findAll calls to use Sort objects
  instead of SortDirection
- Fixed: Updated TrainRepositoryAdapterTest findAll calls to use Sort objects
  instead of SortDirection
- Fixed: Updated UserRepositoryAdapterTest findAll calls to use Sort objects
  instead of SortDirection
- Fixed: Rewrote RouteRepositoryAdapterTest to remove RouteFilter and
  SortDirection; updated findAll calls to use Sort
- Fixed: Updated UserModuleTest listUsersUseCase calls to use GetUsersQuery
- Fixed: Updated StationControllerTest mock from
  execute(anyInt,anyInt,anyString,any) to execute(any()); removed invalid sort
  field test
- Fixed: Updated UserControllerTest mocks from
  execute(anyInt,anyInt,anyString,any) to execute(any()); removed invalid sort
  field test; updated page/size validation assertions
- Fixed: Updated TrainControllerTest mock from
  execute(anyInt,anyInt,anyString,any) to execute(any())
- Fixed: Updated RouteControllerTest mock from
  execute(anyInt,anyInt,anyString,any,any) to execute(any()); removed invalid
  sort field test
- Fixed: Updated CoachControllerTest getCoachesByTrain test to expect
  PageResult/SliceHttpResponse structure instead of List
- Fixed: Updated SeatControllerTest getSeatsByTrain and getAvailableSeats tests
  to expect PageResult/SliceHttpResponse structure instead of List

### opsx-arch-verifier

- No issues found in production code. All design decisions D1-D6 correctly
  implemented.
