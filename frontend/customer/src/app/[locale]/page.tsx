import { useTranslations } from 'next-intl';
import { setRequestLocale } from 'next-intl/server';
import { Button } from '@/components/ui/button.tsx';
import { Link } from '@/i18n/routing.ts';

type Props = {
    params: Promise<{ locale: string }>;
};

export default async function HomePage({ params }: Props) {
    const { locale } = await params;
    setRequestLocale(locale);

    return <HomeContent />;
}

function HomeContent() {
    const t = useTranslations('Auth');

    return (
        <div className='flex flex-col flex-1 items-center justify-center min-h-screen bg-background px-4'>
            <main className='flex flex-col items-center gap-6 text-center max-w-2xl'>
                <h1 className='text-4xl font-semibold tracking-tight text-foreground sm:text-5xl'>
                    TTBS Customer
                </h1>
                <p className='text-lg text-muted-foreground'>
                    Train Ticket Booking System
                </p>
                <div className='flex flex-col gap-3 sm:flex-row'>
                    <Button asChild>
                        <Link href='/login'>{t('login.submit')}</Link>
                    </Button>
                    <Button variant='outline' asChild>
                        <Link href='/register'>{t('register.submit')}</Link>
                    </Button>
                </div>
            </main>
        </div>
    );
}
