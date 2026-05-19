import { TrainFrontIcon } from 'lucide-react';
import Image from 'next/image';
import { useTranslations } from 'next-intl';
import { setRequestLocale } from 'next-intl/server';
import {
    CloudsDecoration,
    RicePaddyHero,
    TrackPattern,
} from '@/components/illustrations/index.ts';
import {
    CtaSection,
    FeaturesSection,
    HowItWorksSection,
    PopularRoutesSection,
} from '@/components/landing/index.ts';
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
        <>
            <div className='relative flex min-h-[calc(100vh-8rem)] flex-col items-center justify-center overflow-hidden px-4 py-16'>
                <Image
                    src='/images/hero-train.webp'
                    alt=''
                    fill
                    priority
                    className='object-cover'
                    sizes='100vw'
                />
                <div className='absolute inset-0 bg-gradient-to-b from-background/80 via-background/60 to-background/90 dark:from-background/90 dark:via-background/75 dark:to-background/95' />

                <RicePaddyHero className='absolute inset-0 h-full w-full opacity-40 dark:opacity-20' />
                <CloudsDecoration className='absolute top-10 left-4 h-32 w-72 animate-float text-primary sm:left-12' />
                <CloudsDecoration className='absolute right-0 bottom-20 h-28 w-64 animate-float delay-400 text-primary/60' />

                <main className='relative z-10 flex w-full max-w-4xl flex-col items-center gap-8 text-center'>
                    <div className='flex flex-col items-center gap-5'>
                        <div className='animate-fade-in delay-100 flex items-center gap-3 rounded-full bg-background/80 px-5 py-3 shadow-lg shadow-primary/10 ring-1 ring-primary/10 backdrop-blur'>
                            <TrainFrontIcon className='h-10 w-10 text-primary sm:h-12 sm:w-12' />
                            <div className='flex flex-col items-start'>
                                <h1 className='text-3xl font-bold tracking-tighter text-foreground sm:text-4xl lg:text-4xl'>
                                    VietRail
                                </h1>
                                <span className='text-xs font-medium text-muted-foreground sm:text-sm'>
                                    Hành trình Việt Nam
                                </span>
                            </div>
                        </div>
                        <p className='delay-200 max-w-xl animate-fade-in text-base leading-8 text-muted-foreground sm:text-lg'>
                            {t('title')}
                        </p>
                    </div>

                    <div className='delay-300 w-full animate-fade-in rounded-3xl border border-primary/15 bg-card/95 p-6 shadow-2xl shadow-primary/10 backdrop-blur sm:p-8'>
                        <TripSearchForm />
                    </div>

                    <TrackPattern className='delay-400 w-full max-w-md animate-fade-in text-primary/50' />
                </main>
            </div>

            <FeaturesSection />
            <HowItWorksSection />
            <PopularRoutesSection />
            <CtaSection />
        </>
    );
}
