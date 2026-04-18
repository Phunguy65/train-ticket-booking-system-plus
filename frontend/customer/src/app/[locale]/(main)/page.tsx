import { TrainFrontIcon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { setRequestLocale } from 'next-intl/server';
import { TripSearchForm } from '@/components/search/index.ts';

type Props = {
    params: Promise<{ locale: string }>;
};

export default async function HomePage({ params }: Props) {
    const { locale } = await params;
    setRequestLocale(locale);

    return <HomeContent />;
}

function HomeContent() {
    const t = useTranslations('Search');

    return (
        <div className='flex min-h-[calc(100vh-8rem)] flex-col items-center justify-center bg-gradient-to-b from-background to-muted/30 px-4 py-12'>
            <main className='flex w-full max-w-4xl flex-col items-center gap-8 text-center'>
                {/* Hero Section */}
                <div className='flex flex-col items-center gap-4'>
                    <div className='flex items-center gap-3'>
                        <TrainFrontIcon className='h-10 w-10 text-primary sm:h-12 sm:w-12' />
                        <h1 className='text-3xl font-bold tracking-tight text-foreground sm:text-4xl lg:text-5xl'>
                            TTBS
                        </h1>
                    </div>
                    <p className='max-w-lg text-base text-muted-foreground sm:text-lg'>
                        {t('title')}
                    </p>
                </div>

                {/* Search Form Card */}
                <div className='w-full rounded-xl border bg-card p-6 shadow-lg sm:p-8'>
                    <TripSearchForm />
                </div>
            </main>
        </div>
    );
}
