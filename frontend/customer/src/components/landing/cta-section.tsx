'use client';

import { TrainFrontIcon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { TrainJourney } from '@/components/illustrations/index.ts';
import { Button } from '@/components/ui/button.tsx';
import { Link } from '@/i18n/routing.ts';

export function CtaSection() {
    const t = useTranslations('Landing.cta');

    return (
        <section className='relative overflow-hidden bg-gradient-to-br from-primary/5 via-background to-accent/5 py-16 md:py-24'>
            <TrainJourney className='absolute inset-0 h-full w-full opacity-20 dark:opacity-10' />
            <div className='container relative mx-auto px-4 text-center'>
                <div className='mx-auto max-w-2xl'>
                    <h2 className='animate-fade-in text-2xl font-bold tracking-tight sm:text-3xl'>
                        {t('title')}
                    </h2>
                    <p className='mt-4 animate-fade-in text-lg text-muted-foreground delay-100'>
                        {t('subtitle')}
                    </p>
                    <div className='mt-8 animate-fade-in delay-200'>
                        <Button
                            size='lg'
                            asChild
                            className='animate-bounce-subtle gap-2 px-8 text-base shadow-lg shadow-primary/25'
                        >
                            <Link href='/'>
                                <TrainFrontIcon className='h-5 w-5' />
                                {t('button')}
                            </Link>
                        </Button>
                    </div>
                </div>
            </div>
        </section>
    );
}
