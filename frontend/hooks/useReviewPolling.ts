import useSWR from 'swr';
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

export function useReviewPolling(reviewId: string): ReviewPollingState {
  // SWR Key: unique identifier for the request
  const key = reviewId ? ['review', reviewId] : null;

  const { data, error, mutate, isLoading } = useSWR(
    key,
    () => fetchReview(reviewId),
    {
      // Smart Polling: Refresh every 1.5s if status is pending
      refreshInterval: (data) => {
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
    loading: isLoading || (!data && !error),
    pendingMessage: (data && data.status === 'pending') ? (data.stageLabel || "Analyzing game...") : null,
    pollStartTime: null, // Legacy field
    pollAttempt: 0,      // Legacy field
    error: error ? (error.message || "Failed to fetch review") : null,
    setReview: (r: Review) => mutate({ status: 'ready', review: r }, false), // Optimistic update
    progressInfo
  };
}
