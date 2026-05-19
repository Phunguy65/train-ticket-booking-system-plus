'use client';

import { useTranslations } from 'next-intl';

export function SeatLegend() {
    const t = useTranslations('Seats');

    const items = [
        {
            label: t('available'),
            className: 'border-primary/50 bg-background',
        },
        {
            label: t('selected'),
            className: 'border-primary bg-primary text-primary-foreground',
        },
        {
            label: t('held'),
            className: 'border-yellow-500/50 bg-yellow-500/10',
        },
        {
            label: t('booked'),
            className: 'border-muted bg-muted text-muted-foreground',
        },
    ];

    return (
        <div className='flex flex-wrap gap-4'>
            {items.map((item) => (
                <div key={item.label} className='flex items-center gap-2'>
                    <div
                        className={`h-6 w-6 rounded border ${item.className}`}
                        aria-hidden='true'
                    />
                    <span className='text-sm text-muted-foreground'>
                        {item.label}
                    </span>
                </div>
            ))}
        </div>
    );
}
