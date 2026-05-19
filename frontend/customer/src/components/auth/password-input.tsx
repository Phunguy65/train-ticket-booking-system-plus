'use client';

import { Eye, EyeOff } from 'lucide-react';
import { useTranslations } from 'next-intl';
import * as React from 'react';
import { Input } from '@/components/ui/input.tsx';
import { cn } from '@/lib/utils.ts';

/**
 * Password input with a show/hide toggle button.
 *
 * - Default state: password is hidden (type="password")
 * - Clicking the eye icon toggles between visible (type="text") and hidden
 * - The toggle button has an accessible `aria-label` translated via
 *   `Auth.password.show` / `Auth.password.hide`
 */
const PasswordInput = React.forwardRef<
    HTMLInputElement,
    Omit<React.ComponentProps<'input'>, 'type'>
>(({ className, ...props }, ref) => {
    const [visible, setVisible] = React.useState(false);
    const t = useTranslations('Auth.password');

    return (
        <div className='relative'>
            <Input
                ref={ref}
                type={visible ? 'text' : 'password'}
                className={cn('pr-10', className)}
                {...props}
            />
            <button
                type='button'
                onClick={() => setVisible((v) => !v)}
                className='absolute inset-y-0 right-0 flex items-center px-3 text-muted-foreground hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring rounded-r-lg disabled:opacity-50 disabled:cursor-not-allowed'
                aria-label={visible ? t('hide') : t('show')}
                disabled={props.disabled}
            >
                {visible ? (
                    <EyeOff className='h-4 w-4' aria-hidden='true' />
                ) : (
                    <Eye className='h-4 w-4' aria-hidden='true' />
                )}
            </button>
        </div>
    );
});
PasswordInput.displayName = 'PasswordInput';

export { PasswordInput };
