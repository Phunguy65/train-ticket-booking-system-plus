# Changelog

## [1.0.0](https://github.com/Phunguy65/train-ticket-booking-system-plus/compare/backend-v0.1.0...backend-v1.0.0) (2026-06-07)


### ⚠ BREAKING CHANGES

* **api:** Query parameters for paged/filter endpoints were flattened from a nested "request" object to top-level query params (e.g. page, size, originStationId, etc.). External clients must update requests to the new parameter layout.
* **auth:** The authenticated profile update endpoint changed from PATCH (partial-merge with JsonNullable) to PUT with full-replacement semantics. Clients must send the complete editable profile payload (fullName and email are required); optional fields must be explicitly set to null to clear them. jackson-databind-nullable and its vendored composite-build/submodule wiring were removed.
* **usecases:** CreateCheckoutSessionCommand no longer includes amount and currency; callers must rely on booking pricing and updated response types.
* **booking:** removed GetBookingById* types; callers must use the new GetBookingDetail* flow and updated DTOs.
* **backend:** routes domain and APIs have been removed/renamed to scheduled-trips; database schema, table names (route_seat_availability -> trip_seat_availability) and public endpoints changed. Update clients and migrations accordingly.
* **train:** remove totalSeats parameter from create/update APIs and commands; total seat counts are now computed and auto-updated by event listeners. Update any callers or API clients to stop supplying totalSeats.
* **api:** Repository and service signatures now use PageResponse and List<SortOrder>; controllers expect request/query records instead of raw params.
* **api:** removed HTTP response DTOs and controllers now return application response records; client-facing JSON shapes may have changed.
* **booking:** train module seat availability uses optimistic locking and some cross-module port/query methods changed; consumers should handle OptimisticLockException.
* **train:** base_price column type changed from DECIMAL to BIGINT and API DTOs now use long for basePrice
* **db:** change monetary columns from DECIMAL to BIGINT (store amounts in smallest currency unit) and remove booking_seats table; downstream code and migrations must adapt to new types and schema.
* **api:** bulk delete endpoints changed from DELETE /{version}/{resource}?ids=... to POST /{version}/{resource}:bulkDelete with JSON body (e.g. {"userIds":[...]})
* **train:** remove PATCH /api/v1.0/seats/{id} and related use case, DTOs and tests
* **train:** Seat DTOs, APIs, and DB mappings now reference coachId instead of trainId; update clients and migrations accordingly
* **booking:** drop bookings.seat_id, replace BookingStatus PENDING with HELD, and replace POST /api/v1.0/bookings single-seat create with POST /api/v1.0/bookings/hold multi-seat hold endpoint
* **api:** seatClass removed from APIs and database schema; pricing is now unified to route.basePrice.
* **train:** remove seats.status column and idx_seats_train_status index
* **db:** All primary and foreign key columns now use UUID. Java entities, repositories, and OpenAPI schemas must be updated to use UUID types (java.util.UUID / string, format: uuid).

### refactor

* **api:** convert bulk delete endpoints and route mappings ([66c358d](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/66c358df49c3b5b399da9bc5d9e1395c942b1fc1))
* **api:** replace HTTP DTOs and mappers with records ([ba982ad](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/ba982ad0151acd283d0e93ec97de4e3adf7fb7b2))
* **api:** unify pagination and sorting across modules ([a548c02](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/a548c021b29bc57b4d4332c06160e47aea41c1fc))
* **backend:** replace route with scheduled trip and add VOs ([7778a3f](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/7778a3f3809bfcf57e499b712e94cbdfb401c629))
* **booking:** replace booking-by-id with detail flow ([6e97990](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/6e97990b27e929882e00e46a9a00f95da3cbe18b))
* **db:** unify money types and add integrity checks ([d97e055](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/d97e05520d79c667916d3be50b08f010998bdcd5))
* **train:** remove seat update use case and endpoint ([12e6e15](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/12e6e15679af0b1660652f60fd460f9765d88f3f))
* **train:** represent route basePrice with Money and long ([f2471a8](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/f2471a83f81d5934118bd93cb25ed542dea47c38))


### Features

