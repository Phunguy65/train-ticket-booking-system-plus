'use client';

import { useEffect } from 'react';
import { refreshToken } from '@/lib/api/generated/index.ts';
import {
    clearTokens,
    getAccessToken,
    getRefreshToken,
    setTokens,
} from './token-store.ts';

export function useAuthInit() {
    useEffect(() => {
        const storedRefreshToken = getRefreshToken();

        if (!storedRefreshToken || getAccessToken()) {
            return;
        }

        let isActive = true;

        const restoreSession = async () => {
            try {
                const result = await refreshToken({
                    body: { refreshToken: storedRefreshToken },
                });
                const data = result.data;

                if (!isActive) {
                    return;
                }

                if (data?.accessToken && data.refreshToken) {
                    setTokens(data.accessToken, data.refreshToken);
                    return;
                }

                clearTokens();
            } catch {
                if (isActive) {
                    clearTokens();
                }
            }
        };

        void restoreSession();

        return () => {
            isActive = false;
        };
    }, []);
}
