'use client';

import { ArrowRightIcon, MapPinIcon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { Link } from '@/i18n/routing.ts';

const routes = [
    { key: 'hnSg', from: 'Hà Nội', to: 'Sài Gòn' },
    { key: 'hnDn', from: 'Hà Nội', to: 'Đà Nẵng' },
    { key: 'sgNt', from: 'Sài Gòn', to: 'Nha Trang' },
    { key: 'hnHue', from: 'Hà Nội', to: 'Huế' },
    { key: 'dnSg', from: 'Đà Nẵng', to: 'Sài Gòn' },
    { key: 'sgDn', from: 'Sài Gòn', to: 'Đà Nẵng' },
] as const;

export function PopularRoutesSection() {
    const t = useTranslations('Landing.popularRoutes');

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
            <div className='mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-3'>
                {routes.map((route, i) => (
                    <Link
                        key={route.key}
                        href='/'
                        className={`group animate-fade-in rounded-xl border bg-card p-5 transition-all duration-200 hover:-translate-y-0.5 hover:shadow-md hover:border-primary/20 delay-${(i + 1) * 100}`}
                    >
                        <div className='flex items-center gap-3'>
                            <div className='flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-primary/10'>
                                <MapPinIcon className='h-5 w-5 text-primary' />
                            </div>
                            <div className='flex-1'>
                                <div className='flex items-center gap-2 font-medium'>
                                    <span>{route.from}</span>
                                    <ArrowRightIcon className='h-4 w-4 text-muted-foreground' />
                                    <span>{route.to}</span>
                                </div>
                                <p className='mt-0.5 text-sm text-muted-foreground'>
                                    {t(`${route.key}.duration`)}
                                </p>
                            </div>
                            <ArrowRightIcon className='h-4 w-4 text-muted-foreground opacity-0 transition-opacity group-hover:opacity-100' />
                        </div>
                    </Link>
                ))}
            </div>
        </section>
    );
}