* **account:** add payment history and printable tickets ([62f1c68](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/62f1c6882e1555c6d431a861a228041a62e9c338))
* **api:** add admin-only user list with slice pagination ([7ef6489](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/7ef648976c34992191d7af7381ac9a5b4df67f7d))
* **api:** add patch endpoints for train seat route station ([aae84e5](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/aae84e5ba6616422806daac0236f692e14fa2933))
* **api:** add path-segment api versioning and adapt controllers ([b8dcc75](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/b8dcc752a6367dfb70d8fa9e658059918f8593d8))
* **api:** add unified jsend fail payload and violations ([9f778b1](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/9f778b17db348e6df2de2acbd2104ece2a1dd704))
* **api:** allow public GET access for trains and stations ([8193ac5](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/8193ac57032ecab4e5672a3b87661ca6df0e8923))
* **api:** enable springdoc and generate customer OpenAPI ([e1a79c4](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/e1a79c4c1e57a36ed257b72da4ceebb7281b5481))
* **api:** flatten query params and introduce auth provider ([06c3fe2](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/06c3fe2be68f3cb57b580e2bfd75849e5441169c))
* **api:** implement soft-delete for trains, seats, stations ([4dea11d](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/4dea11db2c638a61e724c0b9fca6993db34da892))
* **api:** make pagination params optional with defaults ([be71079](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/be7107902edb6d0a7fee603f9c5b9b5a03ebcfb3))
* **api:** mark optional response fields as nullable in OpenAPI ([abc1ce2](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/abc1ce269e48a7c71fff14ae1a04117339a50e35))
* **api:** remove seatclass and unify seat pricing ([e994f0e](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/e994f0e16956bdbcce39581915e34339a43ebc29))
* **api:** standardize OpenAPI contracts and export workflow ([12821e3](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/12821e35f821fa3df73505f2b7622c431613b6bd))
* **auth:** add user auth module with jwt and refresh tokens ([8124451](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/81244513b3ad1cadba321deacbc218f5f0c0575c))
* **auth:** migrate authenticated profile update to PUT ([611d3d6](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/611d3d62c43a4a39dd1166f1bc8aaa8677baa0a0))
* **backend:** add query projections and payment flows ([263ee3f](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/263ee3fc5f51cb30f9a33c5edab4016b46a26bf9))
* **booking:** add booking domain, repository, and apis ([cdc6c80](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/cdc6c8001d5761390edc010c3588aeda24d3dda7))
* **booking:** add cross-module booking validation ports ([95eb151](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/95eb151997c74489f8759f3dce59562475439c48))
* **booking:** add multi-seat hold/confirm flow with expiry ([efd83f2](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/efd83f2c2b5082b8885a3809571c741f6f7efd53))
* **booking:** add payment ports and stale-hold queries ([4053f70](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/4053f70917e28bdb1758e2cca4db0bf188ea5173))
* **booking:** add stepper, summary, price and payment UI ([5f3eaea](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/5f3eaea12716458b68890f48ed0c1a2cde489838))
* **booking:** add user bookings query, use case and endpoints ([9712c4c](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/9712c4c7b5893b303eda44e63c198c28c0eb2c79))
* **booking:** compute booking price using route query ([5c707d1](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/5c707d1d591f9845d549be980f6ab030e5798755))
* **booking:** implement booking module and optimistic locking ([14e1338](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/14e1338a817f140930d52c3969d4e6bcc2014d5f))
* **cache:** add Redis cache and caching for scheduled trips ([ae9dbee](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/ae9dbeee0234b7f5889001f7a92b01f0cbca6ec1))
* **config:** enable virtual threads in backend application ([62937e1](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/62937e1ae5edc8c710c7a60e7f2fe0e9e003dab6))
* **customer:** add auth UI and i18n support with shadcn and next-intl ([ff11135](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/ff111354bf9c18b9e6c359802da6ad2746b30c3a))
* **customer:** add booking flows, pages, and components ([89e670e](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/89e670e83d597b0cd3168d5d8fb5714e6e735209))
* **customer:** migrate admin frontend and add API client ([f0326ec](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/f0326ecba3252ba266df299e4ca3ea4a251cd18f))
* **db:** add b11 baseline and align migrations sql ([f2ae621](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/f2ae62159a1bc2cc11071c971ed738fbe6de6555))
* **db:** add flyway and postgres service integration ([0cb7e48](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/0cb7e4800acccc0bceea49200e5838002754dffd))
* **db:** add refresh tokens migration and tidy build-logic ([3054e28](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/3054e28b0e941182d34212774780d5800efd91e8))
* **db:** add refresh_tokens table for jwt refresh tokens ([9f79d22](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/9f79d22fbd7e42049f1d4bfe16e46532ea9e328e))
* **db:** add soft-delete columns and indexes to baseline ([beef627](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/beef62766b87f35f0853ccb1593c9d8129b862a7))
* **db:** add train_cars table and backfill migration ([205f48b](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/205f48b73399a2e478e5f658a161400a704dc871))
* **db:** migrate primary and foreign keys to uuid ([fc6fccc](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/fc6fcccb2a31b513b6423088e54628c78b4c0f8a))
* **demo:** expand presentation tests and seed routes ([3d48ffa](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/3d48ffaa69db908e33932c72cd8582bba8ffce0d))
* **docs:** add PlantUML linting and report builder scripts ([f437482](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/f437482b11444a833420f3e3d96bec11eaf04930))
* **domain:** add update methods and emit domain events ([f3db746](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/f3db7462a33d0448b446b3294c1f053f5e39dd11))
* **infra:** add caddy reverse proxy and docker cors fix ([bbf9ba2](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/bbf9ba221e4f44cf9273926b4017f24bd130252b))
* **payment:** add user_id and checkout_url to payments queries ([613aa22](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/613aa2239570f135094e98518ee212825d769b47))
* **payment:** centralize stripe-expiry handling in use case ([64eea90](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/64eea907c7efc63e617d5b014a8270439cd5408b))
* **payment:** integrate stripe checkout and webhooks ([4cb33be](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/4cb33be5855190189fa0847937ebd4df207b528b))
* **payment:** integrate Stripe Checkout for bookings ([f0ef52a](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/f0ef52a8fc3c36c25b9869cb941270daabf7d881))
* **payment:** support nullable payment fields and checkout flow ([c17eab6](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/c17eab65e44266812f85f774becaaa3246e824b8))
* **scripts:** add seed data generator and README for dev ([37a4a8e](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/37a4a8e35a82c1506e11b741c87b1e35b2dad279))
* **search:** add cursor-based scheduled trip and station search ([e19a9e6](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/e19a9e61bbd6af857897da837fec3a31b7b5b50e))
* **seats:** add authenticated SSE hook for live updates ([d22a998](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/d22a9984f65520f9376c2ee8731dbfc4f507a905))
* **station:** implement station management api and module ([eda6107](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/eda61070a9d0f9d4fa03272324176d92a42e65c2))
* **station:** prevent deleting stations that have active routes ([99e5488](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/99e5488efa56379fe97786b91e4e42bf94c4318e))
* **train:** add bulk create endpoints for coaches and seats ([10b7e05](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/10b7e05c98d8bb9801805697ea1078b53ae57890))
* **train:** add cascade soft-delete with bulk events ([4c34731](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/4c347318d31f3d5f7eae885c09144e415a5b0aaa))
* **train:** add coach aggregate and migrate seats to coach ([cf8d7e7](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/cf8d7e771ec8f264c6b1e106ba7cb36276096156))
* **train:** add coach API (create, list, get by id) ([ce615ab](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/ce615ab868662e1670aef70861799448f577f7e3))
* **train:** add coach seat map query and use case ([e4fa787](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/e4fa7875f439d18724fbb61f8dcc157d22baa139))
* **train:** add enriched scheduled trip detail response ([10d6544](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/10d6544200b5dcee5bc91c4d7fec1c145fefb202))
* **train:** add scheduled trip and route template models ([2a3e297](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/2a3e297a270b81d1c4bc3ad3370af6d69c3dc4f8))
* **train:** add soft-delete and bulk delete for coaches ([3787701](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/37877016393ce8897f6185a58446282dc9064e92))
* **train:** add soft-delete routes with bulk API and cascade ([329c5bf](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/329c5bf8f008e0b66a7924eede3c1f20084aca54))
* **train:** add train module (domain, persistence, web, tests) ([b11b6c4](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/b11b6c4af9bb8417c9370b51a75551ef09a3e6a7))
* **train:** cancel bookings on seat delete via helper ([853b3a1](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/853b3a151e645cbe45054005956e305cd77ba653))
* **train:** capture price snapshot when holding route seats ([00ad22a](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/00ad22ade390c86988b4786525647a441c336ba8))
* **train:** implement route crud api and domain slice ([ae02ce8](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/ae02ce87ee6431e4b2955a92a7c9ee3001cc7eee))
* **train:** implement seat management and availability ([8b56b03](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/8b56b0347b7be1f5f202a6b07650ac3ccde780cc))
* **train:** synchronize total seat counts via events ([f251497](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/f2514971ff5e6870ef49ac2d99ae76ebab92f0f3))
* **trips:** add back-to-home button and show dates in trip cards ([60ad7b7](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/60ad7b7ed3217c8e7608c59be608888b0600ee85))
* **ui:** add landing sections and improve global layout ([aace02b](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/aace02ba1a35bd8342db09f2802fea48d45f7e21))
* **ui:** add multi-ticket print tabs and date picker ([3e73a57](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/3e73a579fdbe94f01818bc4c57cc72298bb08fb4))
* **ui:** introduce VN visual theme, fonts and favicon ([2b1676a](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/2b1676a4ab8d7ede0821a6a57403ea4ca6978544))
* **ui:** redesign travel theme and add auth layout ([9ae7871](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/9ae78717c6ce0a9d59975190360ff6550d389cbf))
* **user:** add admin user creation endpoint and usecase ([6cdfdd1](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/6cdfdd173719fb3391b2bb20478309c2d0365950))
* **user:** add patch endpoints and update user usecase ([1aeb869](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/1aeb8696e1555d011e062d766de3533fd5a43673))
* **user:** add profile fields and booking user info snapshot ([b5a34b1](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/b5a34b1550f6542d18580fba678f772f8592fe51))
* **user:** add soft-delete users with bulk delete endpoints ([c89cd91](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/c89cd91a7e79ea0327a560d267da57eba11b4287))
* **user:** add user summary projection and summary queries ([0305e06](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/0305e06c27eaed78f4121eae3525050880c314cc))


