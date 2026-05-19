import { useTranslations } from 'next-intl';
import { setRequestLocale } from 'next-intl/server';
import { AuthLayout } from '@/components/auth/auth-layout.tsx';
import { LoginForm } from '@/components/auth/login-form.tsx';
import { Link } from '@/i18n/routing.ts';

type Props = {
    params: Promise<{ locale: string }>;
};

export default async function LoginPage({ params }: Props) {
    const { locale } = await params;
    setRequestLocale(locale);

    return <LoginPageContent />;
}

function LoginPageContent() {
    const t = useTranslations('Auth.login');

    return (
        <AuthLayout
            title={t('title')}
            subtitle={t('subtitle')}
            backgroundImage='/images/auth-login.webp'
            footer={
                <span className='text-muted-foreground'>
                    {t('noAccount')}{' '}
                    <Link
                        href='/register'
                        className='font-medium text-foreground underline-offset-4 hover:underline'
                    >
                        {t('registerLink')}
                    </Link>
                </span>
            }
        >
            <LoginForm />
        </AuthLayout>
    );
}
