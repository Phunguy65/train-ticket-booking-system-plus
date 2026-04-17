import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, type Mock, vi } from 'vitest';
import { TestProviders } from '@/test-utils/providers.tsx';
import { RegisterForm } from './register-form.tsx';

const pushMock = vi.fn();
vi.mock('@/i18n/routing.ts', () => ({
    useRouter: () => ({ push: pushMock }),
    Link: ({ children }: { children: React.ReactNode }) => <>{children}</>,
    routing: { locales: ['vi', 'en'], defaultLocale: 'vi' },
}));

let registerMutationMock: {
    mutate: Mock;
    isPending: boolean;
    isError: boolean;
    error: Error | null;
};

vi.mock('@/lib/api/index.ts', () => ({
    registerMutation: () => ({
        mutationFn: registerMutationMock.mutate,
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

describe('RegisterForm', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        pushMock.mockClear();
        toastMock.mockClear();
        registerMutationMock = {
            mutate: vi.fn(),
            isPending: false,
            isError: false,
            error: null,
        };
    });

    it('renders all fields and the submit button in Vietnamese', () => {
        render(
            <TestProviders>
                <RegisterForm />
            </TestProviders>,
        );

        expect(screen.getByLabelText('Họ và tên')).toBeInTheDocument();
        expect(screen.getByLabelText('Email')).toBeInTheDocument();
        expect(screen.getByLabelText('Mật khẩu')).toBeInTheDocument();
        expect(screen.getByLabelText('Xác nhận mật khẩu')).toBeInTheDocument();
        expect(
            screen.getByRole('button', { name: 'Đăng ký' }),
        ).toBeInTheDocument();
    });

    it('shows all required-field errors when submitting an empty form', async () => {
        const user = userEvent.setup();

        render(
            <TestProviders>
                <RegisterForm />
            </TestProviders>,
        );

        await user.click(screen.getByRole('button', { name: 'Đăng ký' }));

        expect(
            await screen.findByText('Vui lòng nhập họ và tên'),
        ).toBeInTheDocument();
        expect(screen.getByText('Vui lòng nhập email')).toBeInTheDocument();
        expect(screen.getByText('Vui lòng nhập mật khẩu')).toBeInTheDocument();
        expect(
            screen.getByText('Vui lòng xác nhận mật khẩu'),
        ).toBeInTheDocument();
    });

    it('shows "password min length" when password is shorter than 8 chars', async () => {
        const user = userEvent.setup();

        render(
            <TestProviders>
                <RegisterForm />
            </TestProviders>,
        );

        await user.type(screen.getByLabelText('Họ và tên'), 'Nguyen A');
        await user.type(screen.getByLabelText('Email'), 'user@example.com');
        await user.type(screen.getByLabelText('Mật khẩu'), 'short');
        await user.type(screen.getByLabelText('Xác nhận mật khẩu'), 'short');
        await user.click(screen.getByRole('button', { name: 'Đăng ký' }));

        expect(
            await screen.findByText('Mật khẩu phải có ít nhất 8 ký tự'),
        ).toBeInTheDocument();
    });

    it('shows "password mismatch" when confirm password does not match', async () => {
        const user = userEvent.setup();

        render(
            <TestProviders>
                <RegisterForm />
            </TestProviders>,
        );

        await user.type(screen.getByLabelText('Họ và tên'), 'Nguyen A');
        await user.type(screen.getByLabelText('Email'), 'user@example.com');
        await user.type(screen.getByLabelText('Mật khẩu'), 'secret123');
        await user.type(
            screen.getByLabelText('Xác nhận mật khẩu'),
            'different123',
        );
        await user.click(screen.getByRole('button', { name: 'Đăng ký' }));

        expect(
            await screen.findByText('Mật khẩu xác nhận không khớp'),
        ).toBeInTheDocument();
    });

    it('shows "fullName min length" when full name is 1 character', async () => {
        const user = userEvent.setup();

        render(
            <TestProviders>
                <RegisterForm />
            </TestProviders>,
        );

        await user.type(screen.getByLabelText('Họ và tên'), 'A');
        await user.type(screen.getByLabelText('Email'), 'user@example.com');
        await user.type(screen.getByLabelText('Mật khẩu'), 'password123');
        await user.type(
            screen.getByLabelText('Xác nhận mật khẩu'),
            'password123',
        );
        await user.click(screen.getByRole('button', { name: 'Đăng ký' }));

        expect(
            await screen.findByText('Họ và tên phải có ít nhất 2 ký tự'),
        ).toBeInTheDocument();
    });

    it('calls registerMutation with correct payload when form is valid', async () => {
        const user = userEvent.setup();

        render(
            <TestProviders>
                <RegisterForm />
            </TestProviders>,
        );

        await user.type(screen.getByLabelText('Họ và tên'), 'Nguyen Van A');
        await user.type(screen.getByLabelText('Email'), 'test@example.com');
        await user.type(screen.getByLabelText('Mật khẩu'), 'password123');
        await user.type(
            screen.getByLabelText('Xác nhận mật khẩu'),
            'password123',
        );
        await user.click(screen.getByRole('button', { name: 'Đăng ký' }));

        await waitFor(() => {
            expect(registerMutationMock.mutate).toHaveBeenCalledWith(
                expect.objectContaining({
                    body: {
                        email: 'test@example.com',
                        password: 'password123',
                        fullName: 'Nguyen Van A',
                    },
                }),
                expect.anything(),
            );
        });
    });
});
