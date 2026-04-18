import { useTranslations } from 'next-intl';

export function Footer() {
    const t = useTranslations('Navigation');

    return (
        <footer className='border-t bg-background py-6'>
            <div className='container px-4 text-center text-sm text-muted-foreground'>
                <p>
                    {t('footerCopyright', { year: new Date().getFullYear() })}
                </p>
            </div>
        </footer>
    );
}
