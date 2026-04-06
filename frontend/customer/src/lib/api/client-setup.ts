import { client } from './generated/client.gen.js';
import { toApiClientError, unwrapJsendSuccessResponse } from './jsend.js';

let isConfigured = false;

export const setupGeneratedApiClient = () => {
    if (isConfigured) {
        return;
    }

    client.interceptors.response.use((response) =>
        unwrapJsendSuccessResponse(response),
    );
    client.interceptors.error.use((error, response) =>
        toApiClientError(error, response),
    );

    isConfigured = true;
};
