'use client';

import { useTranslations } from 'next-intl';
import { QRCodeSVG } from 'qrcode.react';

type TicketQRCodeProps = {
    bookingId: string;
    className?: string;
};

export function TicketQRCode({ bookingId, className }: TicketQRCodeProps) {
    const t = useTranslations('Ticket');

    const qrValue = `VIETRAIL-TICKET:${bookingId}`;

    return (
        <div className={className}>
            <QRCodeSVG
                value={qrValue}
                size={120}
                level='M'
                includeMargin={false}
                title={t('qrCodeTitle')}
            />
            <p className='text-xs text-center text-muted-foreground mt-2 print:text-black'>
                {t('scanToVerify')}
            </p>
        </div>
    );
}
