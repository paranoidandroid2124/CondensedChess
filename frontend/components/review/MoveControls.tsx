export function MoveControls({
  timeline,
  selected,
  onSelect
}: {
  timeline: { ply: number }[];
  selected?: number;
  onSelect: (ply: number) => void;
}) {
  const idx = timeline.findIndex((t) => t.ply === selected);
  const prev = idx > 0 ? timeline[idx - 1] : null;
  const next = idx >= 0 && idx < timeline.length - 1 ? timeline[idx + 1] : null;

  return (
    <div className="flex items-center gap-2 rounded-2xl border border-white/10 bg-white/5 p-2 text-xs text-white/80">
      <button
        onClick={() => prev && onSelect(prev.ply)}
        disabled={!prev}
        className={`rounded-lg px-3 py-2 ${prev ? "hover:bg-white/10" : "opacity-40 cursor-not-allowed"}`}
      >
        ← Prev
      </button>
      <div className="flex-1 text-center text-white/70">
        {selected != null ? `Ply ${selected}` : "Start"}
      </div>
      <button
        onClick={() => next && onSelect(next.ply)}
        disabled={!next}
        className={`rounded-lg px-3 py-2 ${next ? "hover:bg-white/10" : "opacity-40 cursor-not-allowed"}`}
      >
        Next →
      </button>
    </div>
  );
}

