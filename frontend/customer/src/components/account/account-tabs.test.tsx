import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from '@/components/ui/tabs.tsx';
import { TestProviders } from '@/test-utils/providers.tsx';

// Mock account components
vi.mock('@/components/account/bookings-list.tsx', () => ({
    BookingsList: () => (
        <div data-testid='bookings-list'>Bookings List Content</div>
    ),
}));

vi.mock('@/components/account/payments-list.tsx', () => ({
    PaymentsList: () => (
        <div data-testid='payments-list'>Payments List Content</div>
    ),
}));

// Test component that mimics the account page structure
function AccountPageTest() {
    return (
        <div className='container px-4 py-8'>
            <h1 className='mb-6 text-2xl font-bold'>Tài khoản của tôi</h1>
            <Tabs defaultValue='bookings'>
                <TabsList className='mb-6'>
                    <TabsTrigger value='bookings' className='gap-2'>
                        Đơn đặt vé
                    </TabsTrigger>
                    <TabsTrigger value='payments' className='gap-2'>
                        Thanh toán
                    </TabsTrigger>
                </TabsList>
                <TabsContent value='bookings'>
                    <div data-testid='bookings-list'>Bookings List Content</div>
                </TabsContent>
                <TabsContent value='payments'>
                    <div data-testid='payments-list'>Payments List Content</div>
                </TabsContent>
            </Tabs>
        </div>
    );
}

describe('AccountPage Tabs', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders page title', () => {
        render(
            <TestProviders>
                <AccountPageTest />
            </TestProviders>,
        );

        expect(screen.getByText('Tài khoản của tôi')).toBeInTheDocument();
    });

    it('renders both tab triggers', () => {
        render(
            <TestProviders>
                <AccountPageTest />
            </TestProviders>,
        );

        expect(
            screen.getByRole('tab', { name: 'Đơn đặt vé' }),
        ).toBeInTheDocument();
        expect(
            screen.getByRole('tab', { name: 'Thanh toán' }),
        ).toBeInTheDocument();
    });

    it('shows bookings tab as default active tab', () => {
        render(
            <TestProviders>
                <AccountPageTest />
            </TestProviders>,
        );

        const bookingsTab = screen.getByRole('tab', { name: 'Đơn đặt vé' });
        expect(bookingsTab).toHaveAttribute('data-state', 'active');
    });

    it('shows bookings list content by default', () => {
        render(
            <TestProviders>
                <AccountPageTest />
            </TestProviders>,
        );

        expect(screen.getByTestId('bookings-list')).toBeInTheDocument();
    });

    it('switches to payments tab when clicked', async () => {
        const user = userEvent.setup();

        render(
            <TestProviders>
                <AccountPageTest />
            </TestProviders>,
        );

        const paymentsTab = screen.getByRole('tab', { name: 'Thanh toán' });
        await user.click(paymentsTab);

        await waitFor(() => {
            expect(paymentsTab).toHaveAttribute('data-state', 'active');
        });
    });

    it('shows payments list content when payments tab is active', async () => {
        const user = userEvent.setup();

        render(
            <TestProviders>
                <AccountPageTest />
            </TestProviders>,
        );

        const paymentsTab = screen.getByRole('tab', { name: 'Thanh toán' });
        await user.click(paymentsTab);

        await waitFor(() => {
            expect(screen.getByTestId('payments-list')).toBeInTheDocument();
        });
    });

    it('deactivates bookings tab when payments tab is active', async () => {
        const user = userEvent.setup();

        render(
            <TestProviders>
                <AccountPageTest />
            </TestProviders>,
        );

        const paymentsTab = screen.getByRole('tab', { name: 'Thanh toán' });
        const bookingsTab = screen.getByRole('tab', { name: 'Đơn đặt vé' });

        await user.click(paymentsTab);

        await waitFor(() => {
            expect(paymentsTab).toHaveAttribute('data-state', 'active');
            expect(bookingsTab).toHaveAttribute('data-state', 'inactive');
        });
    });

    it('can switch back to bookings tab', async () => {
        const user = userEvent.setup();

        render(
            <TestProviders>
                <AccountPageTest />
            </TestProviders>,
        );

        // Click payments tab
        const paymentsTab = screen.getByRole('tab', { name: 'Thanh toán' });
        await user.click(paymentsTab);

        await waitFor(() => {
            expect(paymentsTab).toHaveAttribute('data-state', 'active');
        });

        // Click bookings tab
        const bookingsTab = screen.getByRole('tab', { name: 'Đơn đặt vé' });
        await user.click(bookingsTab);

        await waitFor(() => {
            expect(bookingsTab).toHaveAttribute('data-state', 'active');
        });

        expect(screen.getByTestId('bookings-list')).toBeInTheDocument();
    });

    it('maintains tab accessibility attributes', () => {
        render(
            <TestProviders>
                <AccountPageTest />
            </TestProviders>,
        );

        const tabList = screen.getByRole('tablist');
        expect(tabList).toBeInTheDocument();

        const tabs = screen.getAllByRole('tab');
        expect(tabs).toHaveLength(2);

        // Each tab should have proper ARIA attributes
        for (const tab of tabs) {
            expect(tab).toHaveAttribute('aria-selected');
        }
    });
});
