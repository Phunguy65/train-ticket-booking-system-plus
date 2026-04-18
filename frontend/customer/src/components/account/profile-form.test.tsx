import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { TestProviders } from '@/test-utils/providers.tsx';
import { ProfileForm } from './profile-form.tsx';

// Mock the locale-aware router
vi.mock('@/i18n/routing.ts', () => ({
    useRouter: () => ({ push: vi.fn() }),
    Link: ({ children }: { children: React.ReactNode }) => <>{children}</>,
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
                phone: '0901234567',
                dateOfBirth: '1990-01-15',
                gender: 'male',
                idDocumentNumber: '123456789',
                addressLine: 'Ho Chi Minh City',
            }),
    }),
    updateAuthenticatedUserMutation: () => ({
        mutationFn: vi.fn(),
    }),
}));

// Mock toast
vi.mock('@/lib/toast.ts', () => ({
    showSuccessToast: vi.fn(),
    showApiErrorToast: vi.fn(),
}));

describe('ProfileForm', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('renders profile form fields in Vietnamese', async () => {
        render(
            <TestProviders>
                <ProfileForm />
            </TestProviders>,
        );

        await waitFor(() => {
            expect(screen.getByText('Hồ sơ cá nhân')).toBeInTheDocument();
        });

        expect(screen.getByLabelText('Họ và tên')).toBeInTheDocument();
        expect(screen.getByLabelText('Email')).toBeInTheDocument();
        expect(screen.getByLabelText('Số điện thoại')).toBeInTheDocument();
        expect(screen.getByLabelText('Ngày sinh')).toBeInTheDocument();
        expect(screen.getByLabelText('Giới tính')).toBeInTheDocument();
        expect(screen.getByLabelText('Số CMND/CCCD')).toBeInTheDocument();
        expect(screen.getByLabelText('Địa chỉ')).toBeInTheDocument();
    });

    it('shows save button and can be clicked', async () => {
        render(
            <TestProviders>
                <ProfileForm />
            </TestProviders>,
        );

        await waitFor(() => {
            expect(screen.getByText('Hồ sơ cá nhân')).toBeInTheDocument();
        });

        const saveButton = screen.getByRole('button', { name: 'Lưu thay đổi' });
        expect(saveButton).toBeInTheDocument();
        expect(saveButton).not.toBeDisabled();
    });

    it('renders gender select field', async () => {
        render(
            <TestProviders>
                <ProfileForm />
            </TestProviders>,
        );

        await waitFor(() => {
            expect(screen.getByText('Giới tính')).toBeInTheDocument();
        });
    });

    it('shows validation error when full name is cleared and form is submitted', async () => {
        const user = userEvent.setup();

        render(
            <TestProviders>
                <ProfileForm />
            </TestProviders>,
        );

        await waitFor(() => {
            expect(screen.getByLabelText('Họ và tên')).toBeInTheDocument();
        });

        // Clear the full name field
        const fullNameInput = screen.getByLabelText('Họ và tên');
        await user.clear(fullNameInput);

        // Submit the form
        const submitButton = screen.getByRole('button', {
            name: 'Lưu thay đổi',
        });
        await user.click(submitButton);

        await waitFor(() => {
            // Should show a validation error
            expect(
                screen.getByText('Vui lòng nhập họ và tên'),
            ).toBeInTheDocument();
        });
    });
});
