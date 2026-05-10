'use client';

import { useQuery } from '@tanstack/react-query';
import { AlertCircleIcon, PrinterIcon, ShareIcon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { use, useCallback, useEffect, useState } from 'react';
import { TicketPrint } from '@/components/ticket/index.ts';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx';
import { Button } from '@/components/ui/button.tsx';
import { Skeleton } from '@/components/ui/skeleton.tsx';
import { getBookingOptions } from '@/lib/api/index.ts';
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
            <div className='container max-w-md px-4 py-8'>
                <Skeleton className='h-[500px]' />
            </div>
        );
    }

    // Error state
    if (isError) {
        return (
            <div className='container max-w-md px-4 py-8'>
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
            <div className='container max-w-md px-4 py-8'>
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
            <div className='container max-w-md px-4 py-8'>
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

    return (
        <div className='container max-w-md px-4 py-8'>
            {/* Action buttons - hidden when printing */}
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

            {/* Printable ticket content */}
            <TicketPrint
                bookingId={bookingId}
                passengers={
                    passengers && passengers.length > 0
                        ? passengers.map((p) => ({
                              seatId: p.seatId,
                              fullName: p.fullName,
                              idDocumentNumber: p.idDocumentNumber,
                          }))
                        : undefined
                }
                trip={{
                    trainName: trip?.train?.name,
                    trainNumber: trip?.train?.trainNumber,
                    origin: trip?.route?.origin?.name,
                    destination: trip?.route?.destination?.name,
                    departureTime: trip?.departureTime,
                    arrivalTime: trip?.arrivalTime,
                }}
                seats={seats.map((s) => ({
                    seatId: s.seatId,
                    coachNumber: s.coachNumber,
                    seatNumber: s.seatNumber,
                }))}
            />
        </div>
    );
}
