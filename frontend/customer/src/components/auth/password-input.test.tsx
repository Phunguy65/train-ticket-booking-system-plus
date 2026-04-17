import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { NextIntlClientProvider } from 'next-intl';
import { describe, expect, it } from 'vitest';
import viMessages from '@/messages/vi.json' with { type: 'json' };
import { PasswordInput } from './password-input.tsx';

function renderWithI18n(ui: React.ReactNode) {
    return render(
        <NextIntlClientProvider locale='vi' messages={viMessages}>
            {ui}
        </NextIntlClientProvider>,
    );
}

describe('PasswordInput', () => {
    it('renders as hidden (type="password") by default', () => {
        renderWithI18n(<PasswordInput data-testid='password-input' />);

        const input = screen.getByTestId('password-input');
        expect(input).toHaveAttribute('type', 'password');
    });

    it('toggles to visible (type="text") when eye button is clicked', async () => {
        const user = userEvent.setup();
        renderWithI18n(<PasswordInput data-testid='password-input' />);

        const input = screen.getByTestId('password-input');
        const toggleButton = screen.getByRole('button', {
            name: 'Hiện mật khẩu',
        });

        await user.click(toggleButton);

        expect(input).toHaveAttribute('type', 'text');
    });

    it('toggles back to hidden when eye button is clicked again', async () => {
        const user = userEvent.setup();
        renderWithI18n(<PasswordInput data-testid='password-input' />);

        const input = screen.getByTestId('password-input');
        const toggleButton = screen.getByRole('button', {
            name: 'Hiện mật khẩu',
        });

        // Toggle visible
        await user.click(toggleButton);
        expect(input).toHaveAttribute('type', 'text');

        // Toggle back to hidden - button aria-label has changed
        const hideButton = screen.getByRole('button', { name: 'Ẩn mật khẩu' });
        await user.click(hideButton);

        expect(input).toHaveAttribute('type', 'password');
    });

    it('respects disabled prop on both input and toggle button', () => {
        renderWithI18n(<PasswordInput data-testid='password-input' disabled />);

        const input = screen.getByTestId('password-input');
        const toggleButton = screen.getByRole('button', {
            name: 'Hiện mật khẩu',
        });

        expect(input).toBeDisabled();
        expect(toggleButton).toBeDisabled();
    });

    it('updates aria-label for toggle button based on visibility state', async () => {
        const user = userEvent.setup();
        renderWithI18n(<PasswordInput data-testid='password-input' />);

        // Initially hidden - aria-label should be "Hiện mật khẩu" (show password)
        expect(
            screen.getByRole('button', { name: 'Hiện mật khẩu' }),
        ).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: 'Hiện mật khẩu' }));

        // Now visible - aria-label should be "Ẩn mật khẩu" (hide password)
        expect(
            screen.getByRole('button', { name: 'Ẩn mật khẩu' }),
        ).toBeInTheDocument();
    });

    it('is keyboard accessible (toggle button is in tab order)', () => {
        renderWithI18n(<PasswordInput data-testid='password-input' />);

        const toggleButton = screen.getByRole('button', {
            name: 'Hiện mật khẩu',
        });

        // No tabIndex means it defaults to 0 (in tab order)
        expect(toggleButton).not.toHaveAttribute('tabindex', '-1');
    });
});
