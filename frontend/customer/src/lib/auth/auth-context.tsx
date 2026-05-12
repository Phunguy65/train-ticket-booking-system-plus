'use client';

import {
    createContext,
    type ReactNode,
    useContext,
    useMemo,
    useState,
} from 'react';
import { useAuthInit } from './use-auth-init.ts';

type AuthContextValue = {
    isReady: boolean;
};

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

type AuthProviderProps = {
    children: ReactNode;
};

export function AuthProvider({ children }: AuthProviderProps) {
    const [isReady, setIsReady] = useState(false);

    useAuthInit(setIsReady);

    const value = useMemo(() => ({ isReady }), [isReady]);

    return (
        <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);

    if (!context) {
        throw new Error('useAuth must be used within AuthProvider');
    }

    return context;
}
