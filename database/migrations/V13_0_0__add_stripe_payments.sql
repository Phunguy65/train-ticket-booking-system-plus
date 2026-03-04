-- ============================================================
-- V13.0.0 — Add Stripe payments table and checkout_session_id to bookings
-- ============================================================
-- ============================================================
-- PHASE 1: Create payments table
-- ============================================================
CREATE TABLE payments (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    checkout_session_id VARCHAR(255) NOT NULL,
    stripe_event_id VARCHAR(255),
    amount DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'vnd',
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT fk_payments_booking FOREIGN KEY (booking_id) REFERENCES bookings (id),
    CONSTRAINT uq_payments_checkout_session_id UNIQUE (checkout_session_id),
    CONSTRAINT uq_payments_stripe_event_id UNIQUE (stripe_event_id),
    CONSTRAINT chk_payments_status CHECK (status IN ('PENDING', 'PAID', 'CANCELLED'))
);

COMMENT ON TABLE payments IS 'Stripe Checkout Session payment records';

COMMENT ON COLUMN payments.checkout_session_id IS 'Stripe Checkout Session ID (cs_...)';

COMMENT ON COLUMN payments.stripe_event_id IS 'Stripe webhook event ID for idempotency';

COMMENT ON COLUMN payments.status IS 'Payment lifecycle: PENDING → PAID or CANCELLED';

-- Index for webhook-to-payment lookup
CREATE INDEX idx_payments_checkout_session_id ON payments (checkout_session_id);

-- Index for booking-to-payment lookup
CREATE INDEX idx_payments_booking_id ON payments (booking_id);

-- ============================================================
-- PHASE 2: Add checkout_session_id to bookings
-- ============================================================
ALTER TABLE bookings
ADD COLUMN checkout_session_id VARCHAR(255);

COMMENT ON COLUMN bookings.checkout_session_id IS 'Stripe Checkout Session ID; nullable for backward compat';

-- Index for reconciliation job queries
CREATE INDEX idx_bookings_checkout_session_id ON bookings (checkout_session_id)
WHERE
    checkout_session_id IS NOT NULL;
