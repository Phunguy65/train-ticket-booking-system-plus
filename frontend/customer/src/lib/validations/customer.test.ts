import { describe, expect, it } from 'vitest';
import { profileSchema, tripSearchSchema } from './customer.ts';

describe('tripSearchSchema', () => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    it('should validate valid search params', () => {
        const result = tripSearchSchema.safeParse({
            originStationId: 'station-1',
            destinationStationId: 'station-2',
            departureDate: today,
        });

        expect(result.success).toBe(true);
    });

    it('should reject empty origin', () => {
        const result = tripSearchSchema.safeParse({
            originStationId: '',
            destinationStationId: 'station-2',
            departureDate: today,
        });

        expect(result.success).toBe(false);
        if (!result.success) {
            expect(result.error.issues[0].path).toContain('originStationId');
            expect(result.error.issues[0].message).toBe('origin.required');
        }
    });

    it('should reject same origin and destination', () => {
        const result = tripSearchSchema.safeParse({
            originStationId: 'station-1',
            destinationStationId: 'station-1',
            departureDate: today,
        });

        expect(result.success).toBe(false);
        if (!result.success) {
            expect(result.error.issues[0].path).toContain(
                'destinationStationId',
            );
            expect(result.error.issues[0].message).toBe(
                'destination.sameAsOrigin',
            );
        }
    });

    it('should reject past departure date', () => {
        const yesterday = new Date();
        yesterday.setDate(yesterday.getDate() - 1);
        yesterday.setHours(0, 0, 0, 0);

        const result = tripSearchSchema.safeParse({
            originStationId: 'station-1',
            destinationStationId: 'station-2',
            departureDate: yesterday,
        });

        expect(result.success).toBe(false);
        if (!result.success) {
            expect(result.error.issues[0].path).toContain('departureDate');
            expect(result.error.issues[0].message).toBe('departureDate.past');
        }
    });
});

describe('profileSchema', () => {
    it('should validate valid profile data', () => {
        const result = profileSchema.safeParse({
            fullName: 'John Doe',
            email: 'john@example.com',
            phone: '0912345678',
            dateOfBirth: new Date('1990-01-01'),
            gender: 'male',
            idDocumentNumber: '123456789',
            addressLine: '123 Main St',
        });

        expect(result.success).toBe(true);
    });

    it('should validate minimal profile data', () => {
        const result = profileSchema.safeParse({
            fullName: 'John Doe',
            email: 'john@example.com',
        });

        expect(result.success).toBe(true);
    });

    it('should reject short full name', () => {
        const result = profileSchema.safeParse({
            fullName: 'J',
            email: 'john@example.com',
        });

        expect(result.success).toBe(false);
        if (!result.success) {
            expect(result.error.issues[0].path).toContain('fullName');
            expect(result.error.issues[0].message).toBe('fullName.minLength');
        }
    });

    it('should reject invalid email', () => {
        const result = profileSchema.safeParse({
            fullName: 'John Doe',
            email: 'invalid-email',
        });

        expect(result.success).toBe(false);
        if (!result.success) {
            expect(result.error.issues[0].path).toContain('email');
            expect(result.error.issues[0].message).toBe('email.invalid');
        }
    });

    it('should reject invalid phone number', () => {
        const result = profileSchema.safeParse({
            fullName: 'John Doe',
            email: 'john@example.com',
            phone: '12345',
        });

        expect(result.success).toBe(false);
        if (!result.success) {
            expect(result.error.issues[0].path).toContain('phone');
            expect(result.error.issues[0].message).toBe('phone.invalid');
        }
    });

    it('should accept valid Vietnamese phone numbers', () => {
        const validPhones = ['0912345678', '84912345678', '+84912345678'];

        for (const phone of validPhones) {
            const result = profileSchema.safeParse({
                fullName: 'John Doe',
                email: 'john@example.com',
                phone,
            });

            expect(result.success).toBe(true);
        }
    });
});
