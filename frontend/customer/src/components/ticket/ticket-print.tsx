'use client';

import { useTranslations } from 'next-intl';
import { formatDateTime } from '@/lib/customer-utils.ts';
import { TicketQRCode } from './ticket-qr-code.tsx';

type PassengerInfo = {
    seatId?: string;
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
    /** @deprecated Use passengers array instead */
    passenger?: {
        fullName?: string;
        idDocumentNumber?: string;
    };
    /** Multiple passengers with seat assignments */
    passengers?: PassengerInfo[];
    trip: {
        trainName?: string;
        trainNumber?: string;
        origin?: string;
        destination?: string;
        departureTime?: string;
        arrivalTime?: string;
    };
    seats: SeatInfo[];
};

export function TicketPrint({
    bookingId,
    passenger,
    passengers,
    trip,
    seats,
}: TicketPrintProps) {
    const t = useTranslations('Ticket');

    // Build seat lookup for passenger-to-seat mapping
    const seatMap = new Map(seats.map((s) => [s.seatId, s]));

    // Use passengers array if provided, otherwise fall back to legacy single passenger
    const passengerList: Array<{
        fullName?: string;
        idDocumentNumber?: string;
        seat?: SeatInfo;
    }> =
        passengers && passengers.length > 0
            ? passengers.map((p) => ({
                  fullName: p.fullName,
                  idDocumentNumber: p.idDocumentNumber,
                  seat: seatMap.get(p.seatId),
              }))
            : passenger
              ? [
                    {
                        fullName: passenger.fullName,
                        idDocumentNumber: passenger.idDocumentNumber,
                    },
                ]
              : [];

    return (
        <div className='ticket-content bg-white p-6 max-w-md mx-auto border border-gray-200 rounded-lg print:border-black print:rounded-none'>
            {/* Header */}
            <div className='text-center mb-6'>
                <h1 className='text-2xl font-bold text-primary print:text-black'>
                    VietRail
                </h1>
                <p className='text-sm text-muted-foreground print:text-gray-600'>
                    {t('electronicTicket')}
                </p>
            </div>

            {/* QR Code */}
            <div className='flex justify-center mb-6'>
                <TicketQRCode bookingId={bookingId} />
            </div>

            {/* Booking ID */}
            <div className='text-center mb-4'>
                <p className='text-xs text-muted-foreground print:text-gray-600'>
                    {t('bookingId')}
                </p>
                <p className='font-mono text-sm'>{bookingId}</p>
            </div>

            <hr className='my-4 border-dashed' />

            {/* Route */}
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

            {/* Train & Timing */}
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

            {/* Seats */}
            <div className='mb-4'>
                <p className='text-xs text-muted-foreground print:text-gray-600 mb-2'>
                    {t('seats')}
                </p>
                <div className='flex flex-wrap gap-2'>
                    {seats.map((seat) => (
                        <span
                            key={seat.seatId}
                            className='px-3 py-1 bg-muted print:bg-gray-100 rounded text-sm font-medium'
                        >
                            {t('coachSeat', {
                                coach: seat.coachNumber,
                                seat: seat.seatNumber,
                            })}
                        </span>
                    ))}
                </div>
            </div>

            <hr className='my-4 border-dashed' />

            {/* Passengers */}
            <div className='text-sm'>
                <p className='text-xs text-muted-foreground print:text-gray-600 mb-2'>
                    {passengerList.length > 1
                        ? t('passengers')
                        : t('passenger')}
                </p>
                {passengerList.length > 0 ? (
                    <div className='space-y-3'>
                        {passengerList.map((p, idx) => (
                            <div
                                key={p.seat?.seatId || idx}
                                className='border-l-2 border-gray-300 pl-3'
                            >
                                <div className='flex items-center justify-between'>
                                    <p className='font-medium'>{p.fullName}</p>
                                    {p.seat && (
                                        <span className='text-xs bg-muted print:bg-gray-100 px-2 py-0.5 rounded'>
                                            {t('coachSeat', {
                                                coach: p.seat.coachNumber,
                                                seat: p.seat.seatNumber,
                                            })}
                                        </span>
                                    )}
                                </div>
                                {p.idDocumentNumber && (
                                    <p className='text-xs text-muted-foreground print:text-gray-600'>
                                        {t('idDocument')}: {p.idDocumentNumber}
                                    </p>
                                )}
                            </div>
                        ))}
                    </div>
                ) : (
                    <p className='text-muted-foreground'>-</p>
                )}
            </div>

            {/* Footer */}
            <div className='mt-6 pt-4 border-t border-dashed text-center'>
                <p className='text-xs text-muted-foreground print:text-gray-600'>
                    {t('footer')}
                </p>
            </div>
        </div>
    );
}
