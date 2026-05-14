'use client';

import { useTranslations } from 'next-intl';
import { formatDateTime } from '@/lib/customer-utils.ts';
import { TicketQRCode } from './ticket-qr-code.tsx';

type PassengerInfo = {
    fullName?: string;
    idDocumentNumber?: string;
};

type SeatInfo = {
    seatId?: string;
    coachNumber?: number;
    seatNumber?: string;
};

type TicketPrintProps = {
    bookingId: string;
    passenger: PassengerInfo;
    trip: {
        trainName?: string;
        trainNumber?: string;
        origin?: string;
        destination?: string;
        departureTime?: string;
        arrivalTime?: string;
    };
    seat: SeatInfo;
};

export function TicketPrint({
    bookingId,
    passenger,
    trip,
    seat,
}: TicketPrintProps) {
    const t = useTranslations('Ticket');
    const passengerName = passenger.fullName || t('passenger');
    const seatLabel = t('coachSeat', {
        coach: seat.coachNumber ?? '-',
        seat: seat.seatNumber ?? '-',
    });

    return (
        <article
            aria-label={t('ticketAriaLabel', {
                passenger: passengerName,
            })}
            className='ticket-card ticket-content bg-white p-6 max-w-md mx-auto border border-gray-200 rounded-lg print:border-black print:rounded-none print:break-after-page'
        >
            <div className='mb-6 flex items-start justify-between gap-4'>
                <div>
                    <h1 className='text-2xl font-bold text-primary print:text-black'>
                        VietRail
                    </h1>
                    <p className='text-sm text-muted-foreground print:text-gray-600'>
                        {t('electronicTicket')}
                    </p>
                </div>
            </div>

            <div className='mb-6 rounded-lg bg-muted/60 p-4 print:bg-gray-100'>
                <p className='text-xs text-muted-foreground print:text-gray-600'>
                    {t('passenger')}
                </p>
                <div className='mt-1 flex flex-wrap items-center justify-between gap-3'>
                    <p className='text-xl font-bold'>{passengerName}</p>
                    <span className='rounded-md bg-background px-3 py-1 text-sm font-semibold print:bg-white'>
                        {seatLabel}
                    </span>
                </div>
            </div>

            <div className='flex justify-center mb-6'>
                <TicketQRCode bookingId={bookingId} seatId={seat.seatId} />
            </div>

            <div className='mb-4'>
                <div className='flex justify-between items-center'>
                    <div className='text-center'>
                        <p className='text-lg font-semibold'>{trip.origin}</p>
                        <p className='text-xs text-muted-foreground print:text-gray-600'>
                            {t('departure')}
                        </p>
                    </div>
                    <div className='flex-1 px-4'>
                        <div className='border-t-2 border-dashed border-gray-300 relative'>
                            <span className='absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 bg-white px-2 text-xs'>
                                →
                            </span>
                        </div>
                    </div>
                    <div className='text-center'>
                        <p className='text-lg font-semibold'>
                            {trip.destination}
                        </p>
                        <p className='text-xs text-muted-foreground print:text-gray-600'>
                            {t('arrival')}
                        </p>
                    </div>
                </div>
            </div>

            <div className='grid grid-cols-2 gap-4 mb-4 text-sm'>
                <div>
                    <p className='text-xs text-muted-foreground print:text-gray-600'>
                        {t('train')}
                    </p>
                    <p className='font-medium'>
                        {trip.trainName}{' '}
                        {trip.trainNumber && `(${trip.trainNumber})`}
                    </p>
                </div>
                <div>
                    <p className='text-xs text-muted-foreground print:text-gray-600'>
                        {t('departureTime')}
                    </p>
                    <p className='font-medium'>
                        {trip.departureTime
                            && formatDateTime(trip.departureTime)}
                    </p>
                </div>
            </div>

            <div className='grid grid-cols-2 gap-4 text-sm'>
                <div>
                    <p className='text-xs text-muted-foreground print:text-gray-600'>
                        {t('bookingId')}
                    </p>
                    <p className='font-mono font-medium'>{bookingId}</p>
                </div>
                {passenger.idDocumentNumber && (
                    <div>
                        <p className='text-xs text-muted-foreground print:text-gray-600'>
                            {t('idDocument')}
                        </p>
                        <p className='font-medium'>
                            {passenger.idDocumentNumber}
                        </p>
                    </div>
                )}
            </div>

            <div className='mt-6 pt-4 border-t border-dashed text-center'>
                <p className='text-xs text-muted-foreground print:text-gray-600'>
                    {t('footer')}
                </p>
            </div>
        </article>
    );
}
