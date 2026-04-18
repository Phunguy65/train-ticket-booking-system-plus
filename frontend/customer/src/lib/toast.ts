import { toast } from 'sonner';
import { ApiFailError, ApiTechnicalError } from './api/errors.ts';

export type ToastMessages = {
    network: string;
    unknown: string;
};

/**
 * Extract error message from various error types.
 * Handles ApiFailError, ApiTechnicalError, standard Error, and JSend response objects.
 *
 * @param error - the error to extract message from
 * @param fallback - fallback message if no message found
 */
export function getErrorMessage(error: unknown, fallback?: string): string {
    if (error instanceof ApiFailError || error instanceof ApiTechnicalError) {
        return error.message;
    }

    if (error instanceof Error) {
        return error.message;
    }

    // Handle raw JSend response objects (from TypeScript types)
    if (
        typeof error === 'object'
        && error !== null
        && 'data' in error
        && typeof (error as Record<string, unknown>).data === 'object'
    ) {
        const data = (error as { data?: { message?: string } }).data;
        if (data?.message) {
            return data.message;
        }
    }

    return fallback ?? 'An error occurred';
}

/**
 * Show a toast notification for a network/technical API error.
 *
 * Use this from mutation `onError` handlers (or `useEffect` after an error) to
 * surface transient infrastructure issues — connection failures, 5xx, etc.
 *
 * Business/validation errors (`ApiFailError`) are NOT toasted here because
 * they are expected to be rendered inline in the form via `Alert`.
 *
 * @param error - the error thrown by the mutation
 * @param messages - translated toast messages keyed by scenario
 */
export function showNetworkErrorToast(
    error: unknown,
    messages: ToastMessages,
): void {
    if (error instanceof ApiTechnicalError) {
        toast.error(messages.network);
        return;
    }

    if (error instanceof ApiFailError) {
        return;
    }

    toast.error(messages.unknown);
}

/**
 * Show a toast notification for any API error.
 *
 * @param error - the error thrown
 * @param messages - translated toast messages keyed by scenario
 */
export function showApiErrorToast(
    error: unknown,
    messages: ToastMessages & { fail?: string },
): void {
    if (error instanceof ApiTechnicalError) {
        toast.error(messages.network);
        return;
    }

    if (error instanceof ApiFailError) {
        toast.error(messages.fail ?? error.message ?? messages.unknown);
        return;
    }

    toast.error(messages.unknown);
}

/**
 * Show a success toast notification.
 *
 * @param message - translated success message
 */
export function showSuccessToast(message: string): void {
    toast.success(message);
}

/**
 * Show an info toast notification.
 *
 * @param message - translated info message
 */
export function showInfoToast(message: string): void {
    toast.info(message);
}

/**
 * Show a warning toast notification.
 *
 * @param message - translated warning message
 */
export function showWarningToast(message: string): void {
    toast.warning(message);
}

/**
 * Check if an error is a specific API error code.
 *
 * @param error - the error to check
 * @param code - the error code to check for
 */
export function isApiErrorCode(error: unknown, code: string): boolean {
    return error instanceof ApiFailError && error.code === code;
}

/**
 * Check if an error is a seat unavailable error.
 *
 * @param error - the error to check
 */
export function isSeatUnavailableError(error: unknown): boolean {
    return isApiErrorCode(error, 'SEAT_NOT_AVAILABLE');
}

/**
 * Check if an error is an unauthorized error.
 *
 * @param error - the error to check
 */
export function isUnauthorizedError(error: unknown): boolean {
    return (
        error instanceof ApiFailError
        && (error.statusCode === 401
            || error.code === 'USER_INVALID_CREDENTIALS'
            || error.code === 'USER_INVALID_REFRESH_TOKEN')
    );
}

/**
 * Check if an error is a not found error.
 *
 * @param error - the error to check
 */
export function isNotFoundError(error: unknown): boolean {
    return (
        error instanceof ApiFailError
        && (error.statusCode === 404
            || error.code === 'BOOKING_NOT_FOUND'
            || error.code === 'SCHEDULED_TRIP_NOT_FOUND'
            || error.code === 'USER_NOT_FOUND')
    );
}
