# Changelog

## [1.0.0](https://github.com/Phunguy65/train-ticket-booking-system-plus/compare/customer-v0.1.0...customer-v1.0.0) (2026-06-07)


### ⚠ BREAKING CHANGES

* **api:** Query parameters for paged/filter endpoints were flattened from a nested "request" object to top-level query params (e.g. page, size, originStationId, etc.). External clients must update requests to the new parameter layout.

### Features

* **account:** add payment history and printable tickets ([62f1c68](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/62f1c6882e1555c6d431a861a228041a62e9c338))
* **api:** allow public GET access for trains and stations ([8193ac5](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/8193ac57032ecab4e5672a3b87661ca6df0e8923))
* **api:** enable springdoc and generate customer OpenAPI ([e1a79c4](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/e1a79c4c1e57a36ed257b72da4ceebb7281b5481))
* **api:** flatten query params and introduce auth provider ([06c3fe2](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/06c3fe2be68f3cb57b580e2bfd75849e5441169c))
* **api:** make pagination params optional with defaults ([be71079](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/be7107902edb6d0a7fee603f9c5b9b5a03ebcfb3))
* **api:** standardize OpenAPI contracts and export workflow ([12821e3](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/12821e35f821fa3df73505f2b7622c431613b6bd))
* **booking:** add stepper, summary, price and payment UI ([5f3eaea](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/5f3eaea12716458b68890f48ed0c1a2cde489838))
* **customer:** add auth UI and i18n support with shadcn and next-intl ([ff11135](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/ff111354bf9c18b9e6c359802da6ad2746b30c3a))
* **customer:** add booking flows, pages, and components ([89e670e](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/89e670e83d597b0cd3168d5d8fb5714e6e735209))
* **customer:** migrate admin frontend and add API client ([f0326ec](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/f0326ecba3252ba266df299e4ca3ea4a251cd18f))
* **demo:** expand presentation tests and seed routes ([3d48ffa](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/3d48ffaa69db908e33932c72cd8582bba8ffce0d))
* **infra:** add caddy reverse proxy and docker cors fix ([bbf9ba2](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/bbf9ba221e4f44cf9273926b4017f24bd130252b))
* **payment:** support nullable payment fields and checkout flow ([c17eab6](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/c17eab65e44266812f85f774becaaa3246e824b8))
* **seats:** add authenticated SSE hook for live updates ([d22a998](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/d22a9984f65520f9376c2ee8731dbfc4f507a905))
* **trips:** add back-to-home button and show dates in trip cards ([60ad7b7](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/60ad7b7ed3217c8e7608c59be608888b0600ee85))
* **ui:** add landing sections and improve global layout ([aace02b](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/aace02ba1a35bd8342db09f2802fea48d45f7e21))
* **ui:** add multi-ticket print tabs and date picker ([3e73a57](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/3e73a579fdbe94f01818bc4c57cc72298bb08fb4))
* **ui:** introduce VN visual theme, fonts and favicon ([2b1676a](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/2b1676a4ab8d7ede0821a6a57403ea4ca6978544))
* **ui:** redesign travel theme and add auth layout ([9ae7871](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/9ae78717c6ce0a9d59975190360ff6550d389cbf))
* **user:** add profile fields and booking user info snapshot ([b5a34b1](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/b5a34b1550f6542d18580fba678f772f8592fe51))
* **user:** add user summary projection and summary queries ([0305e06](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/0305e06c27eaed78f4121eae3525050880c314cc))


### Documentation

* **presentation:** add interactive test-run UI to slides ([551072b](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/551072bcc01284371290bd840427495db6b2dd23))
* **readme:** add project overview and setup instructions ([60b8d2f](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/60b8d2f265e6011a8de8d5c03ecb07ae33c262ae))
* **usecases:** add comprehensive use case and overview docs ([2eaefed](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/2eaefed47bf3c7390daf438c2460edf0949fc8c3))
* **usecases:** add UC-12 payment and expand test matrices ([358f4c3](https://github.com/Phunguy65/train-ticket-booking-system-plus/commit/358f4c39a07b63f924f744777c0a045a9946c1f6))
