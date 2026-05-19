## Context

The train ticket booking system uses a Docker Compose stack with:
- **Frontend**: Next.js (customer app) on port 3000 (exposed as 41250 on host)
- **Backend**: Spring Boot on port 8080 (exposed as 43565 on host)
- **Database**: PostgreSQL on port 5432
- **Cache**: Valkey (Redis-compatible) on port 6379

Currently, the frontend proxies API calls via Next.js rewrites (`next.config.ts`), which adds a server-side hop. The backend enforces CORS with `CORS_ALLOWED_ORIGINS=http://localhost:3000`. When accessing the Docker-exposed frontend at `http://localhost:41250`, the browser sends `Origin: http://localhost:41250` which is rejected by Spring Security's CORS filter, causing 403 Forbidden on all API calls.

There is no reverse proxy in front of the services. Each service exposes its own port directly to the host.

## Goals / Non-Goals

**Goals:**
- Fix the immediate 403 Forbidden on `/api/v1/auth/register` when accessed via Docker port 41250
- Add Caddy as a reverse proxy to unify frontend and backend under a single origin
- Provide dev configuration (HTTP on a single port) and prod configuration (auto-HTTPS)
- Eliminate CORS complexity by making all API calls same-origin through Caddy
- Remove Next.js rewrites dependency for API routing

**Non-Goals:**
- Subdomain-based routing (e.g., `api.example.com`) — path-based routing is sufficient
- Load balancing across multiple backend instances
- WebSocket/SSE configuration changes (existing SSE endpoints continue to work through Caddy transparently)
- Changing the backend API path structure (`/api/v1/...` remains unchanged)
- Production domain name selection or DNS configuration

## Decisions

### 1. Path-based routing over subdomain routing

**Choice**: Route `/api/*` to backend, everything else to frontend.

**Rationale**: The backend already uses `/api/v1/...` prefix. Path-based routing requires no DNS changes, works identically in dev and prod, and eliminates CORS entirely (same origin). Subdomain routing would still require CORS configuration.

**Alternatives considered**:
- Subdomain routing (`api.example.com`): Requires DNS setup, still cross-origin, more complex dev setup
- Keep Next.js rewrites: Adds latency, couples routing to frontend framework, doesn't solve the CORS issue for direct backend access

### 2. No path stripping at Caddy level

**Choice**: Caddy forwards requests to backend with the full path intact (e.g., `/api/v1/auth/register` → backend receives `/api/v1/auth/register`).

**Rationale**: The backend's `@RequestMapping` annotations already expect the `/api` prefix (via `/{version}/auth` under a base path). The OpenAPI spec and generated client use `/api/v1/...` paths. Stripping would break the contract.

### 3. Dev uses HTTP on port 2015, prod uses auto-HTTPS on 443

**Choice**: Dev Caddyfile uses `http://localhost:2015` (explicit HTTP scheme prevents auto-HTTPS). Prod Caddyfile uses a domain name which triggers automatic Let's Encrypt certificate provisioning.

**Rationale**: Dev doesn't need HTTPS (adds trust store complexity, self-signed cert warnings). Prod gets zero-config HTTPS with automatic renewal. The `http://` prefix in dev explicitly disables Caddy's auto-HTTPS behavior.

### 4. Keep CORS config in backend as fallback

**Choice**: Don't remove `SecurityConfig.corsConfigurationSource()` — update `CORS_ALLOWED_ORIGINS` to include all dev origins. In production with Caddy, CORS headers become irrelevant (same-origin) but the config remains as defense-in-depth.

**Rationale**: Developers may still run the backend directly (without Caddy) during local development. Keeping CORS config ensures the backend works standalone. The cost is minimal (one env var).

### 5. Frontend API client baseUrl remains empty string

**Choice**: The frontend's `createClientConfig` already uses `baseUrl: ''` (relative URLs). This works with both Next.js rewrites and Caddy proxy — no change needed.

**Rationale**: Relative URLs (`/api/v1/auth/register`) resolve against the current origin. Whether that origin is `http://localhost:3000` (dev without Caddy), `http://localhost:2015` (dev with Caddy), or `https://app.example.com` (prod), the request goes to the same host and Caddy routes it correctly.

### 6. Docker Compose: Caddy as optional service with profile

**Choice**: Add Caddy service with a Docker Compose profile (`--profile proxy`) so it doesn't break existing workflows. Developers can choose to use Caddy or continue with the current direct-port setup.

**Rationale**: Non-breaking change. Existing `docker-compose up` continues to work as before. `docker-compose --profile proxy up` adds Caddy. This allows gradual adoption.

## Risks / Trade-offs

- **[Risk] Caddy data volume loss** → Certificates regenerated, may hit Let's Encrypt rate limits in prod. Mitigation: Named Docker volume `caddy_data` with explicit persistence.
- **[Risk] Port conflict** → Port 2015 (dev) or 80/443 (prod) may be in use. Mitigation: Document port requirements; dev port is configurable via env var.
- **[Risk] Next.js rewrite removal breaks standalone frontend dev** → Developers running `bun run dev` without Caddy lose API proxying. Mitigation: Keep rewrites as fallback; only remove in Docker/prod builds where Caddy is present.
- **[Trade-off] Additional infrastructure component** → Caddy adds complexity. Benefit: Eliminates CORS, provides HTTPS, simplifies API routing. Net positive for production readiness.
- **[Trade-off] Two Caddyfile variants** → Dev and prod configs diverge. Mitigation: Keep them minimal and similar in structure; only the site address differs.
