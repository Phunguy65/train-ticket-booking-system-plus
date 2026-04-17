import type { Metadata } from 'next';
import { Geist, Geist_Mono } from 'next/font/google';
import { getLocale } from 'next-intl/server';
import '@/app/globals.css';
import { Providers } from '@/app/providers.tsx';
import { Toaster } from '@/components/ui/sonner.tsx';

const geistSans = Geist({
    variable: '--font-geist-sans',
    subsets: ['latin'],
});

const geistMono = Geist_Mono({
    variable: '--font-geist-mono',
    subsets: ['latin'],
});

export const metadata: Metadata = {
    title: 'TTBS Customer',
    description: 'Customer app for train ticket booking.',
};

export default async function RootLayout({
    children,
}: Readonly<{
    children: React.ReactNode;
}>) {
    const locale = await getLocale();

    return (
        <html
            lang={locale}
            className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
        >
            <body className='min-h-full flex flex-col'>
                <Providers>{children}</Providers>
                <Toaster richColors position='top-right' />
            </body>
        </html>
    );
}
