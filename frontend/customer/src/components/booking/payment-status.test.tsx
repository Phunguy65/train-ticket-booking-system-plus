import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TestProviders } from '@/test-utils/providers.tsx';
import { PaymentStatus } from './payment-status.tsx';

// Mock the locale-aware router
vi.mock('@/i18n/routing.ts', () => ({
    useRouter: () => ({ push: vi.fn() }),
    Link: ({ children, href }: { children: React.ReactNode; href: string }) => (
        <a href={href}>{children}</a>
    ),
    routing: { locales: ['vi', 'en'], defaultLocale: 'vi' },
}));

describe('PaymentStatus', () => {
    beforeEach(() => {
        vi.useFakeTimers({ shouldAdvanceTime: true });
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    describe('PENDING state', () => {
        it('renders pending state with countdown', () => {
            // Set deadline 10 minutes from now
            const deadline = new Date(
                Date.now() + 10 * 60 * 1000,
            ).toISOString();

            render(
                <TestProviders>
                    <PaymentStatus
                        state='PENDING'
                        paymentDeadline={deadline}
                        checkoutUrl='https://payment.example.com'
                    />
                </TestProviders>,
            );

            expect(screen.getByText('Chờ thanh toán')).toBeInTheDocument();
            expect(
                screen.getByRole('link', { name: /thanh toán ngay/i }),
            ).toBeInTheDocument();
        });

        it('shows urgent styling when less than 5 minutes remaining', () => {
            // Set deadline 3 minutes from now (less than 5 minutes)
            const deadline = new Date(Date.now() + 3 * 60 * 1000).toISOString();

            render(
                <TestProviders>
                    <PaymentStatus state='PENDING' paymentDeadline={deadline} />
                </TestProviders>,
            );

            // Should show urgent message
            expect(screen.getByText(/sắp hết thời gian/i)).toBeInTheDocument();
        });

        it('transitions to expired state when countdown reaches zero', async () => {
            // Set deadline 2 seconds from now
            const deadline = new Date(Date.now() + 2000).toISOString();

            render(
                <TestProviders>
                    <PaymentStatus state='PENDING' paymentDeadline={deadline} />
                </TestProviders>,
            );

            // Initially should be pending
            expect(screen.getByText('Chờ thanh toán')).toBeInTheDocument();

            // Advance time past deadline
            await act(async () => {
                vi.advanceTimersByTime(3000);
            });

            await waitFor(() => {
                expect(
                    screen.getByText('Hết hạn thanh toán'),
                ).toBeInTheDocument();
            });
        });

        it('renders in compact mode', () => {
            const deadline = new Date(
                Date.now() + 10 * 60 * 1000,
            ).toISOString();

            render(
                <TestProviders>
                    <PaymentStatus
                        state='PENDING'
                        paymentDeadline={deadline}
                        compact
                    />
                </TestProviders>,
            );

            expect(screen.getByText('còn lại')).toBeInTheDocument();
        });
    });

    describe('REDIRECTING state', () => {
        it('renders redirecting state', () => {
            render(
                <TestProviders>
                    <PaymentStatus state='REDIRECTING' />
                </TestProviders>,
            );

            expect(screen.getByText('Đang chuyển hướng')).toBeInTheDocument();
        });
    });

    describe('SUCCESS state', () => {
        it('renders success state', () => {
            render(
                <TestProviders>
                    <PaymentStatus state='SUCCESS' />
                </TestProviders>,
            );

            expect(
                screen.getByText('Thanh toán thành công'),
            ).toBeInTheDocument();
        });
    });

    describe('FAILED state', () => {
        it('renders failed state with retry button', async () => {
            vi.useRealTimers(); // Use real timers for userEvent
            const user = userEvent.setup();
            const onRetry = vi.fn();

            render(
                <TestProviders>
                    <PaymentStatus state='FAILED' onRetry={onRetry} />
                </TestProviders>,
            );

            expect(screen.getByText('Thanh toán thất bại')).toBeInTheDocument();

            const retryButton = screen.getByRole('button', {
                name: /thử lại/i,
            });
            await user.click(retryButton);

            expect(onRetry).toHaveBeenCalledTimes(1);
        });
    });

    describe('EXPIRED state', () => {
        it('renders expired state with start over button', async () => {
            vi.useRealTimers(); // Use real timers for userEvent
            const user = userEvent.setup();
            const onStartOver = vi.fn();

            render(
                <TestProviders>
                    <PaymentStatus state='EXPIRED' onStartOver={onStartOver} />
                </TestProviders>,
            );

            expect(screen.getByText('Hết hạn thanh toán')).toBeInTheDocument();

            const startOverButton = screen.getByRole('button', {
                name: /bắt đầu lại/i,
            });
            await user.click(startOverButton);

            expect(onStartOver).toHaveBeenCalledTimes(1);
        });
    });
});
