'use client';

import { useTranslations } from 'next-intl';
import { AccountTabs } from '@/components/account/index.ts';

export default function AccountPage() {
    const t = useTranslations('Account');

    return (
        <div className='container mx-auto px-4 py-8 md:py-12'>
            <h1 className='mb-6 text-2xl font-bold'>{t('title')}</h1>
            <AccountTabs />
        </div>
    );
}
