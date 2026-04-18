/**
 * Integration tests for customer booking flows.
 *
 * These tests verify the key customer journeys work correctly:
 * - Trip search validation and navigation
 * - Seat selection to booking handoff via URL params
 * - Booking creation flow
 * - Account cancellation flow
 */

import { describe, expect, it } from 'vitest';
import {
    calculateTotalPrice,
    canAddMoreSeats,
    canCancelBooking,
    formatPrice,
    formatTime,
    generateIdempotencyKey,
    MAX_SEATS_PER_BOOKING,
} from '@/lib/customer-utils.ts';
import {
    buildBookingUrl,
    parseBookingContext,
    parseTripSearchParams,
    serializeTripSearchParams,
    type TripSearchParams,
} from '@/lib/search-params.ts';
import { profileSchema, tripSearchSchema } from '@/lib/validations/customer.ts';

describe('Customer Flow Integration: Trip Search', () => {
    it('validates search form with all required fields', () => {
        const validData = {
            originStationId: 'station-1',
            destinationStationId: 'station-2',
            departureDate: new Date('2030-05-01'),
        };

        const result = tripSearchSchema.safeParse(validData);
        expect(result.success).toBe(true);
    });

    it('rejects search when origin and destination are the same', () => {
        const invalidData = {
            originStationId: 'station-1',
            destinationStationId: 'station-1',
            departureDate: new Date('2030-05-01'),
        };

        const result = tripSearchSchema.safeParse(invalidData);
        expect(result.success).toBe(false);
        if (!result.success) {
            // The refine creates an issue with the path ['destinationStationId']
            const issues = result.error.issues;
            expect(
                issues.some((i) => i.message === 'destination.sameAsOrigin'),
            ).toBe(true);
        }
    });

    it('rejects search when departure date is in the past', () => {
        const invalidData = {
            originStationId: 'station-1',
            destinationStationId: 'station-2',
            departureDate: new Date('2020-01-01'),
        };

        const result = tripSearchSchema.safeParse(invalidData);
        expect(result.success).toBe(false);
    });

    it('serializes search params to URLSearchParams and parses back correctly', () => {
        const searchData: TripSearchParams = {
            originStationId: 'station-1',
            destinationStationId: 'station-2',
            departureDate: '2026-05-01',
        };

        const serialized = serializeTripSearchParams(searchData);
        expect(serialized.get('origin')).toBe('station-1');
        expect(serialized.get('destination')).toBe('station-2');
        expect(serialized.get('date')).toBe('2026-05-01');

        const parsed = parseTripSearchParams(serialized);
        expect(parsed.originStationId).toBe('station-1');
        expect(parsed.destinationStationId).toBe('station-2');
        expect(parsed.departureDate).toBe('2026-05-01');
    });
});

describe('Customer Flow Integration: Seat Selection to Booking Handoff', () => {
    it('enforces maximum 5 seats per booking', () => {
        expect(canAddMoreSeats(0)).toBe(true);
        expect(canAddMoreSeats(4)).toBe(true);
        expect(canAddMoreSeats(5)).toBe(false);
        expect(canAddMoreSeats(6)).toBe(false);
        expect(MAX_SEATS_PER_BOOKING).toBe(5);
    });

    it('calculates total price correctly for multiple seats', () => {
        const pricePerSeat = 500000;

        expect(calculateTotalPrice(1, pricePerSeat)).toBe(500000);
        expect(calculateTotalPrice(3, pricePerSeat)).toBe(1500000);
        expect(calculateTotalPrice(5, pricePerSeat)).toBe(2500000);
    });

    it('builds booking URL with trip and seat context', () => {
        const context = {
            tripId: 'trip-123',
            seatIds: ['seat-1', 'seat-2', 'seat-3'],
        };

        const url = buildBookingUrl(context);
        expect(url).toContain('/booking?');
        expect(url).toContain('tripId=trip-123');
        expect(url).toContain('seats=seat-1%2Cseat-2%2Cseat-3');
    });

    it('parses booking context from URL search params', () => {
        const searchParams = new URLSearchParams(
            'tripId=trip-123&seats=seat-1,seat-2,seat-3',
        );

        const context = parseBookingContext(searchParams);
        expect(context).not.toBeNull();
        expect(context?.tripId).toBe('trip-123');
        expect(context?.seatIds).toEqual(['seat-1', 'seat-2', 'seat-3']);
    });

    it('returns null for invalid booking context', () => {
        const emptyParams = new URLSearchParams();
        expect(parseBookingContext(emptyParams)).toBeNull();

        const missingSeats = new URLSearchParams('tripId=trip-123');
        expect(parseBookingContext(missingSeats)).toBeNull();

        const missingTrip = new URLSearchParams('seats=seat-1');
        expect(parseBookingContext(missingTrip)).toBeNull();
    });
});

