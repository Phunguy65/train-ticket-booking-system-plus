import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { TestProviders } from '@/test-utils/providers.tsx';
import { TripSearchForm } from './trip-search-form.tsx';

// Mock the locale-aware router
const pushMock = vi.fn();
vi.mock('@/i18n/routing.ts', () => ({
    useRouter: () => ({ push: pushMock }),
    Link: ({ children }: { children: React.ReactNode }) => <>{children}</>,
    routing: { locales: ['vi', 'en'], defaultLocale: 'vi' },
}));

// Mock API calls for station search
vi.mock('@/lib/api/index.ts', () => ({
    searchStationsOptions: () => ({
        queryKey: ['searchStations'],
        queryFn: vi.fn().mockResolvedValue([]),
    }),
}));

// Mock toast
vi.mock('@/lib/toast.ts', () => ({
    showApiErrorToast: vi.fn(),
}));

describe('TripSearchForm', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        pushMock.mockClear();
    });

    it('renders origin, destination, date fields and search button in Vietnamese', () => {
        render(
            <TestProviders>
                <TripSearchForm />
            </TestProviders>,
        );

        expect(screen.getByText('Ga đi')).toBeInTheDocument();
        expect(screen.getByText('Ga đến')).toBeInTheDocument();
        expect(screen.getByText('Ngày đi')).toBeInTheDocument();
        expect(
            screen.getByRole('button', { name: 'Tìm chuyến' }),
        ).toBeInTheDocument();
    });

    it('shows validation error when origin is not selected', async () => {
        const user = userEvent.setup();

        render(
            <TestProviders>
                <TripSearchForm />
            </TestProviders>,
        );

        const submitButton = screen.getByRole('button', { name: 'Tìm chuyến' });
        await user.click(submitButton);

        await waitFor(() => {
            expect(screen.getByText('Vui lòng chọn ga đi')).toBeInTheDocument();
        });
    });

    it('shows validation error when destination is not selected', async () => {
        const user = userEvent.setup();

        render(
            <TestProviders>
                <TripSearchForm />
            </TestProviders>,
        );

        const submitButton = screen.getByRole('button', { name: 'Tìm chuyến' });
        await user.click(submitButton);

        await waitFor(() => {
            expect(
                screen.getByText('Vui lòng chọn ga đến'),
            ).toBeInTheDocument();
        });
    });

    it('has a swap button to exchange origin and destination', () => {
        render(
            <TestProviders>
                <TripSearchForm />
            </TestProviders>,
        );

        const swapButton = screen.getByRole('button', { name: 'Đổi ga' });
        expect(swapButton).toBeInTheDocument();
    });

    it('submit button exists and is not disabled initially', () => {
        render(
            <TestProviders>
                <TripSearchForm />
            </TestProviders>,
        );

        const submitButton = screen.getByRole('button', { name: 'Tìm chuyến' });
        expect(submitButton).toBeInTheDocument();
        expect(submitButton).not.toBeDisabled();
    });
});
