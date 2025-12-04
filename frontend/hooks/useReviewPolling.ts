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
  progressInfo: { stage?: string; stageLabel?: string; totalProgress?: number } | null;
}

export function useReviewPolling(reviewId: string): ReviewPollingState {
  const [review, setReview] = useState<Review | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [pendingMessage, setPendingMessage] = useState<string | null>(null);
  const [pollStartTime, setPollStartTime] = useState<number | null>(null);
  const [pollAttempt, setPollAttempt] = useState<number>(0);
  const [error, setError] = useState<string | null>(null);
  const [progressInfo, setProgressInfo] = useState<{ stage?: string; stageLabel?: string; totalProgress?: number } | null>(null);

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
            totalProgress: res.totalProgress
          });
          setLoading(true);
          if (attempt < 80) {
            timer = setTimeout(() => poll(attempt + 1), 1500);
          } else {
            setPendingMessage("Analysis taking longer than expected. Please refresh later.");
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
