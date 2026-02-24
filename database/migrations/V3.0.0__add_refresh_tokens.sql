-- ============================================================
-- V3.0.0 — Add refresh_tokens table for JWT refresh token management
--
-- Purpose: Store hashed refresh tokens with expiration and revocation tracking
-- Features:
--   - UUID primary key with uuidv7() default (monotonic ordering)
--   - Foreign key to users table
--   - SHA-256 token hash storage (unique)
--   - Soft delete via revoked_at timestamp (audit trail)
--   - Timezone-aware timestamps (TIMESTAMPTZ, UTC)
-- ============================================================
CREATE TABLE refresh_tokens (
    id UUID NOT NULL DEFAULT uuidv7(),
    user_id UUID NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- ============================================================
-- Foreign Key Constraints
-- ============================================================
ALTER TABLE refresh_tokens
ADD CONSTRAINT refresh_tokens_user_id_fkey FOREIGN KEY (user_id) REFERENCES users (id);

-- ============================================================
-- Indexes for Performance
-- ============================================================
-- Unique index for fast lookup by token hash (primary query pattern)
CREATE UNIQUE INDEX idx_refresh_tokens_token_hash ON refresh_tokens (token_hash);

-- Index for user-based queries (revoke all user tokens)
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

-- Composite index for efficient active token queries by user
-- Supports: WHERE user_id = ? AND revoked_at IS NULL
CREATE INDEX idx_refresh_tokens_user_revoked ON refresh_tokens (user_id, revoked_at);

-- ============================================================
-- Comments for Documentation
-- ============================================================
COMMENT ON TABLE refresh_tokens IS 'Stores hashed JWT refresh tokens with expiration and revocation tracking';

COMMENT ON COLUMN refresh_tokens.id IS 'Primary key (UUIDv7 for monotonic ordering)';

COMMENT ON COLUMN refresh_tokens.user_id IS 'Foreign key to users table';

COMMENT ON COLUMN refresh_tokens.token_hash IS 'SHA-256 hash of raw refresh token (64 hex chars), never store plain token';

COMMENT ON COLUMN refresh_tokens.expires_at IS 'Token expiration timestamp (UTC)';

COMMENT ON COLUMN refresh_tokens.revoked_at IS 'Token revocation timestamp (UTC); NULL means token is still active';

COMMENT ON COLUMN refresh_tokens.created_at IS 'Token creation timestamp (UTC)';
