'use client';

import { useTranslations } from 'next-intl';
import type { Seat } from '@/lib/api/generated/types.gen.ts';
import { cn } from '@/lib/utils.ts';

type SeatGridProps = {
    seats: Seat[];
    selectedSeats: Set<string>;
    onSeatToggle: (seatId: string, isAvailable: boolean) => void;
};

export function SeatGrid({
    seats,
    selectedSeats,
    onSeatToggle,
}: SeatGridProps) {
    const t = useTranslations('Seats');

    // Sort seats by seat number for consistent display
    const sortedSeats = [...seats].sort((a, b) => {
        const numA = parseInt(a.seatNumber?.replace(/\D/g, '') ?? '0', 10);
        const numB = parseInt(b.seatNumber?.replace(/\D/g, '') ?? '0', 10);
        return numA - numB;
    });

    return (
        <div className='rounded-lg border bg-muted/30 p-4'>
            <div className='grid grid-cols-4 gap-2 sm:grid-cols-6 md:grid-cols-8'>
                {sortedSeats.map((seat) => {
                    const seatId = seat.id ?? '';
                    const isAvailable = seat.status === 'AVAILABLE';
                    const isSelected = selectedSeats.has(seatId);
                    const isBooked = seat.status === 'BOOKED';
                    const isHeld = seat.status === 'HELD';

                    return (
                        <button
                            key={seat.id}
                            type='button'
                            onClick={() => onSeatToggle(seatId, isAvailable)}
                            disabled={!isAvailable && !isSelected}
                            aria-label={
                                isSelected
                                    ? t('deselectSeat', {
                                          number: seat.seatNumber ?? '',
                                      })
                                    : isAvailable
                                      ? t('selectSeat', {
                                            number: seat.seatNumber ?? '',
                                        })
                                      : t('unavailable')
                            }
                            aria-pressed={isSelected}
                            className={cn(
                                'flex h-10 w-full items-center justify-center rounded-md border text-xs font-medium transition-colors',
                                'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                                {
                                    // Available
                                    'border-primary/50 bg-background hover:bg-primary/10':
                                        isAvailable && !isSelected,
                                    // Selected
                                    'border-primary bg-primary text-primary-foreground':
                                        isSelected,
                                    // Booked
                                    'cursor-not-allowed border-muted bg-muted text-muted-foreground':
                                        isBooked,
                                    // Held
                                    'cursor-not-allowed border-yellow-500/50 bg-yellow-500/10 text-yellow-700 dark:text-yellow-400':
                                        isHeld,
                                },
                            )}
                        >
                            {seat.seatNumber}
                        </button>
                    );
                })}
            </div>
        </div>
    );
}
