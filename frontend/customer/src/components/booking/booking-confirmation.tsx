'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
    AlertCircleIcon,
    CheckCircleIcon,
    ExternalLinkIcon,
    Loader2Icon,
} from 'lucide-react';
import { useSearchParams } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useEffect, useMemo, useState } from 'react';
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
    createBookingMutation,
    getAuthenticatedUserOptions,
    getAvailableSeatsOptions,
    getBookingPaymentOptions,
    getRouteTemplateOptions,
    getScheduledTripOptions,
    getStationOptions,
    getTrainOptions,
} from '@/lib/api/index.ts';
import {
    calculateTotalPrice,
    formatDateTime,
    formatPrice,
    formatTime,
    generateIdempotencyKey,
} from '@/lib/customer-utils.ts';
import { parseBookingContext } from '@/lib/search-params.ts';
import { isSeatUnavailableError, showApiErrorToast } from '@/lib/toast.ts';

export function BookingConfirmation() {
    const t = useTranslations('Booking');
    const tErrors = useTranslations('Errors');
    const searchParams = useSearchParams();
    const queryClient = useQueryClient();

    // Parse context from URL
    const context = useMemo(
        () => parseBookingContext(searchParams),
        [searchParams],
    );

    const [bookingResult, setBookingResult] = useState<{
        bookingId: string;
        paymentDeadline: string;
    } | null>(null);

    // Fetch payment info after booking is created
    const { data: payment, isLoading: paymentLoading } = useQuery({
        ...getBookingPaymentOptions({
            path: { bookingId: bookingResult?.bookingId ?? '' },
            query: { request: {} },
        }),
        enabled: !!bookingResult?.bookingId,
    });

    // Auto-redirect to checkout URL when payment is loaded
    useEffect(() => {
        if (payment?.checkoutUrl) {
            // Brief delay to show success message before redirect
            const timer = setTimeout(() => {
                window.location.href = payment.checkoutUrl as string;
            }, 2000);
            return () => clearTimeout(timer);
        }
    }, [payment?.checkoutUrl]);

    // Fetch authenticated user
    const { data: user, isLoading: userLoading } = useQuery({
        ...getAuthenticatedUserOptions(),
    });

    // Fetch trip details
    const { data: trip, isLoading: tripLoading } = useQuery({
        ...getScheduledTripOptions({
            path: { id: context?.tripId ?? '' },
            query: { request: {} },
        }),
        enabled: !!context?.tripId,
    });

    // Fetch route template for pricing info
    const { data: routeTemplate, isLoading: routeLoading } = useQuery({
        ...getRouteTemplateOptions({
            path: { id: trip?.routeTemplateId ?? '' },
            query: { request: {} },
        }),
        enabled: !!trip?.routeTemplateId,
    });

    // Fetch origin and destination stations for display
    const { data: originStation, isLoading: originLoading } = useQuery({
        ...getStationOptions({
            path: { id: routeTemplate?.originStationId ?? '' },
            query: { request: {} },
        }),
        enabled: !!routeTemplate?.originStationId,
    });

    const { data: destinationStation, isLoading: destinationLoading } =
        useQuery({
            ...getStationOptions({
                path: { id: routeTemplate?.destinationStationId ?? '' },
                query: { request: {} },
            }),
            enabled: !!routeTemplate?.destinationStationId,
        });

    // Fetch train for display info
    const { data: train, isLoading: trainLoading } = useQuery({
        ...getTrainOptions({
            path: { id: trip?.trainId ?? '' },
            query: { request: {} },
        }),
        enabled: !!trip?.trainId,
    });

    // Fetch seat details to show names
    const { data: availableSeats, isLoading: seatsLoading } = useQuery({
        ...getAvailableSeatsOptions({
            path: { scheduledTripId: context?.tripId ?? '' },
            query: { request: { size: 100 } },
        }),
        enabled: !!context?.tripId,
    });

    // Create booking mutation
    const createBooking = useMutation({
        ...createBookingMutation(),
        onSuccess: (data) => {
            // Invalidate bookings query
            queryClient.invalidateQueries({ queryKey: ['getUserBookings'] });

            if (data.id && data.paymentDeadline) {
                setBookingResult({
                    bookingId: data.id,
                    paymentDeadline: data.paymentDeadline,
                });
            }
        },
        onError: (error) => {
            if (isSeatUnavailableError(error)) {
                showApiErrorToast(error, {
                    network: tErrors('networkError'),
                    unknown: tErrors('unknownError'),
                    fail: t('seatsUnavailable'),
                });
            } else {
                showApiErrorToast(error, {
                    network: tErrors('networkError'),
                    unknown: tErrors('unknownError'),
                });
            }
        },
    });

    // Handle booking confirmation
    const handleConfirm = () => {
        if (!context) return;

        createBooking.mutate({
            body: {
                scheduledTripId: context.tripId,
                seatIds: context.seatIds,
                idempotencyKey: generateIdempotencyKey(),
            },
        });
    };

    // Loading state
    const isLoading =
        userLoading
        || tripLoading
        || routeLoading
        || originLoading
        || destinationLoading
        || trainLoading
        || seatsLoading;

    if (isLoading) {
        return (
            <div className='space-y-6'>
                <Skeleton className='h-8 w-48' />
                <div className='grid gap-6 md:grid-cols-2'>
                    <Skeleton className='h-48' />
                    <Skeleton className='h-48' />
                </div>
            </div>
        );
    }

    // Missing context
    if (!context) {
        return (
            <Alert>
                <AlertCircleIcon className='h-4 w-4' />
                <AlertTitle>{t('missingContext')}</AlertTitle>
                <AlertDescription className='space-y-4'>
                    <p>{t('missingContextDescription')}</p>
                    <Button asChild>
                        <Link href='/'>{t('backToSearch')}</Link>
                    </Button>
                </AlertDescription>
            </Alert>
        );
    }

    // Booking success
    if (bookingResult) {
        const isRedirecting = !!payment?.checkoutUrl;
        const isLoadingPayment = paymentLoading;

        return (
            <div className='mx-auto max-w-lg space-y-6'>
                <Alert variant='success'>
                    <CheckCircleIcon className='h-4 w-4' />
                    <AlertTitle>{t('success')}</AlertTitle>
                    <AlertDescription>
                        {t('successDescription', {
                            bookingId: bookingResult.bookingId,
                        })}
                    </AlertDescription>
                </Alert>

                <Card>
                    <CardContent className='space-y-4 p-6'>
                        <p className='text-sm text-muted-foreground'>
                            {t('paymentDeadline', {
                                deadline: formatDateTime(
                                    bookingResult.paymentDeadline,
                                ),
                            })}
                        </p>

                        {/* Payment redirect status */}
                        {isLoadingPayment && (
                            <div className='flex items-center gap-2 text-sm text-muted-foreground'>
                                <Loader2Icon className='h-4 w-4 motion-safe:animate-spin' />
                                <span>{t('loadingPayment')}</span>
                            </div>
                        )}

                        {isRedirecting && (
                            <div className='flex items-center gap-2 text-sm text-primary'>
                                <ExternalLinkIcon className='h-4 w-4' />
                                <span>{t('redirectingToPayment')}</span>
                            </div>
                        )}

                        <div className='flex flex-col gap-2'>
                            {payment?.checkoutUrl && (
                                <Button asChild>
                                    <a
                                        href={payment.checkoutUrl}
                                        target='_blank'
                                        rel='noopener noreferrer'
                                    >
                                        <ExternalLinkIcon className='mr-2 h-4 w-4' />
                                        {t('proceedToPayment')}
                                    </a>
                                </Button>
                            )}
                            <Button
                                variant={
                                    payment?.checkoutUrl ? 'outline' : 'default'
                                }
                                asChild
                            >
                                <Link
                                    href={`/booking/${bookingResult.bookingId}`}
                                >
                                    {t('detail.title')}
                                </Link>
                            </Button>
                            <Button variant='outline' asChild>
                                <Link href='/account'>
                                    {t('detail.backToBookings')}
                                </Link>
                            </Button>
                        </div>
                    </CardContent>
                </Card>
            </div>
        );
    }

    // Calculate prices
    const pricePerSeat = routeTemplate?.basePrice ?? 0;
    const totalPrice = calculateTotalPrice(
        context.seatIds.length,
        pricePerSeat,
    );

    // Find selected seat details
    const selectedSeatDetails = context.seatIds.map((seatId) => {
        const seat = availableSeats?.content?.find((s) => s.id === seatId);
        return { id: seatId, seatNumber: seat?.seatNumber ?? seatId };
    });

    return (
        <div className='space-y-6'>
            <h1 className='text-2xl font-bold'>{t('title')}</h1>

            <div className='grid gap-6 md:grid-cols-2'>
                {/* Trip Summary */}
                <Card>
                    <CardHeader>
                        <CardTitle>{t('tripSummary')}</CardTitle>
                    </CardHeader>
                    <CardContent className='space-y-3'>
                        <div className='flex justify-between'>
                            <span className='text-muted-foreground'>
                                {train?.name}
                            </span>
                            <span>{train?.trainNumber}</span>
                        </div>
                        <div className='flex justify-between'>
                            <span className='text-muted-foreground'>
                                {originStation?.name}
                            </span>
                            <span>
                                {trip?.departureTime
                                    ? formatTime(trip.departureTime)
                                    : '-'}
                            </span>
                        </div>
                        <div className='flex justify-between'>
                            <span className='text-muted-foreground'>
                                {destinationStation?.name}
                            </span>
                            <span>
                                {trip?.arrivalTime
                                    ? formatTime(trip.arrivalTime)
                                    : '-'}
                            </span>
                        </div>
                    </CardContent>
                </Card>

                {/* Passenger Info */}
                <Card>
                    <CardHeader>
                        <CardTitle>{t('passengerInfo')}</CardTitle>
                    </CardHeader>
                    <CardContent className='space-y-3'>
                        <div className='flex justify-between'>
                            <span className='text-muted-foreground'>
                                {user?.fullName}
                            </span>
                        </div>
                        <div className='flex justify-between'>
                            <span className='text-muted-foreground'>
                                {user?.email}
                            </span>
                        </div>
                        {user?.phone && (
                            <div className='flex justify-between'>
                                <span className='text-muted-foreground'>
                                    {user.phone}
                                </span>
                            </div>
                        )}
                    </CardContent>
                </Card>

                {/* Selected Seats */}
                <Card>
                    <CardHeader>
                        <CardTitle>{t('seatsSummary')}</CardTitle>
                    </CardHeader>
                    <CardContent>
                        <div className='flex flex-wrap gap-2'>
                            {selectedSeatDetails.map((seat) => (
                                <span
                                    key={seat.id}
                                    className='inline-flex items-center rounded-md bg-primary px-2 py-1 text-sm font-medium text-primary-foreground'
                                >
                                    {seat.seatNumber}
                                </span>
                            ))}
                        </div>
                    </CardContent>
                </Card>

                {/* Price Details */}
                <Card>
                    <CardHeader>
                        <CardTitle>{t('priceDetails')}</CardTitle>
                    </CardHeader>
                    <CardContent className='space-y-3'>
                        <div className='flex justify-between'>
                            <span className='text-muted-foreground'>
                                {t('pricePerSeat')}
                            </span>
                            <span>{formatPrice(pricePerSeat)}</span>
                        </div>
                        <div className='flex justify-between'>
                            <span className='text-muted-foreground'>
                                {t('totalSeats', {
                                    count: context.seatIds.length,
                                })}
                            </span>
                        </div>
                        <div className='flex justify-between border-t pt-3'>
                            <span className='font-semibold'>
                                {t('totalPrice')}
                            </span>
                            <span className='text-lg font-bold text-primary'>
                                {formatPrice(totalPrice)}
                            </span>
                        </div>
                    </CardContent>
                </Card>
            </div>

            {/* Error Alert */}
            {createBooking.isError && (
                <Alert variant='destructive'>
                    <AlertCircleIcon className='h-4 w-4' />
                    <AlertTitle>{t('error')}</AlertTitle>
                    <AlertDescription>
                        {isSeatUnavailableError(createBooking.error)
                            ? t('seatsUnavailable')
                            : tErrors('unknownError')}
                    </AlertDescription>
                </Alert>
            )}

            {/* Actions */}
            <div className='flex flex-col gap-3 sm:flex-row sm:justify-end'>
                <Button
                    variant='outline'
                    asChild
                    disabled={createBooking.isPending}
                >
                    <Link href={`/trips/${context.tripId}/seats`}>
                        {t('backToSeats')}
                    </Link>
                </Button>
                <Button
                    onClick={handleConfirm}
                    disabled={createBooking.isPending}
                >
                    {createBooking.isPending ? (
                        <>
                            <Loader2Icon className='mr-2 h-4 w-4 motion-safe:animate-spin' />
                            {t('confirming')}
                        </>
                    ) : (
                        t('confirm')
                    )}
                </Button>
            </div>
        </div>
    );
}
