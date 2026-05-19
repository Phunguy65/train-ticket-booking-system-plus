import { describe, expect, it } from 'vitest';
import { ApiFailError, ApiTechnicalError } from './errors.ts';
import {
    createApiFetch,
    toApiClientError,
    unwrapJsendSuccessResponse,
} from './setup.ts';

describe('Api errors', () => {
    it('maps JSend fail payloads to ApiFailError', () => {
        const error = toApiClientError(
            {
                data: {
                    code: 'USER_EMAIL_ALREADY_EXISTS',
                    errors: [
                        {
                            code: 'REQUIRED',
                            field: 'email',
                            message: 'Email is required',
                        },
                    ],
                    message: 'Email already exists',
                },
                status: 'fail',
            },
            new Response(null, { status: 409 }),
        );

        expect(error).toBeInstanceOf(ApiFailError);
        expect((error as ApiFailError).code).toBe('USER_EMAIL_ALREADY_EXISTS');
        expect((error as ApiFailError).message).toBe('Email already exists');
        expect((error as ApiFailError).statusCode).toBe(409);
        expect((error as ApiFailError).violations).toEqual([
            {
                code: 'REQUIRED',
                field: 'email',
                message: 'Email is required',
            },
        ]);
    });

    it('maps JSend technical payloads to ApiTechnicalError', () => {
        const error = toApiClientError(
            {
                message: 'Unexpected technical failure.',
                status: 'error',
            },
            new Response(null, { status: 500 }),
        );

        expect(error).toBeInstanceOf(ApiTechnicalError);
        expect((error as ApiTechnicalError).message).toBe(
            'Unexpected technical failure.',
        );
        expect((error as ApiTechnicalError).statusCode).toBe(500);
    });

    it('maps JSend payloads without a response object', () => {
        const failError = toApiClientError({
            data: {
                code: 'ACCESS_DENIED',
                message: 'Denied',
            },
            status: 'fail',
        });
        const technicalError = toApiClientError({
            message: 'Boom',
            status: 'error',
        });

        expect(failError).toBeInstanceOf(ApiFailError);
        expect((failError as ApiFailError).statusCode).toBeUndefined();
        expect(technicalError).toBeInstanceOf(ApiTechnicalError);
        expect(
            (technicalError as ApiTechnicalError).statusCode,
        ).toBeUndefined();
    });

    it('leaves non-JSend errors untouched', () => {
        const error = new Error('Network failed');

        expect(toApiClientError(error)).toBe(error);
    });

    it('defaults missing fail message and violations', () => {
        const error = toApiClientError(
            {
                data: {
                    code: 'ACCESS_DENIED',
                },
                status: 'fail',
            },
            new Response(null, { status: 403 }),
        );

        expect(error).toBeInstanceOf(ApiFailError);
        expect((error as ApiFailError).message).toBe('Request failed');
        expect((error as ApiFailError).violations).toEqual([]);
    });

    it('defaults missing technical error message', () => {
        const error = toApiClientError(
            {
                status: 'error',
            },
            new Response(null, { status: 500 }),
        );

        expect(error).toBeInstanceOf(ApiTechnicalError);
        expect((error as ApiTechnicalError).message).toBe(
            'Unexpected technical failure',
        );
    });

    it('falls back to ApiTechnicalError for non-JSend server errors', () => {
        const error = toApiClientError(
            '{broken',
            new Response(null, { status: 502 }),
        );

        expect(error).toBeInstanceOf(ApiTechnicalError);
        expect((error as ApiTechnicalError).message).toBe('{broken');
        expect((error as ApiTechnicalError).statusCode).toBe(502);
    });

    it('leaves non-JSend primitives and objects untouched', () => {
        const objectError = { foo: 'bar' };

        expect(toApiClientError('error string')).toBe('error string');
        expect(toApiClientError(123)).toBe(123);
        expect(toApiClientError(null)).toBeNull();
        expect(toApiClientError(objectError)).toBe(objectError);
    });
});

