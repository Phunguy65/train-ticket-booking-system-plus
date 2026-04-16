import { ApiFailError, ApiTechnicalError } from './errors.ts';
import type { ErrorCode, Violation } from './generated/index.ts';

type JsendSuccess<T = unknown> = {
    data?: T;
    status?: 'success';
};

type JsendFail = {
    data?: {
        code?: ErrorCode;
        errors?: Array<Violation>;
        message?: string;
    };
    status?: 'fail';
};

type JsendError = {
    message?: string;
    status?: 'error';
};

const isRecord = (value: unknown): value is Record<string, unknown> =>
    typeof value === 'object' && value !== null;

const isJsendSuccess = (value: unknown): value is JsendSuccess =>
    isRecord(value) && value.status === 'success';

const isJsendFail = (value: unknown): value is JsendFail =>
    isRecord(value) && value.status === 'fail';

const isJsendError = (value: unknown): value is JsendError =>
    isRecord(value) && value.status === 'error';

const cloneHeaders = (response: Response) => {
    const headers = new Headers(response.headers);
    headers.delete('content-length');
    return headers;
};

export const unwrapJsendSuccessResponse = async (
    response: Response,
): Promise<Response> => {
    if (!response.ok || response.status === 204) {
        return response;
    }

    const contentType = response.headers.get('content-type');
    if (!contentType?.includes('json')) {
        return response;
    }

    const body = await response.clone().text();
    if (!body) {
        return response;
    }

    let parsed: unknown;
    try {
        parsed = JSON.parse(body);
    } catch {
        return response;
    }

    if (!isJsendSuccess(parsed)) {
        return response;
    }

    const data = parsed.data;
    if (data === undefined || data === null) {
        return new Response(null, {
            headers: cloneHeaders(response),
            status: response.status,
            statusText: response.statusText,
        });
    }

    return new Response(JSON.stringify(data), {
        headers: cloneHeaders(response),
        status: response.status,
        statusText: response.statusText,
    });
};

export const toApiClientError = (
    error: unknown,
    response?: Response,
): unknown => {
    if (isJsendFail(error)) {
        return new ApiFailError({
            code: error.data?.code,
            message: error.data?.message,
            statusCode: response?.status,
            violations: error.data?.errors ?? [],
        });
    }

    if (isJsendError(error)) {
        return new ApiTechnicalError({
            message: error.message,
            statusCode: response?.status,
        });
    }

    if (response && response.status >= 400) {
        return new ApiTechnicalError({
            message:
                typeof error === 'string' && error.length > 0
                    ? error
                    : undefined,
            statusCode: response.status,
        });
    }

    return error;
};
