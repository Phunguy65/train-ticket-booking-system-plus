'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { AlertCircleIcon, Loader2Icon } from 'lucide-react';
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
import {
    StickyFooter,
    StickyFooterSpacer,
} from '@/components/ui/sticky-footer.tsx';
import { Link, useRouter } from '@/i18n/routing.ts';
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
    generateIdempotencyKey,
} from '@/lib/customer-utils.ts';
import { parseBookingContext } from '@/lib/search-params.ts';
import { isSeatUnavailableError, showApiErrorToast } from '@/lib/toast.ts';
import { BookingStepper } from './booking-stepper.tsx';
import {
    BookingSummaryCard,
    type SeatSummary,
    type TripSummary,
} from './booking-summary-card.tsx';
import { PaymentStatus, type PaymentUIState } from './payment-status.tsx';
import { PriceBreakdown, PriceSummary } from './price-breakdown.tsx';

export function BookingConfirmation() {
    const t = useTranslations('Booking');
    const tErrors = useTranslations('Errors');
    const searchParams = useSearchParams();
    const queryClient = useQueryClient();
    const router = useRouter();

    // Parse context from URL
    const context = useMemo(
        () => parseBookingContext(searchParams),
        [searchParams],
    );

    const [bookingResult, setBookingResult] = useState<{
        bookingId: string;
        paymentDeadline: string;
    } | null>(null);

    // Derive payment UI state
    const [paymentUIState, setPaymentUIState] =
        useState<PaymentUIState>('PENDING');

    // Fetch payment info after booking is created
    const { data: payment } = useQuery({
        ...getBookingPaymentOptions({
            path: { bookingId: bookingResult?.bookingId ?? '' },
            query: { request: {} },
        }),
        enabled: !!bookingResult?.bookingId,
    });

    // Auto-redirect to checkout URL when payment is loaded
    useEffect(() => {
        if (payment?.checkoutUrl) {
            setPaymentUIState('REDIRECTING');
            // Brief delay to show redirecting message before redirect
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

    // Handle start over from expired state
    const handleStartOver = () => {
        router.push('/');
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
                <Skeleton className='h-12 w-full' />
                <div className='grid gap-6 lg:grid-cols-[1fr,350px]'>
                    <Skeleton className='h-64' />
                    <div className='space-y-4'>
                        <Skeleton className='h-48' />
                        <Skeleton className='h-32' />
                    </div>
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

    // Calculate prices
    const pricePerSeat = routeTemplate?.basePrice ?? 0;
    const totalPrice = calculateTotalPrice(
        context.seatIds.length,
        pricePerSeat,
    );

    // Build trip summary
    const tripSummary: TripSummary = {
        trainName: train?.name,
        trainNumber: train?.trainNumber,
        originStation: originStation?.name,
        destinationStation: destinationStation?.name,
        departureTime: trip?.departureTime,
        arrivalTime: trip?.arrivalTime,
        departureDate: trip?.departureTime,
    };

    // Build seat summary
    const seatSummary: SeatSummary[] = context.seatIds.map((seatId) => {
        const seat = availableSeats?.content?.find((s) => s.id === seatId);
        return { id: seatId, seatNumber: seat?.seatNumber ?? seatId };
    });

    // Booking success - show payment status
    if (bookingResult) {
        return (
            <div className='space-y-6'>
                <BookingStepper currentStep='payment' tripId={context.tripId} />

                <div className='mx-auto max-w-2xl space-y-6'>
                    {/* Payment Status */}
                    <PaymentStatus
                        state={paymentUIState}
                        paymentDeadline={bookingResult.paymentDeadline}
                        checkoutUrl={payment?.checkoutUrl}
                        onStartOver={handleStartOver}
                    />

                    {/* Booking Reference */}
                    <Card>
                        <CardHeader>
                            <CardTitle>{t('detail.bookingId')}</CardTitle>
                        </CardHeader>
                        <CardContent>
                            <p className='font-mono text-lg'>
                                {bookingResult.bookingId}
                            </p>
                        </CardContent>
                    </Card>

                    {/* Trip Summary */}
                    <BookingSummaryCard
                        trip={tripSummary}
                        seats={seatSummary}
                    />

                    {/* Actions */}
                    <div className='flex flex-col gap-3 sm:flex-row sm:justify-center'>
                        <Button variant='outline' asChild>
                            <Link href={`/booking/${bookingResult.bookingId}`}>
                                {t('detail.title')}
                            </Link>
                        </Button>
                        <Button variant='outline' asChild>
                            <Link href='/account'>
                                {t('detail.backToBookings')}
                            </Link>
                        </Button>
                    </div>
                </div>
            </div>
        );
    }

    // Pre-booking review state
    return (
        <div className='space-y-6'>
            <BookingStepper currentStep='review' tripId={context.tripId} />

            <h1 className='text-2xl font-bold'>{t('title')}</h1>

            <div className='grid gap-6 lg:grid-cols-[1fr,350px]'>
                {/* Left Column: Trip & Passenger Info */}
                <div className='space-y-6'>
                    {/* Trip Summary Card */}
                    <BookingSummaryCard
                        trip={tripSummary}
                        seats={seatSummary}
                    />

                    {/* Passenger Info */}
                    <Card>
                        <CardHeader>
                            <CardTitle>{t('passengerInfo')}</CardTitle>
                        </CardHeader>
                        <CardContent className='space-y-2'>
                            <p className='font-medium'>{user?.fullName}</p>
                            <p className='text-sm text-muted-foreground'>
                                {user?.email}
                            </p>
                            {user?.phone && (
                                <p className='text-sm text-muted-foreground'>
                                    {user.phone}
                                </p>
                            )}
                        </CardContent>
                    </Card>
                </div>

                {/* Right Column: Price & Actions (Desktop) */}
                <div className='hidden lg:block lg:space-y-6'>
                    {/* Price Breakdown */}
                    <PriceBreakdown
                        pricePerSeat={pricePerSeat}
                        seatCount={context.seatIds.length}
                    />

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
                    <div className='flex flex-col gap-3'>
                        <Button
                            onClick={handleConfirm}
                            disabled={createBooking.isPending}
                            className='w-full'
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
                        <Button
                            variant='outline'
                            asChild
                            disabled={createBooking.isPending}
                        >
                            <Link href={`/trips/${context.tripId}/seats`}>
                                {t('backToSeats')}
                            </Link>
                        </Button>
                    </div>
                </div>
            </div>

            {/* Mobile: Price Breakdown */}
            <div className='lg:hidden'>
                <PriceBreakdown
                    pricePerSeat={pricePerSeat}
                    seatCount={context.seatIds.length}
                />
            </div>

            {/* Mobile Error Alert */}
            {createBooking.isError && (
                <Alert variant='destructive' className='lg:hidden'>
                    <AlertCircleIcon className='h-4 w-4' />
                    <AlertTitle>{t('error')}</AlertTitle>
                    <AlertDescription>
                        {isSeatUnavailableError(createBooking.error)
                            ? t('seatsUnavailable')
                            : tErrors('unknownError')}
                    </AlertDescription>
                </Alert>
            )}

            {/* Mobile Sticky Footer */}
            <StickyFooterSpacer />
            <StickyFooter>
                <div className='flex items-center justify-between gap-4'>
                    <PriceSummary total={totalPrice} />
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
            </StickyFooter>
        </div>
    );
}
