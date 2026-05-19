import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, type Mock, vi } from 'vitest';
import { TestProviders } from '@/test-utils/providers.tsx';
import { LoginForm } from './login-form.tsx';

// Mock the locale-aware router so submitting doesn't actually navigate
const pushMock = vi.fn();
vi.mock('@/i18n/routing.ts', () => ({
    useRouter: () => ({ push: pushMock }),
    Link: ({ children }: { children: React.ReactNode }) => <>{children}</>,
    routing: { locales: ['vi', 'en'], defaultLocale: 'vi' },
}));

// Mock next/navigation's useSearchParams
let searchParamsMock = new URLSearchParams();
vi.mock('next/navigation', async () => {
    const actual =
        await vi.importActual<typeof import('next/navigation')>(
            'next/navigation',
        );
    return {
        ...actual,
        useSearchParams: () => searchParamsMock,
    };
});

// Mock the API mutation
let loginMutationMock: {
    mutate: Mock;
    isPending: boolean;
    isError: boolean;
    error: Error | null;
};

vi.mock('@/lib/api/index.ts', () => ({
    loginMutation: () => ({
        mutationFn: loginMutationMock.mutate,
    }),
    ApiFailError: class ApiFailError extends Error {
        code: string;
        violations: Array<unknown>;
        constructor(options: { code?: string; message?: string }) {
            super(options.message ?? 'Request failed');
            this.name = 'ApiFailError';
            this.code = options.code ?? '';
            this.violations = [];
        }
    },
    ApiTechnicalError: class ApiTechnicalError extends Error {
        constructor(options?: { message?: string }) {
            super(options?.message ?? 'Unexpected technical failure');
            this.name = 'ApiTechnicalError';
        }
    },
}));

// Mock the toast utility
const toastMock = vi.fn();
vi.mock('@/lib/toast.ts', () => ({
    showNetworkErrorToast: (...args: unknown[]) => toastMock(...args),
}));

describe('LoginForm', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        pushMock.mockClear();
        toastMock.mockClear();
        searchParamsMock = new URLSearchParams();
        loginMutationMock = {
            mutate: vi.fn(),
            isPending: false,
            isError: false,
            error: null,
        };
    });

    it('renders email and password fields and the submit button in Vietnamese', () => {
        render(
            <TestProviders>
                <LoginForm />
            </TestProviders>,
        );

        expect(screen.getByLabelText('Email')).toBeInTheDocument();
        expect(screen.getByLabelText('Mật khẩu')).toBeInTheDocument();
        expect(
            screen.getByRole('button', { name: 'Đăng nhập' }),
        ).toBeInTheDocument();
    });

    it('shows validation errors when submitting an empty form', async () => {
        const user = userEvent.setup();

        render(
            <TestProviders>
                <LoginForm />
            </TestProviders>,
        );

        await user.click(screen.getByRole('button', { name: 'Đăng nhập' }));

        expect(
            await screen.findByText('Vui lòng nhập email'),
        ).toBeInTheDocument();
        expect(screen.getByText('Vui lòng nhập mật khẩu')).toBeInTheDocument();
    });

    it('shows "invalid email" when email format is wrong', async () => {
        const user = userEvent.setup();

        render(
            <TestProviders>
                <LoginForm />
            </TestProviders>,
        );

        await user.type(screen.getByLabelText('Email'), 'not-an-email');
        await user.type(screen.getByLabelText('Mật khẩu'), 'secret123');
        await user.click(screen.getByRole('button', { name: 'Đăng nhập' }));

        expect(
            await screen.findByText('Email không hợp lệ'),
        ).toBeInTheDocument();
    });

    it('shows success banner when ?registered=true query param is present', () => {
        searchParamsMock = new URLSearchParams('registered=true');

        render(
            <TestProviders>
                <LoginForm />
            </TestProviders>,
        );

        expect(
            screen.getByText('Đăng ký thành công! Vui lòng đăng nhập.'),
        ).toBeInTheDocument();

        // Should use success variant with role="status" (polite announcement, not assertive)
        const statusAlert = screen.getByRole('status');
        expect(statusAlert).toHaveClass('bg-green-50');
    });

    it('does not show success banner when query param is absent', () => {
        render(
            <TestProviders>
                <LoginForm />
            </TestProviders>,
        );

        expect(
            screen.queryByText('Đăng ký thành công! Vui lòng đăng nhập.'),
        ).not.toBeInTheDocument();
    });

    it('calls loginMutation with correct payload when form is valid', async () => {
        const user = userEvent.setup();

        render(
            <TestProviders>
                <LoginForm />
            </TestProviders>,
        );

        await user.type(screen.getByLabelText('Email'), 'test@example.com');
        await user.type(screen.getByLabelText('Mật khẩu'), 'password123');
        await user.click(screen.getByRole('button', { name: 'Đăng nhập' }));

        await waitFor(() => {
            expect(loginMutationMock.mutate).toHaveBeenCalledWith(
                expect.objectContaining({
                    body: {
                        email: 'test@example.com',
                        password: 'password123',
                    },
                }),
                expect.anything(),
            );
        });
    });
});
