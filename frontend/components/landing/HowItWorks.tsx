export default function HowItWorks() {
    const steps = [
        {
            title: "Upload Game or Paste Link",
            desc: "Upload PGN files or paste links from popular chess platforms.",
            icon: "📤",
        },
        {
            title: "Engine + LLM Analysis",
            desc: "Stockfish + CondensedChess algorithms generate Practicality / Study Scores, semantic tags, and narrative commentary.",
            icon: "🧠",
        },
        {
            title: "Study Points Curated",
            desc: "Extract 5-10 key moments per game and present them as book-style chapters with explanations.",
            icon: "📚",
        },
    ];

    return (
        <section className="glass-card rounded-3xl p-8 sm:p-10">
            <div className="flex flex-col gap-6 md:flex-row md:items-center md:justify-between">
                <div>
                    <p className="text-xs uppercase tracking-[0.2em] text-white/60">How it works</p>
                    <h2 className="font-display text-3xl text-white">Pipeline</h2>
                </div>
                <div className="flex flex-wrap gap-2 text-xs">
                    <span className="rounded-full bg-white/10 px-3 py-1.5">Stockfish</span>
                    <span className="rounded-full bg-white/10 px-3 py-1.5">Concept scores</span>
                    <span className="rounded-full bg-white/10 px-3 py-1.5">LLM summary</span>
                </div>
            </div>
            <div className="mt-8 grid gap-6 md:grid-cols-3">
                {steps.map((step, idx) => (
                    <div key={step.title} className="rounded-2xl border border-white/10 bg-white/5 p-6">
                        <div className="mb-4 flex items-center gap-3">
                            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-white/10 text-2xl">
                                {step.icon}
                            </div>
                            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-accent-teal text-sm font-bold text-ink">
                                {idx + 1}
                            </div>
                        </div>
                        <h4 className="text-lg font-semibold text-white">{step.title}</h4>
                        <p className="mt-2 text-sm leading-relaxed text-white/70">{step.desc}</p>
                    </div>
                ))}
            </div>
        </section>
    );
}
