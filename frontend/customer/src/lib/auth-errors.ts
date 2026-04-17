import { ApiFailError } from '@/lib/api/errors.ts';

/**
 * Login form error resolution result.
 */
export interface LoginFormError {
    message: string;
}

/**
 * Register form error resolution result.
 */
export interface RegisterFormError {
    message: string;
    showLoginLink: boolean;
}

/**
 * Resolve an API error to a translated user-facing message for the login form.
 * Returns `null` for technical/network errors (handled via toast).
 */
export function resolveLoginError(
    error: unknown,
    translations: {
        invalidCredentials: string;
        unknownError: string;
    },
): LoginFormError | null {
    if (error instanceof ApiFailError) {
        if (error.code === 'USER_INVALID_CREDENTIALS') {
            return { message: translations.invalidCredentials };
        }
        return { message: error.message || translations.unknownError };
    }
    return null;
}

/**
 * Resolve an API error to a translated user-facing message for the register form.
 * Returns `null` for technical/network errors (handled via toast).
 *
 * For USER_EMAIL_ALREADY_EXISTS, `showLoginLink` is true so the form can render
 * a login link alongside the error message.
 */
export function resolveRegisterError(
    error: unknown,
    translations: {
        emailExists: string;
        unknownError: string;
    },
): RegisterFormError | null {
    if (error instanceof ApiFailError) {
        if (error.code === 'USER_EMAIL_ALREADY_EXISTS') {
            return {
                message: translations.emailExists,
                showLoginLink: true,
            };
        }
        return {
            message: error.message || translations.unknownError,
            showLoginLink: false,
        };
    }
    return null;
}
