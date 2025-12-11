"use client";

import { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { apiFetch } from '@/lib/api';

export interface User {
    id: string;
    email: string;
    tier: string;
    createdAt?: string;
}

interface AuthContextType {
    user: User | null;
    loading: boolean;
    login: (email: string, password: string) => Promise<void>;
    register: (email: string, password: string) => Promise<void>;
    loginWithGoogle: (idToken: string) => Promise<void>;
    logout: () => void;
    checkAuth: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null);
    const [loading, setLoading] = useState(true);

    const checkAuth = async () => {
        const token = localStorage.getItem('token');
        if (!token) {
            setLoading(false);
            return;
        }

        try {
            const userData = await apiFetch<User>('/auth/me');
            setUser(userData);
        } catch (error) {
            console.error("Auth check failed:", error);
            localStorage.removeItem('token');
            setUser(null);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        checkAuth();
    }, []);

    const login = async (email: string, password: string) => {
        try {
            const res = await apiFetch<{ token: string; user: User }>('/auth/login', {
                method: 'POST',
                body: JSON.stringify({ email, password }),
            });
            localStorage.setItem('token', res.token);
            setUser(res.user);
        } catch (e) {
            throw e;
        }
    };

    const register = async (email: string, password: string) => {
        try {
            await apiFetch<{ id: string }>('/auth/register', {
                method: 'POST',
                body: JSON.stringify({ email, password }),
            });
        } catch (e) {
            throw e;
        }
    };

    const loginWithGoogle = async (idToken: string) => {
        try {
            const res = await apiFetch<{ token: string; user: User }>('/auth/login/google', {
                method: 'POST',
                body: JSON.stringify({ idToken }),
            });
            localStorage.setItem('token', res.token);
            setUser(res.user);
        } catch (e) {
            console.error("Google Login failed", e);
            throw e;
        }
    };

    const logout = () => {
        localStorage.removeItem('token');
        setUser(null);
        window.location.href = '/'; // Simple redirect to home
    };

    return (
        <AuthContext.Provider value={{ user, loading, login, register, loginWithGoogle, logout, checkAuth }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
}
