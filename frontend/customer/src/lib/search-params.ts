/**
 * Parse search parameters from URL into typed values.
 */
export type TripSearchParams = {
    originStationId?: string;
    destinationStationId?: string;
    departureDate?: string;
    sortBy?: 'DEPARTURE_TIME' | 'PRICE' | 'DURATION';
    sortDirection?: 'ASC' | 'DESC';
    minPrice?: number;
    maxPrice?: number;
    availableOnly?: boolean;
    cursor?: string;
};

/**
 * Parse trip search parameters from URLSearchParams.
 *
 * @param searchParams - URL search parameters
 */
export function parseTripSearchParams(
    searchParams: URLSearchParams,
): TripSearchParams {
    const params: TripSearchParams = {};

    const origin = searchParams.get('origin');
    if (origin) params.originStationId = origin;

    const destination = searchParams.get('destination');
    if (destination) params.destinationStationId = destination;

    const date = searchParams.get('date');
    if (date) params.departureDate = date;

    const sortBy = searchParams.get('sortBy');
    if (
        sortBy === 'DEPARTURE_TIME'
        || sortBy === 'PRICE'
        || sortBy === 'DURATION'
    ) {
        params.sortBy = sortBy;
    }

    const sortDirection = searchParams.get('sortDirection');
    if (sortDirection === 'ASC' || sortDirection === 'DESC') {
        params.sortDirection = sortDirection;
    }

    const minPrice = searchParams.get('minPrice');
    if (minPrice) {
        const parsed = parseInt(minPrice, 10);
        if (!Number.isNaN(parsed)) params.minPrice = parsed;
    }

    const maxPrice = searchParams.get('maxPrice');
    if (maxPrice) {
        const parsed = parseInt(maxPrice, 10);
        if (!Number.isNaN(parsed)) params.maxPrice = parsed;
    }

    const availableOnly = searchParams.get('availableOnly');
    if (availableOnly === 'true') params.availableOnly = true;

    const cursor = searchParams.get('cursor');
    if (cursor) params.cursor = cursor;

    return params;
}

/**
 * Serialize trip search parameters to URLSearchParams.
 *
 * @param params - Trip search parameters
 */
export function serializeTripSearchParams(
    params: TripSearchParams,
): URLSearchParams {
    const searchParams = new URLSearchParams();

    if (params.originStationId) {
        searchParams.set('origin', params.originStationId);
    }

    if (params.destinationStationId) {
        searchParams.set('destination', params.destinationStationId);
    }

    if (params.departureDate) {
        searchParams.set('date', params.departureDate);
    }

    if (params.sortBy) {
        searchParams.set('sortBy', params.sortBy);
    }

    if (params.sortDirection) {
        searchParams.set('sortDirection', params.sortDirection);
    }

    if (params.minPrice !== undefined) {
        searchParams.set('minPrice', params.minPrice.toString());
    }

    if (params.maxPrice !== undefined) {
        searchParams.set('maxPrice', params.maxPrice.toString());
    }

    if (params.availableOnly) {
        searchParams.set('availableOnly', 'true');
    }

    return searchParams;
}

/**
 * Build a search URL from trip search parameters.
 *
 * @param params - Trip search parameters
 */
export function buildTripSearchUrl(params: TripSearchParams): string {
    const searchParams = serializeTripSearchParams(params);
    return `/search?${searchParams.toString()}`;
}

/**
 * Booking context passed through URL params.
 */
export type BookingContext = {
    tripId: string;
    seatIds: string[];
};

/**
 * Parse booking context from URLSearchParams.
 *
 * @param searchParams - URL search parameters
 */
export function parseBookingContext(
    searchParams: URLSearchParams,
): BookingContext | null {
    const tripId = searchParams.get('tripId');
    const seatsParam = searchParams.get('seats');

    if (!tripId || !seatsParam) {
        return null;
    }

    const seatIds = seatsParam.split(',').filter(Boolean);
    if (seatIds.length === 0) {
        return null;
    }

    return { tripId, seatIds };
}

/**
 * Serialize booking context to URLSearchParams.
 *
 * @param context - Booking context
 */
export function serializeBookingContext(
    context: BookingContext,
): URLSearchParams {
    const searchParams = new URLSearchParams();
    searchParams.set('tripId', context.tripId);
    searchParams.set('seats', context.seatIds.join(','));
    return searchParams;
}

/**
 * Build a booking URL from booking context.
 *
 * @param context - Booking context
 */
export function buildBookingUrl(context: BookingContext): string {
    const searchParams = serializeBookingContext(context);
    return `/booking?${searchParams.toString()}`;
}
