import { useState, useEffect, useRef } from 'react';
import { StudyModel } from '../types/StudyModel';
import { API_URL } from '../lib/api';



export interface StudyLoadingState {
    status: 'loading' | 'analyzing' | 'error' | 'ready';
    stage?: string;
    progress?: number;
    message?: string;
}

export interface UseStudyDataResult {
    data: StudyModel | null;
    loadingState: StudyLoadingState;
    refresh: () => void;
}

export function useStudyData(jobId: string | undefined): UseStudyDataResult {
    const [data, setData] = useState<StudyModel | null>(null);
    const [loadingState, setLoadingState] = useState<StudyLoadingState>({ status: 'loading' });
    const [retryCount, setRetryCount] = useState(0);

    // Polling ref to prevent overlap or leaks
    const pollTimeout = useRef<NodeJS.Timeout | null>(null);

    const refresh = () => setRetryCount((c) => c + 1);

    useEffect(() => {
        if (!jobId) {
            setLoadingState({ status: 'error', message: "No Job ID provided" });
            return;
        }

        let isMounted = true;

        const fetchData = async () => {
            try {
                // Use standard result endpoint
                const res = await fetch(`${API_URL}/result/${jobId}`, {
                    headers: {
                        'x-api-key': process.env.NEXT_PUBLIC_API_KEY || ''
                    }
                });

                if (res.status === 202) {
                    const progress = await res.json();
                    if (isMounted) {
                        setLoadingState({
                            status: 'analyzing',
                            stage: progress.stage || 'Processing...',
                            progress: progress.progress || 0
                        });
                        // Poll again in 1s
                        pollTimeout.current = setTimeout(fetchData, 1000);
                    }
                    return;
                }

                if (!res.ok) {
                    const errBody = await res.json().catch(() => ({}));
                    const msg = errBody.error?.message || `Status: ${res.status}`;
                    throw new Error(msg);
                }

                const raw = await res.json();

                // New API v3 always returns wrapped result { jobId, status, result, schemaVersion }
                // Check if 'status' is 'ready' explicitly if we want to be strict, or just take result.

                if (raw.error) {
                    throw new Error(raw.error.message || "Unknown API Error");
                }

                const studyData = raw.result || raw; // Fallback for legacy unwrapped

                if (!studyData.book) {
                    console.warn("Got result but missing Book data. Raw:", raw);
                    throw new Error("Analysis incomplete: No book data found.");
                }

                if (isMounted) {
                    setData(studyData as StudyModel);
                    setLoadingState({ status: 'ready' });
                }

            } catch (err: any) {
                if (isMounted) {
                    console.warn("[useStudyData] Poll error:", err);
                    // Retry a few times on network error? Or just fail?
                    // For now fail.
                    setLoadingState({ status: 'error', message: err.message || 'Unknown error' });
                }
            }
        };

        setLoadingState({ status: 'loading' });
        fetchData();

        return () => {
            isMounted = false;
            if (pollTimeout.current) clearTimeout(pollTimeout.current);
        };
    }, [jobId, retryCount]);

    return { data, loadingState, refresh };
}
