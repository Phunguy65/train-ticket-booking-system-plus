import { useTranslations } from 'next-intl';
import { setRequestLocale } from 'next-intl/server';
import { AuthLayout } from '@/components/auth/auth-layout.tsx';
import { RegisterForm } from '@/components/auth/register-form.tsx';
import { Link } from '@/i18n/routing.ts';

type Props = {
    params: Promise<{ locale: string }>;
};

export default async function RegisterPage({ params }: Props) {
    const { locale } = await params;
    setRequestLocale(locale);

    return <RegisterPageContent />;
}

function RegisterPageContent() {
    const t = useTranslations('Auth.register');

    return (
        <AuthLayout
            title={t('title')}
            subtitle={t('subtitle')}
            backgroundImage='/images/auth-register.webp'
            footer={
                <span className='text-muted-foreground'>
                    {t('hasAccount')}{' '}
                    <Link
                        href='/login'
                        className='font-medium text-foreground underline-offset-4 hover:underline'
                    >
                        {t('loginLink')}
                    </Link>
                </span>
            }
        >
            <RegisterForm />
        </AuthLayout>
    );
}
