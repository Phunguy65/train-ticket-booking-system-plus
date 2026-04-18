'use client';

import { useQuery } from '@tanstack/react-query';
import { AlertCircleIcon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx';
import { Badge } from '@/components/ui/badge.tsx';
import { Button } from '@/components/ui/button.tsx';
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.tsx';
import { Skeleton } from '@/components/ui/skeleton.tsx';
import { Link } from '@/i18n/routing.ts';
import { getBookingOptions } from '@/lib/api/index.ts';
import type { BookingStatus } from '@/lib/customer-utils.ts';
import {
    formatDateTime,
    formatPrice,
    formatTime,
} from '@/lib/customer-utils.ts';
import { getErrorMessage } from '@/lib/toast.ts';

type BookingDetailProps = {
    bookingId: string;
};

export function BookingDetail({ bookingId }: BookingDetailProps) {
    const t = useTranslations('Booking.detail');
    const tStatus = useTranslations('Status');

    const {
        data: booking,
        isLoading,
        isError,
        error,
        refetch,
    } = useQuery({
        ...getBookingOptions({
            path: { id: bookingId },
            query: { request: {} },
        }),
    });

    // Loading state
    if (isLoading) {
        return (
            <div className='space-y-6'>
                <Skeleton className='h-8 w-48' />
                <div className='grid gap-6 md:grid-cols-2'>
                    <Skeleton className='h-64' />
                    <Skeleton className='h-64' />
                </div>
            </div>
        );
    }

    // Error state
    if (isError) {
        return (
            <Alert variant='destructive'>
                <AlertCircleIcon className='h-4 w-4' />
                <AlertTitle>{t('notFound')}</AlertTitle>
                <AlertDescription className='flex items-center gap-4'>
                    <span>{getErrorMessage(error, t('notFound'))}</span>
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

    if (!booking) {
        return (
            <Alert>
                <AlertCircleIcon className='h-4 w-4' />
                <AlertTitle>{t('notFound')}</AlertTitle>
            </Alert>
        );
    }

    const statusVariant = getStatusVariant(booking.status as BookingStatus);

    return (
        <div className='space-y-6'>
            <div className='flex flex-wrap items-center justify-between gap-4'>
                <h1 className='text-2xl font-bold'>{t('title')}</h1>
                <Badge variant={statusVariant}>
                    {tStatus(booking.status as BookingStatus)}
                </Badge>
            </div>

            <div className='grid gap-6 md:grid-cols-2'>
                {/* Booking Info */}
                <Card>
                    <CardHeader>
                        <CardTitle>{t('bookingId')}</CardTitle>
                    </CardHeader>
                    <CardContent className='space-y-3'>
                        <div className='flex justify-between'>
                            <span className='text-muted-foreground'>
                                {t('id')}
                            </span>
                            <span className='font-mono text-sm'>
                                {booking.id}
                            </span>
                        </div>
                        <div className='flex justify-between'>
                            <span className='text-muted-foreground'>
                                {t('status')}
                            </span>
                            <Badge variant={statusVariant}>
                                {tStatus(booking.status as BookingStatus)}
                            </Badge>
                        </div>
                        <div className='flex justify-between'>
                            <span className='text-muted-foreground'>
                                {t('createdAt')}
                            </span>
                            <span>
                                {booking.createdAt
                                    ? formatDateTime(booking.createdAt)
                                    : '-'}
                            </span>
                        </div>
                        {booking.paymentDeadline
                            && booking.status === 'HELD' && (
                                <div className='flex justify-between'>
                                    <span className='text-muted-foreground'>
                                        {t('paymentDeadline')}
                                    </span>
                                    <span className='text-destructive'>
                                        {formatDateTime(
                                            booking.paymentDeadline,
                                        )}
                                    </span>
                                </div>
                            )}
                        <div className='flex justify-between border-t pt-3'>
                            <span className='font-semibold'>{t('total')}</span>
                            <span className='text-lg font-bold text-primary'>
                                {booking.totalPrice
                                    ? formatPrice(booking.totalPrice)
                                    : '-'}
                            </span>
                        </div>
                    </CardContent>
                </Card>

                {/* Trip Info */}
                <Card>
                    <CardHeader>
                        <CardTitle>{t('trip')}</CardTitle>
                    </CardHeader>
                    <CardContent className='space-y-3'>
                        {booking.trip && (
                            <>
                                <div className='flex justify-between'>
                                    <span className='text-muted-foreground'>
                                        {booking.trip.train?.name}
                                    </span>
                                    <span>
                                        {booking.trip.train?.trainNumber}
                                    </span>
                                </div>
                                <div className='flex justify-between'>
                                    <span className='text-muted-foreground'>
                                        {booking.trip.route?.origin?.name}
                                    </span>
                                    <span>
                                        {booking.trip.departureTime
                                            ? formatTime(
                                                  booking.trip.departureTime,
                                              )
                                            : '-'}
                                    </span>
                                </div>
                                <div className='flex justify-between'>
                                    <span className='text-muted-foreground'>
                                        {booking.trip.route?.destination?.name}
                                    </span>
                                    <span>
                                        {booking.trip.arrivalTime
                                            ? formatTime(
                                                  booking.trip.arrivalTime,
                                              )
                                            : '-'}
                                    </span>
                                </div>
                            </>
                        )}
                    </CardContent>
                </Card>

                {/* Seats */}
                <Card>
                    <CardHeader>
                        <CardTitle>{t('seats')}</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className='flex flex-wrap gap-2'>
                            {booking.seats?.map((seat) => (
                                <span
                                    key={seat.id}
                                    className='inline-flex items-center rounded-md bg-secondary px-2 py-1 text-sm font-medium'
                                >
                                    {seat.seatNumber}
                                </span>
                            ))}
                        </div>
                    </CardContent>
                </Card>

                {/* Passenger Info */}
                <Card>
                    <CardHeader>
                        <CardTitle>{t('passenger')}</CardTitle>
                    </CardHeader>
                    <CardContent className='space-y-3'>
                        {booking.passengerInfo && (
                            <>
                                <div>{booking.passengerInfo.fullName}</div>
                                <div className='text-sm text-muted-foreground'>
                                    {booking.passengerInfo.email}
                                </div>
                                {booking.passengerInfo.phone && (
                                    <div className='text-sm text-muted-foreground'>
                                        {booking.passengerInfo.phone}
                                    </div>
                                )}
                            </>
                        )}
                    </CardContent>
                </Card>
            </div>

            {/* Payment Action */}
            {booking.status === 'HELD' && booking.payment?.checkoutUrl && (
                <div className='flex justify-end'>
                    <Button asChild>
                        <a
                            href={booking.payment.checkoutUrl}
                            target='_blank'
                            rel='noopener noreferrer'
                        >
                            {t('pay')}
                        </a>
                    </Button>
                </div>
            )}

            <div className='flex justify-start'>
                <Button variant='outline' asChild>
                    <Link href='/account'>{t('backToBookings')}</Link>
                </Button>
            </div>
        </div>
    );
}

function getStatusVariant(
    status: BookingStatus,
): 'default' | 'secondary' | 'destructive' | 'outline' {
    switch (status) {
        case 'CONFIRMED':
            return 'default';
        case 'HELD':
            return 'secondary';
        case 'CANCELLED':
            return 'destructive';
        default:
            return 'outline';
    }
}
