import { z } from 'zod';

/**
 * Login form schema.
 *
 * Validation messages are translation keys (e.g., "email.required") that
 * will be resolved via next-intl's `useTranslations('Validation')` in the
 * form component. This keeps the schema locale-agnostic and lets the UI
 * translate messages at render time.
 */
export const loginSchema = z.object({
    email: z
        .string()
        .min(1, { message: 'email.required' })
        .email({ message: 'email.invalid' }),
    password: z.string().min(1, { message: 'password.required' }),
});

export type LoginFormValues = z.infer<typeof loginSchema>;

/**
 * Register form schema.
 *
 * - `password` must be at least 8 characters (matches backend)
 * - `confirmPassword` must match `password` (enforced via `.refine`)
 * - `fullName` must be at least 2 characters
 */
export const registerSchema = z
    .object({
        fullName: z
            .string()
            .min(1, { message: 'fullName.required' })
            .min(2, { message: 'fullName.minLength' }),
        email: z
            .string()
            .min(1, { message: 'email.required' })
            .email({ message: 'email.invalid' }),
        password: z
            .string()
            .min(1, { message: 'password.required' })
            .min(8, { message: 'password.minLength' }),
        confirmPassword: z
            .string()
            .min(1, { message: 'confirmPassword.required' }),
    })
    .refine((data) => data.password === data.confirmPassword, {
        path: ['confirmPassword'],
        message: 'confirmPassword.mismatch',
    });

export type RegisterFormValues = z.infer<typeof registerSchema>;
