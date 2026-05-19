import { describe, expect, it } from 'vitest';
import { loginSchema, registerSchema } from './auth.ts';

describe('loginSchema', () => {
    it('accepts a valid email + password', () => {
        const result = loginSchema.safeParse({
            email: 'user@example.com',
            password: 'secret123',
        });

        expect(result.success).toBe(true);
    });

    it('rejects an empty email with key "email.required"', () => {
        const result = loginSchema.safeParse({
            email: '',
            password: 'secret123',
        });

        expect(result.success).toBe(false);
        if (!result.success) {
            const emailIssue = result.error.issues.find((i) =>
                i.path.includes('email'),
            );
            expect(emailIssue?.message).toBe('email.required');
        }
    });

    it('rejects an invalid email with key "email.invalid"', () => {
        const result = loginSchema.safeParse({
            email: 'not-an-email',
            password: 'secret123',
        });

        expect(result.success).toBe(false);
        if (!result.success) {
            const emailIssue = result.error.issues.find((i) =>
                i.path.includes('email'),
            );
            expect(emailIssue?.message).toBe('email.invalid');
        }
    });

    it('rejects an empty password with key "password.required"', () => {
        const result = loginSchema.safeParse({
            email: 'user@example.com',
            password: '',
        });

        expect(result.success).toBe(false);
        if (!result.success) {
            const passwordIssue = result.error.issues.find((i) =>
                i.path.includes('password'),
            );
            expect(passwordIssue?.message).toBe('password.required');
        }
    });
});

describe('registerSchema', () => {
    const validPayload = {
        fullName: 'Nguyen Van A',
        email: 'user@example.com',
        password: 'secret123',
        confirmPassword: 'secret123',
    };

    it('accepts a valid payload', () => {
        const result = registerSchema.safeParse(validPayload);
        expect(result.success).toBe(true);
    });

    it('rejects fullName shorter than 2 characters with key "fullName.minLength"', () => {
        const result = registerSchema.safeParse({
            ...validPayload,
            fullName: 'A',
        });

        expect(result.success).toBe(false);
        if (!result.success) {
            const issue = result.error.issues.find((i) =>
                i.path.includes('fullName'),
            );
            expect(issue?.message).toBe('fullName.minLength');
        }
    });

    it('rejects an empty fullName with key "fullName.required"', () => {
        const result = registerSchema.safeParse({
            ...validPayload,
            fullName: '',
        });

        expect(result.success).toBe(false);
        if (!result.success) {
            const issue = result.error.issues.find((i) =>
                i.path.includes('fullName'),
            );
            expect(issue?.message).toBe('fullName.required');
        }
    });

    it('rejects password shorter than 8 characters with key "password.minLength"', () => {
        const result = registerSchema.safeParse({
            ...validPayload,
            password: 'short',
            confirmPassword: 'short',
        });

        expect(result.success).toBe(false);
        if (!result.success) {
            const issue = result.error.issues.find(
                (i) =>
                    i.path.includes('password')
                    && !i.path.includes('confirmPassword'),
            );
            expect(issue?.message).toBe('password.minLength');
        }
    });

    it('rejects mismatched password + confirmPassword with key "confirmPassword.mismatch"', () => {
        const result = registerSchema.safeParse({
            ...validPayload,
            confirmPassword: 'different-password',
        });

        expect(result.success).toBe(false);
        if (!result.success) {
            const issue = result.error.issues.find((i) =>
                i.path.includes('confirmPassword'),
            );
            expect(issue?.message).toBe('confirmPassword.mismatch');
        }
    });

    it('rejects an empty confirmPassword with key "confirmPassword.required"', () => {
        const result = registerSchema.safeParse({
            ...validPayload,
            confirmPassword: '',
        });

        expect(result.success).toBe(false);
        if (!result.success) {
            const issue = result.error.issues.find((i) =>
                i.path.includes('confirmPassword'),
            );
            expect(issue?.message).toBe('confirmPassword.required');
        }
    });

    it('rejects an invalid email with key "email.invalid"', () => {
        const result = registerSchema.safeParse({
            ...validPayload,
            email: 'not-an-email',
        });

        expect(result.success).toBe(false);
        if (!result.success) {
            const issue = result.error.issues.find((i) =>
                i.path.includes('email'),
            );
            expect(issue?.message).toBe('email.invalid');
        }
    });
});
