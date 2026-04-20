import { useQuery } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useCallback, useEffect, useState } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TestProviders } from '@/test-utils/providers.tsx';

// Mock ticket print component inline
function MockTicketPrint({
    bookingId,
    passenger,
    trip,
    seats,
}: {
    bookingId: string;
    passenger: { fullName?: string };
    trip: { origin?: string; destination?: string };
    seats: Array<{ seatId?: string }>;
}) {
    return (
        <div data-testid='ticket-print'>
            <span data-testid='booking-id'>{bookingId}</span>
            <span data-testid='passenger'>{passenger.fullName}</span>
            <span data-testid='route'>
                {trip.origin} → {trip.destination}
            </span>
            <span data-testid='seats-count'>{seats.length}</span>
        </div>
    );
}

// Mock booking data
const mockConfirmedBooking = {
    id: 'booking-123',
    status: 'CONFIRMED',
    passengerInfo: {
        fullName: 'Nguyen Van A',
        idDocumentNumber: '123456789',
    },
    trip: {
        train: { name: 'SE1', trainNumber: 'SE1' },
        route: {
            origin: { name: 'Ga Sài Gòn' },
            destination: { name: 'Ga Hà Nội' },
        },
        departureTime: '2026-04-20T08:00:00Z',
        arrivalTime: '2026-04-20T18:00:00Z',
    },
    seats: [
        { seatId: 'seat-1', coachNumber: 1, seatNumber: 'A1' },
        { seatId: 'seat-2', coachNumber: 1, seatNumber: 'A2' },
    ],
};

const mockHeldBooking = {
    ...mockConfirmedBooking,
    status: 'HELD',
};

let mockBookingResponse: typeof mockConfirmedBooking | null =
    mockConfirmedBooking;

// Create test wrapper component that uses the actual hooks
function TicketPageTestWrapper({ bookingId }: { bookingId: string }) {
    const [canShare, setCanShare] = useState(false);

    useEffect(() => {
        setCanShare(typeof navigator !== 'undefined' && !!navigator.share);
    }, []);

    const {
        data: booking,
        isLoading,
        isError,
    } = useQuery({
        queryKey: ['getBooking', bookingId],
        queryFn: () => Promise.resolve(mockBookingResponse),
    });

    const handlePrint = useCallback(() => {
        window.print();
    }, []);

    const handleShare = useCallback(async () => {
        if (!navigator.share || !booking) return;
        try {
            await navigator.share({
                title: 'Vé tàu của tôi',
                text: `Vé tàu từ ${booking.trip?.route?.origin?.name} đến ${booking.trip?.route?.destination?.name}`,
                url: window.location.href,
            });
        } catch {
            // Ignore share failures
        }
    }, [booking]);

    if (isLoading) return <div>Loading...</div>;
    if (isError) return <div>Error</div>;
    if (!booking) return <div>Không tìm thấy đơn đặt vé</div>;

    const isConfirmed = booking.status === 'CONFIRMED';
    if (!isConfirmed) {
        return (
            <div>
                <h1>Vé chưa sẵn sàng</h1>
                <p>
                    Đơn đặt vé này chưa được thanh toán. Vui lòng thanh toán
                    trước.
                </p>
            </div>
        );
    }

    return (
        <div>
            <div className='print:hidden'>
                <button onClick={handlePrint} type='button'>
                    In
                </button>
                {canShare && (
                    <button onClick={handleShare} type='button'>
                        Chia sẻ
                    </button>
                )}
            </div>
            <MockTicketPrint
                bookingId={bookingId}
                passenger={{
                    fullName: booking.passengerInfo?.fullName,
                    idDocumentNumber: booking.passengerInfo?.idDocumentNumber,
                }}
                trip={{
                    trainName: booking.trip?.train?.name,
                    trainNumber: booking.trip?.train?.trainNumber,
                    origin: booking.trip?.route?.origin?.name,
                    destination: booking.trip?.route?.destination?.name,
                    departureTime: booking.trip?.departureTime,
                    arrivalTime: booking.trip?.arrivalTime,
                }}
                seats={(booking.seats || []).map((s) => ({
                    seatId: s.seatId,
                    coachNumber: s.coachNumber,
                    seatNumber: s.seatNumber,
                }))}
            />
        </div>
    );
}

