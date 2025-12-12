import FeatureHighlights from "../../components/landing/FeatureHighlights";
import Link from "next/link";

export default function ProductPage() {
    return (
        <div className="relative px-6 py-10 sm:px-12 lg:px-16">
            <div className="mx-auto max-w-6xl">
                {/* Header */}
                <div className="mb-12 text-center">
                    <Link href="/" className="inline-block text-sm text-accent-teal hover:underline mb-4">
                        ← Back to Home
                    </Link>
                    <h1 className="font-display text-4xl sm:text-5xl text-white mt-4">Product</h1>
                    <p className="mt-4 text-lg text-white/70 max-w-2xl mx-auto">
                        Chesstory goes beyond simple engine analysis to deliver chess reviews organized like textbooks.
                    </p>
                </div>

                {/* Feature Highlights */}
                <FeatureHighlights />

                {/* Deep Dive Sections */}
                <div className="mt-16 space-y-12">
                    {/* Practicality Score */}
                    <section className="glass-card rounded-3xl p-8 sm:p-10">
                        <h2 className="font-display text-3xl text-white">Practicality Score: Tolerance for Mistakes</h2>
                        <p className="mt-4 text-white/70 leading-relaxed">
                            Engines excel at finding the best move, but they don&apos;t tell you &quot;how dangerous is it to deviate?&quot;
                            Practicality Score measures this using two concepts:
                        </p>
                        <div className="mt-6 grid gap-4 md:grid-cols-2">
                            <div className="rounded-xl border border-white/10 bg-white/5 p-5">
                                <h3 className="font-semibold text-white">Robustness</h3>
                                <p className="mt-2 text-sm text-white/70">
                                    When you miss the best move, how many decent alternatives exist? How safe are they?
                                    High robustness means mistakes aren&apos;t catastrophic.
                                </p>
                            </div>
                            <div className="rounded-xl border border-white/10 bg-white/5 p-5">
                                <h3 className="font-semibold text-white">Horizon</h3>
                                <p className="mt-2 text-sm text-white/70">
                                    How stable is the gap between best and second-best moves?
                                    Wide horizon means a mistake won&apos;t immediately lead to defeat.
                                </p>
                            </div>
                        </div>
                    </section>

                    {/* Study Score */}
                    <section className="glass-card rounded-3xl p-8 sm:p-10">
                        <h2 className="font-display text-3xl text-white">Study Score: Automated Learning Prioritization</h2>
                        <p className="mt-4 text-white/70 leading-relaxed">
                            Not all mistakes are equal. Study Score automatically ranks &quot;which moments to review&quot; by synthesizing:
                        </p>
                        <ul className="mt-6 space-y-3 text-sm text-white/80">
                            <li className="flex gap-3">
                                <span className="text-accent-teal">•</span>
                                <span><strong>Win% swings:</strong> Larger evaluation changes score higher</span>
                            </li>
                            <li className="flex gap-3">
                                <span className="text-accent-teal">•</span>
                                <span><strong>Plan transitions:</strong> Moments where strategic themes shift</span>
                            </li>
                            <li className="flex gap-3">
                                <span className="text-accent-teal">•</span>
                                <span><strong>Phase transitions:</strong> Opening → Middlegame → Endgame boundaries</span>
                            </li>
                            <li className="flex gap-3">
                                <span className="text-accent-teal">•</span>
                                <span><strong>Complexity:</strong> Depth and variety of variations</span>
                            </li>
                        </ul>
                    </section>

                    {/* Semantic Tags */}
                    <section className="glass-card rounded-3xl p-8 sm:p-10">
                        <h2 className="font-display text-3xl text-white">Semantic Tags: Position Characteristic Labels</h2>
                        <p className="mt-4 text-white/70 leading-relaxed">
                            50+ semantic tags clarify the strategic characteristics of each position.
                        </p>
                        <div className="mt-6 flex flex-wrap gap-2">
                            {[
                                "King Safety Crisis",
                                "Fortress Defense",
                                "Conversion Difficulty",
                                "Tactical Blow",
                                "Positional Squeeze",
                                "Initiative Gain",
                                "Endgame Precision",
                                "Pawn Structure Damage",
                                "Piece Sacrifice",
                                "Prophylaxis",
                            ].map((tag) => (
                                <span key={tag} className="rounded-full bg-white/10 px-3 py-1.5 text-sm text-white/80">
                                    {tag}
                                </span>
                            ))}
                            <span className="rounded-full bg-accent-teal/20 px-3 py-1.5 text-sm text-accent-teal font-semibold">
                                +40 more
                            </span>
                        </div>
                        <p className="mt-4 text-xs text-white/60">
                            Note: Tag types are contextually applied based on position features. Multiple instances may appear per game depending on complexity.
                        </p>
                    </section>
                </div>

                {/* CTA */}
                <div className="mt-16 text-center">
                    <h3 className="font-display text-2xl text-white">Ready to try?</h3>
                    <Link
                        href="/app/dashboard"
                        className="mt-6 inline-flex items-center justify-center rounded-full bg-accent-teal px-8 py-4 text-sm font-semibold text-ink shadow-glow transition hover:scale-[1.02]"
                    >
                        Start Analyzing
                    </Link>
                </div>
            </div>
        </div>
    );
}
