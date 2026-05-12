'use client';

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Loader2Icon, MenuIcon, TrainFrontIcon, UserIcon } from 'lucide-react';
import { useTranslations } from 'next-intl';
import { Suspense } from 'react';
import { Avatar, AvatarFallback } from '@/components/ui/avatar.tsx';
import { Button } from '@/components/ui/button.tsx';
import {
    DropdownMenu,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu.tsx';
import { Sheet, SheetContent, SheetTrigger } from '@/components/ui/sheet.tsx';
import { Skeleton } from '@/components/ui/skeleton.tsx';
import { Link, useRouter } from '@/i18n/routing.ts';
import { getAuthenticatedUserOptions, logout } from '@/lib/api/index.ts';
import { useAuth } from '@/lib/auth/auth-context.tsx';
import { clearTokens, getRefreshToken } from '@/lib/auth/token-store.ts';
import { LocaleSwitcher } from './locale-switcher.tsx';

export function Header() {
    return (
        <header className='sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60'>
            <div className='container flex h-14 items-center px-4'>
                <Link
                    href='/'
                    className='flex items-center gap-2 font-semibold'
                >
                    <TrainFrontIcon className='h-5 w-5' />
                    <span className='hidden sm:inline'>TTBS</span>
                </Link>

                <div className='flex flex-1 items-center justify-end gap-2'>
                    <LocaleSwitcher />
                    <Suspense fallback={<Skeleton className='h-8 w-20' />}>
                        <UserNavigation />
                    </Suspense>
                    <MobileNavigation />
                </div>
            </div>
        </header>
    );
}

function useLogout() {
    const router = useRouter();
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: async () => {
            const refreshToken = getRefreshToken();

            if (refreshToken) {
                await logout({ body: { refreshToken } }).catch(() => {});
            }

            clearTokens();
            queryClient.clear();
        },
        onSuccess: () => {
            router.push('/login');
        },
    });
}

function UserNavigation() {
    const t = useTranslations('Navigation');
    const logout = useLogout();
    const { isReady: isAuthReady } = useAuth();

    const {
        data: user,
        isLoading,
        isError,
    } = useQuery({
        ...getAuthenticatedUserOptions(),
        enabled: isAuthReady,
        retry: false,
        staleTime: 5 * 60 * 1000,
    });

    if (!isAuthReady || isLoading) {
        return <Skeleton className='h-8 w-20' />;
    }

    if (isError || !user) {
        return (
            <div className='hidden items-center gap-2 sm:flex'>
                <Button variant='ghost' size='sm' asChild>
                    <Link href='/login'>{t('login')}</Link>
                </Button>
                <Button size='sm' asChild>
                    <Link href='/register'>{t('register')}</Link>
                </Button>
            </div>
        );
    }

    const initials =
        user.fullName
            ?.split(' ')
            .map((n) => n[0])
            .join('')
            .toUpperCase()
            .slice(0, 2) || 'U';

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant='ghost' size='icon' className='hidden sm:flex'>
                    <Avatar className='h-8 w-8'>
                        <AvatarFallback className='text-xs'>
                            {initials}
                        </AvatarFallback>
                    </Avatar>
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align='end' className='w-56'>
                <div className='px-2 py-1.5'>
                    <p className='text-sm font-medium'>{user.fullName}</p>
                    <p className='text-xs text-muted-foreground'>
                        {user.email}
                    </p>
                </div>
                <DropdownMenuSeparator />
                <DropdownMenuItem asChild>
                    <Link href='/account'>{t('myBookings')}</Link>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                    <Link href='/account/profile'>{t('profile')}</Link>
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem
                    onClick={() => logout.mutate()}
                    disabled={logout.isPending}
                >
                    {logout.isPending ? (
                        <>
                            <Loader2Icon className='mr-2 h-4 w-4 motion-safe:animate-spin' />
                            {t('loggingOut')}
                        </>
                    ) : (
                        t('logout')
                    )}
                </DropdownMenuItem>
            </DropdownMenuContent>
        </DropdownMenu>
    );
}

function MobileNavigation() {
    const t = useTranslations('Navigation');
    const { isReady: isAuthReady } = useAuth();

    const { data: user, isLoading } = useQuery({
        ...getAuthenticatedUserOptions(),
        enabled: isAuthReady,
        retry: false,
        staleTime: 5 * 60 * 1000,
    });

    return (
        <Sheet>
            <SheetTrigger asChild>
                <Button variant='ghost' size='icon' className='sm:hidden'>
                    <MenuIcon className='h-5 w-5' />
                    <span className='sr-only'>{t('openMenu')}</span>
                </Button>
            </SheetTrigger>
            <SheetContent side='right' className='w-72'>
                <nav className='flex flex-col gap-4 pt-8'>
                    {!isAuthReady || isLoading ? (
                        <div className='space-y-2'>
                            <Skeleton className='h-10 w-full' />
                            <Skeleton className='h-10 w-full' />
                        </div>
                    ) : user ? (
                        <MobileAuthenticatedNav user={user} />
                    ) : (
                        <MobileUnauthenticatedNav />
                    )}
                </nav>
            </SheetContent>
        </Sheet>
    );
}

function MobileUnauthenticatedNav() {
    const t = useTranslations('Navigation');

    return (
        <>
            <Button asChild className='w-full'>
                <Link href='/login'>{t('login')}</Link>
            </Button>
            <Button variant='outline' asChild className='w-full'>
                <Link href='/register'>{t('register')}</Link>
            </Button>
        </>
    );
}

function MobileAuthenticatedNav({
    user,
}: {
    user: { fullName?: string; email?: string };
}) {
    const t = useTranslations('Navigation');
    const logout = useLogout();

    return (
        <>
            <div className='mb-4 flex items-center gap-3 border-b pb-4'>
                <Avatar>
                    <AvatarFallback>
                        <UserIcon className='h-4 w-4' />
                    </AvatarFallback>
                </Avatar>
                <div className='flex-1 overflow-hidden'>
                    <p className='truncate font-medium'>{user.fullName}</p>
                    <p className='truncate text-sm text-muted-foreground'>
                        {user.email}
                    </p>
                </div>
            </div>
            <Button variant='ghost' asChild className='w-full justify-start'>
                <Link href='/account'>{t('myBookings')}</Link>
            </Button>
            <Button variant='ghost' asChild className='w-full justify-start'>
                <Link href='/account/profile'>{t('profile')}</Link>
            </Button>
            <Button
                variant='ghost'
                className='w-full justify-start text-destructive hover:text-destructive'
                onClick={() => logout.mutate()}
                disabled={logout.isPending}
            >
                {logout.isPending ? (
                    <>
                        <Loader2Icon className='mr-2 h-4 w-4 motion-safe:animate-spin' />
                        {t('loggingOut')}
                    </>
                ) : (
                    t('logout')
                )}
            </Button>
        </>
    );
}
