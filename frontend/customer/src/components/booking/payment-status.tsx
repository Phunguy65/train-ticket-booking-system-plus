'use client';

import {
    AlertCircleIcon,
    CheckCircleIcon,
    ClockIcon,
    ExternalLinkIcon,
    Loader2Icon,
    RefreshCwIcon,
    XCircleIcon,
} from 'lucide-react';
import { useTranslations } from 'next-intl';
import { useCallback, useEffect, useState } from 'react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx';
import { Button } from '@/components/ui/button.tsx';
import { cn } from '@/lib/utils.ts';

/**
 * Payment UI state machine states.
 */
export type PaymentUIState =
    | 'PENDING'
    | 'REDIRECTING'
    | 'SUCCESS'
    | 'FAILED'
    | 'EXPIRED';

/**
 * Props for PaymentStatus component.
 */
export type PaymentStatusProps = {
    /**
     * Current payment state.
     */
    state: PaymentUIState;
    /**
     * Payment deadline for countdown (ISO string).
     * Required for PENDING state.
     */
    paymentDeadline?: string;
    /**
     * Checkout URL for payment redirect.
     */
    checkoutUrl?: string;
    /**
     * Callback for retry action (FAILED state).
     */
    onRetry?: () => void;
    /**
     * Callback for start over action (EXPIRED state).
     */
    onStartOver?: () => void;
    /**
     * Additional class names.
     */
    className?: string;
    /**
     * Compact mode for inline display.
     * @default false
     */
    compact?: boolean;
};

/**
 * Format remaining time from milliseconds to display string.
 */
