import Link from "next/link";

export default function HeroSection() {
    return (
        <section className="flex flex-col gap-8 rounded-3xl bg-gradient-to-r from-[rgba(91,141,239,0.14)] via-[rgba(61,214,183,0.12)] to-[rgba(139,92,246,0.1)] p-[1px] shadow-glow">
            <div className="glass-card relative overflow-hidden rounded-[28px] p-10 sm:p-14">
                <div className="absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(139,92,246,0.08),transparent_35%),radial-gradient(circle_at_80%_0%,rgba(61,214,183,0.12),transparent_40%)]" />
                <div className="relative z-10 flex flex-col gap-8 lg:flex-row lg:items-center lg:justify-between">
                    {/* Left: Copy + CTAs */}
                    <div className="max-w-2xl space-y-6">
                        <p className="inline-flex items-center rounded-full bg-white/10 px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-mist">
                            Book-Style Review
                        </p>

                        {/* Eye-catching LLM positioning */}
                        <div className="inline-block rounded-xl border border-accent-teal/30 bg-accent-teal/10 px-4 py-3">
                            <p className="text-sm leading-relaxed text-white/90">
                                <span className="font-semibold text-accent-teal">Engine precision. Human clarity.</span>
                                <br />
                                We translate complex <span className="font-semibold text-white">evaluations</span> into clear explanations.
                            </p>
                        </div>

                        <h1 className="font-display text-4xl leading-tight sm:text-5xl lg:text-6xl">
                            Not just engine bars.
                            <br />
                            <span className="bg-gradient-to-r from-accent-teal to-accent-blue bg-clip-text text-transparent">
                                Chess reviews like a book.
                            </span>
                        </h1>
                        <p className="text-lg leading-relaxed text-white/70">
                            Chesstory picks the most study-worthy moments from your games,
                            <br />
                            organizes them like a chess textbook,
                            <br />
                            and presents them with <strong className="text-white">Practicality & Study Scores</strong>.
                        </p>
                        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
                            <Link
                                href="/app/dashboard"
                                className="inline-flex items-center justify-center rounded-full bg-accent-teal px-6 py-3.5 text-sm font-semibold text-ink shadow-glow transition hover:scale-[1.02]"
                            >
                                Analyze My Games
                            </Link>
                            <Link
                                href="/examples"
                                className="inline-flex items-center justify-center rounded-full border border-white/15 px-6 py-3.5 text-sm font-semibold text-white/80 transition hover:border-white/35"
                            >
                                View Sample Review
                            </Link>
                        </div>
                        <p className="flex items-center gap-2 text-sm text-white/50">
                            <span className="inline-block h-1.5 w-1.5 rounded-full bg-accent-teal/70"></span>
                            Deep analysis takes about 4–5 minutes per game
                        </p>
                    </div>

                    {/* Right: Screenshot Preview */}
                    <div className="glass-card relative w-full max-w-md rounded-2xl border border-white/10 p-6 lg:max-w-lg">
                        <div className="flex items-center justify-between text-xs text-white/70">
                            <span>Sample Insights</span>
                            <span className="rounded-full bg-white/10 px-2 py-1 text-[11px] font-semibold uppercase tracking-wide">
                                Practicality + Study
                            </span>
                        </div>
                        <div className="mt-4 space-y-3 text-sm">
                            <div className="flex justify-between rounded-xl bg-white/5 px-3 py-2.5">
                                <span className="text-white/80">Critical moments</span>
                                <span className="font-semibold text-accent-teal">7</span>
                            </div>
                            <div className="flex justify-between rounded-xl bg-white/5 px-3 py-2.5">
                                <span className="text-white/80">Study Score avg</span>
                                <span className="font-semibold text-accent-blue">8.4</span>
                            </div>
                            <div className="flex justify-between rounded-xl bg-white/5 px-3 py-2.5">
                                <span className="text-white/80">Practicality</span>
                                <span className="font-semibold text-yellow-400">★★★★☆</span>
                            </div>
                            <div className="mt-4 rounded-xl border border-white/10 bg-white/5 p-3">
                                <p className="text-xs text-white/60">Latest Analysis</p>
                                <p className="mt-1 text-sm text-white/90">
                                    &quot;Move 15.Nd5! – tactical breakthrough with high study value&quot;
                                </p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    );
}
