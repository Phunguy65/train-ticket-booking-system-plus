'use client';

import { cva, type VariantProps } from 'class-variance-authority';
import { CheckIcon } from 'lucide-react';
import type * as React from 'react';

import { cn } from '@/lib/utils.ts';

/**
 * Step state for the stepper component.
 */
export type StepState = 'completed' | 'current' | 'upcoming';

/**
 * Individual step configuration.
 */
export type Step = {
    id: string | number;
    label: string;
    description?: string;
};

/**
 * Props for the Stepper component.
 */
export type StepperProps = {
    steps: Step[];
    currentStep: number;
    onStepClick?: (stepIndex: number) => void;
    className?: string;
    /**
     * Only allow navigation to completed steps (backward navigation).
     * @default true
     */
    backwardOnly?: boolean;
};

const stepVariants = cva(
    'relative flex items-center justify-center rounded-full border-2 transition-colors',
    {
        variants: {
            state: {
                completed: 'border-primary bg-primary text-primary-foreground',
                current: 'border-primary bg-background text-primary',
                upcoming: 'border-muted bg-background text-muted-foreground',
            },
            size: {
                default: 'size-8 text-sm',
                sm: 'size-6 text-xs',
            },
        },
        defaultVariants: {
            state: 'upcoming',
            size: 'default',
        },
    },
);

const connectorVariants = cva('h-0.5 flex-1 transition-colors', {
    variants: {
        state: {
            completed: 'bg-primary',
            upcoming: 'bg-muted',
        },
    },
    defaultVariants: {
        state: 'upcoming',
    },
});

/**
 * Desktop stepper component showing full step labels.
 */
function StepperDesktop({
    steps,
    currentStep,
    onStepClick,
    backwardOnly = true,
    className,
}: StepperProps) {
    const getStepState = (index: number): StepState => {
        if (index < currentStep) return 'completed';
        if (index === currentStep) return 'current';
        return 'upcoming';
    };

    const isClickable = (index: number): boolean => {
        if (!onStepClick) return false;
        if (backwardOnly) return index < currentStep;
        return index !== currentStep;
    };

    return (
        <nav aria-label='Progress' className={cn('hidden md:block', className)}>
            <ol className='flex items-center gap-2'>
                {steps.map((step, index) => {
                    const state = getStepState(index);
                    const clickable = isClickable(index);

                    return (
                        <li
                            key={step.id}
                            className='flex flex-1 items-center gap-2 last:flex-initial'
                        >
                            <button
                                type='button'
                                onClick={() =>
                                    clickable && onStepClick?.(index)
                                }
                                disabled={!clickable}
                                className={cn(
                                    'group flex items-center gap-2',
                                    clickable &&
                                        'cursor-pointer hover:opacity-80',
                                    !clickable && 'cursor-default',
                                )}
                                aria-current={
                                    state === 'current' ? 'step' : undefined
                                }
                            >
                                <span
                                    className={cn(
                                        stepVariants({
                                            state,
                                            size: 'default',
                                        }),
                                    )}
                                >
                                    {state === 'completed' ? (
                                        <CheckIcon className='size-4' />
                                    ) : (
                                        <span>{index + 1}</span>
                                    )}
                                </span>
                                <span
                                    className={cn(
                                        'text-sm font-medium whitespace-nowrap',
                                        state === 'current' &&
                                            'text-foreground',
                                        state === 'completed' &&
                                            'text-foreground',
                                        state === 'upcoming' &&
                                            'text-muted-foreground',
                                    )}
                                >
                                    {step.label}
                                </span>
                            </button>

                            {/* Connector line */}
                            {index < steps.length - 1 && (
                                <div
                                    className={cn(
                                        connectorVariants({
                                            state:
                                                index < currentStep
                                                    ? 'completed'
                                                    : 'upcoming',
                                        }),
                                        'min-w-8',
                                    )}
                                    aria-hidden='true'
                                />
                            )}
                        </li>
                    );
                })}
            </ol>
        </nav>
    );
}

/**
 * Mobile stepper component showing compact "Step X of Y" format.
 */
function StepperMobile({
    steps,
    currentStep,
    onStepClick,
    backwardOnly = true,
    className,
    /**
     * Format function for mobile label. Receives (current, total).
     * @default "Step {current} of {total}"
     */
    formatLabel,
}: StepperProps & {
    formatLabel?: (current: number, total: number) => string;
}) {
    const defaultFormat = (current: number, total: number) =>
        `Step ${current} of ${total}`;
    const format = formatLabel ?? defaultFormat;

    return (
        <nav aria-label='Progress' className={cn('md:hidden', className)}>
            <div className='flex items-center gap-3'>
                {/* Progress dots */}
                <div className='flex items-center gap-1.5'>
                    {steps.map((step, index) => {
                        const state =
                            index < currentStep
                                ? 'completed'
                                : index === currentStep
                                  ? 'current'
                                  : 'upcoming';
                        const clickable =
                            onStepClick &&
                            (backwardOnly
                                ? index < currentStep
                                : index !== currentStep);

                        return (
                            <button
                                key={step.id}
                                type='button'
                                onClick={() =>
                                    clickable && onStepClick?.(index)
                                }
                                disabled={!clickable}
                                className={cn(
                                    'size-2 rounded-full transition-colors',
                                    state === 'completed' && 'bg-primary',
                                    state === 'current' && 'bg-primary',
                                    state === 'upcoming' && 'bg-muted',
                                    clickable && 'cursor-pointer',
                                    !clickable && 'cursor-default',
                                )}
                                aria-label={`${step.label}${state === 'current' ? ' (current)' : ''}`}
                            />
                        );
                    })}
                </div>

                {/* Step label */}
                <span className='text-sm text-muted-foreground'>
                    {format(currentStep + 1, steps.length)}
                </span>
            </div>
        </nav>
    );
}

/**
 * Responsive stepper component that shows full labels on desktop
 * and compact dots on mobile.
 */
function Stepper({
    steps,
    currentStep,
    onStepClick,
    backwardOnly = true,
    className,
    mobileFormatLabel,
}: StepperProps & {
    mobileFormatLabel?: (current: number, total: number) => string;
}) {
    return (
        <div className={className} data-slot='stepper'>
            <StepperDesktop
                steps={steps}
                currentStep={currentStep}
                onStepClick={onStepClick}
                backwardOnly={backwardOnly}
            />
            <StepperMobile
                steps={steps}
                currentStep={currentStep}
                onStepClick={onStepClick}
                backwardOnly={backwardOnly}
                formatLabel={mobileFormatLabel}
            />
        </div>
    );
}

export {
    Stepper,
    StepperDesktop,
    StepperMobile,
    stepVariants,
    connectorVariants,
};
