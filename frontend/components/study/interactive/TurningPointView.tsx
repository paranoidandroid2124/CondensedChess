import React, { useState } from 'react';
import { BookTurningPoint } from '../../../types/StudyModel';

interface Props {
    turningPoint: BookTurningPoint;
    onContinue: () => void;
    onTryMove: (uci: string) => void; // Connect to board
}

export const TurningPointView: React.FC<Props> = ({ turningPoint, onContinue }) => {
    const [revealed, setRevealed] = useState(false);

    // In a real app, we would integrate board interaction (drag-drop) to trigger 'reveal' or 'check'.
    // For this view component, we provide buttons.

    const gain = turningPoint.evalAfterBest - turningPoint.evalAfterPlayed;
    const isBlunder = gain > 200; // 2.0 pawns

    return (
        <div className="bg-slate-800 rounded-xl p-6 border border-slate-600 shadow-2xl max-w-lg mx-auto mt-10">
            <div className="text-center mb-6">
                <h3 className="text-2xl font-bold text-amber-400 mb-2">
                    ⚡ Turning Point
                </h3>
                <p className="text-slate-300">
                    A critical moment! One move changed the course of the game.
                </p>
            </div>

            {!revealed ? (
                <div className="space-y-4">
                    <div className="bg-slate-900/50 p-4 rounded text-center border border-dashed border-slate-600">
                        <p className="text-lg font-semibold text-slate-200">
                            {turningPoint.side}&apos;s turn.
                        </p>
                        <p className="text-sm text-slate-400 mt-1">
                            Can you find the best continuation?
                        </p>
                    </div>
                    <button
                        onClick={() => setRevealed(true)}
                        className="w-full py-3 bg-blue-600 hover:bg-blue-500 text-white font-bold rounded-lg transition"
                    >
                        Reveal What Happened
                    </button>
                </div>
            ) : (
                <div className="space-y-6 animate-in fade-in zoom-in duration-300">
                    <div className="grid grid-cols-2 gap-4">
                        <div className="bg-red-900/20 border border-red-500/30 p-3 rounded text-center">
                            <div className="text-red-400 text-sm font-bold uppercase mb-1">Played</div>
                            <div className="text-2xl font-mono text-white">{turningPoint.playedMove}</div>
                            <div className="text-xs text-red-300 mt-1">Eval: {turningPoint.evalAfterPlayed}</div>
                        </div>
                        <div className="bg-emerald-900/20 border border-emerald-500/30 p-3 rounded text-center">
                            <div className="text-emerald-400 text-sm font-bold uppercase mb-1">Best</div>
                            <div className="text-2xl font-mono text-white">{turningPoint.bestMove}</div>
                            <div className="text-xs text-emerald-300 mt-1">Eval: {turningPoint.evalAfterBest}</div>
                        </div>
                    </div>

                    <div className="bg-slate-700/50 p-3 rounded">
                        <p className="text-sm text-slate-300">
                            <span className="font-bold text-amber-400">Impact: </span>
                            You lost <strong>{gain} cp</strong> ({(gain / 100).toFixed(1)} pawns).
                            {turningPoint.mistakeTags.map(t => (
                                <span key={t} className="ml-2 inline-block px-1.5 py-0.5 rounded bg-red-500/20 text-red-200 text-xs border border-red-500/30">
                                    {t}
                                </span>
                            ))}
                        </p>
                    </div>

                    <button
                        onClick={onContinue}
                        className="w-full py-2 bg-slate-600 hover:bg-slate-500 text-white font-medium rounded transition"
                    >
                        Continue Analysis
                    </button>
                </div>
            )}
        </div>
    );
};
