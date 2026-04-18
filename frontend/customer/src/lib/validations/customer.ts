import { z } from 'zod';

/**
 * Trip search form schema.
 *
 * Validation messages are translation keys resolved at render time.
 */
export const tripSearchSchema = z
    .object({
        originStationId: z.string().min(1, { message: 'origin.required' }),
        destinationStationId: z
            .string()
            .min(1, { message: 'destination.required' }),
        departureDate: z.coerce.date({
            error: 'departureDate.required',
        }),
    })
    .refine((data) => data.originStationId !== data.destinationStationId, {
        path: ['destinationStationId'],
        message: 'destination.sameAsOrigin',
    })
    .refine(
        (data) => {
            const today = new Date();
            today.setHours(0, 0, 0, 0);
            return data.departureDate >= today;
        },
        {
            path: ['departureDate'],
            message: 'departureDate.past',
        },
    );

export type TripSearchFormValues = z.infer<typeof tripSearchSchema>;

/**
 * Profile edit form schema.
 */
export const profileSchema = z.object({
    fullName: z
        .string()
        .min(1, { message: 'fullName.required' })
        .min(2, { message: 'fullName.minLength' }),
    email: z
        .string()
        .min(1, { message: 'email.required' })
        .email({ message: 'email.invalid' }),
    phone: z
        .string()
        .regex(/^(\+84|84|0)?[0-9]{9,10}$/, { message: 'phone.invalid' })
        .optional()
        .or(z.literal('')),
    dateOfBirth: z.coerce.date().optional().nullable(),
    gender: z.string().optional().nullable(),
    idDocumentNumber: z.string().optional().nullable(),
    addressLine: z.string().optional().nullable(),
});

export type ProfileFormValues = z.infer<typeof profileSchema>;
