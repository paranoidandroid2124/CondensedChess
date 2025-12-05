export default function ComparisonTable() {
    const features = [
        { name: "Blunder/Mistake detection", standard: "✓", premium: "✓", condensed: "✓" },
        { name: "Practicality Score (suboptimal tolerance)", standard: "—", premium: "—", condensed: "✓" },
        { name: "Study hotspot ranking", standard: "—", premium: "Partial", condensed: "✓" },
        { name: "Book-style explanation", standard: "—", premium: "—", condensed: "✓" },
        { name: "Semantic position tagging", standard: "—", premium: "—", condensed: "✓" },
        { name: "Chapter-like narrative structure", standard: "—", premium: "—", condensed: "✓" },
    ];

    return (
        <section className="glass-card rounded-3xl p-8 sm:p-10">
            <div className="mb-8">
                <p className="text-xs uppercase tracking-[0.2em] text-white/60">Comparison</p>
                <h2 className="font-display text-3xl text-white">vs Traditional Analysis Tools</h2>
                <p className="mt-2 text-sm text-white/70">
                    Most chess platforms offer great analysis. CondensedChess goes one step further.
                </p>
            </div>
            <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                    <thead>
                        <tr className="border-b border-white/10">
                            <th className="pb-3 pr-4 font-semibold text-white">Feature</th>
                            <th className="px-4 pb-3 text-center font-semibold text-white/70">
                                Standard
                                <br />
                                Platforms
                            </th>
                            <th className="px-4 pb-3 text-center font-semibold text-white/70">Premium Tools</th>
                            <th className="px-4 pb-3 text-center font-semibold text-accent-teal">CondensedChess</th>
                        </tr>
                    </thead>
                    <tbody>
                        {features.map((feature, idx) => (
                            <tr key={idx} className="border-b border-white/5">
                                <td className="py-3 pr-4 text-white/80">{feature.name}</td>
                                <td className="px-4 py-3 text-center text-white/60">{feature.standard}</td>
                                <td className="px-4 py-3 text-center text-white/60">{feature.premium}</td>
                                <td className="px-4 py-3 text-center font-semibold text-accent-teal">{feature.condensed}</td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
            <div className="mt-6 rounded-xl bg-white/5 p-4">
                <p className="text-xs text-white/70">
                    <strong className="text-white">TL;DR:</strong> CondensedChess = Engine accuracy + chess book explanations + automated study prioritization
                </p>
            </div>
        </section>
    );
}
