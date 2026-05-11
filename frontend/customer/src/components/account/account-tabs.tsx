'use client';

import { useQuery } from '@tanstack/react-query';
import {
    CalendarCheckIcon,
    ReceiptTextIcon,
    TicketIcon,
    WalletIcon,
} from 'lucide-react';
import { useTranslations } from 'next-intl';
import {
    Card,
    CardContent,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.tsx';
import { Skeleton } from '@/components/ui/skeleton.tsx';
import {
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from '@/components/ui/tabs.tsx';
import type { UserBookingResponse } from '@/lib/api/generated/types.gen.ts';
import {
    getAuthenticatedUserOptions,
    getUserBookingsOptions,
} from '@/lib/api/index.ts';
import { formatPrice } from '@/lib/customer-utils.ts';
import { BookingsList } from './bookings-list.tsx';
import { PaymentsList } from './payments-list.tsx';

export function AccountTabs() {
    const t = useTranslations('Account');

    const { data: user } = useQuery({
        ...getAuthenticatedUserOptions(),
    });

    const { data: bookings, isLoading } = useQuery({
        ...getUserBookingsOptions({
            path: { userId: user?.id ?? '' },
            query: { request: { page: 0, size: 20 } },
        }),
        enabled: !!user?.id,
    });

    return (
        <Tabs defaultValue='bookings' className='space-y-6'>
            <AccountStatsRow
                bookings={bookings?.content ?? []}
                totalBookings={
                    bookings?.total ?? bookings?.content?.length ?? 0
                }
                isLoading={isLoading}
            />
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

type AccountStatsRowProps = {
    bookings: Array<UserBookingResponse>;
    totalBookings: number;
    isLoading: boolean;
};

function AccountStatsRow({
    bookings,
    totalBookings,
    isLoading,
}: AccountStatsRowProps) {
    const t = useTranslations('Account.stats');
    const activeBookings = bookings.filter(
        (booking) =>
            booking.status === 'CONFIRMED' || booking.status === 'HELD',
    ).length;
    const totalSpent = bookings
        .filter((booking) => booking.status === 'CONFIRMED')
        .reduce((sum, booking) => sum + (booking.totalPrice ?? 0), 0);

    const stats = [
        {
            label: t('totalBookings'),
            value: totalBookings.toString(),
            icon: TicketIcon,
            className: 'animate-fade-in delay-100 bg-accent text-primary',
        },
        {
            label: t('upcomingTrips'),
            value: activeBookings.toString(),
            icon: CalendarCheckIcon,
            className:
                'animate-fade-in delay-200 bg-sky-50 text-sky-700 dark:bg-sky-950/40 dark:text-sky-300',
        },
        {
            label: t('totalSpent'),
            value: totalSpent > 0 ? formatPrice(totalSpent) : '0',
            icon: WalletIcon,
            className:
                'animate-fade-in delay-300 bg-orange-50 text-orange-700 dark:bg-orange-950/40 dark:text-orange-300',
        },
    ];

    return (
        <div className='grid gap-4 md:grid-cols-3'>
            {stats.map((stat) => (
                <Card key={stat.label} className={stat.className}>
                    <CardHeader className='flex flex-row items-center justify-between space-y-0 pb-2'>
                        <CardTitle className='font-medium text-sm'>
                            {stat.label}
                        </CardTitle>
                        <stat.icon className='h-5 w-5' />
                    </CardHeader>
                    <CardContent>
                        {isLoading ? (
                            <Skeleton className='h-8 w-24 bg-current/15' />
                        ) : (
                            <p className='font-bold text-3xl tracking-tight'>
                                {stat.value}
                            </p>
                        )}
                    </CardContent>
                </Card>
            ))}
        </div>
    );
}
