import { describe, expect, it } from 'vitest';
import { createApiFetch } from './fetch-runtime.ts';

describe('createApiFetch', () => {
    it('unwraps JSend success responses while preserving fetch semantics', async () => {
        const delegatedFetch = async () =>
            new Response(
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
        const apiFetch = createApiFetch(delegatedFetch as typeof fetch);

        const response = await apiFetch('https://example.com');

        expect(response).toBeInstanceOf(Response);
        expect(await response.json()).toEqual({ token: 'abc' });
    });

    it('leaves non-success responses untouched for client error handling', async () => {
        const delegatedFetch = async () =>
            new Response(
                JSON.stringify({
                    data: {
                        code: 'ACCESS_DENIED',
                        message: 'Denied',
                    },
                    status: 'fail',
                }),
                {
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    status: 403,
                },
            );
        const apiFetch = createApiFetch(delegatedFetch as typeof fetch);

        const response = await apiFetch('https://example.com');

        expect(response.status).toBe(403);
        expect(await response.json()).toEqual({
            data: {
                code: 'ACCESS_DENIED',
                message: 'Denied',
            },
            status: 'fail',
        });
    });
});
