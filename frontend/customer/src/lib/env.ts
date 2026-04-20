/**
 * Environment configuration for the customer frontend.
 *
 * All environment variables are read at build time and must be prefixed with
 * NEXT_PUBLIC_ to be accessible in the browser.
 */

/**
 * Maximum number of seats allowed per booking.
 * @default 5
 */
export const MAX_SEATS_PER_BOOKING =
    Number(process.env.NEXT_PUBLIC_MAX_SEATS_PER_BOOKING) || 5;