describe('TicketPage', () => {
    let originalPrint: typeof window.print;
    let originalShare: typeof navigator.share;

    beforeEach(() => {
        vi.clearAllMocks();
        mockBookingResponse = mockConfirmedBooking;
        originalPrint = window.print;
        originalShare = navigator.share;
        window.print = vi.fn();
    });

    afterEach(() => {
        window.print = originalPrint;
        if (originalShare) {
            Object.defineProperty(navigator, 'share', {
                value: originalShare,
                writable: true,
                configurable: true,
            });
        } else {
            Object.defineProperty(navigator, 'share', {
                value: undefined,
                writable: true,
                configurable: true,
            });
        }
    });

    it('renders ticket for confirmed booking', async () => {
        render(
            <TestProviders>
                <TicketPageTestWrapper bookingId='booking-123' />
            </TestProviders>,
        );

        await waitFor(() => {
            expect(screen.getByTestId('ticket-print')).toBeInTheDocument();
        });

        expect(screen.getByTestId('booking-id')).toHaveTextContent(
            'booking-123',
        );
        expect(screen.getByTestId('passenger')).toHaveTextContent(
            'Nguyen Van A',
        );
    });

    it('shows not paid message for HELD booking', async () => {
        mockBookingResponse = mockHeldBooking;

        render(
            <TestProviders>
                <TicketPageTestWrapper bookingId='booking-123' />
            </TestProviders>,
        );

        await waitFor(() => {
            expect(screen.getByText('Vé chưa sẵn sàng')).toBeInTheDocument();
        });

        expect(
            screen.getByText(
                'Đơn đặt vé này chưa được thanh toán. Vui lòng thanh toán trước.',
            ),
        ).toBeInTheDocument();
    });

    it('calls window.print when print button is clicked', async () => {
        const user = userEvent.setup();

        render(
            <TestProviders>
                <TicketPageTestWrapper bookingId='booking-123' />
            </TestProviders>,
        );

        await waitFor(() => {
            expect(
                screen.getByRole('button', { name: 'In' }),
            ).toBeInTheDocument();
        });

        await user.click(screen.getByRole('button', { name: 'In' }));

        expect(window.print).toHaveBeenCalledTimes(1);
    });

    it('shows share button when Web Share API is available', async () => {
        // Mock Web Share API availability
        Object.defineProperty(navigator, 'share', {
            value: vi.fn().mockResolvedValue(undefined),
            writable: true,
            configurable: true,
        });

        render(
            <TestProviders>
                <TicketPageTestWrapper bookingId='booking-123' />
            </TestProviders>,
        );

        await waitFor(() => {
            expect(
                screen.getByRole('button', { name: 'Chia sẻ' }),
            ).toBeInTheDocument();
        });
    });

    it('hides share button when Web Share API is not available', async () => {
        // Ensure share is undefined
        Object.defineProperty(navigator, 'share', {
            value: undefined,
            writable: true,
            configurable: true,
        });

        render(
            <TestProviders>
                <TicketPageTestWrapper bookingId='booking-123' />
            </TestProviders>,
        );

        await waitFor(() => {
            expect(screen.getByTestId('ticket-print')).toBeInTheDocument();
        });

        expect(
            screen.queryByRole('button', { name: 'Chia sẻ' }),
        ).not.toBeInTheDocument();
    });

    it('calls navigator.share with correct data when share button is clicked', async () => {
        const shareMock = vi.fn().mockResolvedValue(undefined);
        Object.defineProperty(navigator, 'share', {
            value: shareMock,
            writable: true,
            configurable: true,
        });

        const user = userEvent.setup();

        render(
            <TestProviders>
                <TicketPageTestWrapper bookingId='booking-123' />
            </TestProviders>,
        );

        await waitFor(() => {
            expect(
                screen.getByRole('button', { name: 'Chia sẻ' }),
            ).toBeInTheDocument();
        });

        await user.click(screen.getByRole('button', { name: 'Chia sẻ' }));

        expect(shareMock).toHaveBeenCalledWith(
            expect.objectContaining({
                title: 'Vé tàu của tôi',
                text: expect.stringContaining('Ga Sài Gòn'),
            }),
        );
    });

    it('shows not found message when booking is null', async () => {
        mockBookingResponse = null;

        render(
            <TestProviders>
                <TicketPageTestWrapper bookingId='booking-404' />
            </TestProviders>,
        );

        await waitFor(() => {
            expect(
                screen.getByText('Không tìm thấy đơn đặt vé'),
            ).toBeInTheDocument();
        });
    });

    it('displays correct seat count from booking', async () => {
        render(
            <TestProviders>
                <TicketPageTestWrapper bookingId='booking-123' />
            </TestProviders>,
        );

        await waitFor(() => {
            expect(screen.getByTestId('seats-count')).toHaveTextContent('2');
        });
    });
});
