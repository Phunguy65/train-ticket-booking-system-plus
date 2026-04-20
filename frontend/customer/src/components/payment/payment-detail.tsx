'use client';

import { useQuery } from '@tanstack/react-query';
import {
    AlertCircleIcon,
    PrinterIcon,
    TrainIcon,
    UserIcon,
    UsersIcon,
} from 'lucide-react';
import { useTranslations } from 'next-intl';
import { PaymentStatusBadge } from '@/components/payment/index.ts';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx';
import { Badge } from '@/components/ui/badge.tsx';
import { Button } from '@/components/ui/button.tsx';
import {
    Card,
    CardContent,
    CardDescription,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.tsx';
import { Separator } from '@/components/ui/separator.tsx';
import { Skeleton } from '@/components/ui/skeleton.tsx';
import { Link } from '@/i18n/routing.ts';
import { getPaymentOptions } from '@/lib/api/index.ts';
import {
    formatDateTime,
    formatPrice,
    formatShortDate,
} from '@/lib/customer-utils.ts';
import { getErrorMessage } from '@/lib/toast.ts';

type PaymentDetailProps = {
    paymentId: string;
};

export function PaymentDetail({ paymentId }: PaymentDetailProps) {
    const t = useTranslations('PaymentDetail');

    const {
        data: payment,
        isLoading,
        isError,
        error,
        refetch,
    } = useQuery({
        ...getPaymentOptions({
            path: { paymentId },
        }),
    });

    // Loading state
    if (isLoading) {
        return (
            <>
                <Skeleton className='h-8 w-48 mb-6' />
                <Skeleton className='h-64' />
            </>
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

    // Not found state
    if (!payment) {
        return (
            <Alert variant='destructive'>
                <AlertCircleIcon className='h-4 w-4' />
                <AlertTitle>{t('notFound')}</AlertTitle>
            </Alert>
        );
    }

    const isPaid = payment.status === 'PAID';
    const booking = payment.booking;
    const trip = booking?.trip;
    const bookerInfo = booking?.bookerInfo;
    const passengers = booking?.passengers;

    // Build seat lookup for passenger mapping
    const seatMap = new Map(
        (booking?.seats || []).map((seat) => [seat.seatId, seat]),
    );

    return (
        <Card>
            <CardHeader>
                <div className='flex items-center justify-between'>
                    <div>
                        <CardTitle>{t('title')}</CardTitle>
                        <CardDescription>
                            {t('paymentId')}: {payment.paymentId?.slice(0, 8)}
                            ...
                        </CardDescription>
                    </div>
                    <PaymentStatusBadge status={payment.status} />
                </div>
            </CardHeader>
            <CardContent className='space-y-6'>
                {/* Payment Amount */}
                <div className='text-center py-4'>
                    <p className='text-4xl font-bold'>
                        {payment.amount
                            ? formatPrice(Number(payment.amount))
                            : '-'}
                    </p>
                    <p className='text-sm text-muted-foreground mt-1'>
                        {payment.createdAt && formatDateTime(payment.createdAt)}
                    </p>
                </div>

                <Separator />

                {/* Trip Information */}
                {trip && (
                    <>
                        <div className='space-y-3'>
                            <h3 className='font-medium flex items-center gap-2'>
                                <TrainIcon className='h-4 w-4' />
                                {t('tripInfo')}
                            </h3>
                            <div className='grid gap-2 text-sm'>
                                {trip.trainName && (
                                    <div className='flex justify-between'>
                                        <span className='text-muted-foreground'>
                                            {t('train')}
                                        </span>
                                        <span>
                                            {trip.trainName} ({trip.trainNumber}
                                            )
                                        </span>
                                    </div>
                                )}
                                <div className='flex justify-between'>
                                    <span className='text-muted-foreground'>
                                        {t('route')}
                                    </span>
                                    <span>
                                        {trip.origin} → {trip.destination}
                                    </span>
                                </div>
                                {trip.departureTime && (
                                    <div className='flex justify-between'>
                                        <span className='text-muted-foreground'>
                                            {t('departure')}
                                        </span>
                                        <span>
                                            {formatDateTime(trip.departureTime)}
                                        </span>
                                    </div>
                                )}
                                {trip.arrivalTime && (
                                    <div className='flex justify-between'>
                                        <span className='text-muted-foreground'>
                                            {t('arrival')}
                                        </span>
                                        <span>
                                            {formatDateTime(trip.arrivalTime)}
                                        </span>
                                    </div>
                                )}
                            </div>
                        </div>
                        <Separator />
                    </>
                )}

                {/* Seats */}
                {booking?.seats && booking.seats.length > 0 && (
                    <>
                        <div className='space-y-3'>
                            <h3 className='font-medium'>{t('seats')}</h3>
                            <div className='flex flex-wrap gap-2'>
                                {booking.seats.map((seat) => (
                                    <span
                                        key={seat.seatId}
                                        className='px-3 py-1 bg-muted rounded-md text-sm'
                                    >
                                        {t('coachSeat', {
                                            coach: seat.coachNumber,
                                            seat: seat.seatNumber,
                                        })}
                                    </span>
                                ))}
                            </div>
                        </div>
                        <Separator />
                    </>
                )}

                {/* Booker Information */}
                {bookerInfo && (
                    <div className='space-y-3'>
                        <h3 className='font-medium flex items-center gap-2'>
                            <UserIcon className='h-4 w-4' />
                            {t('bookerInfo')}
                        </h3>
                        <div className='grid gap-2 text-sm'>
                            <div className='flex justify-between'>
                                <span className='text-muted-foreground'>
                                    {t('bookerName')}
                                </span>
                                <span>{bookerInfo.fullName}</span>
                            </div>
                            {bookerInfo.email && (
                                <div className='flex justify-between'>
                                    <span className='text-muted-foreground'>
                                        {t('email')}
                                    </span>
                                    <span>{bookerInfo.email}</span>
                                </div>
                            )}
                            {bookerInfo.phone && (
                                <div className='flex justify-between'>
                                    <span className='text-muted-foreground'>
                                        {t('phone')}
                                    </span>
                                    <span>{bookerInfo.phone}</span>
                                </div>
                            )}
                        </div>
                    </div>
                )}

                {/* Passengers by Seat */}
                {passengers && passengers.length > 0 && (
                    <>
                        <Separator />
                        <div className='space-y-3'>
                            <h3 className='font-medium flex items-center gap-2'>
                                <UsersIcon className='h-4 w-4' />
                                {t('passengersInfo')}
                            </h3>
                            <div className='space-y-3'>
                                {passengers.map((passenger) => {
                                    const seat = seatMap.get(passenger.seatId);
                                    return (
                                        <div
                                            key={passenger.seatId}
                                            className='rounded-lg border p-3 text-sm'
                                        >
                                            <div className='mb-2 flex items-center justify-between'>
                                                <span className='font-medium'>
                                                    {passenger.fullName}
                                                </span>
                                                {seat && (
                                                    <Badge variant='secondary'>
                                                        {t('coachSeat', {
                                                            coach: seat.coachNumber,
                                                            seat: seat.seatNumber,
                                                        })}
                                                    </Badge>
                                                )}
                                            </div>
                                            <div className='space-y-1 text-muted-foreground'>
                                                <div>
                                                    {t('idDocument')}:{' '}
                                                    {passenger.idDocumentNumber}
                                                </div>
                                                <div>
                                                    {t('dateOfBirth')}:{' '}
                                                    {passenger.dateOfBirth
                                                        ? formatShortDate(
                                                              passenger.dateOfBirth,
                                                          )
                                                        : '-'}
                                                </div>
                                                <div>
                                                    {t('gender')}:{' '}
                                                    {passenger.gender}
                                                </div>
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        </div>
                    </>
                )}

                {/* Print Ticket Button - Only shown for PAID status */}
                {isPaid && booking?.id && (
                    <>
                        <Separator />
                        <div className='flex justify-center'>
                            <Button asChild>
                                <Link
                                    href={`/ticket/${booking.id}`}
                                    target='_blank'
                                >
                                    <PrinterIcon className='h-4 w-4 mr-2' />
                                    {t('printTicket')}
                                </Link>
                            </Button>
                        </div>
                    </>
                )}
            </CardContent>
        </Card>
    );
}
