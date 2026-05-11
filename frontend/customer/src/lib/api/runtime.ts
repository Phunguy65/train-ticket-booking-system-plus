import { getAccessToken } from '@/lib/auth/token-store.ts';
import { createApiFetch } from './fetch-runtime.ts';
import type { CreateClientConfig } from './generated/client.gen.ts';

export const createClientConfig: CreateClientConfig = (config) => ({
    ...config,
    baseUrl: '',
    auth: () => getAccessToken(),
    fetch: createApiFetch(config?.fetch ?? globalThis.fetch) as typeof fetch,
});
