import Link from "next/link";

export default function ExamplesPage() {
    return (
        <div className="relative px-6 py-10 sm:px-12 lg:px-16">
            <div className="mx-auto max-w-6xl">
                {/* Header */}
                <div className="mb-12 text-center">
                    <Link href="/" className="inline-block text-sm text-accent-teal hover:underline mb-4">
                        ← Back to Home
                    </Link>
                    <h1 className="font-display text-4xl sm:text-5xl text-white mt-4">Example Reviews</h1>
                    <p className="mt-4 text-lg text-white/70 max-w-2xl mx-auto">
                        See how CondensedChess works on real games.
                    </p>
                </div>

                {/* Sample Reviews */}
                <div className="space-y-6">
                    {/* Sample 1 */}
                    <div className="glass-card rounded-3xl p-8 sm:p-10">
                        <div className="flex flex-col md:flex-row md:items-center md:justify-between mb-6">
                            <div>
                                <h2 className="font-display text-2xl text-white">Carlsen vs Sevian</h2>
                                <p className="mt-1 text-sm text-white/60">Reversed Sicilian • Blitz Game</p>
                            </div>
                            <Link
                                href="/review/sample"
                                className="mt-4 md:mt-0 inline-flex items-center justify-center rounded-full bg-accent-teal px-5 py-2.5 text-sm font-semibold text-ink shadow-glow transition hover:scale-[1.02]"
                            >
                                View Full Review
                            </Link>
                        </div>
                        <div className="grid gap-4 md:grid-cols-2">
                            <div className="rounded-xl border border-white/10 bg-white/5 p-4">
                                <p className="text-xs text-white/60">Critical Moments</p>
                                <p className="mt-1 text-2xl font-semibold text-white">7</p>
                            </div>
                            <div className="rounded-xl border border-white/10 bg-white/5 p-4">
                                <p className="text-xs text-white/60">Avg Study Score</p>
                                <p className="mt-1 text-2xl font-semibold text-accent-blue">8.4</p>
                            </div>
                        </div>
                        <div className="mt-4">
                            <p className="text-sm text-white/70">
                                <strong className="text-white">Key Insight:</strong> Move 15.Nd5! demonstrates a tactical breakthrough with Study Score 9.1.
                                Perfect example of initiative-gaining moves in Sicilian structures.
                            </p>
                        </div>
                    </div>

                    {/* Placeholder for more examples */}
                    <div className="glass-card rounded-3xl p-8 text-center">
                        <p className="text-white/60">More examples coming soon...</p>
                        <p className="mt-2 text-sm text-white/50">
                            Upload your own games to see personalized reviews!
                        </p>
                        <Link
                            href="/app/dashboard"
                            className="mt-6 inline-flex items-center justify-center rounded-full border border-white/20 px-6 py-3 text-sm font-semibold text-white transition hover:border-white/40"
                        >
                            Upload Your Game
                        </Link>
                    </div>
                </div>
            </div>
        </div>
    );
}
