import type { Concepts } from "../../types/review";
import { humanizeTag } from "../../lib/review-tags";

export function ConceptChips({ concepts }: { concepts?: Concepts }) {
  if (!concepts) return null;
  const entries = Object.entries(concepts)
    .filter(([, v]) => typeof v === "number")
    .sort((a, b) => (b[1] ?? 0) - (a[1] ?? 0))
    .slice(0, 5);
  return (
    <div className="flex flex-wrap gap-2">
      {entries.map(([name, score]) => (
        <span key={name} className="rounded-full bg-white/10 px-3 py-1 text-xs text-white/80">
          {humanizeTag(name)} {Math.round((score ?? 0) * 100) / 100}
        </span>
      ))}
    </div>
  );
}

