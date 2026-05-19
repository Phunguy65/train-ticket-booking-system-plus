import { describe, expect, it } from 'vitest';
import { setupGeneratedApiClient } from './client-setup.ts';
import { ApiFailError } from './errors.ts';
import { client } from './generated/client.gen.ts';

describe('setupGeneratedApiClient', () => {
    it('registers generated client interceptors only once', () => {
        setupGeneratedApiClient();

        const responseCount =
            client.interceptors.response.fns.filter(Boolean).length;
        const errorCount = client.interceptors.error.fns.filter(Boolean).length;

        setupGeneratedApiClient();
        setupGeneratedApiClient();

        expect(client.interceptors.response.fns.filter(Boolean)).toHaveLength(
            responseCount,
        );
        expect(client.interceptors.error.fns.filter(Boolean)).toHaveLength(
            errorCount,
        );
    });

    it('registers handlers that unwrap success and map JSend failures', async () => {
        setupGeneratedApiClient();

        const responseHandler = client.interceptors.response.fns.at(-1);
        const errorHandler = client.interceptors.error.fns.at(-1);
        const wrappedSuccess = new Response(
            JSON.stringify({
                data: { token: 'abc' },
                status: 'success',
            }),
            {
                headers: {
                    'Content-Type': 'application/json',
                },
                status: 200,
            },
        );

        const unwrapped = await responseHandler?.(
            wrappedSuccess,
            new Request('https://example.com'),
            {} as never,
        );
        const mappedError = await errorHandler?.(
            {
                data: {
                    code: 'ACCESS_DENIED',
                    errors: [],
                    message: 'Denied',
                },
                status: 'fail',
            },
            new Response(null, { status: 403 }),
            new Request('https://example.com'),
            {} as never,
        );

        expect(await unwrapped?.json()).toEqual({ token: 'abc' });
        expect(mappedError).toBeInstanceOf(ApiFailError);
        expect((mappedError as ApiFailError).code).toBe('ACCESS_DENIED');
    });
});
