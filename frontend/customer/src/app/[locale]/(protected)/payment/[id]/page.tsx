'use client';

import { ArrowLeftIcon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { use } from 'react';
import { PaymentDetail } from '@/components/payment/index.ts';
import { Button } from '@/components/ui/button.tsx';
import { Link } from '@/i18n/routing.ts';

type Props = {
    params: Promise<{ id: string }>;
};

export default function PaymentDetailPage({ params }: Props) {
    const { id } = use(params);
    const t = useTranslations('PaymentDetail');

    return (
        <div className='container max-w-2xl px-4 py-8'>
            <div className='mb-6 flex items-center gap-4'>
                <Button variant='ghost' size='sm' asChild>
                    <Link href='/account'>
                        <ArrowLeftIcon className='h-4 w-4 mr-2' />
                        {t('backToAccount')}
                    </Link>
                </Button>
            </div>

            <PaymentDetail paymentId={id} />
        </div>
    );
}
