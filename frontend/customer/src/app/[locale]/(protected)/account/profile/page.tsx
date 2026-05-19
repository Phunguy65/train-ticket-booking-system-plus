import { setRequestLocale } from 'next-intl/server';
import { ProfileForm } from '@/components/account/index.ts';

type Props = {
    params: Promise<{ locale: string }>;
};

export default async function ProfilePage({ params }: Props) {
    const { locale } = await params;
    setRequestLocale(locale);

    return (
        <div className='container mx-auto max-w-2xl px-4 py-8 md:py-12'>
            <ProfileForm />
        </div>
    );
}