function formatCountdown(ms: number): string {
    if (ms <= 0) return '0:00';

    const totalSeconds = Math.floor(ms / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;

    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
}

/**
 * Hook for countdown timer.
 */
function useCountdown(deadline: string | undefined) {
    const [remaining, setRemaining] = useState<number>(() => {
        if (!deadline) return 0;
        return Math.max(0, new Date(deadline).getTime() - Date.now());
    });

    useEffect(() => {
        if (!deadline) return;

        const calculateRemaining = () => {
            const ms = new Date(deadline).getTime() - Date.now();
            setRemaining(Math.max(0, ms));
        };

        calculateRemaining();
        const interval = setInterval(calculateRemaining, 1000);

        return () => clearInterval(interval);
    }, [deadline]);

    const isExpired = remaining <= 0;
    const isUrgent = remaining > 0 && remaining < 5 * 60 * 1000; // < 5 minutes

    return {
        remaining,
        isExpired,
        isUrgent,
        formatted: formatCountdown(remaining),
    };
}

/**
 * Pending payment state component with countdown.
 */
function PendingState({
    paymentDeadline,
    checkoutUrl,
    compact,
    onExpire,
}: {
    paymentDeadline?: string;
    checkoutUrl?: string;
    compact?: boolean;
    onExpire?: () => void;
}) {
    const t = useTranslations('Payment');
    const { formatted, isUrgent, isExpired } = useCountdown(paymentDeadline);

    // Notify parent when expired
    useEffect(() => {
        if (isExpired && onExpire) {
            onExpire();
        }
    }, [isExpired, onExpire]);

    if (compact) {
        return (
            <div className='flex items-center gap-2'>
                <ClockIcon
                    className={cn(
                        'size-4',
                        isUrgent ? 'text-destructive' : 'text-muted-foreground',
                    )}
                />
                <span
                    className={cn(
                        'text-sm font-medium',
                        isUrgent ? 'text-destructive' : 'text-foreground',
                    )}
                >
                    {formatted}
                </span>
                <span className='text-sm text-muted-foreground'>
                    {t('remaining')}
                </span>
            </div>
        );
    }

    return (
        <Alert
            variant='default'
            className={cn(isUrgent && 'border-destructive')}
        >
            <ClockIcon
                className={cn('size-4', isUrgent && 'text-destructive')}
            />
            <AlertTitle className={cn(isUrgent && 'text-destructive')}>
                {t('pending.title')}
            </AlertTitle>
            <AlertDescription className='space-y-3'>
                <p>{t('pending.description', { time: formatted })}</p>
                {isUrgent && (
                    <p className='font-medium text-destructive'>
                        {t('pending.urgent')}
                    </p>
                )}
                {checkoutUrl && (
                    <Button asChild className='mt-2'>
                        <a
                            href={checkoutUrl}
                            target='_blank'
                            rel='noopener noreferrer'
                        >
                            <ExternalLinkIcon className='mr-2 size-4' />
                            {t('pending.payNow')}
                        </a>
                    </Button>
                )}
            </AlertDescription>
        </Alert>
    );
}

/**
 * Redirecting state component.
 */
function RedirectingState({ compact }: { compact?: boolean }) {
    const t = useTranslations('Payment');

    if (compact) {
        return (
            <div className='flex items-center gap-2 text-primary'>
                <Loader2Icon className='size-4 animate-spin' />
                <span className='text-sm'>{t('redirecting.title')}</span>
            </div>
        );
    }

    return (
        <Alert>
            <Loader2Icon className='size-4 animate-spin' />
            <AlertTitle>{t('redirecting.title')}</AlertTitle>
            <AlertDescription>{t('redirecting.description')}</AlertDescription>
        </Alert>
    );
}

/**
 * Success state component.
 */
function SuccessState({ compact }: { compact?: boolean }) {
    const t = useTranslations('Payment');

    if (compact) {
        return (
            <div className='flex items-center gap-2 text-green-600'>
                <CheckCircleIcon className='size-4' />
                <span className='text-sm font-medium'>
                    {t('success.title')}
                </span>
            </div>
        );
    }

    return (
        <Alert variant='success'>
            <CheckCircleIcon className='size-4' />
            <AlertTitle>{t('success.title')}</AlertTitle>
            <AlertDescription>{t('success.description')}</AlertDescription>
        </Alert>
    );
}

/**
 * Failed state component.
 */
function FailedState({
    onRetry,
    compact,
}: {
    onRetry?: () => void;
    compact?: boolean;
}) {
    const t = useTranslations('Payment');

    if (compact) {
        return (
            <div className='flex items-center gap-2'>
                <XCircleIcon className='size-4 text-destructive' />
                <span className='text-sm text-destructive'>
                    {t('failed.title')}
                </span>
                {onRetry && (
                    <Button
                        variant='outline'
                        size='sm'
                        onClick={onRetry}
                        className='ml-2'
                    >
                        <RefreshCwIcon className='mr-1 size-3' />
                        {t('failed.retry')}
                    </Button>
                )}
            </div>
        );
    }

    return (
        <Alert variant='destructive'>
            <XCircleIcon className='size-4' />
            <AlertTitle>{t('failed.title')}</AlertTitle>
            <AlertDescription className='space-y-3'>
                <p>{t('failed.description')}</p>
                {onRetry && (
                    <Button variant='outline' size='sm' onClick={onRetry}>
                        <RefreshCwIcon className='mr-2 size-4' />
                        {t('failed.retry')}
                    </Button>
                )}
            </AlertDescription>
        </Alert>
    );
}

/**
 * Expired state component.
 */
function ExpiredState({
    onStartOver,
    compact,
}: {
    onStartOver?: () => void;
    compact?: boolean;
}) {
    const t = useTranslations('Payment');

    if (compact) {
        return (
            <div className='flex items-center gap-2'>
                <AlertCircleIcon className='size-4 text-destructive' />
                <span className='text-sm text-destructive'>
                    {t('expired.title')}
                </span>
                {onStartOver && (
                    <Button
                        variant='outline'
                        size='sm'
                        onClick={onStartOver}
                        className='ml-2'
                    >
                        {t('expired.startOver')}
                    </Button>
                )}
            </div>
        );
    }

    return (
        <Alert variant='destructive'>
            <AlertCircleIcon className='size-4' />
            <AlertTitle>{t('expired.title')}</AlertTitle>
            <AlertDescription className='space-y-3'>
                <p>{t('expired.description')}</p>
                {onStartOver && (
                    <Button variant='outline' size='sm' onClick={onStartOver}>
                        {t('expired.startOver')}
                    </Button>
                )}
            </AlertDescription>
        </Alert>
    );
}

/**
 * Payment status component that renders the appropriate UI
 * based on the current payment state.
 *
 * States:
 * - PENDING: Shows countdown timer and pay now button
 * - REDIRECTING: Shows loading spinner and redirect message
 * - SUCCESS: Shows success message
 * - FAILED: Shows error message with optional retry action
 * - EXPIRED: Shows expired message with optional start over action
 */
export function PaymentStatus({
    state,
    paymentDeadline,
    checkoutUrl,
    onRetry,
    onStartOver,
    className,
    compact = false,
}: PaymentStatusProps) {
    // Track local expired state from countdown
    const [localExpired, setLocalExpired] = useState(false);

    const handleExpire = useCallback(() => {
        setLocalExpired(true);
    }, []);

    // Derive effective state (countdown expiry overrides PENDING)
    const effectiveState =
        state === 'PENDING' && localExpired ? 'EXPIRED' : state;

    return (
        <div className={cn('w-full', className)} data-slot='payment-status'>
            {effectiveState === 'PENDING' && (
                <PendingState
                    paymentDeadline={paymentDeadline}
                    checkoutUrl={checkoutUrl}
                    compact={compact}
                    onExpire={handleExpire}
                />
            )}
            {effectiveState === 'REDIRECTING' && (
                <RedirectingState compact={compact} />
            )}
            {effectiveState === 'SUCCESS' && <SuccessState compact={compact} />}
            {effectiveState === 'FAILED' && (
                <FailedState onRetry={onRetry} compact={compact} />
            )}
            {effectiveState === 'EXPIRED' && (
                <ExpiredState onStartOver={onStartOver} compact={compact} />
            )}
        </div>
    );
}
