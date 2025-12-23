import useSWR from 'swr';
import { useState } from 'react';
import { fetchReview } from "../lib/review";
import type { Review } from "../types/review";

interface ReviewPollingState {
  review: Review | null;
  loading: boolean;
  pendingMessage: string | null;
  pollStartTime: number | null; // Kept for interface compatibility but unused in SWR logic
  pollAttempt: number;          // Unused/Mocked
  error: string | null;
  setReview: (r: Review) => void; // Mutate function adapter
  progressInfo: { stage?: string; stageLabel?: string; totalProgress?: number; stageProgress?: number; startedAt?: number } | null;
}

const POLL_TIMEOUT_MS = 8 * 60 * 1000; // 8 minutes

export function useReviewPolling(reviewId: string): ReviewPollingState {
  // SWR Key: unique identifier for the request
  const key = reviewId ? ['review', reviewId] : null;
  const [pollStartTime] = useState(() => Date.now());

  // Timeout Logic: Check if we exceeded the max wait time
  const isTimedOut = (Date.now() - pollStartTime) > POLL_TIMEOUT_MS;

  const { data, error, mutate, isLoading } = useSWR(
    key,
    () => fetchReview(reviewId),
    {
      // Smart Polling: Refresh every 1.5s if status is pending AND not timed out
      refreshInterval: (data) => {
        if (isTimedOut) return 0; // Stop polling on timeout
        if (data && data.status === 'pending') return 1500;
        return 0; // Stop polling when complete or error
      },
      revalidateOnFocus: false, // Don't aggressive revalidate if we have final data
      revalidateOnReconnect: true,
      dedupingInterval: 2000,
    }
  );

  const isPending = data?.status === 'pending';
  const review = (data && data.status === 'ready') ? data.review : null;

  // Friendly Error Mapping
  const getFriendlyErrorMessage = (msg: string | null) => {
    if (!msg) return null;
    const lower = msg.toLowerCase();

    if (lower.includes("timeout") || lower.includes("time out"))
      return "Analysis timed out. The position might be too complex, please try a shorter game.";
    if (lower.includes("job_failed") || lower.includes("failed"))
      return "Analysis encountered an unexpected error. Please try again.";

    return msg; // Fallback to original if we don't recognize it
  };

  const rawError = error ? (error.message || "Failed to fetch review") : null;
  const friendlyError = getFriendlyErrorMessage(rawError);

  // Effective Error: Backend error OR Timeout error
  const effectiveError = friendlyError
    ? friendlyError
    : (isTimedOut && isPending)
      ? "Analysis is taking longer than expected. Please try again in a moment."
      : null;

  // Progress Info Construction
  const progressInfo = (data && data.status === 'pending') ? {
    stage: data.stage,
    stageLabel: data.stageLabel,
    totalProgress: data.totalProgress,
    stageProgress: data.stageProgress,
    startedAt: data.startedAt
  } : null;

  return {
    review,
    loading: isLoading || (!data && !error && !isTimedOut),
    pendingMessage: (!effectiveError && data && data.status === 'pending') ? (data.stageLabel || "Analyzing game...") : null,
    pollStartTime: null, // Legacy field
    pollAttempt: 0,      // Legacy field
    error: effectiveError,
    setReview: (r: Review) => mutate({ status: 'ready', review: r }, false), // Optimistic update
    progressInfo
  };
}
