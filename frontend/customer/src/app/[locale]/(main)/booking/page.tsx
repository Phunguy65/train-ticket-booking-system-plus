import { setRequestLocale } from 'next-intl/server';
import { Suspense } from 'react';
import { BookingConfirmation } from '@/components/booking/index.ts';
import { Skeleton } from '@/components/ui/skeleton.tsx';

type Props = {
    params: Promise<{ locale: string }>;
};

export default async function BookingPage({ params }: Props) {
    const { locale } = await params;
    setRequestLocale(locale);

    return (
        <div className='container mx-auto px-4 py-8 md:py-12'>
            <Suspense
                fallback={
                    <div className='space-y-6'>
                        <Skeleton className='h-8 w-48' />
                        <div className='grid gap-6 md:grid-cols-2'>
                            <Skeleton className='h-48' />
                            <Skeleton className='h-48' />
                        </div>
                    </div>
                }
            >
                <BookingConfirmation />
            </Suspense>
        </div>
    );
}
