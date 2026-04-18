import { setRequestLocale } from 'next-intl/server';
import { BookingDetail } from '@/components/booking/index.ts';

type Props = {
    params: Promise<{ locale: string; bookingId: string }>;
};

export default async function BookingDetailPage({ params }: Props) {
    const { locale, bookingId } = await params;
    setRequestLocale(locale);

    return (
        <div className='container px-4 py-8'>
            <BookingDetail bookingId={bookingId} />
        </div>
    );
}
