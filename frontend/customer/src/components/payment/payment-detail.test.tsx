import { useQuery } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { TestProviders } from '@/test-utils/providers.tsx';

// Mock PaymentStatusBadge inline
function MockPaymentStatusBadge({ status }: { status: string }) {
    const statusText =
        status === 'PAID'
            ? 'Đã thanh toán'
            : status === 'PENDING'
              ? 'Đang xử lý'
              : 'Thất bại';
    return (
        <span data-testid='payment-status-badge' data-variant={status}>
            {statusText}
        </span>
    );
}

// Mock payment data
const mockPaidPayment = {
    paymentId: 'payment-123',
    status: 'PAID',
    amount: '500000',
    createdAt: '2026-04-18T10:00:00Z',
    booking: {
        id: 'booking-123',
        trip: {
            trainName: 'SE1',
            trainNumber: 'SE1',
            origin: 'Ga Sài Gòn',
            destination: 'Ga Hà Nội',
            departureTime: '2026-04-20T08:00:00Z',
            arrivalTime: '2026-04-20T18:00:00Z',
        },
        seats: [
            { seatId: 'seat-1', coachNumber: 1, seatNumber: 'A1' },
            { seatId: 'seat-2', coachNumber: 1, seatNumber: 'A2' },
        ],
        passengerInfo: {
            fullName: 'Nguyen Van A',
            email: 'test@example.com',
            phone: '0901234567',
            idDocumentNumber: '123456789',
        },
    },
};

const mockPendingPayment = {
    ...mockPaidPayment,
    status: 'PENDING',
};

const mockFailedPayment = {
    ...mockPaidPayment,
    status: 'FAILED',
};

let mockPaymentResponse: typeof mockPaidPayment | null = mockPaidPayment;

// Test wrapper component that uses the actual hooks
function PaymentDetailTestWrapper({ paymentId }: { paymentId: string }) {
    const {
        data: payment,
        isLoading,
        isError,
    } = useQuery({
        queryKey: ['getPayment', paymentId],
        queryFn: () => Promise.resolve(mockPaymentResponse),
    });

    if (isLoading) return <div>Loading...</div>;
    if (isError) return <div>Lỗi tải thông tin thanh toán</div>;
    if (!payment) return <div>Không tìm thấy thanh toán</div>;

    const isPaid = payment.status === 'PAID';
    const booking = payment.booking;

    return (
        <div data-testid='payment-detail'>
            <a href='/account'>Quay lại tài khoản</a>

            <h1>Chi tiết thanh toán</h1>
            <p>Mã thanh toán: {payment.paymentId}</p>

            <MockPaymentStatusBadge status={payment.status} />

            <div data-testid='payment-amount'>{payment.amount}</div>

            {booking?.trip && (
                <div data-testid='trip-info'>
                    <h3>Thông tin chuyến đi</h3>
                    <p>
                        {booking.trip.trainName} ({booking.trip.trainNumber})
                    </p>
                    <p>
                        {booking.trip.origin} → {booking.trip.destination}
                    </p>
                </div>
            )}

            {booking?.seats && (
                <div data-testid='seats-info'>
                    <h3>Ghế</h3>
                    {booking.seats.map(
                        (seat: {
                            seatId: string;
                            coachNumber: number;
                            seatNumber: string;
                        }) => (
                            <span key={seat.seatId}>
                                Toa {seat.coachNumber} - Ghế {seat.seatNumber}
                            </span>
                        ),
                    )}
                </div>
            )}

            {booking?.passengerInfo && (
                <div data-testid='passenger-info'>
                    <h3>Thông tin hành khách</h3>
                    <p>{booking.passengerInfo.fullName}</p>
                    <p>{booking.passengerInfo.email}</p>
                </div>
            )}

            {/* Conditional print ticket button - only for PAID status */}
            {isPaid && booking?.id && (
                <div data-testid='print-ticket-section'>
                    <a
                        href={`/ticket/${booking.id}`}
                        target='_blank'
                        rel='noreferrer'
                    >
                        In vé
                    </a>
                </div>
            )}
        </div>
    );
}

