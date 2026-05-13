'use client';

import { MailIcon, MapPinIcon, PhoneIcon, TrainFrontIcon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { Link } from '@/i18n/routing.ts';

export function Footer() {
    const t = useTranslations('Navigation');

    return (
        <footer className='border-t bg-muted/30'>
            <div className='container mx-auto px-4 py-12'>
                <div className='grid gap-8 sm:grid-cols-2 lg:grid-cols-4'>
                    <div>
                        <Link
                            href='/'
                            className='flex items-center gap-2 font-semibold'
                        >
                            <TrainFrontIcon className='h-5 w-5 text-primary' />
                            <span>TTBS</span>
                        </Link>
                        <p className='mt-3 text-sm text-muted-foreground'>
                            {t('footerDescription')}
                        </p>
                    </div>

                    <div>
                        <h4 className='mb-3 text-sm font-semibold'>
                            {t('footerServices')}
                        </h4>
                        <ul className='space-y-2 text-sm text-muted-foreground'>
                            <li>
                                <Link
                                    href='/'
                                    className='transition-colors hover:text-foreground'
                                >
                                    {t('footerSearchTrips')}
                                </Link>
                            </li>
                            <li>
                                <Link
                                    href='/account'
                                    className='transition-colors hover:text-foreground'
                                >
                                    {t('footerManageBookings')}
                                </Link>
                            </li>
                            <li>
                                <Link
                                    href='/account/profile'
                                    className='transition-colors hover:text-foreground'
                                >
                                    {t('footerProfile')}
                                </Link>
                            </li>
                        </ul>
                    </div>

                    <div>
                        <h4 className='mb-3 text-sm font-semibold'>
                            {t('footerSupport')}
                        </h4>
                        <ul className='space-y-2 text-sm text-muted-foreground'>
                            <li>{t('footerFaq')}</li>
                            <li>{t('footerTerms')}</li>
                            <li>{t('footerPrivacy')}</li>
                        </ul>
                    </div>

                    <div>
                        <h4 className='mb-3 text-sm font-semibold'>
                            {t('footerContact')}
                        </h4>
                        <ul className='space-y-2 text-sm text-muted-foreground'>
                            <li className='flex items-center gap-2'>
                                <PhoneIcon className='h-4 w-4' />
                                <span>1900 0000</span>
                            </li>
                            <li className='flex items-center gap-2'>
                                <MailIcon className='h-4 w-4' />
                                <span>support@ttbs.vn</span>
                            </li>
                            <li className='flex items-start gap-2'>
                                <MapPinIcon className='mt-0.5 h-4 w-4 shrink-0' />
                                <span>{t('footerAddress')}</span>
                            </li>
                        </ul>
                    </div>
                </div>

                <div className='mt-8 border-t pt-6 text-center text-sm text-muted-foreground'>
                    <p>
                        {t('footerCopyright', {
                            year: new Date().getFullYear(),
                        })}
                    </p>
                </div>
            </div>
        </footer>
    );
}
