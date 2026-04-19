'use client';

import { ChevronDownIcon, TrainFrontIcon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { Collapsible as CollapsiblePrimitive } from 'radix-ui';
import { useState } from 'react';
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.tsx';
import { formatDate, formatTime } from '@/lib/customer-utils.ts';
import { cn } from '@/lib/utils.ts';

/**
 * Trip information for the summary card.
 */
export type TripSummary = {
    trainName?: string;
    trainNumber?: string;
    originStation?: string;
    destinationStation?: string;
    departureTime?: string;
    arrivalTime?: string;
    departureDate?: string;
};

/**
 * Seat information for the summary card.
 */
export type SeatSummary = {
    id: string;
    seatNumber: string;
};

/**
 * Props for BookingSummaryCard component.
 */
export type BookingSummaryCardProps = {
    trip: TripSummary;
    seats?: SeatSummary[];
    className?: string;
    /**
     * Force expand state regardless of viewport.
     * @default false on mobile, true on desktop
     */
    forceExpanded?: boolean;
};

/**
 * Collapsible booking summary card that shows trip and seat information.
 * Mobile: Collapsed by default, shows key info in header.
 * Desktop: Always expanded.
 */
export function BookingSummaryCard({
    trip,
    seats,
    className,
    forceExpanded,
}: BookingSummaryCardProps) {
    const t = useTranslations('Booking');
    const [isOpen, setIsOpen] = useState(false);

    // Header content shown when collapsed on mobile
    const headerContent = (
        <div className='flex items-center justify-between'>
            <div className='flex items-center gap-3'>
                <div className='flex size-10 items-center justify-center rounded-full bg-primary/10 text-primary'>
                    <TrainFrontIcon className='size-5' />
                </div>
                <div>
                    <p className='font-medium'>
                        {trip.trainName || trip.trainNumber}
                    </p>
                    <p className='text-sm text-muted-foreground'>
                        {trip.originStation} → {trip.destinationStation}
                    </p>
                </div>
            </div>
            {/* Chevron for mobile collapsible */}
            <ChevronDownIcon
                className={cn(
                    'size-5 text-muted-foreground transition-transform lg:hidden',
                    isOpen && 'rotate-180',
                )}
            />
        </div>
    );

    // Detailed content
    const detailContent = (
        <div className='mt-4 space-y-3 border-t pt-4'>
            {/* Train Info */}
            {trip.trainNumber && (
                <div className='flex justify-between text-sm'>
                    <span className='text-muted-foreground'>{t('train')}</span>
                    <span>
                        {trip.trainName} ({trip.trainNumber})
                    </span>
                </div>
            )}

            {/* Departure */}
            <div className='flex justify-between text-sm'>
                <span className='text-muted-foreground'>
                    {trip.originStation}
                </span>
                <span>
                    {trip.departureTime ? formatTime(trip.departureTime) : '-'}
                </span>
            </div>

            {/* Arrival */}
            <div className='flex justify-between text-sm'>
                <span className='text-muted-foreground'>
                    {trip.destinationStation}
                </span>
                <span>
                    {trip.arrivalTime ? formatTime(trip.arrivalTime) : '-'}
                </span>
            </div>

            {/* Date */}
            {trip.departureDate && (
                <div className='flex justify-between text-sm'>
                    <span className='text-muted-foreground'>{t('date')}</span>
                    <span>{formatDate(trip.departureDate)}</span>
                </div>
            )}

            {/* Seats */}
            {seats && seats.length > 0 && (
                <div className='border-t pt-3'>
                    <p className='mb-2 text-sm text-muted-foreground'>
                        {t('seatsSummary')}
                    </p>
                    <div className='flex flex-wrap gap-2'>
                        {seats.map((seat) => (
                            <span
                                key={seat.id}
                                className='inline-flex items-center rounded-md bg-secondary px-2 py-1 text-xs font-medium'
                            >
                                {seat.seatNumber}
                            </span>
                        ))}
                    </div>
                </div>
            )}
        </div>
    );

    return (
        <Card className={cn('overflow-hidden', className)}>
            <CardHeader className='pb-0'>
                <CardTitle className='text-base'>{t('tripSummary')}</CardTitle>
            </CardHeader>
            <CardContent>
                {/* Desktop: Always expanded */}
                <div className='hidden lg:block'>
                    {headerContent}
                    {detailContent}
                </div>

                {/* Mobile: Collapsible */}
                <CollapsiblePrimitive.Root
                    open={forceExpanded ?? isOpen}
                    onOpenChange={setIsOpen}
                    className='lg:hidden'
                >
                    <CollapsiblePrimitive.Trigger
                        className='w-full text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 rounded-md'
                        asChild
                    >
                        <button type='button'>{headerContent}</button>
                    </CollapsiblePrimitive.Trigger>
                    <CollapsiblePrimitive.Content className='data-[state=open]:animate-collapsible-down data-[state=closed]:animate-collapsible-up overflow-hidden'>
                        {detailContent}
                    </CollapsiblePrimitive.Content>
                </CollapsiblePrimitive.Root>
            </CardContent>
        </Card>
    );
}
