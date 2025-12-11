import React from "react";
import type { EngineConfig } from "../../hooks/useEngineAnalysis";

type EngineSettingsModalProps = {
    config: EngineConfig;
    setConfig: (c: EngineConfig) => void;
    onClose: () => void;
};

export function EngineSettingsModal({ config, setConfig, onClose }: EngineSettingsModalProps) {

    const handleChange = (key: keyof EngineConfig, val: number) => {
        setConfig({ ...config, [key]: val });
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm" onClick={onClose}>
            <div className="w-[400px] rounded-2xl border border-white/10 bg-[#1a1a1a] p-6 shadow-2xl" onClick={e => e.stopPropagation()}>
                <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl font-display font-bold text-white">Engine Settings</h2>
                    <button onClick={onClose} className="text-white/40 hover:text-white transition">✕</button>
                </div>

                <div className="space-y-6">
                    {/* Threads */}
                    <div className="space-y-2">
                        <div className="flex justify-between text-sm">
                            <span className="text-white/60">Threads</span>
                            <span className="text-accent-teal font-mono">{config.threads}</span>
                        </div>
                        <input
                            type="range" min="1" max="16" step="1"
                            value={config.threads}
                            onChange={(e) => handleChange("threads", parseInt(e.target.value))}
                            className="w-full accent-accent-teal"
                        />
                    </div>

                    {/* Hash */}
                    <div className="space-y-2">
                        <div className="flex justify-between text-sm">
                            <span className="text-white/60">Hash (Memory MB)</span>
                            <span className="text-accent-teal font-mono">{config.hash} MB</span>
                        </div>
                        <select
                            value={config.hash}
                            onChange={(e) => handleChange("hash", parseInt(e.target.value))}
                            className="w-full bg-white/5 border border-white/10 rounded px-3 py-2 text-sm text-white focus:outline-none focus:border-accent-teal"
                        >
                            {[16, 32, 64, 128, 256, 512, 1024].map(v => (
                                <option key={v} value={v} className="bg-[#1a1a1a] text-white">
                                    {v} MB
                                </option>
                            ))}
                        </select>
                    </div>

                    {/* MultiPV */}
                    <div className="space-y-2">
                        <div className="flex justify-between text-sm">
                            <span className="text-white/60">Lines (MultiPV)</span>
                            <span className="text-accent-teal font-mono">{config.multiPv}</span>
                        </div>
                        <input
                            type="range" min="1" max="5" step="1"
                            value={config.multiPv}
                            onChange={(e) => handleChange("multiPv", parseInt(e.target.value))}
                            className="w-full accent-accent-teal"
                        />
                    </div>

                    {/* Depth Limit */}
                    <div className="space-y-2">
                        <div className="flex justify-between text-sm">
                            <span className="text-white/60">Max Depth</span>
                            <span className="text-accent-teal font-mono">{config.depth >= 99 ? "Infinite" : config.depth}</span>
                        </div>
                        <input
                            type="range" min="10" max="99" step="1"
                            value={config.depth}
                            onChange={(e) => handleChange("depth", parseInt(e.target.value))}
                            className="w-full accent-accent-teal"
                        />
                        <div className="text-[10px] text-white/40 text-right">Set to max for Infinite</div>
                    </div>
                </div>

                <div className="mt-8 pt-6 border-t border-white/10 flex justify-end">
                    <button
                        onClick={onClose}
                        className="px-6 py-2 rounded-full bg-white/10 hover:bg-white/20 text-white text-sm font-medium transition"
                    >
                        Done
                    </button>
                </div>
            </div>
        </div>
    );
}
