## MODIFIED Requirements

### Requirement: Frontend API client uses relative URLs for same-origin requests

The system SHALL configure the frontend API client with an empty `baseUrl` so that all API requests are relative to the current origin, enabling transparent routing through either Next.js rewrites (standalone dev) or Caddy reverse proxy (Docker/prod).

#### Scenario: API request uses relative URL

- **WHEN** the frontend makes an API call (e.g., register)
- **THEN** the request URL is `/api/v1/auth/register` (relative, no absolute host)

## ADDED Requirements

### Requirement: CORS allowed origins include all development access points

The system SHALL configure CORS allowed origins to include both `http://localhost:3000` (direct frontend dev) and `http://localhost:41250` (Docker-exposed frontend port) to prevent 403 Forbidden errors during development without Caddy.

#### Scenario: Request from Docker-exposed port is not rejected by CORS

- **WHEN** a browser at `http://localhost:41250` sends a POST to `/api/v1/auth/register` via Next.js rewrite
- **THEN** the backend accepts the request (CORS origin check passes) and processes the registration

#### Scenario: Request from local dev port is not rejected by CORS

- **WHEN** a browser at `http://localhost:3000` sends a POST to `/api/v1/auth/register`
- **THEN** the backend accepts the request (CORS origin check passes) and processes the registration

### Requirement: Next.js rewrites remain for standalone frontend development

The system SHALL keep Next.js rewrites in `next.config.ts` as a fallback for developers running the frontend without Caddy (e.g., `bun run dev` with backend on localhost:8080).

#### Scenario: Frontend dev without Caddy still proxies API calls

- **WHEN** a developer runs `bun run dev` in the frontend directory without Caddy
- **THEN** API calls to `/api/*` are proxied to the backend via Next.js rewrites using `BACKEND_URL` environment variable
