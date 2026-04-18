'use client';

import { useQuery } from '@tanstack/react-query';
import {
    AlertCircleIcon,
    Loader2Icon,
    WifiIcon,
    WifiOffIcon,
} from 'lucide-react';
import { useTranslations } from 'next-intl';
import { useCallback, useEffect, useMemo, useState } from 'react';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx';
import { Button } from '@/components/ui/button.tsx';
import { Skeleton } from '@/components/ui/skeleton.tsx';
import {
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from '@/components/ui/tabs.tsx';
import { useRouter } from '@/i18n/routing.ts';
import type {
    CoachSeatMapResponse,
    Seat,
} from '@/lib/api/generated/types.gen.ts';
import {
    getCoachSeatMapOptions,
    getRouteTemplateOptions,
    getScheduledTripOptions,
} from '@/lib/api/index.ts';
import {
    calculateTotalPrice,
    canAddMoreSeats,
    formatPrice,
    MAX_SEATS_PER_BOOKING,
} from '@/lib/customer-utils.ts';
import {
    mergeSeatsWithUpdates,
    reconcileSelectedSeats,
    type SeatSseEventName,
    type SeatSseUpdate,
    type SseConnectionStatus,
    useSeatSSE,
} from '@/lib/hooks/use-seat-sse.ts';
import { buildBookingUrl } from '@/lib/search-params.ts';
import { getErrorMessage, showInfoToast } from '@/lib/toast.ts';
import { SeatGrid } from './seat-grid.tsx';
import { SeatLegend } from './seat-legend.tsx';

type SeatSelectionProps = {
    tripId: string;
};

/**
 * Connection status indicator component for SSE state.
 */
function ConnectionStatusIndicator({
    status,
}: {
    status: SseConnectionStatus;
}) {
    const t = useTranslations('Seats');

    if (status === 'disconnected') {
        return null;
    }

    const statusConfig = {
        connecting: {
            icon: <Loader2Icon className='h-3 w-3 animate-spin' />,
            text: t('sse.connecting'),
            className: 'text-muted-foreground',
        },
        connected: {
            icon: <WifiIcon className='h-3 w-3' />,
            text: t('sse.connected'),
            className: 'text-green-600 dark:text-green-400',
        },
        reconnecting: {
            icon: <WifiOffIcon className='h-3 w-3' />,
            text: t('sse.reconnecting'),
            className: 'text-yellow-600 dark:text-yellow-400',
        },
    }[status];

    if (!statusConfig) {
        return null;
    }

    return (
        <div
            className={`flex items-center gap-1.5 text-xs ${statusConfig.className}`}
        >
            {statusConfig.icon}
            <span>{statusConfig.text}</span>
        </div>
    );
}

export function SeatSelection({ tripId }: SeatSelectionProps) {
    const t = useTranslations('Seats');
    const router = useRouter();
    const [selectedSeats, setSelectedSeats] = useState<Set<string>>(new Set());
    const [activeCoach, setActiveCoach] = useState<string | undefined>();
    // Local seat state for SSE updates - starts empty, populated from seatMap
    const [liveSeatMap, setLiveSeatMap] = useState<Map<string, Seat>>(
        new Map(),
    );

    // Fetch trip details
    const {
        data: trip,
        isLoading: tripLoading,
        isError: tripError,
    } = useQuery({
        ...getScheduledTripOptions({
            path: { id: tripId },
            query: { request: {} },
        }),
    });

    // Fetch route template for pricing
    const { data: routeTemplate, isLoading: routeLoading } = useQuery({
        ...getRouteTemplateOptions({
            path: { id: trip?.routeTemplateId ?? '' },
            query: { request: {} },
        }),
        enabled: !!trip?.routeTemplateId,
    });

    // Fetch seat map
    const {
        data: seatMap,
        isLoading: seatMapLoading,
        isError: seatMapError,
        error,
        refetch,
    } = useQuery({
        ...getCoachSeatMapOptions({
            path: { scheduledTripId: tripId },
            query: { request: { size: 100 } },
        }),
        enabled: !!tripId,
    });

    // Initialize live seat map from query data
    useEffect(() => {
        if (seatMap?.seats) {
            const seatMapEntries = seatMap.seats
                .filter((s): s is Seat & { id: string } => !!s.id)
                .map((s) => [s.id, s] as const);
            setLiveSeatMap(new Map(seatMapEntries));
        }
    }, [seatMap]);

    // Handle SSE seat updates
    const handleSeatUpdate = useCallback(
        (updates: SeatSseUpdate[], _eventName: SeatSseEventName) => {
            setLiveSeatMap((prev) => {
                const seats = Array.from(prev.values());
                const mergedSeats = mergeSeatsWithUpdates(seats, updates);
                const newMap = new Map<string, Seat>();
                for (const seat of mergedSeats) {
                    if (seat.id) {
                        newMap.set(seat.id, seat);
                    }
                }
                return newMap;
            });

            // Reconcile selected seats - remove any that became unavailable
            setSelectedSeats((prev) => {
                const reconciled = reconcileSelectedSeats(prev, updates);
                // If seats were removed, notify user
                if (reconciled.size < prev.size) {
                    const removedCount = prev.size - reconciled.size;
                    showInfoToast(
                        t('seatsUnavailable', { count: removedCount }),
                    );
                }
                return reconciled;
            });
        },
        [t],
    );

    // Subscribe to SSE seat updates
    const { connectionStatus } = useSeatSSE({
        scheduledTripId: tripId,
        enabled: !!tripId && !seatMapLoading && !seatMapError,
        onSeatUpdate: handleSeatUpdate,
    });

    // Set initial active coach
    // The API returns a single CoachSeatMapResponse, wrap it in an array for consistency
    const coaches: CoachSeatMapResponse[] = useMemo(() => {
        if (!seatMap) return [];
        // Merge live seat updates into coach data
        const liveSeats = Array.from(liveSeatMap.values());
        return [
            {
                ...seatMap,
                seats: liveSeats.length > 0 ? liveSeats : seatMap.seats,
            },
        ];
    }, [seatMap, liveSeatMap]);

    // Set default coach when data loads
    useEffect(() => {
        if (coaches.length > 0 && !activeCoach) {
            setActiveCoach(coaches[0]?.id);
        }
    }, [coaches, activeCoach]);

    // Calculate total price
    const pricePerSeat = routeTemplate?.basePrice ?? 0;
    const totalPrice = calculateTotalPrice(selectedSeats.size, pricePerSeat);

    // Handle seat selection
    const handleSeatToggle = (seatId: string, isAvailable: boolean) => {
        if (!isAvailable) return;

        setSelectedSeats((prev) => {
            const next = new Set(prev);
            if (next.has(seatId)) {
                next.delete(seatId);
            } else {
                if (!canAddMoreSeats(next.size)) {
                    showInfoToast(
                        t('maxSeatsReached', { count: MAX_SEATS_PER_BOOKING }),
                    );
                    return prev;
                }
                next.add(seatId);
            }
            return next;
        });
    };

    // Handle continue to booking
    const handleContinue = () => {
        if (selectedSeats.size === 0) return;

        const url = buildBookingUrl({
            tripId,
            seatIds: Array.from(selectedSeats),
        });
        router.push(url);
    };

    // Loading state
    if (tripLoading || routeLoading || seatMapLoading) {
        return (
            <div className='space-y-6'>
                <Skeleton className='h-8 w-64' />
                <div className='grid gap-6 lg:grid-cols-[1fr,300px]'>
                    <div className='space-y-4'>
                        <Skeleton className='h-10 w-full' />
                        <Skeleton className='h-[400px] w-full' />
                    </div>
                    <div className='space-y-4'>
                        <Skeleton className='h-32 w-full' />
                        <Skeleton className='h-10 w-full' />
                    </div>
                </div>
            </div>
        );
    }

    // Error state
    if (tripError || seatMapError) {
        return (
            <Alert variant='destructive'>
                <AlertCircleIcon className='h-4 w-4' />
                <AlertTitle>
                    {tripError ? t('tripNotFound') : t('error')}
                </AlertTitle>
                <AlertDescription className='flex items-center gap-4'>
                    <span>{getErrorMessage(error, t('error'))}</span>
                    {!tripError && (
                        <Button
                            variant='outline'
                            size='sm'
                            onClick={() => refetch()}
                        >
                            {t('retry')}
                        </Button>
                    )}
                </AlertDescription>
            </Alert>
        );
    }

    // No coaches
    if (coaches.length === 0) {
        return (
            <Alert>
                <AlertCircleIcon className='h-4 w-4' />
                <AlertTitle>{t('error')}</AlertTitle>
                <AlertDescription>{t('tripNotFound')}</AlertDescription>
            </Alert>
        );
    }

    return (
        <div className='space-y-6'>
            <div className='flex items-center justify-between'>
                <h1 className='text-2xl font-bold'>{t('title')}</h1>
                <ConnectionStatusIndicator status={connectionStatus} />
            </div>

            <div className='grid gap-6 lg:grid-cols-[1fr,320px]'>
                {/* Seat Map */}
                <div className='space-y-4'>
                    <SeatLegend />

                    <Tabs
                        value={activeCoach}
                        onValueChange={setActiveCoach}
                        className='w-full'
                    >
                        <TabsList className='w-full'>
                            {coaches.map((coach) => (
                                <TabsTrigger
                                    key={coach.id}
                                    value={coach.id ?? ''}
                                    className='flex-1'
                                >
                                    {t('coach', {
                                        number: coach.carNumber ?? 1,
                                    })}
                                </TabsTrigger>
                            ))}
                        </TabsList>

                        {coaches.map((coach) => (
                            <TabsContent key={coach.id} value={coach.id ?? ''}>
                                <SeatGrid
                                    seats={coach.seats ?? []}
                                    selectedSeats={selectedSeats}
                                    onSeatToggle={handleSeatToggle}
                                />
                            </TabsContent>
                        ))}
                    </Tabs>
                </div>

                {/* Selection Summary */}
                <div className='lg:sticky lg:top-20'>
                    <div className='rounded-lg border bg-card p-4 shadow-sm'>
                        <h2 className='mb-4 font-semibold'>
                            {t('selectedSeats')}
                        </h2>

                        {selectedSeats.size === 0 ? (
                            <p className='text-sm text-muted-foreground'>
                                {t('noSeatsSelected')}
                            </p>
                        ) : (
                            <div className='space-y-3'>
                                <div className='flex flex-wrap gap-2'>
                                    {Array.from(selectedSeats).map((seatId) => {
                                        // Find seat info
                                        const seat = coaches
                                            .flatMap((c) => c.seats ?? [])
                                            .find((s) => s.id === seatId);
                                        return (
                                            <span
                                                key={seatId}
                                                className='inline-flex items-center rounded-md bg-primary px-2 py-1 text-xs font-medium text-primary-foreground'
                                            >
                                                {seat?.seatNumber ?? seatId}
                                            </span>
                                        );
                                    })}
                                </div>

                                <div className='border-t pt-3'>
                                    <div className='flex justify-between text-sm'>
                                        <span className='text-muted-foreground'>
                                            {t('totalPrice')}
                                        </span>
                                        <span className='font-semibold'>
                                            {formatPrice(totalPrice)}
                                        </span>
                                    </div>
                                </div>
                            </div>
                        )}

                        <Button
                            className='mt-4 w-full'
                            disabled={selectedSeats.size === 0}
                            onClick={handleContinue}
                        >
                            {t('continue')}
                        </Button>

                        <p className='mt-2 text-center text-xs text-muted-foreground'>
                            {t('maxSeats', { count: MAX_SEATS_PER_BOOKING })}
                        </p>
                    </div>
                </div>
            </div>
        </div>
    );
}
