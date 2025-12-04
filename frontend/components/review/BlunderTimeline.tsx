import type { TimelineNode } from "../../types/review";

export function BlunderTimeline({
  timeline,
  selected,
  onSelect
}: {
  timeline: (TimelineNode & { label?: string })[];
  selected?: number;
  onSelect: (ply: number) => void;
}) {
  const colorFor = (t: TimelineNode) => {
    if (t.special === "brilliant") return "#c084fc";
    if (t.special === "great") return "#22d3ee";
    if (t.judgement === "blunder") return "#fb7185";
    if (t.judgement === "mistake") return "#f97316";
    if (t.judgement === "inaccuracy") return "#fbbf24";
    if (t.judgement === "good" || t.judgement === "best" || t.judgement === "book") return "#34d399";
    return "#94a3b8";
  };
  return (
    <div className="glass-card rounded-2xl p-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-white/80">Timeline</h3>
        <span className="text-xs text-white/60">blunder/tactical miss/brilliant at a glance</span>
      </div>
      <div className="mt-3 flex items-center gap-2 overflow-x-auto py-2">
        {timeline.map((t) => (
          <button
            key={t.ply}
            onClick={() => onSelect(t.ply)}
            className={`relative flex h-10 w-8 flex-col items-center justify-center rounded-lg transition ${selected === t.ply ? "bg-white/10" : "hover:bg-white/5"
              }`}
            title={`${t.label ?? t.san} (${t.judgement ?? ""}${t.special ? " " + t.special : ""})`}
          >
            <span className="text-[10px] text-white/50">{t.ply}</span>
            <span
              className="mt-1 h-2 w-2 rounded-full"
              style={{ backgroundColor: colorFor(t) }}
            />
          </button>
        ))}
      </div>
    </div>
  );
}

