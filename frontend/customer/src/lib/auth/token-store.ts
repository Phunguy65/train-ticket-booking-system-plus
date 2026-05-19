let accessToken: string | undefined;

const refreshTokenStorageKey = 'ttbs.customer.refreshToken';

const canUseLocalStorage = () => typeof window !== 'undefined';

export const getAccessToken = () => accessToken;

export const getRefreshToken = () => {
    if (!canUseLocalStorage()) {
        return undefined;
    }

    return localStorage.getItem(refreshTokenStorageKey) ?? undefined;
};

export const setTokens = (access: string, refresh: string) => {
    accessToken = access;

    if (canUseLocalStorage()) {
        localStorage.setItem(refreshTokenStorageKey, refresh);
    }
};

export const clearTokens = () => {
    accessToken = undefined;

    if (canUseLocalStorage()) {
        localStorage.removeItem(refreshTokenStorageKey);
    }
};
