export function ReviewErrorView({ error, reviewId }: { error?: string | null; reviewId: string }) {
  return (
    <div className="px-6 py-10 sm:px-12 lg:px-16">
      <div className="mx-auto max-w-3xl rounded-2xl border border-rose-500/30 bg-rose-500/10 p-6">
        <h2 className="text-xl font-semibold text-white">Load failed</h2>
        <p className="text-sm text-white/80">{error ?? "Review not found"}</p>
        <p className="mt-2 text-xs text-white/60">
          Set NEXT_PUBLIC_REVIEW_API_BASE for the API base URL, or open `/review/sample` to view sample data.
        </p>
      </div>
    </div>
  );
}

