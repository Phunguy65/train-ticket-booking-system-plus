import { unwrapJsendSuccessResponse } from './jsend.ts';

/**
 * hey-api client-fetch expects fetch signature: (request: Request) => Promise<Response>
 * This wrapper adapts the standard fetch to include JSend response unwrapping.
 */
type HeyApiFetchFn = (request: Request) => Promise<Response>;

export const createApiFetch =
    (fetchImpl: typeof fetch = globalThis.fetch): HeyApiFetchFn =>
    async (request: Request) => {
        const response = await fetchImpl(request);
        return unwrapJsendSuccessResponse(response);
    };
