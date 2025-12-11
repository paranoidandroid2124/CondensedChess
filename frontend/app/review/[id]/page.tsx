import ReviewClient from "./ReviewClient";
import { ErrorBoundary } from "../../../components/common/ErrorBoundary";

export const dynamic = "force-dynamic";

export default function ReviewPage({ params }: { params: { id: string } }) {
  return (
    <ErrorBoundary>
      <ReviewClient reviewId={params.id} />
    </ErrorBoundary>
  );
}
