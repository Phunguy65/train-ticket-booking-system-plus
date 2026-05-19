'use client';

import {
    ClockIcon,
    ShieldCheckIcon,
    SmartphoneIcon,
    TicketIcon,
} from 'lucide-react';
import { useTranslations } from 'next-intl';

const features = [
    { icon: TicketIcon, key: 'quickBooking' },
    { icon: ShieldCheckIcon, key: 'securePayment' },
    { icon: SmartphoneIcon, key: 'eTicket' },
    { icon: ClockIcon, key: 'realtime' },
] as const;

export function FeaturesSection() {
    const t = useTranslations('Landing.features');

    return (
        <section className='container mx-auto px-4 py-16 md:py-24'>
            <div className='mx-auto max-w-3xl text-center'>
                <h2 className='animate-fade-in text-2xl font-bold tracking-tight sm:text-3xl'>
                    {t('title')}
                </h2>
                <p className='mt-4 animate-fade-in text-muted-foreground delay-100'>
                    {t('subtitle')}
                </p>
            </div>
            <div className='mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4'>
                {features.map((feature, i) => (
                    <div
                        key={feature.key}
                        className={`group relative animate-fade-in rounded-2xl border bg-card p-6 transition-all duration-200 hover:-translate-y-1 hover:shadow-lg hover:border-primary/20 delay-${(i + 1) * 100}`}
                    >
                        <div className='mb-4 inline-flex rounded-xl bg-primary/10 p-3 transition-colors group-hover:bg-primary/15'>
                            <feature.icon className='h-6 w-6 text-primary' />
                        </div>
                        <h3 className='font-semibold'>
                            {t(`${feature.key}.title`)}
                        </h3>
                        <p className='mt-2 text-sm text-muted-foreground'>
                            {t(`${feature.key}.description`)}
                        </p>
                    </div>
                ))}
            </div>
        </section>
    );
}
