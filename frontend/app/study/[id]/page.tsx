"use client";

import React from 'react';
import { useStudyData } from '../../../hooks/useStudyData';
import { StudyProvider } from '../../../components/study/StudyContext';
import { StudyShell } from '../../../components/study/StudyShell';
import { ErrorBoundary } from '../../../components/common/ErrorBoundary';
import { useParams } from 'next/navigation';

export default function StudyPage() {
    const params = useParams();
    const id = Array.isArray(params.id) ? params.id[0] : params.id;

    const { data: study, loadingState, refresh } = useStudyData(id);

    if (loadingState.status === 'analyzing' || loadingState.status === 'loading') {
        return (
            <div className="h-screen flex items-center justify-center bg-slate-950 text-slate-400">
                <div className="flex flex-col items-center gap-6 max-w-md w-full px-8">
                    <div className="relative">
                        <div className="w-16 h-16 border-4 border-slate-700 border-t-blue-500 rounded-full animate-spin" />
                        <div className="absolute inset-0 flex items-center justify-center text-xs font-mono text-slate-500">
                            {Math.round((loadingState.progress || 0) * 100)}%
                        </div>
                    </div>
                    <div className="text-center space-y-2">
                        <h2 className="text-xl font-bold text-slate-200">
                            {loadingState.status === 'loading' ? 'Connecting to Engine...' : 'Analysis in Progress'}
                        </h2>
                        <p className="text-sm text-slate-500 animate-pulse">
                            {loadingState.stage || 'Please wait moment...'}
                        </p>
                        <p className="text-xs text-slate-600">
                            Generating book narrative from your game. This usually takes 10-20 seconds.
                        </p>
                    </div>
                </div>
            </div>
        );
    }

    if (loadingState.status === 'error') {
        return (
            <div className="h-screen flex items-center justify-center bg-slate-950 text-red-400">
                <div className="max-w-md text-center p-8 bg-slate-900 rounded-xl border border-red-900/50">
                    <h2 className="text-xl font-bold mb-2">Analysis Failed</h2>
                    <p className="mb-6">{loadingState.message || "Something went wrong."}</p>
                    <button
                        onClick={refresh}
                        className="px-6 py-2 bg-slate-800 hover:bg-slate-700 rounded text-slate-300 transition border border-slate-700"
                    >
                        Retry
                    </button>
                    <div className="mt-4 text-xs text-slate-600">
                        Job ID: {id}
                    </div>
                </div>
            </div>
        );
    }

    if (!study) return null;

    // Legacy Fallback
    if (!study.schemaVersion || study.schemaVersion < 3) {
        return (
            <div className="h-screen flex items-center justify-center bg-slate-950 text-slate-300">
                <div className="max-w-md text-center p-8 bg-slate-900 rounded-xl border border-slate-700">
                    <h2 className="text-xl font-bold mb-4 text-amber-400">Legacy Analysis Detected</h2>
                    <p className="mb-6 text-slate-400">
                        This game was analyzed with an older version of the engine.
                        To view the new Book Mode, please re-analyze the game.
                    </p>
                    <div className="flex gap-4 justify-center">
                        <a
                            href={`/review/${id}`}
                            className="px-6 py-2 bg-slate-800 hover:bg-slate-700 rounded text-white transition border border-slate-600"
                        >
                            Open in Old View
                        </a>
                        <button
                            onClick={() => {
                                window.location.href = '/';
                            }}
                            className="px-6 py-2 bg-blue-600 hover:bg-blue-500 rounded text-white transition font-semibold"
                        >
                            New Analysis
                        </button>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <ErrorBoundary>
            <StudyProvider study={study}>
                <StudyShell />
            </StudyProvider>
        </ErrorBoundary>
    );
}
