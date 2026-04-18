import { describe, expect, it } from 'vitest';
import { ApiFailError, ApiTechnicalError } from '@/lib/api/index.ts';
import { resolveLoginError, resolveRegisterError } from './auth-errors.ts';

const translations = {
    invalidCredentials: 'Invalid credentials',
    emailExists: 'Email already exists',
    unknownError: 'Unknown error',
};

describe('resolveLoginError', () => {
    it('returns invalidCredentials message for USER_INVALID_CREDENTIALS code', () => {
        const error = new ApiFailError({ code: 'USER_INVALID_CREDENTIALS' });

        const result = resolveLoginError(error, translations);

        expect(result).toEqual({ message: 'Invalid credentials' });
    });

    it('returns error message for other ApiFailError codes', () => {
        const error = new ApiFailError({
            code: 'ACCESS_DENIED',
            message: 'Custom error message',
        });

        const result = resolveLoginError(error, translations);

        expect(result).toEqual({ message: 'Custom error message' });
    });

    it('returns ApiFailError default message when error has no custom message', () => {
        // ApiFailError constructor sets default message "Request failed" when none provided
        const error = new ApiFailError({ code: 'ACCESS_DENIED' });

        const result = resolveLoginError(error, translations);

        // error.message is truthy ("Request failed"), so unknownError is NOT used
        expect(result).toEqual({ message: 'Request failed' });
    });

    it('returns null for ApiTechnicalError (network errors)', () => {
        const error = new ApiTechnicalError({ message: 'Network failed' });

        const result = resolveLoginError(error, translations);

        expect(result).toBeNull();
    });

    it('returns null for generic Error', () => {
        const error = new Error('Something went wrong');

        const result = resolveLoginError(error, translations);

        expect(result).toBeNull();
    });

    it('returns null for non-error values', () => {
        expect(resolveLoginError('string error', translations)).toBeNull();
        expect(resolveLoginError(null, translations)).toBeNull();
        expect(resolveLoginError(undefined, translations)).toBeNull();
    });
});

describe('resolveRegisterError', () => {
    it('returns emailExists message with showLoginLink=true for USER_EMAIL_ALREADY_EXISTS', () => {
        const error = new ApiFailError({ code: 'USER_EMAIL_ALREADY_EXISTS' });

        const result = resolveRegisterError(error, translations);

        expect(result).toEqual({
            message: 'Email already exists',
            showLoginLink: true,
        });
    });

    it('returns error message with showLoginLink=false for other ApiFailError codes', () => {
        const error = new ApiFailError({
            code: 'ACCESS_DENIED',
            message: 'Custom error message',
        });

        const result = resolveRegisterError(error, translations);

        expect(result).toEqual({
            message: 'Custom error message',
            showLoginLink: false,
        });
    });

    it('returns ApiFailError default message when error has no custom message', () => {
        // ApiFailError constructor sets default message "Request failed" when none provided
        const error = new ApiFailError({ code: 'ACCESS_DENIED' });

        const result = resolveRegisterError(error, translations);

        // error.message is truthy ("Request failed"), so unknownError is NOT used
        expect(result).toEqual({
            message: 'Request failed',
            showLoginLink: false,
        });
    });

    it('returns null for ApiTechnicalError (network errors)', () => {
        const error = new ApiTechnicalError({ message: 'Network failed' });

        const result = resolveRegisterError(error, translations);

        expect(result).toBeNull();
    });

    it('returns null for generic Error', () => {
        const error = new Error('Something went wrong');

        const result = resolveRegisterError(error, translations);

        expect(result).toBeNull();
    });
});
