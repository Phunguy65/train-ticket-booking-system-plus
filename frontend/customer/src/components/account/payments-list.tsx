'use client';

import { useQuery } from '@tanstack/react-query';
import { AlertCircleIcon, CreditCardIcon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { PaymentStatusBadge } from '@/components/payment/index.ts';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx';
import { Button } from '@/components/ui/button.tsx';
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.tsx';
import { Skeleton } from '@/components/ui/skeleton.tsx';
import { Link } from '@/i18n/routing.ts';
import {
    getAuthenticatedUserOptions,
    getUserPaymentsOptions,
} from '@/lib/api/index.ts';
import type { PaymentStatus } from '@/lib/customer-utils.ts';
import { formatDateTime, formatPrice } from '@/lib/customer-utils.ts';
import { getErrorMessage } from '@/lib/toast.ts';

export function PaymentsList() {
    const t = useTranslations('AccountPayments');

    // Get authenticated user for userId
    const { data: user } = useQuery({
        ...getAuthenticatedUserOptions(),
    });

    const {
        data: payments,
        isLoading,
        isError,
        error,
        refetch,
    } = useQuery({
        ...getUserPaymentsOptions({
            path: { userId: user?.id ?? '' },
            query: { request: { page: 0, size: 20 } },
        }),
        enabled: !!user?.id,
    });

    // Loading state
    if (isLoading) {
        return (
            <div className='space-y-4'>
                {[1, 2, 3].map((id) => (
                    <Skeleton key={`payment-skeleton-${id}`} className='h-32' />
                ))}
            </div>
        );
    }

    // Error state
    if (isError) {
        return (
            <Alert variant='destructive'>
                <AlertCircleIcon className='h-4 w-4' />
                <AlertTitle>{t('error')}</AlertTitle>
                <AlertDescription className='flex items-center gap-4'>
                    <span>{getErrorMessage(error, t('error'))}</span>
                    <Button
                        variant='outline'
                        size='sm'
                        onClick={() => refetch()}
                    >
                        {t('retry')}
                    </Button>
                </AlertDescription>
            </Alert>
        );
    }

    // Empty state
    if (!payments?.content || payments.content.length === 0) {
        return (
            <div className='flex flex-col items-center justify-center py-12 text-center'>
                <CreditCardIcon className='h-12 w-12 text-muted-foreground' />
                <h2 className='mt-4 text-lg font-semibold'>{t('empty')}</h2>
                <p className='mt-2 text-muted-foreground'>
                    {t('emptyDescription')}
                </p>
                <Button className='mt-4' asChild>
                    <Link href='/'>{t('searchTrips')}</Link>
                </Button>
            </div>
        );
    }

    return (
        <div className='space-y-4'>
            {payments.content.map((payment) => (
                <PaymentCard key={payment.id} payment={payment} />
            ))}
        </div>
    );
}

type PaymentCardProps = {
    payment: {
        id?: string;
        status?: PaymentStatus;
        amount?: number;
        currency?: string;
        createdAt?: string;
        bookingId?: string;
        booking?: {
            origin?: string;
            destination?: string;
            departureTime?: string;
        };
    };
};

function PaymentCard({ payment }: PaymentCardProps) {
    const t = useTranslations('AccountPayments');

    return (
        <Card>
            <CardHeader className='flex flex-row items-center justify-between space-y-0 pb-2'>
                <CardTitle className='text-sm font-medium'>
                    {payment.booking?.origin} → {payment.booking?.destination}
                </CardTitle>
                <PaymentStatusBadge status={payment.status} />
            </CardHeader>
            <CardContent>
                <div className='flex flex-wrap items-center justify-between gap-4'>
                    <div className='space-y-1'>
                        <p className='text-2xl font-bold'>
                            {payment.amount ? formatPrice(payment.amount) : '-'}
                        </p>
                        <p className='text-xs text-muted-foreground'>
                            {payment.createdAt
                                ? formatDateTime(payment.createdAt)
                                : '-'}
                        </p>
                        {payment.booking?.departureTime && (
                            <p className='text-xs text-muted-foreground'>
                                {t('departureDate')}:{' '}
                                {formatDateTime(payment.booking.departureTime)}
                            </p>
                        )}
                    </div>

                    <div className='flex gap-2'>
                        <Button variant='outline' size='sm' asChild>
                            <Link href={`/payment/${payment.id}`}>
                                {t('viewDetails')}
                            </Link>
                        </Button>
                    </div>
                </div>
            </CardContent>
        </Card>
    );
}
