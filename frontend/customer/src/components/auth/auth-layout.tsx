import Image from 'next/image';
import type { ReactNode } from 'react';

type AuthLayoutProps = {
    title: string;
    subtitle?: string;
    children: ReactNode;
    footer?: ReactNode;
    backgroundImage?: string;
};

export function AuthLayout({
    title,
    subtitle,
    children,
    footer,
    backgroundImage,
}: AuthLayoutProps) {
    return (
        <main className='grid min-h-screen grid-cols-1 bg-gradient-to-br from-background via-background to-muted md:grid-cols-2'>
            <section className='relative hidden overflow-hidden md:flex md:items-end md:justify-start'>
                {backgroundImage && (
                    <>
                        <Image
                            src={backgroundImage}
                            alt=''
                            fill
                            className='object-cover'
                            sizes='50vw'
                            priority
                        />
                        <div className='absolute inset-0 bg-gradient-to-t from-black/70 via-black/30 to-black/10' />
                    </>
                )}
                {!backgroundImage && (
                    <div className='absolute inset-0 bg-gradient-to-br from-primary/80 via-primary/60 to-accent/40' />
                )}
                <div className='relative z-10 flex flex-col gap-3 p-10'>
                    <h2 className='text-3xl font-bold tracking-tight text-white'>
                        VietRail
                    </h2>
                    <p className='text-lg font-medium text-white/90'>
                        Hành trình Việt Nam
                    </p>
                    <p className='max-w-xs text-sm text-white/70'>
                        Đặt vé tàu nhanh chóng, an toàn, tiện lợi.
                    </p>
                </div>
            </section>
            <section className='flex min-h-screen items-center justify-center px-4 py-10 sm:px-6 lg:px-10'>
                <div className='w-full max-w-sm animate-slide-up rounded-3xl border border-border/70 bg-card/95 p-6 shadow-xl shadow-primary/5 backdrop-blur sm:p-8 md:border-none md:bg-transparent md:p-0 md:shadow-none'>
                    <div className='mb-8 flex justify-center'>
                        <div className='relative h-[52px] w-[52px] overflow-hidden rounded-full'>
                            <Image
                                src='/images/logo.jpg'
                                alt='VietRail'
                                fill
                                className='object-cover'
                                sizes='52px'
                            />
                        </div>
                    </div>
                    <div className='mb-6'>
                        <h1 className='text-2xl font-bold tracking-tight text-foreground'>
                            {title}
                        </h1>
                        {subtitle && (
                            <p className='mt-2 text-sm text-muted-foreground'>
                                {subtitle}
                            </p>
                        )}
                    </div>
                    {children}
                    {footer && (
                        <div className='mt-6 text-center text-sm'>{footer}</div>
                    )}
                </div>
            </section>
        </main>
    );
}
