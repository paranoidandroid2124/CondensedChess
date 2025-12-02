import ReviewClient from "./ReviewClient";

export const dynamic = "force-dynamic";

export default function ReviewPage({ params }: { params: { id: string } }) {
  return <ReviewClient reviewId={params.id} />;
}