describe('Customer Flow Integration: Booking Creation', () => {
    it('generates unique idempotency keys', () => {
        const key1 = generateIdempotencyKey();
        const key2 = generateIdempotencyKey();
        const key3 = generateIdempotencyKey();

        expect(key1).not.toBe(key2);
        expect(key2).not.toBe(key3);
        expect(key1).not.toBe(key3);

        // Keys should start with "booking-"
        expect(key1.startsWith('booking-')).toBe(true);
    });

    it('formats prices in VND currency', () => {
        const formatted = formatPrice(500000);
        expect(formatted).toContain('500');
        // Should contain VND indicator
        expect(formatted.toLowerCase()).toMatch(/vn[dđ]|₫/i);
    });

    it('formats time correctly', () => {
        const time = '2026-04-18T14:30:00Z';
        const formatted = formatTime(time);
        // Should contain hour and minute
        expect(formatted).toMatch(/\d{1,2}:\d{2}/);
    });
});

describe('Customer Flow Integration: Account and Booking Status', () => {
    it('determines if booking can be cancelled based on status', () => {
        expect(canCancelBooking('HELD')).toBe(true);
        expect(canCancelBooking('CONFIRMED')).toBe(false);
        expect(canCancelBooking('CANCELLED')).toBe(false);
        expect(canCancelBooking(undefined)).toBe(false);
    });

    it('validates profile form data', () => {
        const validProfile = {
            fullName: 'Nguyen Van A',
            email: 'test@example.com',
            phone: '0901234567',
            dateOfBirth: new Date('1990-01-15'),
            gender: 'male',
            idDocumentNumber: '123456789',
            addressLine: 'Ho Chi Minh City',
        };

        const result = profileSchema.safeParse(validProfile);
        expect(result.success).toBe(true);
    });

    it('validates profile with minimal required fields', () => {
        const minimalProfile = {
            fullName: 'Nguyen Van A',
            email: 'test@example.com',
            phone: '',
            dateOfBirth: null,
            gender: null,
            idDocumentNumber: null,
            addressLine: null,
        };

        const result = profileSchema.safeParse(minimalProfile);
        expect(result.success).toBe(true);
    });

    it('rejects profile with invalid email', () => {
        const invalidProfile = {
            fullName: 'Nguyen Van A',
            email: 'invalid-email',
            phone: null,
            dateOfBirth: null,
            gender: null,
            idDocumentNumber: null,
            addressLine: null,
        };

        const result = profileSchema.safeParse(invalidProfile);
        expect(result.success).toBe(false);
    });

    it('rejects profile with short full name', () => {
        const invalidProfile = {
            fullName: 'A',
            email: 'test@example.com',
            phone: null,
            dateOfBirth: null,
            gender: null,
            idDocumentNumber: null,
            addressLine: null,
        };

        const result = profileSchema.safeParse(invalidProfile);
        expect(result.success).toBe(false);
    });
});

describe('Customer Flow Integration: End-to-End Booking Journey', () => {
    it('complete search to booking params flow', () => {
        // Step 1: User fills search form
        const searchInput = {
            originStationId: 'station-sgn',
            destinationStationId: 'station-hn',
            departureDate: new Date('2030-05-15'),
        };

        // Validate search
        const searchResult = tripSearchSchema.safeParse(searchInput);
        expect(searchResult.success).toBe(true);

        // Step 2: Serialize to URLSearchParams for /search page
        const searchParams: TripSearchParams = {
            originStationId: searchInput.originStationId,
            destinationStationId: searchInput.destinationStationId,
            departureDate: searchInput.departureDate
                .toISOString()
                .split('T')[0],
        };
        const serialized = serializeTripSearchParams(searchParams);
        expect(serialized.get('origin')).toBe('station-sgn');

        // Step 3: User selects a trip and goes to seat selection
        const tripId = 'trip-123';

        // Step 4: User selects seats (max 5)
        const selectedSeats = ['seat-1', 'seat-2'];
        expect(canAddMoreSeats(selectedSeats.length)).toBe(true);

        // Try to add more seats up to limit
        for (let i = selectedSeats.length; i < MAX_SEATS_PER_BOOKING; i++) {
            expect(canAddMoreSeats(i)).toBe(true);
        }
        expect(canAddMoreSeats(MAX_SEATS_PER_BOOKING)).toBe(false);

        // Step 5: Build booking URL for handoff
        const bookingUrl = buildBookingUrl({
            tripId,
            seatIds: selectedSeats,
        });
        expect(bookingUrl).toContain(tripId);
        expect(bookingUrl).toContain('seat-1');
        expect(bookingUrl).toContain('seat-2');

        // Step 6: Parse booking context on /booking page
        const urlParams = new URLSearchParams(bookingUrl.split('?')[1]);
        const bookingContext = parseBookingContext(urlParams);
        expect(bookingContext).not.toBeNull();
        expect(bookingContext?.tripId).toBe(tripId);
        expect(bookingContext?.seatIds).toEqual(selectedSeats);

        // Step 7: Calculate total price
        const pricePerSeat = 450000;
        const totalPrice = calculateTotalPrice(
            selectedSeats.length,
            pricePerSeat,
        );
        expect(totalPrice).toBe(900000);

        // Step 8: Generate idempotency key for booking creation
        const idempotencyKey = generateIdempotencyKey();
        expect(idempotencyKey.startsWith('booking-')).toBe(true);
    });
});
