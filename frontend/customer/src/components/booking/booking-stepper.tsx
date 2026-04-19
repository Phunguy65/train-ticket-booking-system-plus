'use client';

import { useTranslations } from 'next-intl';
import { useCallback, useMemo } from 'react';
import { type Step, Stepper } from '@/components/ui/stepper.tsx';
import { useRouter } from '@/i18n/routing.ts';
import { cn } from '@/lib/utils.ts';

/**
 * Booking flow step identifiers.
 */
export type BookingStep = 'search' | 'seats' | 'review' | 'payment';

/**
 * Step index mapping for BookingStepper.
 */
export const BOOKING_STEP_INDEX: Record<BookingStep, number> = {
    search: 0,
    seats: 1,
    review: 2,
    payment: 3,
} as const;

/**
 * Props for BookingStepper component.
 */
export type BookingStepperProps = {
    /**
     * Current step in the booking flow.
     */
    currentStep: BookingStep;
    /**
     * Trip ID for backward navigation to seats page.
     */
    tripId?: string;
    /**
     * Callback for step navigation. If not provided, uses router navigation.
     */
    onStepClick?: (step: BookingStep) => void;
    /**
     * Additional class names.
     */
    className?: string;
};

/**
 * Booking-specific stepper with localized Search, Seats, Review, and Payment labels.
 * Supports backward-only navigation to previously completed steps.
 */
export function BookingStepper({
    currentStep,
    tripId,
    onStepClick,
    className,
}: BookingStepperProps) {
    const t = useTranslations('Stepper');
    const router = useRouter();

    // Build localized steps
    const steps: Step[] = useMemo(
        () => [
            { id: 'search', label: t('search') },
            { id: 'seats', label: t('seats') },
            { id: 'review', label: t('review') },
            { id: 'payment', label: t('payment') },
        ],
        [t],
    );

    const currentStepIndex = BOOKING_STEP_INDEX[currentStep];

    // Handle step navigation
    const handleStepClick = useCallback(
        (stepIndex: number) => {
            const stepId = Object.keys(BOOKING_STEP_INDEX).find(
                (key) => BOOKING_STEP_INDEX[key as BookingStep] === stepIndex,
            ) as BookingStep | undefined;

            if (!stepId) return;

            // Call custom handler if provided
            if (onStepClick) {
                onStepClick(stepId);
                return;
            }

            // Default router navigation
            switch (stepId) {
                case 'search':
                    router.push('/');
                    break;
                case 'seats':
                    if (tripId) {
                        router.push(`/trips/${tripId}/seats`);
                    }
                    break;
                case 'review':
                    // Can't navigate forward to review without context
                    break;
                case 'payment':
                    // Can't navigate directly to payment
                    break;
            }
        },
        [router, tripId, onStepClick],
    );

    // Mobile format label
    const mobileFormatLabel = useCallback(
        (current: number, total: number) => t('stepOf', { current, total }),
        [t],
    );

    return (
        <Stepper
            steps={steps}
            currentStep={currentStepIndex}
            onStepClick={handleStepClick}
            backwardOnly
            mobileFormatLabel={mobileFormatLabel}
            className={cn('mb-6', className)}
        />
    );
}
