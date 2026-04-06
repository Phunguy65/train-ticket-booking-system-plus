# ─── Stage 1: install dependencies ──────────────────────────────────────────
# Context is the project root so the codegen step can read
# shared/api-contracts/openapi.yaml (referenced by openapi-ts.config.ts).
FROM oven/bun:1.3.11 AS deps

WORKDIR /app

    # Copy lockfile and manifest first for layer caching.
    COPY frontend/customer/package.json frontend/customer/bun.lock ./
    # Install exact versions from the lockfile.  The lockfile must be kept
    # up-to-date in the repository (run `bun install` locally and commit
    # bun.lock) for --frozen-lockfile to pass in CI and here.
    RUN bun install --frozen-lockfile

# ─── Stage 2: build ──────────────────────────────────────────────────────────
FROM oven/bun:1.3.11 AS build

WORKDIR /app

COPY --from=deps /app/node_modules ./node_modules

# Copy the whole customer workspace and the shared API contract.
COPY frontend/customer/ ./
COPY shared/api-contracts/ /shared/api-contracts/

# Regenerate the SDK from the committed contract then produce the standalone
# Next.js build (requires output: 'standalone' in next.config.ts).
RUN bun run codegen && bun run build

# ─── Stage 3: run ────────────────────────────────────────────────────────────
# Run the standalone Next.js server on Bun to keep the runtime aligned with the
# frontend toolchain. Alpine needs libc6-compat so Next image optimization can
# load sharp's native bindings.
FROM oven/bun:1.3.11-alpine AS runner

WORKDIR /app

RUN apk add --no-cache libc6-compat

COPY --from=build --chown=bun:bun /app/.next/standalone ./
COPY --from=build --chown=bun:bun /app/.next/static    ./.next/static
COPY --from=build --chown=bun:bun /app/public          ./public

USER bun

EXPOSE 3000

ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

CMD ["bun", "server.js"]
