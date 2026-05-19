## Why

The customer frontend running in Docker (port 41250) cannot call the backend API — requests to `/api/v1/auth/register` return 403 Forbidden because the CORS allowed origins only include `http://localhost:3000`. Additionally, the current architecture relies on Next.js rewrites to proxy API calls, which adds unnecessary latency and couples the API routing to the frontend framework. A Caddy reverse proxy eliminates CORS entirely by serving frontend and backend under the same origin, provides automatic HTTPS for both dev and prod, and decouples API routing from the frontend.

## What Changes

- Fix immediate 403 by adding Docker-exposed port to CORS allowed origins
- Add Caddy reverse proxy service to docker-compose for path-based routing (`/` → frontend, `/api/*` → backend)
- Create Caddyfile configurations for dev (HTTP) and prod (auto-HTTPS with Let's Encrypt)
- Remove Next.js rewrites from `next.config.ts` (no longer needed with Caddy handling routing)
- Simplify Spring Security CORS configuration (can be restricted or removed when all traffic flows through Caddy)
- Update port exposure in docker-compose (frontend/backend only accessible via Caddy network, not host-bound)

## Capabilities

### New Capabilities

- `caddy-reverse-proxy`: Caddy-based reverse proxy configuration for dev and prod environments, handling path-based routing and automatic HTTPS certificate management.

### Modified Capabilities

- `customer-api-contract`: The base URL and routing mechanism changes — frontend API client no longer relies on Next.js rewrites; requests go directly through Caddy to backend.

## Impact

- **Infrastructure**: New `caddy` service in docker-compose; new `conf/` directory for Caddyfile; new Docker volumes for certificate persistence
- **Frontend**: `next.config.ts` rewrites removed; `BACKEND_URL` build arg no longer needed for Docker build
- **Backend**: `CORS_ALLOWED_ORIGINS` env var simplified (only needed for local dev without Caddy); `SecurityConfig.java` CORS source may be simplified
- **Ports**: External access moves from `:41250` (frontend) and `:43565` (backend) to a single Caddy port (`:2015` dev, `:443` prod)
- **Environment**: `.env` and `.env.example` updated with Caddy-related variables
