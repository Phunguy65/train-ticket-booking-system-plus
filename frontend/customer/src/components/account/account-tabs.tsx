'use client';

import { ReceiptTextIcon, TicketIcon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import {
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from '@/components/ui/tabs.tsx';
import { BookingsList } from './bookings-list.tsx';
import { PaymentsList } from './payments-list.tsx';

export function AccountTabs() {
    const t = useTranslations('Account');

    return (
        <Tabs defaultValue='bookings'>
            <TabsList className='mb-6'>
                <TabsTrigger value='bookings' className='gap-2'>
                    <TicketIcon className='h-4 w-4' />
                    {t('tabs.bookings')}
                </TabsTrigger>
                <TabsTrigger value='payments' className='gap-2'>
                    <ReceiptTextIcon className='h-4 w-4' />
                    {t('tabs.payments')}
                </TabsTrigger>
            </TabsList>
            <TabsContent value='bookings'>
                <BookingsList />
            </TabsContent>
            <TabsContent value='payments'>
                <PaymentsList />
            </TabsContent>
        </Tabs>
    );
}
