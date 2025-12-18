"use client";

import Link from "next/link";
import React from "react";
// We'll reuse some review components for the visual heavy lifting
import { BoardSection } from "../../../components/review/BoardSection";

export default function SampleReviewPage() {
    // Carlsen vs Gukesh Data
    // PGN: 1. e4 e5 ... 1-0
    const moments = [
        {
            id: 1,
            ply: 16, // 8... Nd7
            title: "Developing Central Tension",
            tag: "Opening Edge",
            description: "White seizes an early development advantage and grabs space, placing immediate pressure on the position. Black attempts to resolve the tension through the exchange 7... Bxf3 and continues to coordinate pieces with 8... Nd7. Despite these neutral maneuvers, White maintains a spatial edge that creates an early crisis for king safety as the middlegame approaches.",
            fen: "r2qkb1r/1ppn1ppp/p1np4/4p3/B1P1P3/2N2Q1P/PP1P1PP1/R1B1K2R w KQkq - 1 9",
            move: { from: "f6", to: "d7" }, // 8... Nd7
            color: "text-emerald-400",
            winProb: 54, // Approx
        },
        {
            id: 2,
            ply: 29, // 15. Nxc6
            title: "Volatility and Structural Tension",
            tag: "Tactical Storm",
            description: "White initiated a sharp sequence of complications with 15. Nxc6, forcing Black to address immediate tactical threats near the center. Black attempted to stabilize the position by castling with 17... O-O, though the presence of hanging pawns on the c and d files kept the atmosphere volatile. The complexity continued through 19... cxd6, as both sides engaged in a violent series of piece trades that redefined the central pawn structure.",
            fen: "r2qk2r/1ppq1pb1/p1Np2p1/2n4p/2P1P3/3P3P/PP3PB1/R1B1QRK1 b kq - 0 15",
            move: { from: "d4", to: "c6" }, // 15. Nxc6
            color: "text-amber-400",
            winProb: 62,
        },
        {
            id: 3,
            ply: 51, // 26. Rxd6
            title: "Storming the Isolated Center",
            tag: "Complex Exchange",
            description: "White initiated a volatile sequence in the center, beginning with the provocative 21. Be7. Seeking to dismantle the defense of Black's isolated queen's pawn, White followed up with 23. cxd5 to unlock the position and increase the tactical pressure. The skirmish reached its climax with 26. Rxd6, as White traded pieces to neutralize Black's central presence and simplify the chaotic board state.",
            fen: "r7/1pr1n1b1/p2R2p1/3n1pBp/4N2P/3N4/PP3PP1/4R1K1 b - - 0 26",
            move: { from: "d1", to: "d6" }, // 26. Rxd6
            color: "text-rose-400",
            winProb: 88,
        },
        {
            id: 4,
            ply: 60, // 30... Rc6
            title: "Back Rank Conversion Struggles",
            tag: "Endgame Blunder",
            description: "Black initially navigated the endgame with precision, using 26... Nxd5 to simplify the position through a favorable piece trade. However, the conversion phase became erratic after 28... Ne7, which allowed White to regain footing. The tension peaked with 30... Rc6, a move that failed to address the structural weakness of Black's back rank, turning a technical advantage into a struggle for stability.",
            fen: "5b2/1p2n1k1/pnr1N1p1/5pBp/7P/3N2P1/PP3P2/4R1K1 w - - 1 31",
            move: { from: "c7", to: "c6" }, // 30... Rc6
            color: "text-blue-400",
            winProb: 98,
        }
    ];

    return (
        <div className="relative min-h-screen bg-slate-950 text-white">
            {/* Hero Header */}
            <section className="relative overflow-hidden px-6 py-20 sm:px-12 lg:px-16">
                <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_-20%,rgba(45,212,191,0.15),transparent_40%)]" />
                <div className="mx-auto max-w-6xl relative z-10 text-center">
                    <p className="text-xs uppercase tracking-[0.2em] text-accent-teal mb-4">Sample Analysis</p>
                    <h1 className="font-display text-5xl sm:text-6xl lg:text-7xl font-bold mb-6">
                        Carlsen <span className="text-white/40 font-light">vs</span> Gukesh
                    </h1>
                    <p className="text-lg text-white/60 mb-8 max-w-2xl mx-auto">
                        Clutch Chess Showdown 2025 • Round 14 • 1-0
                    </p>
                    <div className="flex justify-center gap-4">
                        <Link
                            href="/app/dashboard"
                            className="px-8 py-3 rounded-full bg-accent-teal text-ink font-bold text-sm hover:scale-105 transition"
                        >
                            Analyze Your Game
                        </Link>
                    </div>
                </div>
            </section>

            {/* Narrative Section (Zig-Zag) */}
            <section className="px-6 pb-20 sm:px-12 lg:px-16 space-y-32">
                {moments.map((moment, idx) => (
                    <div key={moment.id} className="mx-auto max-w-6xl">
                        <div className={`flex flex-col lg:flex-row gap-12 lg:gap-24 items-center ${idx % 2 === 1 ? 'lg:flex-row-reverse' : ''}`}>

                            {/* Text Content */}
                            <div className="flex-1 space-y-6">
                                <div className="inline-flex items-center gap-2 rounded-full border border-white/10 bg-white/5 px-3 py-1">
                                    <span className={`w-2 h-2 rounded-full ${moment.color.replace('text-', 'bg-')}`} />
                                    <span className="text-xs font-semibold tracking-wide uppercase">{moment.tag}</span>
                                </div>

                                <h2 className="text-3xl sm:text-4xl font-display font-bold">
                                    <span className="text-white/30 mr-4 text-2xl">Move {Math.ceil(moment.ply / 2)}</span>
                                    {moment.title}
                                </h2>

                                <p className="text-lg text-white/70 leading-relaxed font-serif">
                                    {moment.description}
                                </p>

                                {/* Mini Stat */}
                                <div className="pt-4 border-t border-white/10 flex items-center gap-6">
                                    <div>
                                        <div className="text-xs text-white/40 uppercase tracking-widest">Win Prob</div>
                                        <div className={`text-2xl font-bold ${moment.color}`}>{moment.winProb}%</div>
                                    </div>
                                    <div>
                                        <div className="text-xs text-white/40 uppercase tracking-widest">Score</div>
                                        <div className="text-2xl font-bold text-white">{(moment.winProb / 10).toFixed(1)}</div>
                                    </div>
                                </div>
                            </div>

                            {/* Visual Content (Board) */}
                            <div className="flex-1 w-full max-w-lg">
                                <div className="relative aspect-square glass-card rounded-2xl border border-white/10 shadow-2xl overflow-hidden">
                                    {/* Board Component */}
                                    <div className="w-full h-full pointer-events-none">
                                        <BoardSection
                                            fen={moment.fen}
                                            orientation="white"
                                            showAdvanced={false}
                                            customShapes={[]}
                                            arrows={[[moment.move.from, moment.move.to, "#22c55e"]]}
                                            onDrop={() => { }}
                                            onSelectPly={() => { }}
                                            onClearArrows={() => { }}
                                        />
                                    </div>

                                    {/* Decoration Glow */}
                                    <div className="absolute -bottom-6 -right-6 w-24 h-24 bg-accent-teal/10 blur-2xl rounded-full" />
                                </div>
                            </div>

                        </div>
                    </div>
                ))}
            </section>

            {/* CTA Footer */}
            <section className="py-20 text-center border-t border-white/5 bg-white/[0.02]">
                <h2 className="text-3xl font-display font-bold mb-6">Ready to see your own story?</h2>
                <Link
                    href="/app/dashboard"
                    className="inline-flex px-10 py-4 rounded-full bg-white text-black font-bold text-sm hover:scale-105 transition"
                >
                    Upload Game
                </Link>
            </section>
        </div>
    );
}
