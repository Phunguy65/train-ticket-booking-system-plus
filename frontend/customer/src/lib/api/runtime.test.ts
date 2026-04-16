import { beforeEach, describe, expect, it, vi } from 'vitest';

describe('createClientConfig', () => {
    beforeEach(() => {
        vi.resetModules();
        vi.clearAllMocks();
    });

    it('wraps the provided fetch with the API runtime fetch', async () => {
        const delegatedFetch = vi.fn(
            async () =>
                new Response(
                    JSON.stringify({
                        data: { ok: true },
                        status: 'success',
                    }),
                    {
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        status: 200,
                    },
                ),
        );

        const { createClientConfig } = await import('./runtime.ts');
        const config = createClientConfig({ fetch: delegatedFetch } as never);

        expect(typeof config.fetch).toBe('function');
        expect(config.fetch).not.toBe(delegatedFetch);

        await config.fetch?.('https://example.com');

        expect(delegatedFetch).toHaveBeenCalledTimes(1);
    });

    it('falls back to global fetch when no custom fetch is provided', async () => {
        const originalFetch = globalThis.fetch;
        const delegatedFetch = vi.fn(
            async () =>
                new Response(
                    JSON.stringify({
                        data: { ok: true },
                        status: 'success',
                    }),
                    {
                        headers: {
                            'Content-Type': 'application/json',
                        },
                        status: 200,
                    },
                ),
        );
        globalThis.fetch = delegatedFetch as typeof fetch;

        try {
            const { createClientConfig } = await import('./runtime.ts');
            const config = createClientConfig({} as never);

            expect(typeof config.fetch).toBe('function');
            expect(config.fetch).not.toBe(delegatedFetch);

            await config.fetch?.('https://example.com');

            expect(delegatedFetch).toHaveBeenCalledTimes(1);
        } finally {
            globalThis.fetch = originalFetch;
        }
    });
});
