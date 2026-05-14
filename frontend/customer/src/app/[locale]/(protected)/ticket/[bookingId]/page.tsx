'use client';

import { useQuery } from '@tanstack/react-query';
import { AlertCircleIcon, PrinterIcon, ShareIcon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { use, useCallback, useEffect, useState } from 'react';
import { TicketPrint } from '@/components/ticket/index.ts';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx';
import { Button } from '@/components/ui/button.tsx';
import { Skeleton } from '@/components/ui/skeleton.tsx';
import {
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from '@/components/ui/tabs.tsx';
import { getBookingOptions } from '@/lib/api/index.ts';
import { formatDateTime } from '@/lib/customer-utils.ts';
import { getErrorMessage } from '@/lib/toast.ts';

type Props = {
    params: Promise<{ bookingId: string }>;
};

export default function TicketPage({ params }: Props) {
    const { bookingId } = use(params);
    const t = useTranslations('Ticket');
    const [canShare, setCanShare] = useState(false);

    // Check Web Share API support on client side
    useEffect(() => {
        setCanShare(typeof navigator !== 'undefined' && !!navigator.share);
    }, []);

    const {
        data: booking,
        isLoading,
        isError,
        error,
        refetch,
    } = useQuery({
        ...getBookingOptions({
            path: { id: bookingId },
        }),
    });

    const handlePrint = useCallback(() => {
        window.print();
    }, []);

    const handleShare = useCallback(async () => {
        if (!navigator.share || !booking) return;

        try {
            await navigator.share({
                title: t('shareTitle'),
                text: t('shareText', {
                    origin: booking.trip?.route?.origin?.name,
                    destination: booking.trip?.route?.destination?.name,
                }),
                url: window.location.href,
            });
        } catch (err) {
            // User cancelled or share failed - silently ignore
            console.debug('Share cancelled or failed:', err);
        }
    }, [booking, t]);

    // Loading state
    if (isLoading) {
        return (
            <div className='container mx-auto max-w-md px-4 py-8 md:py-12'>
                <Skeleton className='h-[500px]' />
            </div>
        );
    }

    // Error state
    if (isError) {
        return (
            <div className='container mx-auto max-w-md px-4 py-8 md:py-12'>
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
            </div>
        );
    }

    // Not found or no booking
    if (!booking) {
        return (
            <div className='container mx-auto max-w-md px-4 py-8 md:py-12'>
                <Alert variant='destructive'>
                    <AlertCircleIcon className='h-4 w-4' />
                    <AlertTitle>{t('notFound')}</AlertTitle>
                </Alert>
            </div>
        );
    }

    // Check if booking is confirmed (paid)
    const isConfirmed = booking.status === 'CONFIRMED';
    if (!isConfirmed) {
        return (
            <div className='container mx-auto max-w-md px-4 py-8 md:py-12'>
                <Alert>
                    <AlertCircleIcon className='h-4 w-4' />
                    <AlertTitle>{t('notPaid')}</AlertTitle>
                    <AlertDescription>
                        {t('notPaidDescription')}
                    </AlertDescription>
                </Alert>
            </div>
        );
    }

    const trip = booking.trip;
    const passengers = booking.passengers;
    const seats = booking.seats || [];
    const seatMap = new Map(seats.map((seat) => [seat.seatId, seat]));
    const tickets =
        passengers && passengers.length > 0
            ? passengers.map((passenger) => ({
                  passenger,
                  seat: seatMap.get(passenger.seatId),
              }))
            : seats.map((seat) => ({
                  passenger: {
                      fullName: booking.passengerInfo?.fullName,
                      idDocumentNumber: booking.passengerInfo?.idDocumentNumber,
                  },
                  seat,
              }));
    const tripSummary = {
        trainName: trip?.train?.name,
        trainNumber: trip?.train?.trainNumber,
        origin: trip?.route?.origin?.name,
        destination: trip?.route?.destination?.name,
        departureTime: trip?.departureTime,
        arrivalTime: trip?.arrivalTime,
    };
    const renderTicket = (
        { passenger, seat }: (typeof tickets)[number],
        index: number,
    ) => (
        <TicketPrint
            key={seat?.seatId || `${bookingId}-${index}`}
            bookingId={bookingId}
            passenger={{
                fullName: passenger.fullName,
                idDocumentNumber: passenger.idDocumentNumber,
            }}
            trip={tripSummary}
            seat={{
                seatId: seat?.seatId,
                coachNumber: seat?.coachNumber,
                seatNumber: seat?.seatNumber,
            }}
        />
    );
    const getTicketValue = (
        { seat }: (typeof tickets)[number],
        index: number,
    ) => seat?.seatId || `${bookingId}-${index}`;
    const getTicketTabLabel = ({
        passenger,
        seat,
    }: (typeof tickets)[number]) => {
        const passengerName = passenger.fullName || t('passenger');
        const seatLabel = t('coachSeat', {
            coach: seat?.coachNumber ?? '-',
            seat: seat?.seatNumber ?? '-',
        });

        return `${passengerName} · ${seatLabel}`;
    };

    return (
        <div className='container mx-auto max-w-md px-4 py-8 md:py-12'>
            <div className='mb-6 flex justify-center gap-4 print:hidden'>
                <Button onClick={handlePrint} className='gap-2'>
                    <PrinterIcon className='h-4 w-4' />
                    {t('print')}
                </Button>
                {canShare && (
                    <Button
                        variant='outline'
                        onClick={handleShare}
                        className='gap-2'
                    >
                        <ShareIcon className='h-4 w-4' />
                        {t('share')}
                    </Button>
                )}
            </div>

            <div className='ticket-summary-header sticky top-0 z-10 mb-6 rounded-lg bg-background/95 p-4 backdrop-blur-sm print:hidden md:static md:bg-muted/50 md:backdrop-blur-none'>
                <p className='text-xs font-medium text-muted-foreground'>
                    {t('bookingId')}
                </p>
                <p className='font-mono text-sm font-semibold'>{bookingId}</p>
                <div className='mt-3 grid grid-cols-2 gap-3 text-sm'>
                    <div>
                        <p className='text-xs text-muted-foreground'>
                            {t('route')}
                        </p>
                        <p className='font-medium'>
                            {tripSummary.origin} → {tripSummary.destination}
                        </p>
                    </div>
                    <div>
                        <p className='text-xs text-muted-foreground'>
                            {t('ticketCount')}
                        </p>
                        <p className='font-medium'>
                            {t('ticketCountValue', { count: tickets.length })}
                        </p>
                    </div>
                    <div className='col-span-2'>
                        <p className='text-xs text-muted-foreground'>
                            {t('train')}
                        </p>
                        <p className='font-medium'>
                            {tripSummary.trainName}{' '}
                            {tripSummary.trainNumber
                                && `(${tripSummary.trainNumber})`}{' '}
                            ·{' '}
                            {tripSummary.departureTime
                                && formatDateTime(tripSummary.departureTime)}
                        </p>
                    </div>
                </div>
            </div>

            {tickets.length >= 2 ? (
                <Tabs defaultValue={getTicketValue(tickets[0], 0)}>
                    <TabsList className='w-full justify-start overflow-x-auto print:hidden'>
                        {tickets.map((ticket, index) => (
                            <TabsTrigger
                                key={getTicketValue(ticket, index)}
                                value={getTicketValue(ticket, index)}
                                className='max-w-56 shrink-0 truncate'
                            >
                                {getTicketTabLabel(ticket)}
                            </TabsTrigger>
                        ))}
                    </TabsList>
                    {tickets.map((ticket, index) => (
                        <TabsContent
                            key={getTicketValue(ticket, index)}
                            value={getTicketValue(ticket, index)}
                            className='data-[state=inactive]:hidden print:data-[state=inactive]:block'
                            forceMount
                        >
                            {renderTicket(ticket, index)}
                        </TabsContent>
                    ))}
                </Tabs>
            ) : (
                tickets.map(renderTicket)
            )}
        </div>
    );
}
