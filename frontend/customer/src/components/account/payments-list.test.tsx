import { render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { TestProviders } from '@/test-utils/providers.tsx';
import { PaymentsList } from './payments-list.tsx';

// Mock the locale-aware router
vi.mock('@/i18n/routing.ts', () => ({
    useRouter: () => ({ push: vi.fn() }),
    Link: ({ children, href }: { children: React.ReactNode; href: string }) => (
        <a href={href}>{children}</a>
    ),
    routing: { locales: ['vi', 'en'], defaultLocale: 'vi' },
}));

// Mock PaymentStatusBadge to simplify testing
vi.mock('@/components/payment/index.ts', () => ({
    PaymentStatusBadge: ({ status }: { status: string }) => (
        <span data-testid='payment-status-badge'>{status}</span>
    ),
}));

// Mock API calls
vi.mock('@/lib/api/index.ts', () => ({
    getAuthenticatedUserOptions: () => ({
        queryKey: ['getAuthenticatedUser'],
        queryFn: () =>
            Promise.resolve({
                id: 'user-1',
                fullName: 'Nguyen Van A',
                email: 'test@example.com',
            }),
    }),
    getUserPaymentsOptions: ({ path }: { path: { userId: string } }) => ({
        queryKey: ['getUserPayments', path.userId],
        queryFn: () =>
            Promise.resolve({
                content: [
                    {
                        id: 'payment-1',
                        status: 'PAID',
                        amount: 500000,
                        currency: 'VND',
                        createdAt: '2026-04-18T10:00:00Z',
                        bookingId: 'booking-1',
                        booking: {
                            origin: 'Hà Nội',
                            destination: 'TP. Hồ Chí Minh',
                            departureTime: '2026-04-20T08:00:00Z',
                        },
                    },
                    {
                        id: 'payment-2',
                        status: 'PENDING',
                        amount: 750000,
                        currency: 'VND',
                        createdAt: '2026-04-17T10:00:00Z',
                        bookingId: 'booking-2',
                        booking: {
                            origin: 'Đà Nẵng',
                            destination: 'Hà Nội',
                            departureTime: '2026-04-25T14:00:00Z',
                        },
                    },
                ],
            }),
    }),
}));

// Mock toast
vi.mock('@/lib/toast.ts', () => ({
    showSuccessToast: vi.fn(),
    showApiErrorToast: vi.fn(),
    getErrorMessage: vi.fn(() => 'Error message'),
}));

describe('PaymentsList', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders payment cards with route information', async () => {
        render(
            <TestProviders>
                <PaymentsList />
            </TestProviders>,
        );

        await waitFor(() => {
            // Should show the route info
            expect(
                screen.getByText('Hà Nội → TP. Hồ Chí Minh'),
            ).toBeInTheDocument();
            expect(screen.getByText('Đà Nẵng → Hà Nội')).toBeInTheDocument();
        });
    });

    it('renders payment amounts', async () => {
        render(
            <TestProviders>
                <PaymentsList />
            </TestProviders>,
        );

        await waitFor(() => {
            // Should show the payment amounts (formatted)
            expect(screen.getByText(/500\.000/)).toBeInTheDocument();
            expect(screen.getByText(/750\.000/)).toBeInTheDocument();
        });
    });

    it('shows view details links for each payment', async () => {
        render(
            <TestProviders>
                <PaymentsList />
            </TestProviders>,
        );

        await waitFor(() => {
            const viewDetailsLinks = screen.getAllByText('Xem chi tiết');
            expect(viewDetailsLinks.length).toBe(2);
        });
    });

    it('displays payment status badges', async () => {
        render(
            <TestProviders>
                <PaymentsList />
            </TestProviders>,
        );

        await waitFor(() => {
            const statusBadges = screen.getAllByTestId('payment-status-badge');
            expect(statusBadges.length).toBe(2);
            expect(statusBadges[0]).toHaveTextContent('PAID');
            expect(statusBadges[1]).toHaveTextContent('PENDING');
        });
    });

    it('navigates to payment detail on view details click', async () => {
        render(
            <TestProviders>
                <PaymentsList />
            </TestProviders>,
        );

        await waitFor(() => {
            const links = screen.getAllByRole('link', { name: 'Xem chi tiết' });
            expect(links[0]).toHaveAttribute('href', '/payment/payment-1');
            expect(links[1]).toHaveAttribute('href', '/payment/payment-2');
        });
    });
});
