import { createApiFetch } from './fetch-runtime.js';
import type { CreateClientConfig } from './generated/client.gen.js';

export const createClientConfig: CreateClientConfig = (config) => ({
    ...config,
    fetch: createApiFetch(config.fetch ?? globalThis.fetch),
});
