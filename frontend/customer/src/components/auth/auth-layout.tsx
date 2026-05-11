import { TrainFrontIcon } from 'lucide-react';
import type { ReactNode } from 'react';
import { TrackPattern } from '@/components/illustrations/index.ts';

type AuthLayoutProps = {
    title: string;
    subtitle?: string;
    children: ReactNode;
    footer?: ReactNode;
    illustration: ReactNode;
};

export function AuthLayout({
    title,
    subtitle,
    children,
    footer,
    illustration,
}: AuthLayoutProps) {
    return (
        <main className='grid min-h-screen grid-cols-1 bg-gradient-to-br from-accent via-background to-background md:grid-cols-2'>
            <section className='relative hidden overflow-hidden bg-gradient-to-br from-accent via-primary/10 to-sky-100 text-primary md:flex md:items-center md:justify-center dark:to-sky-950/40'>
                <div className='absolute top-10 left-10 h-28 w-28 rounded-full bg-primary/10 blur-2xl' />
                <div className='absolute right-12 bottom-12 h-36 w-36 rounded-full bg-sky-400/20 blur-3xl' />
                <TrackPattern className='absolute right-0 bottom-10 left-0 h-20 w-full text-primary/70' />
                <div className='relative z-10 flex w-full max-w-md flex-col items-center gap-8 px-10'>
                    <div className='rounded-full bg-background/80 p-4 shadow-lg ring-1 ring-primary/10 backdrop-blur'>
                        <TrainFrontIcon className='h-10 w-10' />
                    </div>
                    {illustration}
                </div>
            </section>
            <section className='flex min-h-screen items-center justify-center px-4 py-10 sm:px-6 lg:px-10'>
                <div className='w-full max-w-sm animate-slide-up rounded-3xl border border-border/70 bg-card/95 p-6 shadow-xl shadow-primary/5 backdrop-blur sm:p-8 md:border-none md:bg-transparent md:p-0 md:shadow-none'>
                    <div className='mb-8 flex justify-center md:justify-start'>
                        <div className='flex items-center gap-2 rounded-full bg-accent px-4 py-2 text-primary'>
                            <TrainFrontIcon className='h-5 w-5' />
                            <span className='font-semibold tracking-tight'>
                                TTBS
                            </span>
                        </div>
                    </div>
                    <div className='mb-6 space-y-2 text-center md:text-left'>
                        <h1 className='text-3xl font-bold tracking-tight text-foreground'>
                            {title}
                        </h1>
                        {subtitle ? (
                            <p className='text-sm leading-6 text-muted-foreground'>
                                {subtitle}
                            </p>
                        ) : null}
                    </div>
                    <div>{children}</div>
                    {footer ? (
                        <div className='mt-6 text-center text-sm'>{footer}</div>
                    ) : null}
                </div>
            </section>
        </main>
    );
}
