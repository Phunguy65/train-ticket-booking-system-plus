import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { TestProviders } from '@/test-utils/providers.tsx';
import { TicketQRCode } from './ticket-qr-code.tsx';

// Mock qrcode.react
vi.mock('qrcode.react', () => ({
    QRCodeSVG: ({
        value,
        size,
        title,
    }: {
        value: string;
        size: number;
        title: string;
    }) => (
        <svg
            data-testid='qr-code-svg'
            data-value={value}
            width={size}
            height={size}
            aria-label={title}
        >
            <title>{title}</title>
        </svg>
    ),
}));

describe('TicketQRCode', () => {
    it('renders QR code with correct booking ID payload', () => {
        render(
            <TestProviders>
                <TicketQRCode bookingId='booking-123' />
            </TestProviders>,
        );

        const qrCode = screen.getByTestId('qr-code-svg');
        expect(qrCode).toBeInTheDocument();
        expect(qrCode).toHaveAttribute(
            'data-value',
            'VIETRAIL-TICKET:booking-123',
        );
    });

    it('renders QR code with correct size', () => {
        render(
            <TestProviders>
                <TicketQRCode bookingId='booking-456' />
            </TestProviders>,
        );

        const qrCode = screen.getByTestId('qr-code-svg');
        expect(qrCode).toHaveAttribute('width', '120');
        expect(qrCode).toHaveAttribute('height', '120');
    });

    it('has accessible title for QR code', () => {
        render(
            <TestProviders>
                <TicketQRCode bookingId='booking-789' />
            </TestProviders>,
        );

        const title = screen.getByText('Mã QR vé');
        expect(title).toBeInTheDocument();
    });

    it('shows scan instruction text', () => {
        render(
            <TestProviders>
                <TicketQRCode bookingId='booking-abc' />
            </TestProviders>,
        );

        expect(screen.getByText('Quét để xác minh vé')).toBeInTheDocument();
    });

    it('applies custom className when provided', () => {
        const { container } = render(
            <TestProviders>
                <TicketQRCode
                    bookingId='booking-def'
                    className='custom-class'
                />
            </TestProviders>,
        );

        const wrapper = container.firstChild;
        expect(wrapper).toHaveClass('custom-class');
    });
});
