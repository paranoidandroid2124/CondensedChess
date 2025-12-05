export default function FeatureHighlights() {
    const features = [
        {
            title: "Practicality Score",
            desc: "Engines show the best move. We show how forgiving your position is—even for suboptimal moves.",
            detail: "Measures tolerance for mistakes using Robustness & Horizon concepts.",
            badge: "★★★★★",
            color: "from-yellow-500/20 to-orange-500/20",
        },
        {
            title: "Study Score & Timeline",
            desc: "Automatically rank which moments to review based on win% swings, plan transitions, and phase changes.",
            detail: "Quantifies each critical moment with a Study Score to prioritize your learning.",
            badge: "0.0 - 10.0",
            color: "from-accent-blue/20 to-accent-teal/20",
        },
        {
            title: "Semantic Tags",
            desc: "Abstract concepts like King Safety Crisis, Fortress Defense, Conversion Difficulty labeled as tags.",
            detail: "50+ semantic tags clarify position characteristics and learning points.",
            badge: "50+ tags",
            color: "from-purple-500/20 to-pink-500/20",
        },
        {
            title: "Book-style Narrative",
            desc: "Each critical moment explained in 2-3 lines, Doknjas / Sadler style.",
            detail: "LLM-generated human-friendly commentary translates engine evaluations.",
            badge: "LLM",
            color: "from-accent-teal/20 to-green-500/20",
        },
    ];

    return (
        <section className="space-y-4">
            <div className="mb-6">
                <p className="text-xs uppercase tracking-[0.2em] text-white/60">Features</p>
                <h2 className="font-display text-3xl text-white">What Makes Us Different</h2>
            </div>
            <div className="grid gap-5 md:grid-cols-2">
                {features.map((feature) => (
                    <div
                        key={feature.title}
                        className="glass-card group relative overflow-hidden rounded-2xl p-6 transition hover:border-white/25"
                    >
                        <div
                            className={`absolute inset-0 bg-gradient-to-br ${feature.color} opacity-0 transition group-hover:opacity-100`}
                        />
                        <div className="relative z-10">
                            <div className="mb-3 flex items-center justify-between">
                                <h3 className="font-display text-xl text-white">{feature.title}</h3>
                                <span className="rounded-full bg-white/10 px-2.5 py-1 text-xs font-semibold text-white/90">
                                    {feature.badge}
                                </span>
                            </div>
                            <p className="text-sm leading-relaxed text-white/80">{feature.desc}</p>
                            <p className="mt-3 text-xs leading-relaxed text-white/60">{feature.detail}</p>
                        </div>
                    </div>
                ))}
            </div>
        </section>
    );
}
