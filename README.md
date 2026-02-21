# Train Ticket Booking System

A multi-platform train ticket booking system with Spring Boot backend, Kotlin
Compose Multiplatform customer app, and Next.js admin dashboard.

## Architecture

- **Backend**: Spring Boot 3.2 with Java 17, PostgreSQL, Flyway migrations
- **Customer App**: Kotlin Compose Multiplatform (Android, iOS, Desktop)
- **Admin Dashboard**: Next.js 14 with Bun runtime
- **Database**: PostgreSQL 15
- **API**: RESTful with OpenAPI 3.0 specification

## Project Structure

```
train-ticket-booking-system-plus/
├── backend/                 # Spring Boot API
├── frontend/
│   ├── customer/           # Kotlin Compose Multiplatform app
│   └── admin/              # Next.js admin dashboard
├── database/
│   ├── migrations/         # Flyway SQL migrations
│   └── schema/             # Database documentation
├── shared/
│   └── api-contracts/      # OpenAPI specification
└── docker-compose.yml      # Local development environment
```

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 17+ (for local backend development)
- Bun (for admin dashboard development)
- Gradle 8+ (for backend and customer app)

### Quick Start with Docker

```bash
# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

Services will be available at:

- Backend API: http://localhost:8080
- Admin Dashboard: http://localhost:3000
- PostgreSQL: localhost:5432

### Local Development

#### Backend

```bash
cd backend
./gradlew bootRun
```

#### Admin Dashboard

```bash
cd frontend/admin
bun install
bun run dev
```

#### Customer App

```bash
cd frontend/customer
./gradlew :composeApp:run
```

## Database

The system uses PostgreSQL with Flyway for migrations. Initial schema includes:

- Users (authentication and profiles)
- Stations (train stations)
- Trains (train information)
- Routes (scheduled train routes)
- Seats (seat inventory)
- Bookings (ticket reservations)

See `database/schema/erd.md` for detailed entity relationships.

## API Documentation

OpenAPI specification is available at `shared/api-contracts/openapi.yaml`.

Key endpoints:

- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `GET /api/v1/trains` - List trains
- `POST /api/v1/bookings` - Create booking
- `GET /api/v1/bookings` - List user bookings

## Security

- JWT-based authentication
- Idempotency keys for booking operations
- Pessimistic locking to prevent double-booking
- CORS configuration for frontend access

## Environment Variables

### Backend

- `SPRING_PROFILES_ACTIVE` - Active profile (dev/staging/prod)
- `SPRING_DATASOURCE_URL` - PostgreSQL connection URL
- `SPRING_DATASOURCE_USERNAME` - Database username
- `SPRING_DATASOURCE_PASSWORD` - Database password

### Admin Dashboard

- `NEXT_PUBLIC_API_URL` - Backend API URL
- `NEXTAUTH_URL` - NextAuth callback URL
- `NEXTAUTH_SECRET` - NextAuth secret key

## License

Proprietary - All rights reserved
