import { unwrapJsendSuccessResponse } from './jsend.js';

export const createApiFetch =
    (fetchImpl: typeof fetch = globalThis.fetch): typeof fetch =>
    async (input, init) => {
        const response = await fetchImpl(input, init);
        return unwrapJsendSuccessResponse(response);
    };