### Bug Fixes

* **user:** find user including deleted for idempotent delete ([e04db32](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/e04db321521e174692a2947fbf6531d96127204e))
* **user:** use saveAndFlush to prevent duplicate user rows ([250178a](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/250178a819fb566802ae21ba3b530e5fd4ef5881))


### Documentation

* **config:** clarify clean architecture and test structure ([c26b004](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/c26b004ffc3030094faf59d048f6d3f76a7c954c))
* **config:** expand architecture spec and project layout ([2f0ab26](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/2f0ab262993bf97b7b23416bc07fcc36b706e3a9))
* **config:** update openspec config layout and content ([c1c83f6](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/c1c83f610aa264068983fba0c556bee1bf0624f5))
* **db:** add comprehensive database schema for booking system ([626346b](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/626346bee2987828b93a7d4fe6b826a345dc0fa6))
* **docs:** normalize headings and fix formatting across docs ([f251a8d](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/f251a8df696b5ea5ae0af98a7843bf844b486237))
* **openspec:** add user management and auth change specs ([4296b0a](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/4296b0a66ceecb8592635f48ac4aeeb02037ca56))
* **openspec:** remove multi-seat and stripe change specs ([a458f57](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/a458f57e5e555d0581f170012248124ec92cb42d))
* **presentation:** add interactive test-run UI to slides ([551072b](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/551072bcc01284371290bd840427495db6b2dd23))
* **presentation:** add presentation slides and script ([b641aa7](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/b641aa7be7cd4a73c363460e74897db7895b7f9f))
* **readme:** add project overview and setup instructions ([60b8d2f](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/60b8d2f265e6011a8de8d5c03ecb07ae33c262ae))
* **stripe:** remove outdated Stripe session expiry comment in code ([0d05ae8](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/0d05ae88e1e2dfa8e4a37f5de953f57a69304313))
* **usecases:** add comprehensive use case and overview docs ([2eaefed](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/2eaefed47bf3c7390daf438c2460edf0949fc8c3))
* **usecases:** add UC-12 payment and expand test matrices ([358f4c3](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/358f4c39a07b63f924f744777c0a045a9946c1f6))
* **usecases:** add use case docs for auth and booking ([5b9a44b](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/5b9a44b8d816fcd9daa18584ece69be02fb95d2e))
