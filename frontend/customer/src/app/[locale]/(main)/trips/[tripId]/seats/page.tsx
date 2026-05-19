import { setRequestLocale } from 'next-intl/server';
import { SeatSelection } from '@/components/seats/index.ts';

type Props = {
    params: Promise<{ locale: string; tripId: string }>;
};

export default async function SeatsPage({ params }: Props) {
    const { locale, tripId } = await params;
    setRequestLocale(locale);

    return (
        <div className='container mx-auto px-4 py-8 md:py-12'>
            <SeatSelection tripId={tripId} />
        </div>
    );
}
