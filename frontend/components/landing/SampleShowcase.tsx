export default function SampleShowcase() {
    const sampleMoves = [
        {
            move: "9...d5?!",
            practicality: 2,
            study: 8.4,
            comment: "Premature central break. White's pieces coordinate better afterward.",
            tags: ["Positional Mistake", "Pawn Structure"],
        },
        {
            move: "15.Nd5!",
            practicality: 5,
            study: 9.1,
            comment: "Tactical breakthrough! Knight sacrifice forces major concessions.",
            tags: ["Tactical Blow", "Initiative Gain"],
        },
        {
            move: "23.Rxe6",
            practicality: 4,
            study: 7.8,
            comment: "Clean exchange sacrifice for long-term compensation.",
            tags: ["Sacrifice", "Dynamic Compensation"],
        },
        {
            move: "28...Qg5?",
            practicality: 1,
            study: 8.9,
            comment: "King safety crisis. Black's attack was premature.",
            tags: ["King Safety Crisis", "Blunder"],
        },
    ];

    return (
        <section className="glass-card rounded-3xl p-8 sm:p-10">
            <div className="mb-8">
                <p className="text-xs uppercase tracking-[0.2em] text-white/60">Example</p>
                <h2 className="font-display text-3xl text-white">Sample Review</h2>
                <p className="mt-2 text-sm text-white/70">
                    Example: Carlsen vs Sevian, Reversed Sicilian
                    <br />
                    CondensedChess extracted <strong className="text-white">7 study points</strong> from this game
                </p>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
                {sampleMoves.map((item, idx) => (
                    <div
                        key={idx}
                        className="group cursor-pointer rounded-xl border border-white/10 bg-white/5 p-4 transition hover:border-white/25 hover:bg-white/10"
                    >
                        <div className="mb-3 flex items-center justify-between">
                            <span className="font-mono text-lg font-semibold text-accent-teal">{item.move}</span>
                            <div className="flex gap-2 text-xs">
                                <span className="rounded-full bg-yellow-500/20 px-2 py-1 font-semibold text-yellow-400">
                                    {"★".repeat(item.practicality)}
                                    {"☆".repeat(5 - item.practicality)}
                                </span>
                                <span className="rounded-full bg-accent-blue/20 px-2 py-1 font-semibold text-accent-blue">
                                    Study {item.study}
                                </span>
                            </div>
                        </div>
                        <p className="text-sm leading-relaxed text-white/80">{item.comment}</p>
                        <div className="mt-3 flex flex-wrap gap-1.5">
                            {item.tags.map((tag) => (
                                <span key={tag} className="rounded-md bg-white/10 px-2 py-0.5 text-xs text-white/70">
                                    {tag}
                                </span>
                            ))}
                        </div>
                    </div>
                ))}
            </div>

            <div className="mt-6 text-center">
                <a
                    href="/examples"
                    className="inline-flex items-center gap-2 rounded-full border border-white/20 px-5 py-2.5 text-sm font-semibold text-white transition hover:border-white/40"
                >
                    View Full Review
                    <span className="text-accent-teal">→</span>
                </a>
            </div>
        </section>
    );
}
