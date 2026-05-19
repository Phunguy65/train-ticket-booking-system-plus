'use client';

import {
    ArmchairIcon,
    CreditCardIcon,
    SearchIcon,
    TicketCheckIcon,
} from 'lucide-react';
import { useTranslations } from 'next-intl';

const steps = [
    { step: 1, icon: SearchIcon, key: 'search' },
    { step: 2, icon: ArmchairIcon, key: 'selectSeat' },
    { step: 3, icon: CreditCardIcon, key: 'payment' },
    { step: 4, icon: TicketCheckIcon, key: 'getTicket' },
] as const;

export function HowItWorksSection() {
    const t = useTranslations('Landing.howItWorks');

    return (
        <section className='bg-muted/50 py-16 md:py-24'>
            <div className='container mx-auto px-4'>
                <h2 className='animate-fade-in text-center text-2xl font-bold tracking-tight sm:text-3xl'>
                    {t('title')}
                </h2>
                <p className='mx-auto mt-4 max-w-2xl animate-fade-in text-center text-muted-foreground delay-100'>
                    {t('subtitle')}
                </p>
                <div className='mt-12 grid gap-8 sm:grid-cols-2 lg:grid-cols-4'>
                    {steps.map((step, i) => (
                        <div
                            key={step.key}
                            className={`relative animate-fade-in text-center delay-${(i + 1) * 100}`}
                        >
                            <div className='mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-primary text-lg font-bold text-primary-foreground shadow-lg shadow-primary/25'>
                                {step.step}
                            </div>
                            <step.icon className='mx-auto mb-3 h-8 w-8 text-primary/70' />
                            <h3 className='font-semibold'>
                                {t(`${step.key}.title`)}
                            </h3>
                            <p className='mt-1 text-sm text-muted-foreground'>
                                {t(`${step.key}.description`)}
                            </p>
                            {i < steps.length - 1 && (
                                <div className='absolute top-7 left-[calc(50%+2.5rem)] hidden h-px w-[calc(100%-5rem)] bg-border lg:block' />
                            )}
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
}
