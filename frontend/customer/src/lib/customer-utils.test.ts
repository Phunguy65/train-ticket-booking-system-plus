import { describe, expect, it } from 'vitest';
import {
    calculateTotalPrice,
    canAddMoreSeats,
    canCancelBooking,
    canPayBooking,
    formatDuration,
    formatPrice,
    generateIdempotencyKey,
    MAX_SEATS_PER_BOOKING,
} from './customer-utils.ts';

describe('formatPrice', () => {
    it('should format price in VND', () => {
        const result = formatPrice(350000);
        expect(result).toContain('350');
        expect(result).toContain('000');
    });

    it('should handle zero', () => {
        const result = formatPrice(0);
        expect(result).toContain('0');
    });
});

describe('formatDuration', () => {
    it('should format hours only', () => {
        expect(formatDuration(120)).toBe('2h');
    });

    it('should format minutes only', () => {
        expect(formatDuration(45)).toBe('45m');
    });

    it('should format hours and minutes', () => {
        expect(formatDuration(150)).toBe('2h 30m');
    });

    it('should handle zero', () => {
        expect(formatDuration(0)).toBe('0m');
    });
});

describe('canCancelBooking', () => {
    it('should return true for HELD status', () => {
        expect(canCancelBooking('HELD')).toBe(true);
    });

    it('should return false for CONFIRMED status', () => {
        expect(canCancelBooking('CONFIRMED')).toBe(false);
    });

    it('should return false for CANCELLED status', () => {
        expect(canCancelBooking('CANCELLED')).toBe(false);
    });

    it('should return false for undefined', () => {
        expect(canCancelBooking(undefined)).toBe(false);
    });
});

describe('canPayBooking', () => {
    it('should return true for HELD booking with future deadline', () => {
        const futureDeadline = new Date();
        futureDeadline.setHours(futureDeadline.getHours() + 1);

        expect(
            canPayBooking({
                status: 'HELD',
                paymentDeadline: futureDeadline.toISOString(),
            }),
        ).toBe(true);
    });

    it('should return false for non-HELD booking', () => {
        expect(
            canPayBooking({
                status: 'CONFIRMED',
            }),
        ).toBe(false);
    });

    it('should return false for expired deadline', () => {
        const pastDeadline = new Date();
        pastDeadline.setHours(pastDeadline.getHours() - 1);

        expect(
            canPayBooking({
                status: 'HELD',
                paymentDeadline: pastDeadline.toISOString(),
            }),
        ).toBe(false);
    });

    it('should return false for undefined booking', () => {
        expect(canPayBooking(undefined)).toBe(false);
    });
});

describe('canAddMoreSeats', () => {
    it('should return true when under limit', () => {
        expect(canAddMoreSeats(0)).toBe(true);
        expect(canAddMoreSeats(4)).toBe(true);
    });

    it('should return false when at limit', () => {
        expect(canAddMoreSeats(MAX_SEATS_PER_BOOKING)).toBe(false);
    });

    it('should return false when over limit', () => {
        expect(canAddMoreSeats(MAX_SEATS_PER_BOOKING + 1)).toBe(false);
    });
});

describe('calculateTotalPrice', () => {
    it('should calculate total price', () => {
        expect(calculateTotalPrice(3, 350000)).toBe(1050000);
    });

    it('should return zero for zero seats', () => {
        expect(calculateTotalPrice(0, 350000)).toBe(0);
    });

    it('should return zero for zero price', () => {
        expect(calculateTotalPrice(3, 0)).toBe(0);
    });
});

describe('generateIdempotencyKey', () => {
    it('should generate a unique key', () => {
        const key1 = generateIdempotencyKey();
        const key2 = generateIdempotencyKey();

        expect(key1).toMatch(/^booking-\d+-[a-z0-9]+$/);
        expect(key1).not.toBe(key2);
    });
});
