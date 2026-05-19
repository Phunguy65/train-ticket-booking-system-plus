import { describe, expect, it } from 'vitest';
import {
    buildBookingUrl,
    buildTripSearchUrl,
    parseBookingContext,
    parseTripSearchParams,
    serializeBookingContext,
    serializeTripSearchParams,
} from './search-params.ts';

describe('parseTripSearchParams', () => {
    it('should parse all trip search params', () => {
        const params = new URLSearchParams();
        params.set('origin', 'station-1');
        params.set('destination', 'station-2');
        params.set('date', '2024-01-15');
        params.set('sortBy', 'PRICE');
        params.set('sortDirection', 'DESC');
        params.set('minPrice', '100000');
        params.set('maxPrice', '500000');
        params.set('availableOnly', 'true');

        const result = parseTripSearchParams(params);

        expect(result).toEqual({
            originStationId: 'station-1',
            destinationStationId: 'station-2',
            departureDate: '2024-01-15',
            sortBy: 'PRICE',
            sortDirection: 'DESC',
            minPrice: 100000,
            maxPrice: 500000,
            availableOnly: true,
        });
    });

    it('should return empty object for missing params', () => {
        const params = new URLSearchParams();
        const result = parseTripSearchParams(params);

        expect(result).toEqual({});
    });

    it('should ignore invalid sort values', () => {
        const params = new URLSearchParams();
        params.set('sortBy', 'INVALID');
        params.set('sortDirection', 'INVALID');

        const result = parseTripSearchParams(params);

        expect(result.sortBy).toBeUndefined();
        expect(result.sortDirection).toBeUndefined();
    });

    it('should ignore non-numeric price values', () => {
        const params = new URLSearchParams();
        params.set('minPrice', 'abc');
        params.set('maxPrice', 'xyz');

        const result = parseTripSearchParams(params);

        expect(result.minPrice).toBeUndefined();
        expect(result.maxPrice).toBeUndefined();
    });
});

describe('serializeTripSearchParams', () => {
    it('should serialize trip search params', () => {
        const result = serializeTripSearchParams({
            originStationId: 'station-1',
            destinationStationId: 'station-2',
            departureDate: '2024-01-15',
            sortBy: 'PRICE',
            sortDirection: 'DESC',
            minPrice: 100000,
            maxPrice: 500000,
            availableOnly: true,
        });

        expect(result.get('origin')).toBe('station-1');
        expect(result.get('destination')).toBe('station-2');
        expect(result.get('date')).toBe('2024-01-15');
        expect(result.get('sortBy')).toBe('PRICE');
        expect(result.get('sortDirection')).toBe('DESC');
        expect(result.get('minPrice')).toBe('100000');
        expect(result.get('maxPrice')).toBe('500000');
        expect(result.get('availableOnly')).toBe('true');
    });

    it('should omit undefined values', () => {
        const result = serializeTripSearchParams({
            originStationId: 'station-1',
        });

        expect(result.get('origin')).toBe('station-1');
        expect(result.has('destination')).toBe(false);
        expect(result.has('date')).toBe(false);
    });
});

describe('buildTripSearchUrl', () => {
    it('should build a valid search URL', () => {
        const url = buildTripSearchUrl({
            originStationId: 'station-1',
            destinationStationId: 'station-2',
            departureDate: '2024-01-15',
        });

        expect(url).toBe(
            '/search?origin=station-1&destination=station-2&date=2024-01-15',
        );
    });
});

describe('parseBookingContext', () => {
    it('should parse booking context from URL', () => {
        const params = new URLSearchParams();
        params.set('tripId', 'trip-123');
        params.set('seats', 'seat-1,seat-2,seat-3');

        const result = parseBookingContext(params);

        expect(result).toEqual({
            tripId: 'trip-123',
            seatIds: ['seat-1', 'seat-2', 'seat-3'],
        });
    });

    it('should return null for missing tripId', () => {
        const params = new URLSearchParams();
        params.set('seats', 'seat-1,seat-2');

        expect(parseBookingContext(params)).toBeNull();
    });

    it('should return null for missing seats', () => {
        const params = new URLSearchParams();
        params.set('tripId', 'trip-123');

        expect(parseBookingContext(params)).toBeNull();
    });

    it('should return null for empty seats', () => {
        const params = new URLSearchParams();
        params.set('tripId', 'trip-123');
        params.set('seats', '');

        expect(parseBookingContext(params)).toBeNull();
    });
});

describe('serializeBookingContext', () => {
    it('should serialize booking context', () => {
        const result = serializeBookingContext({
            tripId: 'trip-123',
            seatIds: ['seat-1', 'seat-2', 'seat-3'],
        });

        expect(result.get('tripId')).toBe('trip-123');
        expect(result.get('seats')).toBe('seat-1,seat-2,seat-3');
    });
});

describe('buildBookingUrl', () => {
    it('should build a valid booking URL', () => {
        const url = buildBookingUrl({
            tripId: 'trip-123',
            seatIds: ['seat-1', 'seat-2'],
        });

        expect(url).toBe('/booking?tripId=trip-123&seats=seat-1%2Cseat-2');
    });
});
