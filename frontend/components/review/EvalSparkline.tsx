import React, { useMemo, useState } from "react";
import type { TimelineNode } from "../../types/review";

export function EvalSparkline({
  timeline,
  spikePlys,
  markers,
  onSelectPly
}: {
  timeline: TimelineNode[];
  spikePlys?: Array<{ ply: number; concept: string }>;
  markers?: Array<{ ply: number; kind?: string; label?: string }>;
  onSelectPly?: (ply: number) => void;
}) {
  const [hoverIdx, setHoverIdx] = useState<number | null>(null);

  const { poly, values, w, h, coords } = useMemo(() => {
    const width = Math.max(260, timeline.length * 9);
    const height = 80;
    const vals = timeline
      .map((t) => t.winPctAfterForPlayer ?? t.winPctBefore)
      .filter((v): v is number => typeof v === "number")
      .map((v, idx) => {
        const turn = timeline[idx].turn;
        return turn === "black" ? 100 - v : v;
      });
    if (!vals.length) return { poly: "", values: [], w: width, h: height, coords: [] as Array<[number, number]> };
    const minVal = 0;
    const maxVal = 100;
    const range = maxVal - minVal;
    const coordinates = vals.map((v, idx) => {
      const x = vals.length === 1 ? width / 2 : (idx / (vals.length - 1)) * width;
      const y = height - ((v - minVal) / range) * height;
      return [x, y] as [number, number];
    });
    return {
      poly: coordinates.map(([x, y]) => `${x.toFixed(1)},${y.toFixed(1)}`).join(" "),
      values: vals,
      w: width,
      h: height,
      coords: coordinates
    };
  }, [timeline]);

  const last = values.length ? values[values.length - 1] : null;
  const hoverVal = hoverIdx != null ? values[hoverIdx] : null;
  const hoverPoint = hoverIdx != null ? coords[hoverIdx] : null;
  const hoverLabel = hoverVal != null ? `${hoverVal.toFixed(1)}%` : "";

  const handleMove = (e: React.MouseEvent<SVGSVGElement>) => {
    if (!coords.length) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const scaleX = w / rect.width;
    const x = (e.clientX - rect.left) * scaleX;
    let closest = 0;
    let best = Math.abs(coords[0][0] - x);
    coords.forEach(([cx], idx) => {
      const d = Math.abs(cx - x);
      if (d < best) {
        best = d;
        closest = idx;
      }
    });
    setHoverIdx(closest);
  };

  return (
    <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
      <div className="flex items-center justify-between text-xs text-white/70">
        <span>Eval trend (win %)</span>
        <div className="text-right">
          <div className="text-[11px] text-white/50">White win%</div>
          <div className="font-semibold text-accent-teal">{last != null ? last.toFixed(1) : "–"}</div>
        </div>
      </div>
      <svg
        className="mt-2 h-24 w-full"
        viewBox={`0 0 ${w} ${h}`}
        preserveAspectRatio="none"
        onMouseMove={handleMove}
        onMouseLeave={() => setHoverIdx(null)}
      >
        <line x1={0} x2={w} y1={h * 0.5} y2={h * 0.5} stroke="rgba(255,255,255,0.12)" strokeWidth={1} strokeDasharray="4 4" />
        {poly ? (
          <>
            <polygon
              points={`${poly} ${w},${h} 0,${h}`}
              fill="rgba(255,255,255,0.70)"
            />
            <polyline points={poly} fill="none" stroke="#f5f5f5" strokeWidth="3.5" strokeLinejoin="round" />
          </>
        ) : null}
        {hoverPoint ? (
          <>
            <circle cx={hoverPoint[0]} cy={hoverPoint[1]} r={4} fill="#5b8def" />
            {hoverLabel ? (
              <>
                {(() => {
                  const padding = 8;
                  const rectW = Math.max(46, hoverLabel.length * 7 + padding * 2);
                  const rectH = 18;
                  const rectX = Math.min(Math.max(hoverPoint[0] - rectW / 2, 0), w - rectW);
                  const rectY = Math.max(hoverPoint[1] - rectH - 6, 0);
                  const textX = rectX + rectW / 2;
                  const textY = rectY + rectH / 2 + 4;
                  return (
                    <>
                      <rect x={rectX} y={rectY} width={rectW} height={rectH} rx={6} fill="rgba(0,0,0,0.65)" />
                      <text
                        x={textX}
                        y={textY}
                        fill="#e7ecff"
                        fontSize={12}
                        fontFamily="Inter, system-ui, sans-serif"
                        fontWeight={600}
                        textAnchor="middle"
                      >
                        {hoverLabel}
                      </text>
                    </>
                  );
                })()}
              </>
            ) : null}
          </>
        ) : null}
        {((markers && markers.length) || (spikePlys && spikePlys.length))
          ? (markers && markers.length ? markers : spikePlys?.map((s) => ({ ply: s.ply, kind: "spike", label: s.concept })) || [])
            .map((s) => {
              const idx = timeline.findIndex((t) => t.ply === s.ply);
              if (idx === -1 || !coords[idx]) return null;
              return { ...s, idx, point: coords[idx] };
            })
            .filter(Boolean)
            .map((s, i) => (
              <g
                key={`${s?.ply}-${i}`}
                onClick={() => onSelectPly?.(s!.ply)}
                className="cursor-pointer"
              >
                <circle
                  cx={s!.point[0]}
                  cy={s!.point[1]}
                  r={5}
                  fill={
                    s!.kind === "blunder"
                      ? "#f87171"
                      : s!.kind === "mistake"
                        ? "#fb923c"
                        : s!.kind === "inaccuracy"
                          ? "#fbbf24"
                          : s!.kind === "brilliant"
                            ? "#a855f7"
                            : s!.kind === "great"
                              ? "#22d3ee"
                              : "#c084fc"
                  }
                  opacity={0.9}
                >
                  <title>{`${s!.label ?? s!.kind ?? "event"} @ ply ${s!.ply}`}</title>
                </circle>
              </g>
            ))
          : null}
      </svg>
    </div>
  );
}
