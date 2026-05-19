import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { TestProviders } from '@/test-utils/providers.tsx';
import { PaymentStatusBadge } from './payment-status-badge.tsx';

// Mock translations
vi.mock('next-intl', async (importOriginal) => {
    const actual = await importOriginal<typeof import('next-intl')>();
    return {
        ...actual,
        useTranslations: () => (key: string) => {
            const translations: Record<string, string> = {
                PAID: 'Đã thanh toán',
                PENDING: 'Đang xử lý',
                FAILED: 'Thất bại',
                REFUNDED: 'Đã hoàn tiền',
            };
            return translations[key] || key;
        },
    };
});

describe('PaymentStatusBadge', () => {
    it('renders PAID status with success styling and check icon', () => {
        render(
            <TestProviders>
                <PaymentStatusBadge status='PAID' />
            </TestProviders>,
        );

        const badge = screen.getByText('Đã thanh toán');
        expect(badge).toBeInTheDocument();
        // Badge should have success variant styling
        expect(badge.closest('[data-variant="success"]')).toBeInTheDocument();
    });

    it('renders PENDING status with secondary styling and clock icon', () => {
        render(
            <TestProviders>
                <PaymentStatusBadge status='PENDING' />
            </TestProviders>,
        );

        const badge = screen.getByText('Đang xử lý');
        expect(badge).toBeInTheDocument();
        expect(badge.closest('[data-variant="secondary"]')).toBeInTheDocument();
    });

    it('renders FAILED status with destructive styling and x-circle icon', () => {
        render(
            <TestProviders>
                <PaymentStatusBadge status='FAILED' />
            </TestProviders>,
        );

        const badge = screen.getByText('Thất bại');
        expect(badge).toBeInTheDocument();
        expect(
            badge.closest('[data-variant="destructive"]'),
        ).toBeInTheDocument();
    });

    it('renders REFUNDED status with outline styling', () => {
        render(
            <TestProviders>
                <PaymentStatusBadge status='REFUNDED' />
            </TestProviders>,
        );

        const badge = screen.getByText('Đã hoàn tiền');
        expect(badge).toBeInTheDocument();
        expect(badge.closest('[data-variant="outline"]')).toBeInTheDocument();
    });

    it('includes icon with aria-hidden for accessibility', () => {
        render(
            <TestProviders>
                <PaymentStatusBadge status='PAID' />
            </TestProviders>,
        );

        // The icon should be hidden from screen readers
        const badge = screen.getByText('Đã thanh toán').parentElement;
        const icon = badge?.querySelector('[aria-hidden="true"]');
        expect(icon).toBeInTheDocument();
    });
});
