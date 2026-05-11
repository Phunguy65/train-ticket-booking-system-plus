## ADDED Requirements

### Requirement: Caddy reverse proxy routes frontend traffic

The system SHALL provide a Caddy reverse proxy that routes all non-API requests (paths not starting with `/api/`) to the Next.js frontend service.

#### Scenario: Browser requests frontend page through Caddy

- **WHEN** a browser requests `http://localhost:2015/` (dev) or `https://app.example.com/` (prod)
- **THEN** Caddy forwards the request to the frontend service on port 3000 and returns the response to the browser

#### Scenario: Static assets served through Caddy

- **WHEN** a browser requests `http://localhost:2015/_next/static/...`
- **THEN** Caddy forwards the request to the frontend service and returns the static asset

### Requirement: Caddy reverse proxy routes API traffic to backend

The system SHALL route all requests with paths starting with `/api/` to the Spring Boot backend service, preserving the full request path.

#### Scenario: API request routed to backend

- **WHEN** a browser sends `POST /api/v1/auth/register` through Caddy
- **THEN** Caddy forwards the request to the backend service on port 8080 with the path `/api/v1/auth/register` intact

#### Scenario: API request preserves headers

- **WHEN** a request passes through Caddy to the backend
- **THEN** Caddy adds `X-Forwarded-For`, `X-Forwarded-Proto`, and `X-Real-IP` headers with the original client information

### Requirement: Dev configuration uses HTTP on configurable port

The system SHALL provide a development Caddyfile that listens on HTTP (no TLS) on port 2015 by default.

#### Scenario: Dev Caddy starts without HTTPS

- **WHEN** the dev Caddyfile is loaded by Caddy
- **THEN** Caddy listens on `http://localhost:2015` without attempting certificate generation

### Requirement: Prod configuration uses automatic HTTPS

The system SHALL provide a production Caddyfile that uses a domain name to trigger automatic HTTPS certificate provisioning via Let's Encrypt.

#### Scenario: Prod Caddy obtains certificate automatically

- **WHEN** the prod Caddyfile is loaded with a public domain name and ports 80/443 are accessible
- **THEN** Caddy automatically obtains and renews TLS certificates from Let's Encrypt

### Requirement: Caddy service is optional via Docker Compose profile

The system SHALL add the Caddy service under a Docker Compose profile so existing workflows are not disrupted.

#### Scenario: Default docker-compose up excludes Caddy

- **WHEN** a developer runs `docker-compose up` without specifying a profile
- **THEN** the Caddy service is not started; frontend and backend remain accessible on their direct ports

#### Scenario: Caddy enabled with profile flag

- **WHEN** a developer runs `docker-compose --profile proxy up`
- **THEN** the Caddy service starts and routes traffic on port 2015 to frontend and backend

### Requirement: Certificate data persists across container restarts

The system SHALL use a named Docker volume for Caddy's data directory to persist certificates and avoid rate-limit issues with Let's Encrypt.

#### Scenario: Caddy container restart preserves certificates

- **WHEN** the Caddy container is stopped and restarted
- **THEN** previously obtained certificates are still available and Caddy does not re-request them from the CA
