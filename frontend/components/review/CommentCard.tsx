import type { EnhancedTimelineNode } from "../../lib/review-derived";
import type { CriticalNode } from "../../types/review";

interface CommentCardProps {
    move: EnhancedTimelineNode | null;
    critical?: CriticalNode;
}

export function CommentCard({ move, critical }: CommentCardProps) {
    const comment = critical?.comment || move?.shortComment;

    if (!comment) return null;

    return (
        <div className="glass-card p-4 rounded-xl border border-white/10 bg-gradient-to-br from-indigo-500/10 to-purple-500/10 mt-4">
            <div className="flex items-center gap-2 mb-2">
                <span className="text-xl">🤖</span>
                <h3 className="font-semibold text-indigo-200">Coach's Insight</h3>
            </div>
            <p className="text-sm text-gray-200 leading-relaxed whitespace-pre-wrap">
                {comment}
            </p>
        </div>
    );
}
