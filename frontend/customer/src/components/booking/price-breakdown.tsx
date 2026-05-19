'use client';

import { useTranslations } from 'next-intl';
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.tsx';
import { formatPrice } from '@/lib/customer-utils.ts';
import { cn } from '@/lib/utils.ts';

/**
 * Line item in the price breakdown.
 */
export type PriceLineItem = {
    label: string;
    amount: number;
    quantity?: number;
    /**
     * If true, this item is a discount and should be displayed as negative.
     */
    isDiscount?: boolean;
};

/**
 * Props for PriceBreakdown component.
 */
export type PriceBreakdownProps = {
    /**
     * Price per seat in minor units (VND).
     */
    pricePerSeat: number;
    /**
     * Number of seats.
     */
    seatCount: number;
    /**
     * Optional service fee in minor units.
     */
    serviceFee?: number;
    /**
     * Optional discount in minor units (positive number).
     */
    discount?: number;
    /**
     * Optional custom line items to add before the total.
     */
    additionalItems?: PriceLineItem[];
    /**
     * Additional class names.
     */
    className?: string;
    /**
     * If true, renders without the Card wrapper (for inline use).
     * @default false
     */
    inline?: boolean;
};

/**
 * Price breakdown component showing itemized pricing with subtotal,
 * optional fees/discounts, and total.
 */
export function PriceBreakdown({
    pricePerSeat,
    seatCount,
    serviceFee = 0,
    discount = 0,
    additionalItems = [],
    className,
    inline = false,
}: PriceBreakdownProps) {
    const t = useTranslations('PriceBreakdown');

    // Calculate totals
    const subtotal = pricePerSeat * seatCount;
    const totalBeforeDiscount = subtotal + serviceFee;
    const total = Math.max(0, totalBeforeDiscount - discount);

    const content = (
        <div className='space-y-3'>
            {/* Seat price line */}
            <div className='flex justify-between text-sm'>
                <span className='text-muted-foreground'>
                    {t('pricePerSeat')} × {seatCount}
                </span>
                <span>{formatPrice(subtotal)}</span>
            </div>

            {/* Service fee */}
            {serviceFee > 0 && (
                <div className='flex justify-between text-sm'>
                    <span className='text-muted-foreground'>
                        {t('serviceFee')}
                    </span>
                    <span>{formatPrice(serviceFee)}</span>
                </div>
            )}

            {/* Additional items */}
            {additionalItems.map((item) => (
                <div key={item.label} className='flex justify-between text-sm'>
                    <span className='text-muted-foreground'>
                        {item.label}
                        {item.quantity
                            && item.quantity > 1
                            && ` × ${item.quantity}`}
                    </span>
                    <span className={item.isDiscount ? 'text-green-600' : ''}>
                        {item.isDiscount ? '-' : ''}
                        {formatPrice(item.amount * (item.quantity ?? 1))}
                    </span>
                </div>
            ))}

            {/* Discount */}
            {discount > 0 && (
                <div className='flex justify-between text-sm'>
                    <span className='text-muted-foreground'>
                        {t('discount')}
                    </span>
                    <span className='text-green-600'>
                        -{formatPrice(discount)}
                    </span>
                </div>
            )}

            {/* Total */}
            <div className='flex justify-between border-t pt-3'>
                <span className='font-semibold'>{t('total')}</span>
                <span className='text-lg font-bold text-primary'>
                    {formatPrice(total)}
                </span>
            </div>
        </div>
    );

    if (inline) {
        return <div className={className}>{content}</div>;
    }

    return (
        <Card className={cn(className)}>
            <CardHeader>
                <CardTitle className='text-base'>{t('title')}</CardTitle>
            </CardHeader>
            <CardContent>{content}</CardContent>
        </Card>
    );
}

/**
 * Compact price summary showing just the total.
 * Useful for sticky footers and mobile CTAs.
 */
export function PriceSummary({
    total,
    label,
    className,
}: {
    total: number;
    label?: string;
    className?: string;
}) {
    const t = useTranslations('PriceBreakdown');

    return (
        <div className={cn('flex items-center justify-between', className)}>
            <span className='text-sm text-muted-foreground'>
                {label ?? t('total')}
            </span>
            <span className='text-lg font-bold text-primary'>
                {formatPrice(total)}
            </span>
        </div>
    );
}
