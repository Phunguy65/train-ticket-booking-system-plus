import { createApiFetch } from './fetch-runtime.ts';
import type { CreateClientConfig } from './generated/client.gen.ts';

export const createClientConfig: CreateClientConfig = (config) => ({
    ...config,
    baseUrl: '',
    fetch: createApiFetch(config?.fetch ?? globalThis.fetch),
});
