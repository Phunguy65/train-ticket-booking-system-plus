## 1. Fix CORS 403 (Immediate)

- [x] 1.1 Update `.env` to set `CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:41250`
- [x] 1.2 Update `.env.example` to document multiple origins pattern: `CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:41250` ← (verify: backend accepts requests from both origins without 403)

## 2. Create Caddy Configuration Files

- [x] 2.1 Create `conf/Caddyfile.dev` with HTTP listener on `:2015`, routing `/api/*` to `backend:8080` and all other paths to `customer:3000`
- [x] 2.2 Create `conf/Caddyfile.prod` with domain-based auto-HTTPS, routing `/api/*` to `backend:8080` and all other paths to `customer:3000`, including `X-Forwarded-*` headers and gzip encoding ← (verify: both Caddyfiles are valid syntax, prod includes header forwarding and compression)

## 3. Add Caddy Service to Docker Compose

- [x] 3.1 Add `caddy` service to `docker-compose.yml` under `profiles: [proxy]` using `caddy:2-alpine` image, mounting `./conf/Caddyfile.dev` as `/etc/caddy/Caddyfile`, exposing port `2015:2015`, with named volumes `caddy_data:/data` and `caddy_config:/config`
- [x] 3.2 Add `caddy_data` and `caddy_config` to the `volumes:` section of docker-compose
- [x] 3.3 Connect caddy service to `trainbooking-network` and add `depends_on` for `backend` and `customer` services ← (verify: `docker-compose --profile proxy config` validates without errors, caddy service is only started with profile flag)

## 4. Documentation

- [x] 4.1 Update `README.md` to document Caddy proxy usage: how to start with `docker-compose --profile proxy up`, available ports, and dev vs prod configuration ← (verify: README accurately describes the new Caddy workflow and port mapping)
