import type { Metadata } from 'next';
import { Geist, Geist_Mono } from 'next/font/google';
import './globals.js';
import { Providers } from './providers.js';

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

export default function RootLayout({
    children,
}: Readonly<{
    children: React.ReactNode;
}>) {
    return (
        <html
            lang='en'
            className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
        >
            <body className='min-h-full flex flex-col'>
                <Providers>{children}</Providers>
            </body>
        </html>
    );
}
