"use client";

import React, { useEffect, useState } from "react";
import { useEngineAnalysis } from "../../hooks/useEngineAnalysis";
import { EngineSettingsModal } from "../../components/review/EngineSettingsModal";

export default function EngineTestPage() {
    const [fen, setFen] = useState("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
    const [isSettingsOpen, setIsSettingsOpen] = useState(false);

    // Mock Review object just to satisfy the hook's interface roughly, or pass null
    // The hook: useEngineAnalysis(review, previewFen, selectedPly)
    // We can pass null review and use previewFen to control what we analyze.
    const {
        engine,
        isAnalyzing,
        engineLines,
        toggleAnalysis,
        config,
        setConfig
    } = useEngineAnalysis(null, fen, null);

    // When engine is ready/changed, we might want to log it
    useEffect(() => {
        console.log("Engine instance:", engine);
    }, [engine]);

    return (
        <div className="min-h-screen bg-[#0a0a0a] text-white p-8 font-sans">
            <div className="max-w-4xl mx-auto space-y-8">

                <header className="border-b border-white/10 pb-6 flex justify-between items-center">
                    <div>
                        <h1 className="text-3xl font-display font-bold">WASM Engine Test</h1>
                        <p className="text-white/40 mt-2">Verify Web Worker integration and performance.</p>
                    </div>
                    <button
                        onClick={() => setIsSettingsOpen(true)}
                        className="px-4 py-2 bg-white/5 hover:bg-white/10 rounded-lg text-sm transition border border-white/10"
                    >
                        ⚙️ Settings
                    </button>
                </header>

                {/* Control Panel */}
                <section className="bg-[#111] p-6 rounded-xl border border-white/5 space-y-4">
                    <div className="space-y-2">
                        <label className="text-xs uppercase tracking-wider text-white/40 font-bold">FEN Position</label>
                        <input
                            className="w-full bg-black/20 border border-white/10 rounded px-3 py-3 font-mono text-sm text-accent-teal focus:outline-none focus:border-accent-teal/50"
                            value={fen}
                            onChange={(e) => setFen(e.target.value)}
                        />
                    </div>

                    <div className="flex gap-4 pt-2">
                        <button
                            onClick={toggleAnalysis}
                            className={`px-6 py-3 rounded-lg font-bold text-sm transition shadow-lg ${isAnalyzing
                                    ? "bg-red-500/20 text-red-400 hover:bg-red-500/30 border border-red-500/50"
                                    : "bg-accent-teal/20 text-accent-teal hover:bg-accent-teal/30 border border-accent-teal/50"
                                }`}
                        >
                            {isAnalyzing ? "■ Stop Analysis" : "▶ Start Analysis"}
                        </button>
                    </div>
                </section>

                {/* Engine Output */}
                <section className="bg-[#111] p-6 rounded-xl border border-white/5 min-h-[300px]">
                    <h2 className="text-lg font-bold mb-4 flex items-center gap-2">
                        Output
                        {isAnalyzing && <span className="w-2 h-2 rounded-full bg-green-500 animate-pulse" />}
                    </h2>

                    <div className="space-y-2">
                        {engineLines.length === 0 && (
                            <div className="text-white/20 italic p-4 text-center">No analysis output yet...</div>
                        )}
                        {engineLines.map((line, i) => (
                            <div key={i} className="flex items-start gap-4 p-3 bg-black/20 rounded border border-white/5 font-mono text-xs">
                                <div className="w-8 text-white/40 text-right">{i + 1}.</div>
                                <div className="flex-1 space-y-1">
                                    <div className="flex gap-4 text-accent-teal">
                                        <span className="font-bold">
                                            {line.cp !== undefined
                                                ? (line.cp / 100).toFixed(2)
                                                : (line.mate ? `M${line.mate}` : "---")}
                                        </span>
                                        <span className="text-white/40">Depth {line.depth}</span>
                                    </div>
                                    <div className="text-white/70 break-all leading-relaxed">
                                        {line.pv}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </section>

                {/* Config Debug */}
                <section className="text-xs font-mono text-white/20">
                    <pre>{JSON.stringify(config, null, 2)}</pre>
                </section>

                {isSettingsOpen && (
                    <EngineSettingsModal
                        config={config}
                        setConfig={setConfig}
                        onClose={() => setIsSettingsOpen(false)}
                    />
                )}

            </div>
        </div>
    );
}
