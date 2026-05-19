'use client';

import { CheckCircleIcon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert.tsx';
import { Button } from '@/components/ui/button.tsx';
import {
    Card,
    CardContent,
    CardDescription,
    CardFooter,
    CardHeader,
    CardTitle,
} from '@/components/ui/card.tsx';
import { Link } from '@/i18n/routing.ts';

export default function PaymentSuccessPage() {
    const t = useTranslations('PaymentSuccess');

    return (
        <div className='container mx-auto max-w-2xl px-4 py-8 md:py-12'>
            <Card>
                <CardHeader className='items-center text-center'>
                    <div className='mb-2 flex size-14 items-center justify-center rounded-full bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'>
                        <CheckCircleIcon
                            className='size-8'
                            aria-hidden='true'
                        />
                    </div>
                    <CardTitle className='text-2xl'>{t('title')}</CardTitle>
                    <CardDescription>{t('description')}</CardDescription>
                </CardHeader>

                <CardContent>
                    <Alert variant='success'>
                        <CheckCircleIcon className='h-4 w-4' />
                        <AlertTitle>{t('title')}</AlertTitle>
                        <AlertDescription>{t('note')}</AlertDescription>
                    </Alert>
                </CardContent>

                <CardFooter className='flex-col gap-3 sm:flex-row sm:justify-center'>
                    <Button asChild className='w-full sm:w-auto'>
                        <Link href='/account'>{t('viewBookings')}</Link>
                    </Button>
                    <Button
                        variant='outline'
                        asChild
                        className='w-full sm:w-auto'
                    >
                        <Link href='/'>{t('backToHome')}</Link>
                    </Button>
                </CardFooter>
            </Card>
        </div>
    );
}
