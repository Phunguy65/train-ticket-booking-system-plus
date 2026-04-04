import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
    input: '../../shared/api-contracts/openapi.yaml',
    output: {
        clean: true,
        path: './src/lib/api/generated',
    },
    plugins: [
        '@hey-api/typescript',
        {
            name: '@hey-api/client-fetch',
            runtimeConfigPath: '../runtime.ts',
        },
        {
            name: '@hey-api/sdk',
            auth: true,
            operations: {
                strategy: 'flat',
            },
            validator: true,
        },
        {
            name: 'zod',
            definitions: true,
            metadata: true,
            requests: true,
            responses: true,
        },
        {
            name: '@tanstack/react-query',
            queryKeys: {
                tags: true,
            },
            queryOptions: true,
            infiniteQueryOptions: true,
            mutationOptions: true,
        },
    ],
});
