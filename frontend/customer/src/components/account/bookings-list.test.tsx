import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { TestProviders } from '@/test-utils/providers.tsx';
import { BookingsList } from './bookings-list.tsx';

// Mock the locale-aware router
vi.mock('@/i18n/routing.ts', () => ({
    useRouter: () => ({ push: vi.fn() }),
    Link: ({ children, href }: { children: React.ReactNode; href: string }) => (
        <a href={href}>{children}</a>
    ),
    routing: { locales: ['vi', 'en'], defaultLocale: 'vi' },
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
    getUserBookingsOptions: ({ path }: { path: { userId: string } }) => ({
        queryKey: ['getUserBookings', path.userId],
        queryFn: () =>
            Promise.resolve({
                content: [
                    {
                        id: 'booking-1',
                        status: 'HELD',
                        totalPrice: 500000,
                        createdAt: '2026-04-18T10:00:00Z',
                        paymentDeadline: '2026-04-18T11:00:00Z',
                        trip: {
                            train: { name: 'SE1', trainNumber: 'SE1' },
                            route: {
                                origin: { name: 'Ga Sai Gon' },
                                destination: { name: 'Ga Ha Noi' },
                            },
                            departureTime: '2026-04-20T08:00:00Z',
                        },
                        seats: [{ id: 'seat-1', seatNumber: 'A1' }],
                    },
                    {
                        id: 'booking-2',
                        status: 'CONFIRMED',
                        totalPrice: 750000,
                        createdAt: '2026-04-17T10:00:00Z',
                        trip: {
                            train: { name: 'SE2', trainNumber: 'SE2' },
                            route: {
                                origin: { name: 'Ga Ha Noi' },
                                destination: { name: 'Ga Sai Gon' },
                            },
                            departureTime: '2026-04-25T14:00:00Z',
                        },
                        seats: [
                            { id: 'seat-2', seatNumber: 'B1' },
                            { id: 'seat-3', seatNumber: 'B2' },
                        ],
                    },
                ],
            }),
    }),
    cancelBookingMutation: () => ({
        mutationFn: vi.fn(),
    }),
}));

// Mock toast
vi.mock('@/lib/toast.ts', () => ({
    showSuccessToast: vi.fn(),
    showApiErrorToast: vi.fn(),
}));

describe('BookingsList', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders booking cards with price information', async () => {
        render(
            <TestProviders>
                <BookingsList />
            </TestProviders>,
        );

        await waitFor(() => {
            // Should show the booking prices
            expect(screen.getByText(/500\.000/)).toBeInTheDocument();
            expect(screen.getByText(/750\.000/)).toBeInTheDocument();
        });
    });

    it('displays booking status badges with localized text', async () => {
        render(
            <TestProviders>
                <BookingsList />
            </TestProviders>,
        );

        await waitFor(() => {
            expect(screen.getByText('Chờ thanh toán')).toBeInTheDocument();
            expect(screen.getByText('Đã xác nhận')).toBeInTheDocument();
        });
    });

    it('shows view details links for bookings', async () => {
        render(
            <TestProviders>
                <BookingsList />
            </TestProviders>,
        );

        await waitFor(() => {
            const viewDetailsLinks = screen.getAllByText('Xem chi tiết');
            expect(viewDetailsLinks.length).toBe(2);
        });
    });

    it('shows cancel button only for HELD bookings', async () => {
        render(
            <TestProviders>
                <BookingsList />
            </TestProviders>,
        );

        await waitFor(() => {
            // Cancel button should only appear for HELD bookings
            const cancelButtons = screen.getAllByRole('button', {
                name: /hủy/i,
            });
            expect(cancelButtons.length).toBe(1);
        });
    });

    it('opens cancel confirmation dialog when cancel is clicked', async () => {
        const user = userEvent.setup();

        render(
            <TestProviders>
                <BookingsList />
            </TestProviders>,
        );

        await waitFor(() => {
            expect(
                screen.getByRole('button', { name: /hủy/i }),
            ).toBeInTheDocument();
        });

        const cancelButton = screen.getByRole('button', { name: /hủy/i });
        await user.click(cancelButton);

        await waitFor(() => {
            expect(screen.getByText('Xác nhận hủy vé')).toBeInTheDocument();
        });
    });
});
