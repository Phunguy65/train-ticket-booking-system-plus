import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { TestProviders } from '@/test-utils/providers.tsx';
import { TicketPrint } from './ticket-print.tsx';

// Mock TicketQRCode component
vi.mock('./ticket-qr-code.tsx', () => ({
    TicketQRCode: ({
        bookingId,
        seatId,
    }: {
        bookingId: string;
        seatId?: string;
    }) => (
        <div
            data-testid='ticket-qr-code'
            data-booking-id={bookingId}
            data-seat-id={seatId}
        >
            QR Code
        </div>
    ),
}));

const defaultProps = {
    bookingId: 'booking-123',
    passenger: {
        fullName: 'Nguyen Van A',
        idDocumentNumber: '123456789',
    },
    trip: {
        trainName: 'SE1',
        trainNumber: 'SE1',
        origin: 'Ga Sài Gòn',
        destination: 'Ga Hà Nội',
        departureTime: '2026-04-20T08:00:00Z',
        arrivalTime: '2026-04-20T18:00:00Z',
    },
    seat: { seatId: 'seat-1', coachNumber: 1, seatNumber: 'A1' },
};

describe('TicketPrint', () => {
    it('renders ticket header with VietRail branding', () => {
        render(
            <TestProviders>
                <TicketPrint {...defaultProps} />
            </TestProviders>,
        );

        expect(screen.getByText('VietRail')).toBeInTheDocument();
        expect(screen.getByText('Vé điện tử')).toBeInTheDocument();
    });

    it('renders QR code with correct booking ID', () => {
        render(
            <TestProviders>
                <TicketPrint {...defaultProps} />
            </TestProviders>,
        );

        const qrCode = screen.getByTestId('ticket-qr-code');
        expect(qrCode).toBeInTheDocument();
        expect(qrCode).toHaveAttribute('data-booking-id', 'booking-123');
        expect(qrCode).toHaveAttribute('data-seat-id', 'seat-1');
    });

    it('displays booking ID', () => {
        render(
            <TestProviders>
                <TicketPrint {...defaultProps} />
            </TestProviders>,
        );

        expect(screen.getByText('Mã đặt vé')).toBeInTheDocument();
        expect(screen.getByText('booking-123')).toBeInTheDocument();
    });

    it('displays route information with origin and destination', () => {
        render(
            <TestProviders>
                <TicketPrint {...defaultProps} />
            </TestProviders>,
        );

        expect(screen.getByText('Ga Sài Gòn')).toBeInTheDocument();
        expect(screen.getByText('Ga Hà Nội')).toBeInTheDocument();
    });

    it('displays train information', () => {
        render(
            <TestProviders>
                <TicketPrint {...defaultProps} />
            </TestProviders>,
        );

        expect(screen.getByText('SE1 (SE1)')).toBeInTheDocument();
    });

    it('displays seat information for the passenger seat', () => {
        render(
            <TestProviders>
                <TicketPrint {...defaultProps} />
            </TestProviders>,
        );

        expect(screen.getByText('Toa 1 - Ghế A1')).toBeInTheDocument();
        expect(screen.queryByText('Toa 1 - Ghế A2')).not.toBeInTheDocument();
    });

    it('displays passenger name', () => {
        render(
            <TestProviders>
                <TicketPrint {...defaultProps} />
            </TestProviders>,
        );

        expect(screen.getByText('Hành khách')).toBeInTheDocument();
        expect(screen.getByText('Nguyen Van A')).toBeInTheDocument();
    });

    it('displays ID document number when provided', () => {
        render(
            <TestProviders>
                <TicketPrint {...defaultProps} />
            </TestProviders>,
        );

        expect(screen.getByText('CMND/CCCD')).toBeInTheDocument();
        expect(screen.getByText('123456789')).toBeInTheDocument();
    });

    it('does not show ID document when not provided', () => {
        const propsWithoutId = {
            ...defaultProps,
            passenger: {
                fullName: 'Nguyen Van A',
            },
        };

        render(
            <TestProviders>
                <TicketPrint {...propsWithoutId} />
            </TestProviders>,
        );

        expect(screen.queryByText(/CMND\/CCCD:/)).not.toBeInTheDocument();
    });

    it('displays footer instruction text', () => {
        render(
            <TestProviders>
                <TicketPrint {...defaultProps} />
            </TestProviders>,
        );

        expect(
            screen.getByText(
                'Vui lòng xuất trình vé này cùng CMND/CCCD khi lên tàu',
            ),
        ).toBeInTheDocument();
    });

    it('has print-friendly styling class', () => {
        const { container } = render(
            <TestProviders>
                <TicketPrint {...defaultProps} />
            </TestProviders>,
        );

        const ticketContent = container.querySelector('.ticket-content');
        expect(ticketContent).toBeInTheDocument();
        expect(ticketContent).toHaveClass('ticket-card');
        expect(ticketContent).toHaveClass('print:break-after-page');
    });
});
