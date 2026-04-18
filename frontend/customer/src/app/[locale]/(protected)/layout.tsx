'use client';

import { useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
import { Footer, Header } from '@/components/navigation/index.ts';
import { Skeleton } from '@/components/ui/skeleton.tsx';
import { usePathname, useRouter } from '@/i18n/routing.ts';
import { getAuthenticatedUserOptions } from '@/lib/api/index.ts';

type Props = {
    children: React.ReactNode;
};

export default function ProtectedLayout({ children }: Props) {
    const router = useRouter();
    const pathname = usePathname();

    const {
        data: user,
        isLoading,
        isError,
    } = useQuery({
        ...getAuthenticatedUserOptions(),
        retry: false,
    });

    useEffect(() => {
        if (!isLoading && (isError || !user)) {
            router.replace(`/login?redirect=${encodeURIComponent(pathname)}`);
        }
    }, [isLoading, isError, user, router, pathname]);

    if (isLoading) {
        return (
            <div className='flex min-h-screen flex-col'>
                <Header />
                <main className='container flex-1 px-4 py-8'>
                    <div className='space-y-4'>
                        <Skeleton className='h-8 w-48' />
                        <Skeleton className='h-64 w-full' />
                    </div>
                </main>
                <Footer />
            </div>
        );
    }

    if (isError || !user) {
        return null;
    }

    return (
        <div className='flex min-h-screen flex-col'>
            <Header />
            <main className='flex-1'>{children}</main>
            <Footer />
        </div>
    );
}
