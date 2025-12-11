"use client";

import Link from "next/link";
import dynamic from "next/dynamic";
import { useEffect, useState } from "react";
import { apiFetch } from "../../../lib/api";
import { useAuth } from "../../../hooks/useAuth";

const UploadCard = dynamic(() => import("../../../components/UploadCard"), { ssr: false });

type GameSummary = {
    id: string;
    white: string;
    black: string;
    result: string;
    date: string;
    eco: string;
    createdAt: string;
};

export default function DashboardPage() {
    const { user, loading: authLoading } = useAuth();
    const [games, setGames] = useState<GameSummary[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const isAuthenticated = !!user;

    useEffect(() => {
        if (!isAuthenticated) return;

        const fetchGames = async () => {
            try {
                const data = await apiFetch<any[]>("/api/game-review/list?limit=10");
                // Map backend fields to UI fields
                const mapped = data.map(g => ({
                    id: g.id,
                    white: g.white,
                    black: g.black,
                    result: g.result,
                    date: g.createdAt.split("T")[0], // Simple date display
                    eco: g.eco,
                    createdAt: g.createdAt
                }));
                setGames(mapped);
            } catch (err) {
                console.error("Failed to fetch games", err);
                setError("Failed to load your games.");
            } finally {
                setLoading(false);
            }
        };

        fetchGames();
    }, [isAuthenticated]);

    if (authLoading) return <div className="p-12 text-center text-white/50">Loading session...</div>;

    return (
        <div className="relative px-6 py-10 sm:px-12 lg:px-16">
            <div className="mx-auto max-w-7xl">
                {/* Header */}
                <div className="mb-8">
                    <h1 className="font-display text-3xl sm:text-4xl text-white">Dashboard</h1>
                    <p className="mt-2 text-sm text-white/70">
                        {user ? `Welcome back, ${user.email.split('@')[0]}!` : "Your personalized chess study hub"}
                    </p>
                </div>

                {/* Stats Cards - Mock for now */}
                {/* <div className="grid gap-4 md:grid-cols-2 mb-8"> ... </div> */}

                <div className="grid gap-8 lg:grid-cols-3">
                    {/* Main: Recent Games List */}
                    <div className="lg:col-span-2 space-y-6">
                        <div className="flex items-center justify-between">
                            <h2 className="font-display text-2xl text-white">Analysis History</h2>
                        </div>

                        {loading ? (
                            <div className="text-white/50">Loading games...</div>
                        ) : error ? (
                            <div className="text-rose-400">{error}</div>
                        ) : games.length === 0 ? (
                            <div className="glass-card rounded-2xl p-8 text-center">
                                <p className="text-white/70">No games analyzed yet.</p>
                                <p className="text-white/50 text-sm mt-2">Upload a PGN on the right to get started!</p>
                            </div>
                        ) : (
                            <div className="space-y-3">
                                {games.map((game) => (
                                    <Link
                                        key={game.id}
                                        href={`/review/${game.id}`}
                                        className="glass-card block rounded-xl p-4 transition hover:border-white/25 hover:bg-white/5"
                                    >
                                        <div className="flex items-center justify-between mb-2">
                                            <span className="text-sm font-semibold text-white">
                                                {game.white} vs {game.black}
                                            </span>
                                            <span className="text-xs font-semibold text-white/80">{game.result}</span>
                                        </div>
                                        <p className="text-xs text-white/60">{game.eco} • {game.date}</p>
                                    </Link>
                                ))}
                            </div>
                        )}
                    </div>

                    {/* Sidebar: Upload */}
                    <div className="space-y-6">
                        <div>
                            <h2 className="font-display text-xl text-white mb-4">Analyze New Game</h2>
                            <UploadCard />
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
