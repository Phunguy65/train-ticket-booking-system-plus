import { toast } from 'sonner';
import { ApiFailError, ApiTechnicalError } from './api/errors.ts';

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
    messages: { network: string; unknown: string },
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
