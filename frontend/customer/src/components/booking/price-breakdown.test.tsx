import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { TestProviders } from '@/test-utils/providers.tsx';
import { PriceBreakdown, PriceSummary } from './price-breakdown.tsx';

describe('PriceBreakdown', () => {
    it('renders seat pricing with quantity', () => {
        render(
            <TestProviders>
                <PriceBreakdown pricePerSeat={350000} seatCount={2} />
            </TestProviders>,
        );

        // Check for seat count in label
        expect(screen.getByText(/× 2/)).toBeInTheDocument();
        // Check for total
        expect(screen.getByText('Tổng cộng')).toBeInTheDocument();
    });

    it('shows service fee when provided', () => {
        render(
            <TestProviders>
                <PriceBreakdown
                    pricePerSeat={350000}
                    seatCount={2}
                    serviceFee={50000}
                />
            </TestProviders>,
        );

        expect(screen.getByText('Phí dịch vụ')).toBeInTheDocument();
    });

    it('shows discount when provided', () => {
        render(
            <TestProviders>
                <PriceBreakdown
                    pricePerSeat={350000}
                    seatCount={2}
                    discount={100000}
                />
            </TestProviders>,
        );

        expect(screen.getByText('Giảm giá')).toBeInTheDocument();
    });

    it('calculates total correctly', () => {
        // Price: 350000 * 2 = 700000
        // Service: 50000
        // Discount: -100000
        // Total: 650000
        render(
            <TestProviders>
                <PriceBreakdown
                    pricePerSeat={350000}
                    seatCount={2}
                    serviceFee={50000}
                    discount={100000}
                />
            </TestProviders>,
        );

        // Total should be displayed (using VND format with period separator)
        expect(screen.getByText('650.000 ₫')).toBeInTheDocument();
    });

    it('renders inline mode without card wrapper', () => {
        const { container } = render(
            <TestProviders>
                <PriceBreakdown pricePerSeat={350000} seatCount={1} inline />
            </TestProviders>,
        );

        // Should not have card structure (no header with title)
        expect(
            container.querySelector('[data-slot="card"]'),
        ).not.toBeInTheDocument();
    });

    it('renders custom line items', () => {
        render(
            <TestProviders>
                <PriceBreakdown
                    pricePerSeat={350000}
                    seatCount={1}
                    additionalItems={[
                        { label: 'Insurance', amount: 20000 },
                        {
                            label: 'Loyalty discount',
                            amount: 50000,
                            isDiscount: true,
                        },
                    ]}
                />
            </TestProviders>,
        );

        expect(screen.getByText('Insurance')).toBeInTheDocument();
        expect(screen.getByText('Loyalty discount')).toBeInTheDocument();
    });
});

describe('PriceSummary', () => {
    it('renders total with default label', () => {
        render(
            <TestProviders>
                <PriceSummary total={700000} />
            </TestProviders>,
        );

        expect(screen.getByText('Tổng cộng')).toBeInTheDocument();
        expect(screen.getByText('700.000 ₫')).toBeInTheDocument();
    });

    it('renders with custom label', () => {
        render(
            <TestProviders>
                <PriceSummary total={700000} label='Subtotal' />
            </TestProviders>,
        );

        expect(screen.getByText('Subtotal')).toBeInTheDocument();
    });
});
