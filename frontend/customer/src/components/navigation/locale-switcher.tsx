'use client';

import { GlobeIcon } from 'lucide-react';
import { useLocale } from 'next-intl';
import { Button } from '@/components/ui/button.tsx';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu.tsx';
import { routing, usePathname, useRouter } from '@/i18n/routing.ts';

const localeNames: Record<string, string> = {
    vi: 'Tiếng Việt',
    en: 'English',
};

export function LocaleSwitcher() {
    const locale = useLocale();
    const router = useRouter();
    const pathname = usePathname();

    const handleLocaleChange = (newLocale: string) => {
        router.replace(pathname, { locale: newLocale });
    };

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant='ghost' size='sm' className='gap-1.5'>
                    <GlobeIcon className='h-4 w-4' />
                    <span className='hidden sm:inline'>
                        {localeNames[locale]}
                    </span>
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align='end'>
                {routing.locales.map((loc) => (
                    <DropdownMenuItem
                        key={loc}
                        onClick={() => handleLocaleChange(loc)}
                        className={locale === loc ? 'bg-accent' : ''}
                    >
                        {localeNames[loc]}
                    </DropdownMenuItem>
                ))}
            </DropdownMenuContent>
        </DropdownMenu>
    );
}
