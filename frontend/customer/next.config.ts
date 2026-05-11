import type { NextConfig } from 'next';
import createNextIntlPlugin from 'next-intl/plugin';

const withNextIntl = createNextIntlPlugin('./src/i18n/request.ts');

const nextConfig: NextConfig = {
    output: 'standalone',
    productionBrowserSourceMaps: false,
    reactCompiler: true,
    turbopack: {
        resolveExtensions: [
            '.ts',
            '.tsx',
            '.js',
            '.jsx',
            '.json',
            '.cjs',
            '.mjs',
            '.css',
        ],
    },
    typescript: {
        ignoreBuildErrors: true,
    },
    async rewrites() {
        const backendUrl =
            process.env.BACKEND_URL || 'http://localhost:8080';
        return [
            {
                source: '/api/:path*',
                destination: `${backendUrl}/api/:path*`,
            },
        ];
    },
};

export default withNextIntl(nextConfig);
