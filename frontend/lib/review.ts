import type { Review } from "../types/review";

export const API_BASE = process.env.NEXT_PUBLIC_REVIEW_API_BASE || "http://localhost:8080";

export type ReviewFetchResult =
  | { status: "ready"; review: Review }
  | { status: "pending"; message?: string; stage?: string; stageLabel?: string; stageProgress?: number; totalProgress?: number; startedAt?: number };

export async function fetchOpeningLookup(sans: string[]): Promise<import("../types/review").OpeningStats | null> {
  if (!sans.length) return null;
  const q = encodeURIComponent(sans.join(" "));
  const url = `${API_BASE.replace(/\/$/, "")}/opening/lookup?sans=${q}`;
  const res = await fetch(url, { cache: "no-store" });
  if (res.status === 404) return null;
  if (!res.ok) {
    throw new Error(`lookup failed (${res.status})`);
  }
  return (await res.json()) as import("../types/review").OpeningStats;
}

export async function fetchReview(id: string): Promise<ReviewFetchResult> {
  if (id === "sample") {
    const res = await fetch("/mock/sample-review.json", { cache: "no-store" });
    if (!res.ok) {
      throw new Error("Failed to load sample review");
    }
    const json = await res.json();
    return { status: "ready", review: json };
  }

  const url = `${API_BASE.replace(/\/$/, "")}/result/${id}`;
  const res = await fetch(url, { cache: "no-store" });

  // Backend returns 202 while analysis is pending
  if (res.status === 202) {
    let data: any = {};
    try {
      data = await res.json();
    } catch {
      // ignore
    }
    return {
      status: "pending",
      message: data.status,
      stage: data.stage,
      stageLabel: data.stageLabel,
      stageProgress: data.stageProgress,
      totalProgress: data.totalProgress,
      startedAt: data.startedAt
    };
  }

  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Failed to fetch review (${res.status}): ${text}`);
  }

  const json = await res.json();
  // Wrap as ready
  return { status: "ready", review: json as Review };
}

export async function addBranch(jobId: string, ply: number, uci: string): Promise<Review> {
  const url = `${API_BASE.replace(/\/$/, "")}/analysis/branch`;
  const res = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ jobId, ply, uci }),
    cache: "no-store"
  });
  if (res.status === 202) {
    const json = await res.json().catch(() => ({}));
    throw new Error(json.status || "analysis_pending");
  }
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`branch failed (${res.status}): ${text}`);
  }
  const json = await res.json();
  return json as Review;
}
