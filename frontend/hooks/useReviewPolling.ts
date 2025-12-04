import { useEffect, useState } from "react";
import { fetchReview } from "../lib/review";
import type { Review } from "../types/review";

interface ReviewPollingState {
  review: Review | null;
  loading: boolean;
  pendingMessage: string | null;
  pollStartTime: number | null;
  pollAttempt: number;
  error: string | null;
  setReview: (r: Review) => void;
  progressInfo: { stage?: string; stageLabel?: string; totalProgress?: number; stageProgress?: number; startedAt?: number } | null;
}

export function useReviewPolling(reviewId: string): ReviewPollingState {
  const [review, setReview] = useState<Review | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [pendingMessage, setPendingMessage] = useState<string | null>(null);
  const [pollStartTime, setPollStartTime] = useState<number | null>(null);
  const [pollAttempt, setPollAttempt] = useState<number>(0);
  const [error, setError] = useState<string | null>(null);
  const [progressInfo, setProgressInfo] = useState<{ stage?: string; stageLabel?: string; totalProgress?: number; stageProgress?: number; startedAt?: number } | null>(null);

  useEffect(() => {
    let mounted = true;
    let timer: NodeJS.Timeout | null = null;

    const poll = async (attempt: number) => {
      if (!mounted) return;
      setPollAttempt(attempt);
      try {
        const res = await fetchReview(reviewId);
        if (res.status === "pending") {
          setPendingMessage(res.stageLabel || "Analyzing game...");
          setProgressInfo({
            stage: res.stage,
            stageLabel: res.stageLabel,
            totalProgress: res.totalProgress,
            stageProgress: res.stageProgress,
            startedAt: res.startedAt
          });
          setLoading(true);
          if (attempt < 2400) { // Wait up to ~1 hour
            timer = setTimeout(() => poll(attempt + 1), 1500);
          } else {
            setPendingMessage("Analysis is taking a long time. You can refresh to check again.");
            setLoading(false);
          }
          return;
        }
        setPendingMessage(null);
        setPollStartTime(null);
        setPollAttempt(0);
        setProgressInfo(null);
        setReview(res.review);
        setLoading(false);
      } catch (err) {
        // Retry on error, but back off slightly
        // We allow many retries because analysis can be long and network might flicker
        console.log(`Polling error (attempt ${attempt}), retrying...`, err);
        if (attempt < 2400) {
          timer = setTimeout(() => poll(attempt + 1), 3000); // Retry slower on error
          return;
        }
        setError(err instanceof Error ? err.message : "Failed to fetch review");
        setLoading(false);
      }
    };

    setLoading(true);
    setError(null);
    setPendingMessage(null);
    setPollStartTime(Date.now());
    setPollAttempt(0);
    setProgressInfo(null);
    poll(0);

    return () => {
      mounted = false;
      if (timer) clearTimeout(timer);
    };
  }, [reviewId]);

  return { review, loading, pendingMessage, pollStartTime, pollAttempt, error, setReview, progressInfo };
}
