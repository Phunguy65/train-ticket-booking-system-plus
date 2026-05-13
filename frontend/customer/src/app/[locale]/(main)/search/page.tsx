import { setRequestLocale } from 'next-intl/server';
import { Suspense } from 'react';
import { SearchResults, TripListSkeleton } from '@/components/trips/index.ts';

type Props = {
    params: Promise<{ locale: string }>;
};

export default async function SearchPage({ params }: Props) {
    const { locale } = await params;
    setRequestLocale(locale);

    return (
        <div className='container mx-auto px-4 py-8 md:py-12'>
            <Suspense fallback={<TripListSkeleton count={5} />}>
                <SearchResults />
            </Suspense>
        </div>
    );
}
