"use client";

import { useAuth } from '@/hooks/useAuth';
import { useState } from 'react';
import Link from 'next/link';

export default function RegisterPage() {
    const { register } = useAuth();
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            await register(email, password);
            // After register, redirect to login or auto login. 
            // For now redirect to login with query param?
            // Or simple alert.
            alert("Registration successful! Please login.");
            window.location.href = '/login';
        } catch (err: any) {
            setError(err.message || 'Registration failed');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex min-h-screen flex-col items-center justify-center bg-space-black p-4 text-white">
            <div className="glass-card w-full max-w-md space-y-8 rounded-xl p-8 shadow-2xl">
                <div className="text-center">
                    <h2 className="font-display text-3xl font-bold tracking-tight text-white">
                        Create an account
                    </h2>
                    <p className="mt-2 text-sm text-gray-400">
                        Join Chesstory today
                    </p>
                </div>

                <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
                    <div className="space-y-4">
                        <div>
                            <label htmlFor="email" className="block text-sm font-medium text-gray-300">
                                Email address
                            </label>
                            <input
                                id="email"
                                name="email"
                                type="email"
                                autoComplete="email"
                                required
                                className="mt-1 block w-full rounded-md border border-white/10 bg-white/5 px-3 py-2 text-white placeholder-gray-500 focus:border-accent-teal focus:outline-none focus:ring-1 focus:ring-accent-teal sm:text-sm"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                            />
                        </div>
                        <div>
                            <label htmlFor="password" className="block text-sm font-medium text-gray-300">
                                Password
                            </label>
                            <input
                                id="password"
                                name="password"
                                type="password"
                                autoComplete="new-password"
                                required
                                className="mt-1 block w-full rounded-md border border-white/10 bg-white/5 px-3 py-2 text-white placeholder-gray-500 focus:border-accent-teal focus:outline-none focus:ring-1 focus:ring-accent-teal sm:text-sm"
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                            />
                        </div>
                    </div>

                    {error && (
                        <div className="text-sm text-red-500">
                            {error}
                        </div>
                    )}

                    <div>
                        <button
                            type="submit"
                            disabled={loading}
                            className="group relative flex w-full justify-center rounded-full bg-accent-teal px-4 py-2 text-sm font-bold text-ink transition hover:scale-[1.02] focus:outline-none focus:ring-2 focus:ring-accent-teal focus:ring-offset-2 disabled:opacity-50"
                        >
                            {loading ? 'Sign up' : 'Sign up'}
                        </button>
                    </div>
                </form>

                <div className="text-center text-sm">
                    <p className="text-gray-400">
                        Already have an account?{' '}
                        <Link href="/login" className="font-medium text-accent-teal hover:text-accent-blue">
                            Sign in
                        </Link>
                    </p>
                </div>
            </div>
        </div>
    );
}
