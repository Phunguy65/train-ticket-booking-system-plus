import type {
    BookingResponse,
    UserBookingResponse,
} from '@/lib/api/generated/types.gen.ts';

/**
 * Booking status type.
 */
export type BookingStatus = 'HELD' | 'CONFIRMED' | 'CANCELLED';

/**
 * Trip status type.
 */
export type TripStatus =
    | 'SCHEDULED'
    | 'BOARDING'
    | 'DEPARTED'
    | 'ARRIVED'
    | 'CANCELLED';

/**
 * Payment status type.
 */
export type PaymentStatus =
    | 'PENDING'
    | 'PAID'
    | 'CANCELLED'
    | 'FAILED'
    | 'REFUNDED';

/**
 * Format a price in VND minor units to a display string.
 *
 * @param amount - Price in minor units (e.g., 350000)
 * @param locale - Locale for formatting (default: 'vi-VN')
 */
export function formatPrice(amount: number, locale = 'vi-VN'): string {
    return new Intl.NumberFormat(locale, {
        style: 'currency',
        currency: 'VND',
        maximumFractionDigits: 0,
    }).format(amount);
}

/**
 * Format a date string or Date object to a localized date string.
 *
 * @param date - Date string (ISO) or Date object
 * @param locale - Locale for formatting
 */
export function formatDate(date: string | Date, locale = 'vi-VN'): string {
    const d = typeof date === 'string' ? new Date(date) : date;
    return new Intl.DateTimeFormat(locale, {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
    }).format(d);
}

/**
 * Format a date string or Date object to a localized short date string.
 *
 * @param date - Date string (ISO) or Date object
 * @param locale - Locale for formatting
 */
export function formatShortDate(date: string | Date, locale = 'vi-VN'): string {
    const d = typeof date === 'string' ? new Date(date) : date;
    return new Intl.DateTimeFormat(locale, {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
    }).format(d);
}

/**
 * Format a date string or Date object to a localized time string.
 *
 * @param date - Date string (ISO) or Date object
 * @param locale - Locale for formatting
 */
export function formatTime(date: string | Date, locale = 'vi-VN'): string {
    const d = typeof date === 'string' ? new Date(date) : date;
    return new Intl.DateTimeFormat(locale, {
        hour: '2-digit',
        minute: '2-digit',
    }).format(d);
}

/**
 * Format a date string or Date object to a localized datetime string.
 *
 * @param date - Date string (ISO) or Date object
 * @param locale - Locale for formatting
 */
export function formatDateTime(date: string | Date, locale = 'vi-VN'): string {
    const d = typeof date === 'string' ? new Date(date) : date;
    return new Intl.DateTimeFormat(locale, {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
    }).format(d);
}

/**
 * Format a duration in minutes to a human-readable string.
 *
 * @param minutes - Duration in minutes
 */
export function formatDuration(minutes: number): string {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;

    if (hours === 0) {
        return `${mins}m`;
    }

    if (mins === 0) {
        return `${hours}h`;
    }

    return `${hours}h ${mins}m`;
}

/**
 * Check if a booking can be cancelled.
 *
 * @param status - Booking status
 */
export function canCancelBooking(status: BookingStatus | undefined): boolean {
    return status === 'HELD';
}

/**
 * Check if a booking can proceed to payment.
 *
 * @param booking - Booking object
 */
export function canPayBooking(
    booking: BookingResponse | UserBookingResponse | undefined,
): boolean {
    if (!booking) return false;
    if (booking.status !== 'HELD') return false;

    // Check if payment deadline has passed
    if (booking.paymentDeadline) {
        const deadline = new Date(booking.paymentDeadline);
        if (deadline < new Date()) {
            return false;
        }
    }

    return true;
}

/**
 * Maximum seats allowed per booking.
 */
export const MAX_SEATS_PER_BOOKING = 5;

/**
 * Check if more seats can be added to the selection.
 *
 * @param currentCount - Current number of selected seats
 */
export function canAddMoreSeats(currentCount: number): boolean {
    return currentCount < MAX_SEATS_PER_BOOKING;
}

/**
 * Calculate total price from seat count and price per seat.
 *
 * @param seatCount - Number of seats
 * @param pricePerSeat - Price per seat in minor units
 */
export function calculateTotalPrice(
    seatCount: number,
    pricePerSeat: number,
): number {
    return seatCount * pricePerSeat;
}

/**
 * Generate a unique idempotency key for booking creation.
 */
export function generateIdempotencyKey(): string {
    return `booking-${Date.now()}-${Math.random().toString(36).slice(2, 11)}`;
}
