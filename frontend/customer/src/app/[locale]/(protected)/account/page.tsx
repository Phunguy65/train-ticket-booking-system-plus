import { useTranslations } from 'next-intl';
import { setRequestLocale } from 'next-intl/server';
import { BookingsList } from '@/components/account/index.ts';

type Props = {
    params: Promise<{ locale: string }>;
};

export default async function AccountPage({ params }: Props) {
    const { locale } = await params;
    setRequestLocale(locale);

    return <AccountPageContent />;
}

function AccountPageContent() {
    const t = useTranslations('Account');

    return (
        <div className='container px-4 py-8'>
            <h1 className='mb-6 text-2xl font-bold'>{t('title')}</h1>
            <BookingsList />
        </div>
    );
}