describe('unwrapJsendSuccessResponse', () => {
    it('unwraps success payloads before validation', async () => {
        const response = new Response(
            JSON.stringify({
                data: { accessToken: 'token' },
                status: 'success',
            }),
            {
                headers: {
                    'Content-Type': 'application/json',
                },
                status: 200,
            },
        );

        const unwrapped = await unwrapJsendSuccessResponse(response);

        expect(await unwrapped.json()).toEqual({ accessToken: 'token' });
    });

    it('unwraps primitive and array success payloads', async () => {
        const arrayResponse = new Response(
            JSON.stringify({ data: ['A1', 'A2'], status: 'success' }),
            {
                headers: {
                    'Content-Type': 'application/json',
                    'X-Trace-Id': 'trace-id',
                },
                status: 200,
            },
        );

        const arrayUnwrapped = await unwrapJsendSuccessResponse(arrayResponse);

        expect(await arrayUnwrapped.json()).toEqual(['A1', 'A2']);
        expect(arrayUnwrapped.headers.get('X-Trace-Id')).toBe('trace-id');
        expect(arrayUnwrapped.headers.get('content-length')).toBeNull();
    });

    it('converts empty JSend success payloads into empty responses', async () => {
        const response = new Response(JSON.stringify({ status: 'success' }), {
            headers: {
                'Content-Type': 'application/json',
            },
            status: 200,
        });

        const unwrapped = await unwrapJsendSuccessResponse(response);

        expect(await unwrapped.text()).toBe('');
    });

    it('converts explicit null success payloads into empty responses', async () => {
        const response = new Response(
            JSON.stringify({ data: null, status: 'success' }),
            {
                headers: {
                    'Content-Type': 'application/json',
                },
                status: 200,
            },
        );

        const unwrapped = await unwrapJsendSuccessResponse(response);

        expect(await unwrapped.text()).toBe('');
    });

    it('does not unwrap non-2xx responses or 204 responses', async () => {
        const failPayload = {
            data: { code: 'ACCESS_DENIED', message: 'Denied' },
            status: 'fail',
        };
        const failResponse = new Response(JSON.stringify(failPayload), {
            headers: {
                'Content-Type': 'application/json',
            },
            status: 400,
        });
        const noContentResponse = new Response(null, {
            headers: {
                'Content-Type': 'application/json',
            },
            status: 204,
        });

        const untouchedFail = await unwrapJsendSuccessResponse(failResponse);
        const untouchedNoContent =
            await unwrapJsendSuccessResponse(noContentResponse);

        expect(await untouchedFail.json()).toEqual(failPayload);
        expect(untouchedNoContent.status).toBe(204);
        expect(await untouchedNoContent.text()).toBe('');
    });

    it('does not unwrap non-json, malformed json, or empty bodies', async () => {
        const textResponse = new Response('plain text', {
            headers: {
                'Content-Type': 'text/plain',
            },
            status: 200,
        });
        const malformedResponse = new Response('{broken', {
            headers: {
                'Content-Type': 'application/json',
            },
            status: 200,
        });
        const emptyBodyResponse = new Response('', {
            headers: {
                'Content-Type': 'application/json',
            },
            status: 200,
        });

        expect(
            await (await unwrapJsendSuccessResponse(textResponse)).text(),
        ).toBe('plain text');
        expect(
            await (await unwrapJsendSuccessResponse(malformedResponse)).text(),
        ).toBe('{broken');
        expect(
            await (await unwrapJsendSuccessResponse(emptyBodyResponse)).text(),
        ).toBe('');
    });

    it('keeps non-JSend JSON responses unchanged', async () => {
        const response = new Response(JSON.stringify({ ok: true }), {
            headers: {
                'Content-Type': 'application/json',
            },
            status: 200,
        });

        const unwrapped = await unwrapJsendSuccessResponse(response);

        expect(await unwrapped.json()).toEqual({ ok: true });
    });

    it('returns a readable unwrapped response body', async () => {
        const response = new Response(
            JSON.stringify({
                data: { id: '123' },
                status: 'success',
            }),
            {
                headers: {
                    'Content-Type': 'application/json',
                },
                status: 200,
            },
        );

        const unwrapped = await unwrapJsendSuccessResponse(response);

        expect(await unwrapped.json()).toEqual({ id: '123' });
    });
});

describe('createApiFetch', () => {
    it('returns a response object while unwrapping success payloads', async () => {
        const response = new Response(
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
        const apiFetch = createApiFetch(async () => response as Response);

        const result = await apiFetch('https://example.com');

        expect(result).toBeInstanceOf(Response);
        expect(await result.json()).toEqual({ token: 'abc' });
    });
});
