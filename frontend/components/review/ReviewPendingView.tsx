export function ReviewPendingView({
  reviewId,
  pendingMessage
}: {
  reviewId: string;
  pendingMessage: string | null;
}) {
  return (
    <div className="px-6 py-10 sm:px-12 lg:px-16">
      <div className="mx-auto max-w-3xl rounded-2xl border border-white/10 bg-white/5 p-6">
        <h2 className="text-xl font-semibold text-white">Analyzing...</h2>
        <p className="text-sm text-white/80">Job {reviewId} is still processing. You can refresh later.</p>
        <p className="mt-2 text-xs text-white/60">
          {pendingMessage ?? "Set NEXT_PUBLIC_REVIEW_API_BASE for the API base URL, or open `/review/sample` to view sample data."}
        </p>
      </div>
    </div>
  );
}

