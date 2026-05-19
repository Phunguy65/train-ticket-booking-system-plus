'use client';

import {
    ArrowRightIcon,
    ClockIcon,
    TrainFrontIcon,
    UsersIcon,
} from 'lucide-react';
import { useTranslations } from 'next-intl';
import { Badge } from '@/components/ui/badge.tsx';
import { Button } from '@/components/ui/button.tsx';
import { Card, CardContent } from '@/components/ui/card.tsx';
import { Link } from '@/i18n/routing.ts';
import type { SearchScheduledTripsResponse } from '@/lib/api/generated/types.gen.ts';
import {
    formatDuration,
    formatPrice,
    formatShortDate,
    formatTime,
} from '@/lib/customer-utils.ts';

type TripCardProps = {
    trip: SearchScheduledTripsResponse;
};

export function TripCard({ trip }: TripCardProps) {
    const t = useTranslations('Trips');

    const hasAvailableSeats = (trip.availableSeatCount ?? 0) > 0;

    return (
        <Card className='transition-all duration-200 hover:shadow-md hover:-translate-y-0.5 hover:border-primary/20'>
            <CardContent className='p-4 sm:p-6'>
                <div className='flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between'>
                    {/* Train Info */}
                    <div className='flex items-center gap-3'>
                        <div className='flex h-10 w-10 items-center justify-center rounded-full bg-primary/10'>
                            <TrainFrontIcon className='h-5 w-5 text-primary' />
                        </div>
                        <div>
                            <p className='font-semibold'>{trip.train?.name}</p>
                            <p className='text-sm text-muted-foreground'>
                                {trip.train?.trainNumber}
                            </p>
                        </div>
                    </div>

                    {/* Route & Time */}
                    <div className='flex flex-1 items-center justify-center gap-4 sm:gap-8'>
                        <div className='text-center'>
                            <p className='text-lg font-semibold'>
                                {trip.departureTime
                                    ? formatTime(trip.departureTime)
                                    : '-'}
                            </p>
                            {trip.departureTime && (
                                <p className='text-xs text-muted-foreground'>
                                    {formatShortDate(trip.departureTime)}
                                </p>
                            )}
                            <p className='text-sm text-muted-foreground'>
                                {trip.route?.origin?.name}
                            </p>
                        </div>

                        <div className='flex flex-col items-center gap-1'>
                            <div className='flex items-center gap-2 text-muted-foreground'>
                                <ClockIcon className='h-4 w-4' />
                                <span className='text-sm'>
                                    {trip.durationMinutes
                                        ? formatDuration(trip.durationMinutes)
                                        : '-'}
                                </span>
                            </div>
                            <div className='flex w-24 items-center'>
                                <div className='h-px flex-1 bg-border' />
                                <ArrowRightIcon className='h-4 w-4 text-muted-foreground' />
                            </div>
                        </div>

                        <div className='text-center'>
                            <p className='text-lg font-semibold'>
                                {trip.arrivalTime
                                    ? formatTime(trip.arrivalTime)
                                    : '-'}
                            </p>
                            {trip.arrivalTime && (
                                <p className='text-xs text-muted-foreground'>
                                    {formatShortDate(trip.arrivalTime)}
                                </p>
                            )}
                            <p className='text-sm text-muted-foreground'>
                                {trip.route?.destination?.name}
                            </p>
                        </div>
                    </div>

                    {/* Price & Availability */}
                    <div className='flex flex-col items-end gap-2'>
                        <p className='text-xl font-bold text-primary'>
                            {trip.route?.basePrice
                                ? formatPrice(trip.route.basePrice)
                                : '-'}
                        </p>
                        <div className='flex items-center gap-2'>
                            <UsersIcon className='h-4 w-4 text-muted-foreground' />
                            {hasAvailableSeats ? (
                                <Badge variant='secondary'>
                                    {t('available', {
                                        count: trip.availableSeatCount ?? 0,
                                    })}
                                </Badge>
                            ) : (
                                <Badge variant='destructive'>
                                    {t('noSeats')}
                                </Badge>
                            )}
                        </div>
                    </div>

                    {/* Action */}
                    <div className='flex sm:ml-4'>
                        <Button
                            asChild
                            disabled={!hasAvailableSeats}
                            className='w-full sm:w-auto'
                        >
                            <Link href={`/trips/${trip.id}/seats`}>
                                {t('selectSeats')}
                            </Link>
                        </Button>
                    </div>
                </div>
            </CardContent>
        </Card>
    );
}