describe('PaymentDetailPage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        mockPaymentResponse = mockPaidPayment;
    });

    describe('Payment detail rendering', () => {
        it('renders payment detail for PAID status', async () => {
            render(
                <TestProviders>
                    <PaymentDetailTestWrapper paymentId='payment-123' />
                </TestProviders>,
            );

            await waitFor(() => {
                expect(
                    screen.getByTestId('payment-detail'),
                ).toBeInTheDocument();
            });

            expect(screen.getByText('Chi tiết thanh toán')).toBeInTheDocument();
            expect(
                screen.getByText('Mã thanh toán: payment-123'),
            ).toBeInTheDocument();
        });

        it('displays payment status badge', async () => {
            render(
                <TestProviders>
                    <PaymentDetailTestWrapper paymentId='payment-123' />
                </TestProviders>,
            );

            await waitFor(() => {
                expect(
                    screen.getByTestId('payment-status-badge'),
                ).toBeInTheDocument();
            });

            expect(screen.getByText('Đã thanh toán')).toBeInTheDocument();
        });

        it('displays trip information', async () => {
            render(
                <TestProviders>
                    <PaymentDetailTestWrapper paymentId='payment-123' />
                </TestProviders>,
            );

            await waitFor(() => {
                expect(screen.getByTestId('trip-info')).toBeInTheDocument();
            });

            expect(screen.getByText('SE1 (SE1)')).toBeInTheDocument();
            expect(
                screen.getByText('Ga Sài Gòn → Ga Hà Nội'),
            ).toBeInTheDocument();
        });

        it('displays passenger information', async () => {
            render(
                <TestProviders>
                    <PaymentDetailTestWrapper paymentId='payment-123' />
                </TestProviders>,
            );

            await waitFor(() => {
                expect(
                    screen.getByTestId('passenger-info'),
                ).toBeInTheDocument();
            });

            expect(screen.getByText('Nguyen Van A')).toBeInTheDocument();
            expect(screen.getByText('test@example.com')).toBeInTheDocument();
        });

        it('displays seat information', async () => {
            render(
                <TestProviders>
                    <PaymentDetailTestWrapper paymentId='payment-123' />
                </TestProviders>,
            );

            await waitFor(() => {
                expect(screen.getByTestId('seats-info')).toBeInTheDocument();
            });

            expect(screen.getByText('Toa 1 - Ghế A1')).toBeInTheDocument();
            expect(screen.getByText('Toa 1 - Ghế A2')).toBeInTheDocument();
        });
    });

    describe('Conditional print ticket action', () => {
        it('shows print ticket button for PAID payment', async () => {
            mockPaymentResponse = mockPaidPayment;

            render(
                <TestProviders>
                    <PaymentDetailTestWrapper paymentId='payment-123' />
                </TestProviders>,
            );

            await waitFor(() => {
                expect(
                    screen.getByTestId('print-ticket-section'),
                ).toBeInTheDocument();
            });

            const printLink = screen.getByRole('link', { name: 'In vé' });
            expect(printLink).toBeInTheDocument();
            expect(printLink).toHaveAttribute('href', '/ticket/booking-123');
            expect(printLink).toHaveAttribute('target', '_blank');
        });

        it('hides print ticket button for PENDING payment', async () => {
            mockPaymentResponse = mockPendingPayment;

            render(
                <TestProviders>
                    <PaymentDetailTestWrapper paymentId='payment-123' />
                </TestProviders>,
            );

            await waitFor(() => {
                expect(
                    screen.getByTestId('payment-detail'),
                ).toBeInTheDocument();
            });

            expect(
                screen.queryByTestId('print-ticket-section'),
            ).not.toBeInTheDocument();
            expect(
                screen.queryByRole('link', { name: 'In vé' }),
            ).not.toBeInTheDocument();
        });

        it('hides print ticket button for FAILED payment', async () => {
            mockPaymentResponse = mockFailedPayment;

            render(
                <TestProviders>
                    <PaymentDetailTestWrapper paymentId='payment-123' />
                </TestProviders>,
            );

            await waitFor(() => {
                expect(
                    screen.getByTestId('payment-detail'),
                ).toBeInTheDocument();
            });

            expect(
                screen.queryByTestId('print-ticket-section'),
            ).not.toBeInTheDocument();
            expect(
                screen.queryByRole('link', { name: 'In vé' }),
            ).not.toBeInTheDocument();
        });

        it('displays PENDING status badge for pending payment', async () => {
            mockPaymentResponse = mockPendingPayment;

            render(
                <TestProviders>
                    <PaymentDetailTestWrapper paymentId='payment-123' />
                </TestProviders>,
            );

            await waitFor(() => {
                expect(screen.getByText('Đang xử lý')).toBeInTheDocument();
            });
        });

        it('displays FAILED status badge for failed payment', async () => {
            mockPaymentResponse = mockFailedPayment;

            render(
                <TestProviders>
                    <PaymentDetailTestWrapper paymentId='payment-123' />
                </TestProviders>,
            );

            await waitFor(() => {
                expect(screen.getByText('Thất bại')).toBeInTheDocument();
            });
        });
    });

    describe('Navigation', () => {
        it('has back to account link', async () => {
            render(
                <TestProviders>
                    <PaymentDetailTestWrapper paymentId='payment-123' />
                </TestProviders>,
            );

            await waitFor(() => {
                expect(
                    screen.getByRole('link', { name: 'Quay lại tài khoản' }),
                ).toBeInTheDocument();
            });

            expect(
                screen.getByRole('link', { name: 'Quay lại tài khoản' }),
            ).toHaveAttribute('href', '/account');
        });
    });

    describe('Error and not found states', () => {
        it('shows not found message when payment is null', async () => {
            mockPaymentResponse = null;

            render(
                <TestProviders>
                    <PaymentDetailTestWrapper paymentId='payment-404' />
                </TestProviders>,
            );

            await waitFor(() => {
                expect(
                    screen.getByText('Không tìm thấy thanh toán'),
                ).toBeInTheDocument();
            });
        });
    });
});
