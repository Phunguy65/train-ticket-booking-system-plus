import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { NextIntlClientProvider } from 'next-intl';
import type * as React from 'react';
import viMessages from '@/messages/vi.json' with { type: 'json' };

/**
 * Wraps children with the providers required by auth form components:
 * - NextIntlClientProvider with Vietnamese messages (deterministic for tests)
 * - QueryClientProvider with retries disabled (so failed mutations don't retry)
 */
export function TestProviders({ children }: { children: React.ReactNode }) {
    const queryClient = new QueryClient({
        defaultOptions: {
            queries: { retry: false },
            mutations: { retry: false },
        },
    });

    return (
        <QueryClientProvider client={queryClient}>
            <NextIntlClientProvider locale='vi' messages={viMessages}>
                {children}
            </NextIntlClientProvider>
        </QueryClientProvider>
    );
}
