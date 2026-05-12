import type { Metadata } from 'next';
import { Be_Vietnam_Pro, JetBrains_Mono } from 'next/font/google';
import { getLocale } from 'next-intl/server';
import '@/app/globals.css';
import { Providers } from '@/app/providers.tsx';
import { Toaster } from '@/components/ui/sonner.tsx';

const beVietnamPro = Be_Vietnam_Pro({
    variable: '--font-geist-sans',
    subsets: ['latin', 'vietnamese'],
    weight: ['300', '400', '500', '600', '700'],
});

const jetbrainsMono = JetBrains_Mono({
    variable: '--font-geist-mono',
    subsets: ['latin', 'vietnamese'],
    weight: ['400', '500'],
});

export const metadata: Metadata = {
    title: 'TTBS — Hành trình Việt Nam',
    description: 'Đặt vé tàu trực tuyến — Hệ thống đặt vé tàu Việt Nam.',
    icons: {
        icon: '/favicon.png',
    },
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
            className={`${beVietnamPro.variable} ${jetbrainsMono.variable} h-full antialiased`}
        >
            <body className='min-h-full flex flex-col'>
                <Providers>{children}</Providers>
                <Toaster richColors position='top-right' />
            </body>
        </html>
    );
}
