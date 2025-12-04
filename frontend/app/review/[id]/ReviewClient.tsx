"use client";

import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Chessboard } from "react-chessboard";
import { Chess } from "chess.js";
import { addBranch, fetchOpeningLookup, fetchReview } from "../../../lib/review";
import { StockfishEngine, type EngineMessage } from "../../../lib/engine";
import type { Concepts, CriticalNode, OpeningStats, Review, ReviewTreeNode, StudyChapter, TimelineNode } from "../../../types/review";
import { VerticalEvalBar } from "../../../components/VerticalEvalBar";
import { CompressedMoveList } from "../../../components/CompressedMoveList";
import { AnalysisPanel } from "../../../components/AnalysisPanel";
import { BestAlternatives } from "../../../components/BestAlternatives";
import { OpeningExplorerTab } from "../../../components/OpeningExplorerTab";
import { StudyTab } from "../../../components/StudyTab";
import { ConceptsTab } from "../../../components/ConceptsTab";
import { ConceptCards } from "../../../components/ConceptCards";
import { DrawingTools } from "../../../components/DrawingTools";
import { GuessTheMove } from "../../../components/GuessTheMove";
import { CriticalMomentCard } from "../../../components/review/CriticalMomentCard";
import { humanizeTag, displayTag, phaseOf } from "../../../lib/review-tags";
import { uciToSan } from "../../../lib/chess-utils";

type PieceDropArgs = {
  sourceSquare: string;
  targetSquare: string;
  piece?: string;
};

type VariationEntry = {
  node: ReviewTreeNode;
  parentLabel: string;
  parentMoveNumber: number;
  parentPly: number;
  depth?: number;
  pvIndex?: number;
  evalKind: string;
  turn: "white" | "black";
  parentFenBefore?: string;
};

function QuickJump({
  timeline,
  onSelect
}: {
  timeline: TimelineNode[];
  onSelect: (ply: number) => void;
}) {
  const [value, setValue] = useState<string>("");
  const maxPly = timeline[timeline.length - 1]?.ply ?? 0;

  const nearestPly = (target: number) => {
    if (!timeline.length) return null;
    let best = timeline[0].ply;
    let bestDiff = Math.abs(timeline[0].ply - target);
    for (const t of timeline) {
      const diff = Math.abs(t.ply - target);
      if (diff < bestDiff) {
        bestDiff = diff;
        best = t.ply;
      }
    }
    return best;
  };

  const submit = () => {
    const num = parseInt(value, 10);
    if (Number.isNaN(num) || num <= 0) return;
    const clamped = Math.min(Math.max(num, 1), maxPly || num);
    const target = nearestPly(clamped);
    if (target != null) onSelect(target);
  };

  return (
    <div className="flex flex-wrap items-center gap-2 rounded-2xl border border-white/10 bg-white/5 px-3 py-2 text-xs text-white/70">
      <span className="uppercase tracking-[0.16em] text-white/60">Quick nav</span>
      <input
        type="number"
        min={1}
        max={maxPly || undefined}
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onKeyDown={(e) => {
          if (e.key === "Enter") submit();
        }}
        className="w-20 rounded-md border border-white/10 bg-white/5 px-2 py-1 text-white outline-none focus:border-accent-teal/60"
        placeholder="Ply #"
      />
      <button
        type="button"
        onClick={submit}
        className="rounded-md bg-accent-teal/20 px-3 py-1 text-white hover:bg-accent-teal/30"
      >
        Go
      </button>
      {maxPly ? <span className="text-[11px] text-white/50">1 – {maxPly}</span> : null}
    </div>
  );
}

function CollapsibleSection({
  title,
  defaultOpen = true,
  children
}: {
  title: string;
  defaultOpen?: boolean;
  children: React.ReactNode;
}) {
  const [open, setOpen] = useState(defaultOpen);
  return (
    <div className="rounded-2xl border border-white/10 bg-white/5">
      <button
        type="button"
        onClick={() => setOpen((o) => !o)}
        className="flex w-full items-center justify-between px-4 py-2 text-sm text-white/80 hover:text-white"
      >
        <span className="font-semibold">{title}</span>
        <span className="text-xs uppercase tracking-[0.18em] text-white/60">{open ? "Hide" : "Show"}</span>
      </button>
      {open ? <div className="border-t border-white/10 p-3">{children}</div> : null}
    </div>
  );
}

const judgementColors: Record<string, string> = {
  brilliant: "bg-purple-500/20 text-purple-100",
  great: "bg-cyan-500/20 text-cyan-100",
  best: "bg-emerald-500/20 text-emerald-200",
  good: "bg-green-500/10 text-green-100", // excellent 통합
  book: "bg-blue-500/15 text-blue-100",
  inaccuracy: "bg-amber-500/20 text-amber-200",
  mistake: "bg-orange-500/20 text-orange-200",
  blunder: "bg-rose-500/20 text-rose-200"
};
const judgementMarks: Record<string, string> = {
  brilliant: "!!",
  great: "!",
  best: "★",
  good: "✓",
  book: "=",
  inaccuracy: "?!",
  mistake: "?",
  blunder: "??"
};

const pieceEmoji: Record<string, string> = {
  K: "♔",
  Q: "♕",
  R: "♖",
  B: "♗",
  N: "♘"
};

function formatSanHuman(san: string): string {
  const trimmed = san.trim();
  const first = trimmed.charAt(0);
  if (pieceEmoji[first]) {
    return `${pieceEmoji[first]}${trimmed.slice(1)}`;
  }
  return trimmed; // pawns or already symbolic
}

function formatDelta(value?: number) {
  if (value === undefined || Number.isNaN(value)) return "–";
  return `${value > 0 ? "+" : ""}${value.toFixed(1)}%`;
}

function normalizeEvalKind(kind?: string, value?: number) {
  const lower = kind?.toLowerCase();
  if (!lower) return "win%";
  if (lower === "cp") {
    if (value != null && Math.abs(value) > 10) return "win%";
    return "cp";
  }
  if (lower.includes("win")) return "win%";
  return lower;
}

function formatEvalValue(value?: number, evalKind?: string, turn?: "white" | "black") {
  const kind = normalizeEvalKind(evalKind, value);
  if (value === undefined || Number.isNaN(value)) return "Eval —";
  if (kind === "cp") {
    const sign = value > 0 ? "+" : "";
    return `Eval ${sign}${Math.round(value)}cp`;
  }
  const sideLabel = turn === "white" ? "White" : turn === "black" ? "Black" : "Side";
  return `${sideLabel} eval ${value.toFixed(1)}%`;
}

function formatWinPctWhite(value?: number) {
  if (value === undefined || Number.isNaN(value)) return "—";
  return `${value.toFixed(1)}%`;
}

function formatDeltaWithSide(value?: number, turn?: "white" | "black") {
  if (value === undefined || Number.isNaN(value)) return { text: "No eval change", tone: "text-white/60" };
  const sign = value > 0 ? "+" : "";
  const tone = value > 0 ? "text-accent-teal" : value < 0 ? "text-rose-300" : "text-white/70";
  const verb = value > 0 ? "improved" : value < 0 ? "worsened" : "no change";
  return { text: `Move ${verb}: ${sign}${value.toFixed(1)}%`, tone };
}

function formatPvList(pv?: string[]) {
  if (!pv?.length) return "";
  return pv.map((m) => formatSanHuman(m)).join(" ");
}

function convertPvToSan(fen: string | undefined, pv?: string[]) {
  if (!pv || !pv.length || !fen) return pv ?? [];
  try {
    const chess = new Chess(fen);
    const moves: string[] = [];
    pv.forEach((uci) => {
      try {
        const move = chess.move({ from: uci.slice(0, 2), to: uci.slice(2, 4), promotion: uci.slice(4) || undefined });
        moves.push(move?.san ?? uci);
      } catch {
        moves.push(uci);
      }
    });
    return moves;
  } catch {
    return pv;
  }
}

function ConceptChips({ concepts }: { concepts?: Concepts }) {
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

function EvalSparkline({
  timeline,
  spikePlys
}: {
  timeline: TimelineNode[];
  spikePlys?: Array<{ ply: number; concept: string }>;
}) {
  const [hoverIdx, setHoverIdx] = useState<number | null>(null);

  const { poly, values, w, h, min, max, coords } = useMemo(() => {
    const width = Math.max(240, timeline.length * 8);
    const height = 64;
    const vals = timeline
      .map((t) => t.winPctAfterForPlayer ?? t.winPctBefore)
      .filter((v): v is number => typeof v === "number")
      .map((v, idx) => {
        // normalize to White perspective: if it's Black to move, flip to White's win%
        const turn = timeline[idx].turn;
        return turn === "black" ? 100 - v : v;
      });
    if (!vals.length) return { poly: "", values: [], w: width, h: height, min: 0, max: 0, coords: [] as Array<[number, number]> };
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
      min: minVal,
      max: maxVal,
      coords: coordinates
    };
  }, [timeline]);

  const last = timeline.at(-1)?.winPctAfterForPlayer ?? timeline.at(-1)?.winPctBefore;
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
        <span className="font-semibold text-accent-teal">{last ? last.toFixed(1) : "–"}</span>
      </div>
      <svg
        className="mt-2 h-20 w-full"
        viewBox={`0 0 ${w} ${h}`}
        preserveAspectRatio="none"
        onMouseMove={handleMove}
        onMouseLeave={() => setHoverIdx(null)}
      >
        {/* grid lines */}
        <line x1={0} x2={w} y1={h * 0.5} y2={h * 0.5} stroke="rgba(255,255,255,0.12)" strokeWidth={1} />
        <polyline points={poly} fill="none" stroke="url(#sparkgrad)" strokeWidth="3" strokeLinejoin="round" />
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
        {spikePlys && spikePlys.length
          ? spikePlys
            .map((s) => {
              const idx = timeline.findIndex((t) => t.ply === s.ply);
              if (idx === -1 || !coords[idx]) return null;
              return { ...s, point: coords[idx] };
            })
            .filter(Boolean)
            .map((s, i) => (
              <circle key={`${s?.ply}-${i}`} cx={s!.point[0]} cy={s!.point[1]} r={4} fill="#8b5cf6" opacity={0.7}>
                <title>{`${s!.concept} @ ply ${s!.ply}`}</title>
              </circle>
            ))
          : null}
        <defs>
          <linearGradient id="sparkgrad" x1="0%" x2="100%" y1="0%" y2="0%">
            <stop offset="0%" stopColor="#5b8def" />
            <stop offset="100%" stopColor="#3dd6b7" />
          </linearGradient>
        </defs>
      </svg>
    </div>
  );
}

function BoardCard({
  fen,
  squareStyles,
  arrows,
  evalPercent,
  judgementBadge,
  moveSquare,
  onDrop
}: {
  fen?: string;
  squareStyles?: Record<string, React.CSSProperties>;
  arrows?: Array<[string, string, string?]>;
  evalPercent?: number;
  judgementBadge?: string;
  moveSquare?: string; // Target square for the move (e.g. "h5")
  onDrop?: (args: PieceDropArgs) => boolean;
}) {
  const [width, setWidth] = useState(420);
  const draggable = typeof onDrop === "function";

  useEffect(() => {
    const update = () => {
      const w = typeof window !== "undefined" ? window.innerWidth : 480;
      setWidth(Math.max(260, Math.min(520, w - 64)));
    };
    update();
    window.addEventListener("resize", update);
    return () => window.removeEventListener("resize", update);
  }, []);

  // Combine square styles with judgment badge overlay
  const combinedSquareStyles = { ...squareStyles };

  if (moveSquare && judgementBadge) {
    const judgmentColor =
      judgementBadge.includes("??") ? "rgba(220, 38, 38, 0.8)" :  // red for blunder
        judgementBadge.includes("?") && !judgementBadge.includes("!") ? "rgba(251, 146, 60, 0.7)" : // orange for mistake
          judgementBadge.includes("?!") ? "rgba(251, 191, 36, 0.7)" : // yellow for inaccuracy
            judgementBadge.includes("!") ? "rgba(34, 197, 94, 0.7)" : // green for good/brilliant
              "rgba(148, 163, 184, 0.5)"; // gray default

    combinedSquareStyles[moveSquare] = {
      ...combinedSquareStyles[moveSquare],
      position: "relative" as const,
      backgroundColor: judgmentColor,
      boxShadow: `inset 0 0 0 3px ${judgmentColor.replace("0.7", "1")}`,
    };
  }

  return (
    <div className="glass-card rounded-2xl p-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-white/80">Board</h3>
        <span className="text-xs text-white/60">{fen ? "Selected ply" : "Starting position"}</span>
      </div>
      <div className="relative mt-3 flex gap-3">
        <VerticalEvalBar evalPercent={evalPercent} />
        <div className="relative overflow-hidden rounded-xl border border-white/10">
          {/* Remove floating badge */}
          <Chessboard
            id="review-board"
            position={fen || "start"}
            boardWidth={width}
            arePiecesDraggable={draggable}
            onPieceDrop={
              draggable
                ? (sourceSquare, targetSquare, piece) => onDrop({ sourceSquare, targetSquare, piece })
                : undefined
            }
            customLightSquareStyle={{ backgroundColor: "#e7ecff" }}
            customDarkSquareStyle={{ backgroundColor: "#5b8def" }}
            customSquareStyles={combinedSquareStyles}
            customArrows={arrows as any}
          />
        </div>
      </div>
    </div>
  );
}

function MoveTimeline({
  rows,
  selected,
  onSelect,
  variations,
  showAdvanced,
  onSelectVariation,
  onPreviewLine
}: {
  rows: Array<{
    moveNumber: number;
    white?: TimelineNode & { label?: string };
    black?: TimelineNode & { label?: string };
  }>;
  selected?: number;
  onSelect: (ply: number) => void;
  variations?: Record<number, VariationEntry[]>;
  showAdvanced?: boolean;
  onSelectVariation?: (entry: VariationEntry | null) => void;
  onPreviewLine?: (fenBefore?: string, pv?: string[], label?: string) => void;
}) {
  const [expandedVariation, setExpandedVariation] = useState<string | null>(null);

  return (
    <div className="glass-card rounded-2xl p-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-white/80">Move list</h3>
        <span className="text-xs text-white/60">Tap to inspect</span>
      </div>
      <div className="mt-3 overflow-hidden rounded-xl border border-white/10">
        <table className="w-full text-sm text-white">
          <thead className="bg-white/5 text-xs uppercase tracking-[0.16em] text-white/60">
            <tr>
              <th className="px-3 py-2 text-left">#</th>
              <th className="px-3 py-2 text-left">White</th>
              <th className="px-3 py-2 text-left">Black</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => {
              const variationGroups =
                variations &&
                [row.white?.ply, row.black?.ply]
                  .filter((p): p is number => typeof p === "number")
                  .map((p) => ({ ply: p, vars: variations[p] ?? [] }))
                  .filter((g) => g.vars.length);
              return (
                <React.Fragment key={row.moveNumber}>
                  <tr className="border-t border-white/5">
                    <td className="px-3 py-2 text-xs text-white/60">{row.moveNumber}</td>
                    <MoveCell
                      move={row.white}
                      selected={selected}
                      onSelect={(ply) => {
                        onSelectVariation?.(null);
                        onSelect(ply);
                      }}
                      variationCount={row.white?.ply != null ? variations?.[row.white.ply]?.length ?? 0 : 0}
                      showAdvanced={!!showAdvanced}
                    />
                    <MoveCell
                      move={row.black}
                      selected={selected}
                      onSelect={(ply) => {
                        onSelectVariation?.(null);
                        onSelect(ply);
                      }}
                      variationCount={row.black?.ply != null ? variations?.[row.black.ply]?.length ?? 0 : 0}
                      showAdvanced={!!showAdvanced}
                    />
                  </tr>
                  {variationGroups && variationGroups.length ? (
                    <tr className="border-t border-white/5 bg-white/5">
                      <td colSpan={3} className="px-3 py-2">
                        <div className="flex flex-col gap-3">
                          {variationGroups.map((group, idx) => (
                            <div
                              key={`${row.moveNumber}-var-${group.ply}-${idx}`}
                              className="rounded-xl border border-white/10 bg-white/5 px-3 py-2"
                            >
                              {(() => {
                                const commonComment = group.vars.find((v) => v.node.comment)?.node.comment;
                                return commonComment ? (
                                  <div className="mb-2 rounded-lg bg-white/5 px-3 py-2 text-[11px] text-white/70">
                                    {commonComment}
                                  </div>
                                ) : null;
                              })()}
                              <div className="flex items-center justify-between text-xs text-white/60">
                                <div className="flex items-center gap-2">
                                  <span className="text-[11px] text-white/50">↳</span>
                                  <span className="font-semibold text-white/80">{group.vars[0].parentLabel}</span>
                                  <span className="rounded-full bg-white/10 px-2 py-0.5 text-[10px] text-white/70">
                                    Variations {group.vars.length}
                                  </span>
                                </div>
                                <div className="flex items-center gap-2 text-[11px] text-white/50">
                                  <span className="flex items-center gap-1">
                                    <span className={`h-2 w-2 rounded-full ${group.vars[0].turn === "white" ? "bg-white" : "bg-black"} border border-white/20`} />
                                    <span>{group.vars[0].turn === "white" ? "White to move" : "Black to move"}</span>
                                  </span>
                                </div>
                              </div>
                              <div className="mt-1 space-y-1">
                                {group.vars.map((v, vidx) => (
                                  <div
                                    key={`${group.ply}-${vidx}`}
                                    className={`w-full rounded-lg px-2 py-1 text-left transition ${selected === v.node.ply && expandedVariation === `${group.ply}-${vidx}`
                                      ? "bg-white/10 ring-1 ring-accent-teal/50"
                                      : "hover:bg-white/5"
                                      }`}
                                  >
                                    <button
                                      onClick={() => {
                                        onSelectVariation?.(v);
                                        onSelect(v.node.ply);
                                        const key = `${group.ply}-${vidx}`;
                                        setExpandedVariation((prev) => (prev === key ? null : key));
                                      }}
                                      className="flex w-full items-center justify-between gap-2 text-left"
                                    >
                                      <div className="flex flex-wrap items-center gap-2">
                                        <span className="text-white">{formatSanHuman(v.node.san)}</span>
                                        {v.node.glyph ? <span className="text-xs text-white/60">{v.node.glyph}</span> : null}
                                        <span className="rounded-full bg-white/10 px-2 py-0.5 text-[10px] text-white/70">
                                          Line {v.pvIndex ?? vidx + 1}
                                        </span>
                                      </div>
                                      <div className="flex items-center gap-2">
                                        <span className="text-xs text-accent-teal">{formatEvalValue(v.node.eval, v.evalKind, v.turn)}</span>
                                        <span className="text-xs text-white/50">{expandedVariation === `${group.ply}-${vidx}` ? "Hide" : "Show"}</span>
                                      </div>
                                    </button>
                                    {expandedVariation === `${group.ply}-${vidx}` ? (
                                      <div className="mt-1 space-y-1 rounded-md bg-white/5 p-2">
                                        {v.node.pv?.length ? (
                                          <div className="flex flex-wrap items-center gap-2 text-[11px] text-white/60">
                                            <button
                                              className="rounded-full bg-accent-teal/20 px-2 py-0.5 text-white"
                                              onClick={() => {
                                                onPreviewLine?.(v.parentFenBefore, v.node.pv, `Preview: ${v.node.san} (${v.parentLabel})`);
                                              }}
                                            >
                                              Play line
                                            </button>
                                            <div className="flex flex-wrap gap-1">
                                              {convertPvToSan(v.parentFenBefore, v.node.pv).map((m, idxPv) => (
                                                <span
                                                  key={`${group.ply}-${vidx}-pv-${idxPv}`}
                                                  className={`rounded-full px-2 py-0.5 ${idxPv === 0 ? "bg-white/15 text-white" : "bg-white/10 text-white/70"
                                                    }`}
                                                  onClick={() => onSelect(v.node.ply)}
                                                  role="button"
                                                  style={{ cursor: "pointer" }}
                                                >
                                                  {formatSanHuman(m)}
                                                </span>
                                              ))}
                                            </div>
                                          </div>
                                        ) : (
                                          <div className="text-[11px] text-white/50">No engine line available for this variation.</div>
                                        )}
                                        <div className="text-[11px] text-white/50 flex flex-wrap items-center gap-2">
                                          <span>{v.turn === "white" ? "White" : "Black"} perspective</span>
                                          <span className="rounded-full bg-white/5 px-2 py-0.5">Line {v.pvIndex ?? vidx + 1}</span>
                                          {!v.node.comment ? <span className="rounded-full bg-white/5 px-2 py-0.5 text-white/60">No comment</span> : null}
                                        </div>
                                      </div>
                                    ) : null}
                                  </div>
                                ))}
                              </div>
                            </div>
                          ))}
                        </div>
                      </td>
                    </tr>
                  ) : null}
                </React.Fragment>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function MoveCell({
  move,
  selected,
  onSelect,
  variationCount,
  showAdvanced
}: {
  move?: TimelineNode & { label?: string };
  selected?: number;
  onSelect: (ply: number) => void;
  variationCount?: number;
  showAdvanced?: boolean;
}) {
  if (!move) return <td className="px-3 py-2 text-white/40">—</td>;
  const judgement = (move.special as keyof typeof judgementMarks) ?? move.judgement ?? "good";
  const mark = judgementMarks[judgement] ?? "";
  const sanDisplay = formatSanHuman(move.san); // avoid repeating move number; column already shows move #
  const depth = move.evalBeforeDeep?.depth;
  const multiPv = move.evalBeforeDeep?.lines?.length;
  const delta = formatDeltaWithSide(move.deltaWinPct, move.turn);
  const phase = humanizeTag(move.phaseLabel);
  const showDelta =
    Math.abs(move.deltaWinPct ?? 0) >= 1 ||
    judgement === "blunder" ||
    judgement === "mistake" ||
    judgement === "inaccuracy";
  const isBook = judgement === "book";
  const deltaText = showDelta ? delta.text.replace(/\(.*?\)/, "").trim() : "";
  return (
    <td className="px-3 py-2 align-top">
      <button
        onClick={() => onSelect(move.ply)}
        className={`w-full rounded-lg border border-white/5 px-2 py-2 text-left transition ${selected === move.ply ? "bg-white/10 ring-1 ring-accent-teal/60" : "bg-transparent hover:bg-white/5"
          }`}
        title={move.shortComment}
      >
        <div className="flex flex-col gap-0.5">
          <div className="flex items-center justify-between gap-2">
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-sm font-semibold text-white">{sanDisplay}</span>
              {!isBook && mark ? (
                <span
                  className={`rounded-full px-2 py-0.5 text-[11px] ${judgementColors[judgement] ?? "bg-white/10 text-white"
                    }`}
                >
                  {mark}
                </span>
              ) : null}
              {phase ? <span className="rounded-full bg-accent-teal/15 px-2 py-0.5 text-[10px] text-accent-teal/80">{displayTag(phase)}</span> : null}
              {variationCount ? (
                <span className="rounded-full bg-white/10 px-2 py-0.5 text-[10px] text-white/70">Variations {variationCount}</span>
              ) : null}
            </div>
            {showDelta && !isBook ? <span className={`text-[11px] ${delta.tone}`}>{deltaText}</span> : null}
          </div>
          {/* Engine depth/multipv are global; avoid repeating per-move badges */}
          {move.shortComment ? <div className="text-[11px] text-white/70">{move.shortComment}</div> : null}
        </div>
      </button>
    </td>
  );
}

function CriticalList({
  critical,
  fenBeforeByPly,
  onSelectPly
}: {
  critical: CriticalNode[];
  fenBeforeByPly: Record<number, string | undefined>;
  onSelectPly?: (ply: number) => void;
}) {
  return (
    <div className="glass-card rounded-2xl p-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-white/80">Critical moments</h3>
        <span className="text-xs text-white/60">Key swings & ideas</span>
      </div>
      <div className="mt-3 space-y-3">
        {critical.map((c) => (
          <CriticalMomentCard key={c.ply} critical={c} fenBefore={fenBeforeByPly[c.ply]} onSelectPly={onSelectPly} />
        ))}
      </div>
    </div>
  );
}




function OpeningLookupPanel({ stats, loading, error }: { stats?: OpeningStats | null; loading: boolean; error?: string | null }) {
  if (loading) {
    return (
      <div className="glass-card rounded-2xl p-4">
        <div className="animate-pulse space-y-2 text-sm text-white/60">Opening lookup…</div>
      </div>
    );
  }
  if (error) {
    return (
      <div className="glass-card rounded-2xl border border-rose-500/40 bg-rose-500/10 p-4 text-sm text-rose-100">
        Opening lookup 실패: {error}
      </div>
    );
  }
  if (!stats) {
    return (
      <div className="glass-card rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">
        Opening DB 데이터를 찾지 못했습니다.
      </div>
    );
  }
  const hasMoves = stats.topMoves && stats.topMoves.length > 0;
  const hasGames = stats.topGames && stats.topGames.length > 0;
  const totalGames =
    stats.games ??
    Math.max(
      1,
      ...(stats.topMoves ?? []).map((m) => m.games ?? 0)
    );
  const totalWin = stats.winWhite != null ? stats.winWhite : null;
  const totalDraw = stats.draw != null ? stats.draw : null;
  const totalLose = stats.winBlack != null ? stats.winBlack : totalWin != null && totalDraw != null ? 1 - totalWin - totalDraw : null;

  const renderBars = (w?: number, d?: number, b?: number) => {
    const ww = w != null ? Math.max(0, Math.min(1, w)) : 0;
    const dd = d != null ? Math.max(0, Math.min(1, d)) : 0;
    const bb = b != null ? Math.max(0, Math.min(1, b)) : Math.max(0, 1 - ww - dd);
    return (
      <div className="h-2 overflow-hidden rounded-full bg-white/10">
        <div className="flex h-full w-full">
          <div className="h-full bg-emerald-400/80" style={{ width: `${ww * 100}%` }} />
          <div className="h-full bg-white/60" style={{ width: `${dd * 100}%` }} />
          <div className="h-full bg-rose-400/80" style={{ width: `${bb * 100}%` }} />
        </div>
      </div>
    );
  };

  const renderRow = (label: string, games: number, w?: number, d?: number, b?: number) => {
    return (
      <div className="rounded-xl bg-white/5 px-3 py-2">
        <div className="flex items-center justify-between text-xs text-white/70">
          <span className="font-semibold text-white">{label}</span>
          <span>
            {games} games
            {w != null ? ` · W ${(w * 100).toFixed(1)}%` : ""} {d != null ? ` · D ${(d * 100).toFixed(1)}%` : ""}
          </span>
        </div>
        <div className="mt-1 flex items-center justify-between text-[11px] text-white/60">
          <span>White</span>
          <span>Draw</span>
          <span>Black</span>
        </div>
        {renderBars(w, d, b)}
      </div>
    );
  };

  return (
    <div className="glass-card rounded-2xl p-4">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.14em] text-white/60">Opening DB</p>
          <h3 className="font-display text-lg text-white">현재 국면 통계</h3>
        </div>
        <div className="flex flex-col items-end text-[11px] text-white/60">
          <span>Book ply {stats.bookPly}</span>
          <span>Novelty {stats.noveltyPly}</span>
          {stats.games ? <span>Games {stats.games}</span> : null}
        </div>
      </div>
      {hasMoves ? (
        <div className="mt-3 space-y-2">
          <div className="flex items-center justify-between text-xs uppercase tracking-[0.14em] text-white/60">
            <span>다음 수 분포</span>
            <span className="rounded-full bg-white/10 px-2 py-0.5 text-[11px] text-white/70">Σ {totalGames} games</span>
          </div>
          {renderRow("Σ (All)", totalGames, totalWin ?? undefined, totalDraw ?? undefined, totalLose ?? undefined)}
          {stats.topMoves?.map((m) => {
            const w = m.winPct != null ? m.winPct : undefined;
            const d = m.drawPct != null ? m.drawPct : undefined;
            const b = w != null && d != null ? Math.max(0, 1 - w - d) : undefined;
            return renderRow(m.san, m.games ?? 0, w, d, b);
          })}
        </div>
      ) : null}
      {hasGames ? (
        <div className="mt-4 space-y-1">
          <div className="text-[11px] uppercase tracking-[0.14em] text-white/60">상위 대국</div>
          <div className="space-y-1 text-xs text-white/80">
            {stats.topGames?.slice(0, 10).map((g, idx) => (
              <div key={`${g.white}-${g.black}-${idx}`} className="rounded-lg bg-white/5 px-2 py-2">
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-white">
                    {g.white} {g.whiteElo ? `(${g.whiteElo})` : ""}
                  </span>
                  <span className="text-[11px] text-white/60">{g.date ?? ""}</span>
                </div>
                <div className="flex items-center justify-between">
                  <span className="font-semibold text-white">
                    {g.black} {g.blackElo ? `(${g.blackElo})` : ""}
                  </span>
                  <span className="rounded-full bg-white/10 px-2 py-0.5 text-[11px] text-white/80">{g.result}</span>
                </div>
                {g.event ? <div className="text-[11px] text-white/50">{g.event}</div> : null}
              </div>
            ))}
          </div>
        </div>
      ) : null}
    </div>
  );
}

function BlunderTimeline({
  timeline,
  selected,
  onSelect
}: {
  timeline: (TimelineNode & { label?: string })[];
  selected?: number;
  onSelect: (ply: number) => void;
}) {
  const judgmentsOrder = ["brilliant", "great", "blunder", "mistake", "inaccuracy", "good", "best", "book"];
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

function MoveControls({
  timeline,
  selected,
  onSelect
}: {
  timeline: (TimelineNode & { label?: string })[];
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

function TreeView({
  root,
  onSelect,
  selected
}: {
  root?: ReviewTreeNode;
  onSelect: (ply: number) => void;
  selected?: number;
}) {
  if (!root) return null;

  const renderNode = (node: ReviewTreeNode, depth: number, isMainline: boolean): JSX.Element => {
    const mainlineChildren = node.children.filter((c) => c.judgement !== "variation");
    const variations = node.children.filter((c) => c.judgement === "variation");
    const isSelected = selected === node.ply;
    return (
      <div key={`${node.ply}-${node.uci}-${depth}`} className="mb-1">
        <button
          onClick={() => onSelect(node.ply)}
          className={`group flex w-full items-center gap-2 rounded-lg px-2 py-1 text-left ${isSelected ? "bg-white/10 ring-1 ring-accent-teal/60" : "hover:bg-white/5"
            }`}
        >
          <span className="text-[10px] text-white/50" style={{ width: 34, visibility: isMainline ? "visible" : "hidden" }}>
            {node.ply}.
          </span>
          <div
            className={`flex flex-1 items-center gap-2 ${isMainline ? "font-semibold text-white" : "text-white/80"
              }`}
          >
            <span className="text-white">{formatSanHuman(node.san)}</span>
            {node.glyph ? <span className="text-xs text-white/60">{node.glyph}</span> : null}
            {node.comment ? (
              <span className="truncate text-[11px] text-white/60 max-w-[260px]">{node.comment}</span>
            ) : null}
          </div>
        </button>
        {variations.length ? (
          <div className="ml-4 space-y-1 border-l border-white/10 pl-3">
            {variations.map((v, idx) => (
              <div key={`${v.ply}-var-${idx}`} className="flex items-start gap-2">
                <span className="text-[11px] text-white/50">↳</span>
                <button
                  onClick={() => onSelect(v.ply)}
                  className={`flex flex-col rounded-lg px-2 py-1 text-left ${selected === v.ply ? "bg-white/10 ring-1 ring-accent-teal/50" : "hover:bg-white/5"
                    }`}
                >
                  <div className="flex items-center gap-2 text-sm text-white/80">
                    <span className="text-white">{formatSanHuman(v.san)}</span>
                    {v.glyph ? <span className="text-xs text-white/60">{v.glyph}</span> : null}
                  </div>
                  {v.pv?.length ? (
                    <div className="text-[11px] text-white/50">{formatPvList(v.pv)}</div>
                  ) : null}
                  {v.comment ? <div className="text-[11px] text-white/60">{v.comment}</div> : null}
                </button>
              </div>
            ))}
          </div>
        ) : null}
        {mainlineChildren.length
          ? mainlineChildren.map((c) => (
            <div key={`${c.ply}-main`} className="ml-2 border-l border-white/5 pl-2">
              {renderNode(c, depth + 1, true)}
            </div>
          ))
          : null}
      </div>
    );
  };

  return (
    <div className="glass-card rounded-2xl p-4">
      <div className="mb-2 flex items-center justify-between">
        <h3 className="text-sm font-semibold text-white/80">Tree</h3>
        <span className="text-xs text-white/60">mainline + variations</span>
      </div>
      <div className="max-h-[360px] overflow-y-auto pr-2">{renderNode(root, 0, true)}</div>
    </div>
  );
}

function SummaryPanel({
  opening,
  openingStats,
  oppositeColorBishops,
  concepts,
  conceptSpikes,
  showAdvanced,
  summaryText,
  openingSummary,
  bookExitComment,
  openingTrend,
  onSelectPly
}: {
  opening?: Review["opening"];
  openingStats?: Review["openingStats"];
  oppositeColorBishops?: boolean;
  concepts?: Concepts;
  conceptSpikes?: Array<{ ply: number; concept: string; delta: number; label: string }>;
  showAdvanced: boolean;
  summaryText?: string;
  openingSummary?: string;
  bookExitComment?: string;
  openingTrend?: string;
  onSelectPly?: (ply: number) => void;
}) {
  const hasTopMoves = openingStats?.topMoves && openingStats.topMoves.length > 0;
  const hasTopGames = openingStats?.topGames && openingStats.topGames.length > 0;
  const totalGames = openingStats?.games ?? 0;

  return (
    <div className="glass-card rounded-2xl p-4">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.16em] text-white/60">Summary</p>
          <h3 className="font-display text-xl text-white">Game outline</h3>
        </div>
        {oppositeColorBishops ? (
          <span className="rounded-full bg-white/10 px-3 py-1 text-xs text-white/80">Opposite-color bishops</span>
        ) : null}
        {openingStats?.source ? (
          <span className="rounded-full bg-accent-teal/15 px-3 py-1 text-xs text-accent-teal/80">DB: {openingStats.source}</span>
        ) : null}
      </div>
      <div className="mt-4 space-y-2 text-sm text-white/80">
        {opening?.name ? (
          <div className="flex items-center justify-between rounded-xl border border-white/10 bg-white/5 px-3 py-2">
            <div>
              <div className="text-white">{opening.name}</div>
              <div className="text-xs text-white/60">ECO {opening.eco ?? "—"}</div>
            </div>
            <div className="flex flex-col items-end gap-1 text-right text-xs text-white/60">
              {opening.ply ? <span>{opening.ply} ply</span> : null}
              {openingStats ? <span>Novelty @ {openingStats.noveltyPly}</span> : null}
              {openingStats?.source ? <span className="text-accent-teal/80">DB {openingStats.source}</span> : null}
            </div>
          </div>
        ) : (
          <p className="rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-white/70">Opening unknown</p>
        )}
        {openingSummary || bookExitComment || openingTrend ? (
          <div className="rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white/80">
            {openingSummary ? <div className="text-white/90">{openingSummary}</div> : null}
            {bookExitComment ? <div className="text-white/70">{bookExitComment}</div> : null}
            {openingTrend ? <div className="text-white/70">{openingTrend}</div> : null}
          </div>
        ) : null}
        {openingStats ? (
          <div className="rounded-xl border border-white/10 bg-white/5 px-3 py-2">
            <div className="mb-2 flex flex-wrap items-center gap-2 text-[11px] text-white/70">
              <span className="rounded-full bg-white/10 px-2 py-1">Book ply {openingStats.bookPly}</span>
              <span className="rounded-full bg-white/10 px-2 py-1">Novelty {openingStats.noveltyPly}</span>
              {totalGames ? <span className="rounded-full bg-white/10 px-2 py-1">Games {totalGames}</span> : null}
              {openingStats.freq != null ? (
                <span className="rounded-full bg-white/10 px-2 py-1">Master freq {openingStats.freq.toFixed(1)}%</span>
              ) : null}
              {openingStats.winWhite != null || openingStats.winBlack != null || openingStats.draw != null ? (
                <span className="rounded-full bg-white/10 px-2 py-1">
                  W {openingStats.winWhite?.toFixed(1) ?? "—"} / D {openingStats.draw?.toFixed(1) ?? "—"} / L{" "}
                  {openingStats.winBlack?.toFixed(1) ?? "—"}%
                </span>
              ) : null}
            </div>
            {hasTopMoves ? (
              <div className="space-y-2">
                {openingStats?.topMoves?.slice(0, 6).map((m) => {
                  const games = m.games ?? 0;
                  const win = m.winPct ?? 0;
                  const draw = m.drawPct ?? 0;
                  const total = totalGames || Math.max(...(openingStats.topMoves?.map((tm) => tm.games ?? 0) ?? [1]));
                  const pct = total ? Math.max(4, (games / total) * 100) : 0;
                  return (
                    <div key={m.uci} className="space-y-1">
                      <div className="flex items-center justify-between text-xs text-white/70">
                        <span className="font-semibold text-white">{m.san}</span>
                        <span>
                          {games} games · W {win.toFixed(1)}% {draw ? ` · D ${draw.toFixed(1)}%` : ""}
                        </span>
                      </div>
                      <div className="h-2 overflow-hidden rounded-full bg-white/10">
                        <div className="h-full bg-gradient-to-r from-emerald-400 to-teal-500" style={{ width: `${Math.min(100, pct)}%` }} />
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : null}
            {hasTopGames ? (
              <div className="mt-3 space-y-1">
                <div className="text-[11px] uppercase tracking-[0.14em] text-white/60">Top games</div>
                <div className="space-y-1 text-xs text-white/80">
                  {openingStats?.topGames?.slice(0, 6).map((g, idx) => (
                    <div key={`${g.white}-${g.black}-${idx}`} className="rounded-lg bg-white/5 px-2 py-2">
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-white">
                          {g.white} {g.whiteElo ? `(${g.whiteElo})` : ""}
                        </span>
                        <span className="text-[11px] text-white/60">{g.date ?? ""}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="font-semibold text-white">
                          {g.black} {g.blackElo ? `(${g.blackElo})` : ""}
                        </span>
                        <span className="rounded-full bg-white/10 px-2 py-0.5 text-[11px] text-white/80">{g.result}</span>
                      </div>
                      {g.event ? <div className="text-[11px] text-white/50">{g.event}</div> : null}
                    </div>
                  ))}
                </div>
              </div>
            ) : null}
          </div>
        ) : null}
        {summaryText ? (
          <div className="rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white/80">
            {summaryText}
          </div>
        ) : null}
        {showAdvanced ? (
          <div className="pt-3">
            <p className="mb-2 text-xs uppercase tracking-[0.16em] text-white/60">Concept tags</p>
            <ConceptChips concepts={concepts} />
            {conceptSpikes && conceptSpikes.length ? (
              <div className="pt-3">
                <p className="mb-2 text-xs uppercase tracking-[0.16em] text-white/60">Concept spikes</p>
                <div className="flex flex-wrap gap-2">
                  {conceptSpikes.map((s) => (
                    <span
                      key={`${s.concept}-${s.ply}`}
                      className="rounded-full bg-white/10 px-3 py-1 text-xs text-white/80"
                      title={`Ply ${s.ply} ${s.label}`}
                      onClick={() => onSelectPly?.(s.ply)}
                      role="button"
                      style={{ cursor: onSelectPly ? "pointer" : "default" }}
                    >
                      {displayTag(s.concept)} +{s.delta.toFixed(2)} @ {s.label}
                    </span>
                  ))}
                </div>
              </div>
            ) : null}
          </div>
        ) : null}
      </div>
    </div>
  );
}

function StudyPanel({ chapters, onSelect }: { chapters?: StudyChapter[]; onSelect: (ply: number) => void }) {
  if (!chapters || !chapters.length) return null;
  return (
    <div className="glass-card rounded-2xl p-4">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.14em] text-white/60">Chapters</p>
          <h3 className="text-sm font-semibold text-white/90">Training chapters</h3>
        </div>
        <span className="rounded-full bg-white/10 px-2 py-1 text-[11px] text-white/70">#{chapters.length}</span>
      </div>
      <div className="mt-3 space-y-3">
        {chapters.map((ch) => (
          <button
            key={ch.id}
            onClick={() => onSelect(ch.anchorPly)}
            className="w-full rounded-xl border border-white/10 bg-white/5 p-3 text-left hover:border-accent-teal/40"
          >
            <div className="flex items-center justify-between text-xs text-white/70">
              <div className="flex items-center gap-2">
                <span className="font-semibold text-white">Ply {ch.anchorPly}</span>
                <span className="rounded-full bg-white/10 px-2 py-0.5 text-[11px] text-white/70">
                  Study score / Quality {ch.studyScore?.toFixed?.(1) ?? "–"}
                </span>
              </div>
              <span className={`rounded-full px-2 py-0.5 text-[11px] ${ch.deltaWinPct < 0 ? "bg-rose-500/15 text-rose-100" : "bg-emerald-500/15 text-emerald-100"}`}>
                Δ {formatDelta(ch.deltaWinPct)}
              </span>
            </div>
            <div className="mt-1 text-sm text-white">
              {formatSanHuman(ch.played)}
              {ch.best ? (
                <span className="text-xs text-white/60">
                  {" "}
                  vs {formatSanHuman(ch.best)}
                </span>
              ) : null}
            </div>
            <div className="mt-1 flex flex-wrap gap-2 text-[11px] text-white/70">
              {ch.tags.slice(0, 5).map((t) => (
                <span key={t} className="rounded-full bg-white/10 px-2 py-0.5">
                  {displayTag(t)}
                </span>
              ))}
            </div>
            <div className="mt-2 space-y-1 text-[11px] text-white/70">
              {ch.lines.map((l) => (
                <div key={l.label} className="flex items-center gap-2">
                  <span className="rounded-full bg-white/10 px-2 py-0.5 text-white/80">
                    {l.label === "played"
                      ? "You should've played"
                      : l.label === "engine"
                        ? "Best move"
                        : l.label === "alt"
                          ? "Practical alternative"
                          : humanizeTag(l.label)}
                  </span>
                  <span className="text-white/80">{l.pv.map(formatSanHuman).join(" ")}</span>
                  <span className="text-white/50">{l.winPct.toFixed(1)}%</span>
                </div>
              ))}
            </div>
            {ch.summary ? (
              <p className="mt-2 text-sm text-white/80">{ch.summary}</p>
            ) : (
              <p className="mt-2 text-xs text-white/50">No summary (server fallback).</p>
            )}
          </button>
        ))}
      </div>
    </div>
  );
}

function SummaryHero({ timeline, critical, review }: { timeline: (TimelineNode & { label?: string })[]; critical: CriticalNode[]; review?: Review }) {
  type SummaryStats = {
    total: number;
    counts: Record<string, number>;
    worst: { label: string; delta: number } | null;
    topCritical?: { ply: number; reason: string } | null;
  };
  const stats = useMemo<SummaryStats>(() => {
    const counts = { blunder: 0, mistake: 0, inaccuracy: 0, good: 0, best: 0 };
    let worstDelta = 0;
    let worstMove: { label: string; delta: number } | null = null;
    timeline.forEach((t) => {
      const j = t.judgement?.toLowerCase() ?? "good";
      if (j === "blunder") counts.blunder++;
      else if (j === "mistake") counts.mistake++;
      else if (j === "inaccuracy") counts.inaccuracy++;
      else if (j === "best") counts.best++;
      else counts.good++;
      if (t.deltaWinPct && t.deltaWinPct < worstDelta) {
        worstDelta = t.deltaWinPct;
        const moveNumber = Math.ceil(t.ply / 2);
        const turnPrefix = t.ply % 2 === 1 ? "." : "...";
        worstMove = { label: `${moveNumber}${turnPrefix} ${t.san}`, delta: t.deltaWinPct };
      }
    });
    const topCritical = critical.length
      ? { ply: critical[0].ply, reason: critical[0].reason.split(":").pop()?.trim() ?? critical[0].reason }
      : null;
    return { total: timeline.length, counts, worst: worstMove, topCritical };
  }, [timeline, critical]);

  return (
    <div className="glass-card mb-4 rounded-2xl p-4">
      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-white/60">Game summary</p>
          <h2 className="font-display text-2xl text-white">At a glance</h2>
        </div>
        <div className="flex flex-wrap gap-2 text-xs">
          <span className="rounded-full bg-white/10 px-3 py-1">Moves: {stats.total}</span>
          <span className="rounded-full bg-rose-500/15 px-3 py-1 text-rose-100">Blunder {stats.counts.blunder}</span>
          <span className="rounded-full bg-orange-500/15 px-3 py-1 text-orange-100">Mistake {stats.counts.mistake}</span>
          <span className="rounded-full bg-amber-500/15 px-3 py-1 text-amber-100">Inacc {stats.counts.inaccuracy}</span>
        </div>
      </div>
      <div className="mt-4 grid gap-3 md:grid-cols-4">
        <div className="rounded-2xl border border-white/10 bg-white/5 p-3">
          <p className="text-xs uppercase tracking-[0.14em] text-white/60">Accuracy (White)</p>
          {review?.accuracyWhite != null ? (
            <>
              <div className="text-xl font-bold text-accent-teal">{review.accuracyWhite.toFixed(1)}%</div>
              <div className="text-xs text-white/60">Overall precision</div>
            </>
          ) : (
            <p className="text-xs text-white/60">No data</p>
          )}
        </div>
        <div className="rounded-2xl border border-white/10 bg-white/5 p-3">
          <p className="text-xs uppercase tracking-[0.14em] text-white/60">Accuracy (Black)</p>
          {review?.accuracyBlack != null ? (
            <>
              <div className="text-xl font-bold text-accent-teal">{review.accuracyBlack.toFixed(1)}%</div>
              <div className="text-xs text-white/60">Overall precision</div>
            </>
          ) : (
            <p className="text-xs text-white/60">No data</p>
          )}
        </div>
        <div className="rounded-2xl border border-white/10 bg-white/5 p-3">
          <p className="text-xs uppercase tracking-[0.14em] text-white/60">Biggest drop</p>
          {stats.worst ? (
            <>
              <div className="text-sm font-semibold text-white">{stats.worst.label}</div>
              <div className="text-xs text-rose-200">{formatDelta(stats.worst.delta)}</div>
            </>
          ) : (
            <p className="text-xs text-white/60">None</p>
          )}
        </div>
        <div className="rounded-2xl border border-white/10 bg-white/5 p-3">
          <p className="text-xs uppercase tracking-[0.14em] text-white/60">Critical</p>
          {stats.topCritical ? (
            <>
              <div className="text-sm font-semibold text-white">Ply {stats.topCritical.ply}</div>
              <div className="text-xs text-white/70">{stats.topCritical.reason}</div>
            </>
          ) : (
            <p className="text-xs text-white/60">None</p>
          )}
        </div>
      </div>
    </div>
  );
}

export default function ReviewClient({ reviewId }: { reviewId: string }) {
  const [review, setReview] = useState<Review | null>(null);
  const [selectedPly, setSelectedPly] = useState<number | null>(null);
  const [selectedVariation, setSelectedVariation] = useState<VariationEntry | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [pendingMessage, setPendingMessage] = useState<string | null>(null);
  const [pollStartTime, setPollStartTime] = useState<number | null>(null);
  const [pollAttempt, setPollAttempt] = useState<number>(0);
  const [showAdvanced] = useState<boolean>(true);
  const [openingLookup, setOpeningLookup] = useState<OpeningStats | null>(null);
  const [lookupKey, setLookupKey] = useState<string>("");
  const [lookupError, setLookupError] = useState<string | null>(null);
  const [lookupLoading, setLookupLoading] = useState<boolean>(false);
  const [branchSaving, setBranchSaving] = useState<boolean>(false);
  const [branchError, setBranchError] = useState<string | null>(null);
  const [showStudy] = useState<boolean>(true);
  const [previewFen, setPreviewFen] = useState<string | null>(null);
  const [previewArrows, setPreviewArrows] = useState<Array<[string, string, string?]>>([]);
  const [previewLabel, setPreviewLabel] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<"engine" | "opening" | "study" | "concepts">("concepts");
  const [drawingColor, setDrawingColor] = useState<"green" | "red" | "blue" | "orange">("green");
  const jobId = review?.jobId ?? reviewId;
  const [instantPgn, setInstantPgn] = useState<string | null>(null);

  // Engine & Interactive State
  const [engine, setEngine] = useState<StockfishEngine | null>(null);
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [engineLines, setEngineLines] = useState<EngineMessage[]>([]);
  // const [localGame, setLocalGame] = useState<Chess>(new Chess()); // Use enhancedTimeline for state source
  const [customArrows, setCustomArrows] = useState<Array<[string, string, string?]>>([]); // TODO: Implement drawing

  // Study Mode State
  const [isGuessing, setIsGuessing] = useState(false);
  const [guessState, setGuessState] = useState<"waiting" | "correct" | "incorrect" | "giveup">("waiting");
  const [guessFeedback, setGuessFeedback] = useState<string | undefined>();

  useEffect(() => {
    // Check for pending PGN in localStorage for instant display
    const pendingPgn = localStorage.getItem("pending-pgn");
    if (pendingPgn) {
      setInstantPgn(pendingPgn);
      // Clear after reading
      localStorage.removeItem("pending-pgn");
    }
  }, []);

  useEffect(() => {
    const eng = new StockfishEngine((msg) => {
      if (msg.pv) {
        setEngineLines((prev) => {
          // Accumulate up to 3 unique PV lines (MultiPV)
          const existing = prev.find(l => l.pv === msg.pv);
          if (existing) {
            // Update existing line
            return prev.map(l => l.pv === msg.pv ? msg : l);
          } else {
            // Add new line, keep top 3 by depth
            const updated = [...prev, msg].sort((a, b) => (b.depth || 0) - (a.depth || 0));
            return updated.slice(0, 3);
          }
        });
      }
    });
    setEngine(eng);
    return () => eng.stop();
  }, []);

  useEffect(() => {
    if (isAnalyzing && engine && review) {
      let currentFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
      if (previewFen) {
        currentFen = previewFen;
      } else if (selectedPly !== null) {
        const node = review.timeline.find(t => t.ply === selectedPly);
        if (node) currentFen = node.fen;
      } else if (review.timeline?.length) {
        currentFen = review.timeline[review.timeline.length - 1].fen;
      }
      // Enable MultiPV (3 lines)
      setEngineLines([]); // Clear previous lines for new position
      engine.analyze(currentFen, 18, 3);
    } else {
      engine?.stop();
    }
  }, [isAnalyzing, selectedPly, review, engine, previewFen]);

  const toggleAnalysis = () => {
    if (!isAnalyzing) {
      engine?.start();
      setIsAnalyzing(true);
    } else {
      engine?.stop();
      setIsAnalyzing(false);
      setEngineLines([]);
    }
  };

  const handleSaveGame = async () => {
    if (!review) return;
    try {
      const res = await fetch("/game/save", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ id: reviewId, pgn: review.pgn }) // simplistic save
      });
      if (res.ok) alert("Game saved!");
      else alert("Save failed");
    } catch (e) {
      alert("Save failed");
    }
  };

  const clearPreview = useCallback(() => {
    setPreviewFen(null);
    setPreviewArrows([]);
    setPreviewLabel(null);
  }, []);

  useEffect(() => {
    let mounted = true;
    let timer: NodeJS.Timeout | null = null;

    const poll = async (attempt: number) => {
      if (!mounted) return;
      setPollAttempt(attempt);
      try {
        const res = await fetchReview(reviewId);
        if (res.status === "pending") {
          setPendingMessage("Analyzing game...");
          setLoading(true);
          // poll up to ~80 attempts (~120s)
          if (attempt < 80) {
            timer = setTimeout(() => poll(attempt + 1), 1500);
          } else {
            setPendingMessage("Analysis taking longer than expected. Please refresh later.");
            setLoading(false);
          }
          return;
        }
        // ready
        setPendingMessage(null);
        setPollStartTime(null);
        setPollAttempt(0);
        setReview(res.review);
        const lastPly = res.review.timeline?.at(-1)?.ply ?? null;
        setSelectedPly(lastPly);
        setLoading(false);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to fetch review");
        setLoading(false);
      }
    };

    setLoading(true);
    setError(null);
    setPendingMessage(null);
    setPollStartTime(Date.now());
    setPollAttempt(0);
    poll(0);

    return () => {
      mounted = false;
      if (timer) clearTimeout(timer);
    };
  }, [reviewId]);

  const enhancedTimeline = useMemo(() => {
    const base = review?.timeline ?? [];
    if (!base.length) return [];
    const sorted = [...base].sort((a, b) => a.ply - b.ply);
    const chess = new Chess();
    const result: Array<TimelineNode & { label: string; fenBefore: string }> = [];
    sorted.forEach((t) => {
      const moveNumber = Math.ceil(t.ply / 2);
      const turnPrefix = t.ply % 2 === 1 ? "." : "...";
      const label = `${moveNumber}${turnPrefix} ${t.san}`;
      const fenBefore = t.fenBefore || chess.fen();
      try {
        chess.load(fenBefore);
      } catch {
        // ignore load errors
      }
      // advance state to keep chess in sync for later SAN conversions if needed
      const from = t.uci.slice(0, 2);
      const to = t.uci.slice(2, 4);
      const promotion = t.uci.length > 4 ? t.uci.slice(4) : undefined;
      try {
        chess.move({ from, to, promotion });
      } catch {
        // ignore
      }
      result.push({ ...t, label, fenBefore });
    });
    return result;
  }, [review]);

  // Build SAN sequence up to selected ply for opening lookup
  const sanSequence = useMemo(() => {
    if (!enhancedTimeline.length) return [] as string[];
    const sorted = [...enhancedTimeline].sort((a, b) => a.ply - b.ply);
    const cutoff = selectedPly ?? sorted[sorted.length - 1]?.ply ?? 0;
    return sorted
      .filter((t) => t.ply <= cutoff)
      .map((t) => t.san)
      .filter(Boolean);
  }, [enhancedTimeline, selectedPly]);

  const moveRows = useMemo(() => {
    const rows: Array<{
      moveNumber: number;
      white?: TimelineNode & { label?: string };
      black?: TimelineNode & { label?: string };
    }> = [];
    enhancedTimeline.forEach((t) => {
      const moveNum = Math.ceil(t.ply / 2);
      const isWhite = t.ply % 2 === 1; // ply parity determines side
      const row = rows.find((r) => r.moveNumber === moveNum) ?? { moveNumber: moveNum };
      if (isWhite) row.white = t;
      else row.black = t;
      if (!rows.find((r) => r.moveNumber === moveNum)) rows.push(row);
    });
    return rows;
  }, [enhancedTimeline]);

  const conceptSpikes = useMemo(() => {
    const spikes: Array<{ ply: number; concept: string; delta: number; label: string }> = [];
    for (let i = 1; i < enhancedTimeline.length; i++) {
      const prev = enhancedTimeline[i - 1];
      const cur = enhancedTimeline[i];
      if (!cur.concepts) continue;
      Object.keys(cur.concepts).forEach((k) => {
        const key = k as keyof Concepts;
        const deltaFromEngine = cur.conceptDelta?.[key];
        const curVal = cur.concepts?.[key];
        const prevVal = prev.concepts?.[key];
        const delta =
          typeof deltaFromEngine === "number"
            ? deltaFromEngine
            : typeof curVal === "number" && typeof prevVal === "number"
              ? curVal - prevVal
              : null;
        if (typeof delta === "number" && delta >= 0.25) {
          spikes.push({ ply: cur.ply, concept: key, delta, label: cur.label ?? cur.san });
        }
      });
    }
    return spikes.sort((a, b) => b.delta - a.delta).slice(0, 5);
  }, [enhancedTimeline]);

  const fenBeforeByPly = useMemo(() => Object.fromEntries(enhancedTimeline.map((t) => [t.ply, t.fenBefore as string | undefined])), [enhancedTimeline]);

  const selected = useMemo(() => {
    if (!enhancedTimeline.length) return null;
    if (selectedPly === null) return enhancedTimeline[enhancedTimeline.length - 1];
    return enhancedTimeline.find((t) => t.ply === selectedPly) ?? enhancedTimeline[enhancedTimeline.length - 1];
  }, [selectedPly, enhancedTimeline]);

  const activeMove = useMemo(() => {
    if (selectedVariation) {
      const parent = enhancedTimeline.find((t) => t.ply === selectedVariation.parentPly);
      if (parent) {
        return {
          ...parent,
          uci: selectedVariation.node.uci,
          san: selectedVariation.node.san,
          fen: selectedVariation.node.fen,
          judgement: selectedVariation.node.judgement as any,
          winPctAfterForPlayer: selectedVariation.node.eval,
          label: selectedVariation.parentLabel ?? parent.label,
          isCustom: true
        } as TimelineNode & { label?: string };
      }
    }
    return selected;
  }, [selectedVariation, enhancedTimeline, selected]);

  useEffect(() => {
    const fetch = async () => {
      if (!selected || !enhancedTimeline.length) return;
      const movesToPly = enhancedTimeline.filter((t) => t.ply <= selected.ply).map((t) => t.san);
      const newKey = movesToPly.join(" ");
      if (newKey === lookupKey) return; // No change
      setLookupKey(newKey);
      setLookupError(null);
      setLookupLoading(true);
      try {
        const stats = await fetchOpeningLookup(movesToPly);
        setOpeningLookup(stats);
      } catch (err) {
        setLookupError(err instanceof Error ? err.message : "Opening lookup failed");
      } finally {
        setLookupLoading(false);
      }
    };
    fetch();
  }, [selected, enhancedTimeline, lookupKey]);

  const evalPercent = activeMove
    ? activeMove.turn === "white"
      ? activeMove.winPctAfterForPlayer ?? activeMove.winPctBefore
      : activeMove.winPctAfterForPlayer != null
        ? 100 - activeMove.winPctAfterForPlayer
        : activeMove.winPctBefore != null
          ? 100 - activeMove.winPctBefore
          : undefined
    : undefined;
  const judgementBadge =
    activeMove?.special === "brilliant"
      ? "!!"
      : activeMove?.special === "great"
        ? "!"
        : activeMove?.judgement === "blunder"
          ? "??"
          : activeMove?.judgement === "mistake"
            ? "?"
            : activeMove?.judgement === "inaccuracy"
              ? "?!"
              : activeMove?.judgement === "book"
                ? "="
                : undefined;

  const variationMap = useMemo(() => {
    const map: Record<number, VariationEntry[]> = {};
    const rootNode = review?.root;
    if (!rootNode) return map;
    const timelineByPly = new Map(enhancedTimeline.map((t) => [t.ply, t]));
    const collect = (node: ReviewTreeNode) => {
      const vars = node.children?.filter((c) => c.judgement === "variation") ?? [];
      if (vars.length) {
        const parentTimeline = timelineByPly.get(node.ply);
        const parentLabel =
          parentTimeline?.label ?? `${Math.ceil(node.ply / 2)}${node.ply % 2 === 1 ? "." : "..."} ${formatSanHuman(node.san)}`;
        const depth = parentTimeline?.evalBeforeDeep?.depth;
        const pvLines = parentTimeline?.evalBeforeDeep?.lines ?? [];
        const turn = (node.ply % 2 === 1 ? "white" : "black") as "white" | "black";
        map[node.ply] = vars.map((v) => {
          const pvIdx = pvLines.findIndex((l) => l.move === v.uci || l.move === v.san);
          return {
            node: v,
            parentLabel,
            parentMoveNumber: Math.ceil(node.ply / 2),
            parentPly: node.ply,
            depth,
            pvIndex: pvIdx >= 0 ? pvIdx + 1 : undefined,
            evalKind: normalizeEvalKind(v.evalType, v.eval),
            turn,
            parentFenBefore: parentTimeline?.fenBefore
          };
        });
      }
      (node.children ?? []).filter((c) => c.judgement !== "variation").forEach(collect);
    };
    collect(rootNode);
    return map;
  }, [review?.root, enhancedTimeline]);

  const boardSquareStyles = useMemo(() => {
    const styles: Record<string, React.CSSProperties> = {};
    const highlight = (uci: string, color: "red" | "green" | "purple") => {
      if (!uci || uci.length < 4) return;
      const from = uci.slice(0, 2);
      const to = uci.slice(2, 4);
      styles[from] = { ...styles[from], animation: color === "red" ? "pulse-red 1.2s ease-in-out infinite" : color === "green" ? "pulse-green 1.2s ease-in-out infinite" : "pulse-purple 1.2s ease-in-out infinite" };
      styles[to] = { ...styles[to], animation: color === "red" ? "pulse-red 1.2s ease-in-out infinite" : color === "green" ? "pulse-green 1.2s ease-in-out infinite" : "pulse-purple 1.2s ease-in-out infinite" };
    };
    if (previewFen) return styles;
    if (activeMove) {
      const bad = activeMove.judgement === "inaccuracy" || activeMove.judgement === "mistake" || activeMove.judgement === "blunder";
      highlight(activeMove.uci, bad ? "red" : "purple");
      const best = activeMove.evalBeforeDeep?.lines?.[0]?.move;
      if (best && best !== activeMove.uci) {
        highlight(best, "green");
      }
    }
    return styles;
  }, [activeMove, previewFen]);

  const arrows = useMemo(() => {
    if (previewArrows.length) return previewArrows;
    const arr: Array<[string, string, string?]> = [...customArrows];
    if (activeMove) {
      const from = activeMove.uci.slice(0, 2);
      const to = activeMove.uci.slice(2, 4);
      const bad = activeMove.judgement === "inaccuracy" || activeMove.judgement === "mistake" || activeMove.judgement === "blunder";
      arr.push([from, to, bad ? "#f87171" : "#818cf8"]);
      // Only show best move arrow when NOT in guessing mode (Train mode)
      const best = activeMove.evalBeforeDeep?.lines?.[0]?.move;
      if (best && best !== activeMove.uci && !isGuessing) {
        const bFrom = best.slice(0, 2);
        const bTo = best.slice(2, 4);
        arr.push([bFrom, bTo, "#4ade80"]);
      }
    }
    return arr;
  }, [activeMove, previewArrows, customArrows, isGuessing]);

  const handleBoardDrop = useCallback(
    ({ sourceSquare, targetSquare }: PieceDropArgs) => {
      // 1. Guess Mode Interception
      if (isGuessing && activeMove && guessState !== "correct" && guessState !== "giveup") {
        try {
          const chess = new Chess(activeMove.fenBefore || activeMove.fen);
          const move = chess.move({ from: sourceSquare, to: targetSquare, promotion: "q" });
          if (!move) return false;

          const uci = `${move.from}${move.to}${move.promotion ?? ""}`;
          const bestMoveUci = activeMove.evalBeforeDeep?.lines?.[0]?.move;
          const playedMoveUci = activeMove.uci;

          if (bestMoveUci && (uci === bestMoveUci || (uci.length === 4 && bestMoveUci.startsWith(uci)))) {
            setGuessState("correct");
            setGuessFeedback(undefined);
            // Show the move on board
            setPreviewFen(chess.fen());
            setPreviewArrows([[move.from, move.to, "#22c55e"]]);
            return true;
          } else if (uci === playedMoveUci) {
            setGuessState("incorrect");
            setGuessFeedback("That's the move played in the game (the mistake!). Try to find a better one.");
            return false;
          } else {
            setGuessState("incorrect");
            setGuessFeedback("Not quite the best move. Try again!");
            return false;
          }
        } catch {
          return false;
        }
      }

      // 2. Standard Branch Creation
      if (!review || !enhancedTimeline.length || branchSaving) return false;
      const anchorPly = selected?.ply ?? enhancedTimeline.at(-1)?.ply;
      const anchor = enhancedTimeline.find((t) => t.ply === anchorPly);
      if (!anchor) return false;
      try {
        const chess = new Chess(anchor.fenBefore || anchor.fen);
        const move = chess.move({ from: sourceSquare, to: targetSquare, promotion: "q" });
        if (!move) return false;
        const uci = `${move.from}${move.to}${move.promotion ?? ""}`;
        const newFen = chess.fen();

        // Optimistic update: show the move immediately
        setPreviewFen(newFen);
        setBranchSaving(true);
        setBranchError(null);

        addBranch(jobId, anchor.ply, uci)
          .then((updated) => {
            setReview(updated);

            // Find the new move to select it
            // It could be in the main timeline or a variation
            // We look for a node that matches the new FEN or UCI
            let newPly: number | null = null;

            // Check timeline first (if it extended main line)
            const nextNode = updated.timeline.find(t => t.ply === anchor.ply + 1);
            if (nextNode && nextNode.uci === uci) {
              newPly = nextNode.ply;
            } else {
              // Check variations (if we have access to them in the Review object)
              // Since Review object structure for variations isn't fully visible here,
              // we rely on the fact that if it's not in timeline, it might be hard to find without the variations map.
              // However, we can try to find it by FEN in the entire timeline if it became main line.
              // If it's a variation, we might need to rely on the user manually finding it or 
              // we can try to keep previewFen until we find a better way.
              // BUT, for now, let's just clear previewFen. If the move is in variations, 
              // the user will see the anchor position and the new variation in the list.
              // Ideally we want to select it.

              // Let's try to find any node with matching FEN and ply > anchor.ply
              const candidate = updated.timeline.find(t => t.fen === newFen);
              if (candidate) newPly = candidate.ply;
            }

            if (newPly !== null) {
              setSelectedPly(newPly);
            }

            // Clear preview to show the real state
            setPreviewFen(null);
            setBranchSaving(false);
          })
          .catch((err) => {
            setBranchError(err instanceof Error ? err.message : "Failed to add branch");
            setBranchSaving(false);
            setPreviewFen(null); // Revert on error
          });
        return true;
      } catch {
        return false;
      }
    },
    [review, enhancedTimeline, branchSaving, selected, previewFen, jobId, clearPreview, isGuessing, activeMove, guessState]
  );

  useEffect(() => {
    // selecting another ply exits preview
    clearPreview();
  }, [selectedPly, clearPreview]);

  if (loading) {
    const elapsed = pollStartTime ? Math.floor((Date.now() - pollStartTime) / 1000) : 0;
    const estimatedTotal = 30; // Rough estimate: 30 seconds
    const progress = Math.min(95, (pollAttempt / 20) * 100); // Cap at 95% until done

    // If we have instant PGN, parse and show board immediately
    if (instantPgn) {
      try {
        const tempChess = new Chess();
        tempChess.loadPgn(instantPgn);
        const history = tempChess.history({ verbose: true });

        return (
          <div className="px-6 py-10 sm:px-12 lg:px-16">
            <div className="mx-auto max-w-6xl space-y-6">
              {/* Analysis Progress Banner */}
              <div className="rounded-2xl border border-accent-teal/30 bg-accent-teal/10 p-4">
                <div className="flex items-center gap-3">
                  <div className="flex gap-1">
                    {loading ? (
                      <>
                        <div className="w-2 h-2 rounded-full bg-accent-teal animate-pulse" />
                        <div className="w-2 h-2 rounded-full bg-accent-teal animate-pulse [animation-delay:0.2s]" />
                        <div className="w-2 h-2 rounded-full bg-accent-teal animate-pulse [animation-delay:0.4s]" />
                      </>
                    ) : (
                      <div className="w-2 h-2 rounded-full bg-amber-400" />
                    )}
                  </div>
                  <div className="flex-1">
                    <div className="text-sm font-semibold text-white">
                      {loading ? "Analysis in progress..." : "Analysis delayed"}
                    </div>
                    <div className="text-xs text-white/70">
                      {loading
                        ? `Board is ready. Advanced features will appear when complete. (${elapsed}s)`
                        : pendingMessage || "Still processing. You can refresh later."}
                    </div>
                  </div>
                </div>
              </div>

              {/* Basic Board View */}
              <div className="grid gap-6 lg:grid-cols-2">
                <BoardCard
                  fen={tempChess.fen()}
                  evalPercent={undefined}
                  judgementBadge={undefined}
                />
                <div className="space-y-4">
                  <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                    <h3 className="text-sm font-semibold text-white/80 mb-3">Game Moves</h3>
                    <div className="max-h-96 overflow-y-auto text-sm text-white/70">
                      {history.map((move, idx) => (
                        <span key={idx} className="mr-2">
                          {idx % 2 === 0 && `${Math.floor(idx / 2) + 1}. `}
                          {move.san}
                        </span>
                      ))}
                    </div>
                  </div>
                  <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-center text-sm text-white/60">
                    <p>💡 You can use the local engine to analyze positions while waiting</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        );
      } catch (err) {
        // Fall through to normal loading screen if PGN parsing fails
      }
    }

    return (
      <div className="px-6 py-10 sm:px-12 lg:px-16">
        <div className="mx-auto max-w-3xl space-y-6">
          {/* Progress Card */}
          <div className="rounded-3xl border border-white/10 bg-white/5 p-8">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-2xl font-semibold text-white">Analyzing Game...</h2>
              <span className="text-sm text-white/60">{elapsed}s elapsed</span>
            </div>

            {/* Progress Bar */}
            <div className="relative h-2 rounded-full bg-black/20 overflow-hidden mb-4">
              <div
                className="absolute top-0 left-0 h-full bg-gradient-to-r from-accent-teal to-blue-400 transition-all duration-500"
                style={{ width: `${progress}%` }}
              />
            </div>

            {/* Status Message */}
            {pendingMessage ? (
              <div className="flex items-center gap-3 text-white/80">
                <div className="flex gap-1">
                  <div className="w-2 h-2 rounded-full bg-accent-teal animate-pulse" />
                  <div className="w-2 h-2 rounded-full bg-accent-teal animate-pulse [animation-delay:0.2s]" />
                  <div className="w-2 h-2 rounded-full bg-accent-teal animate-pulse [animation-delay:0.4s]" />
                </div>
                <span className="text-sm">{pendingMessage}</span>
              </div>
            ) : null}

            {/* Estimated Time */}
            {elapsed < estimatedTotal && (
              <p className="mt-4 text-xs text-white/50">
                Typically completes in {estimatedTotal} seconds
              </p>
            )}
          </div>

          {/* Skeleton Preview */}
          <div className="animate-pulse space-y-4">
            <div className="grid gap-4 md:grid-cols-2">
              <div className="h-96 rounded-2xl bg-white/5" />
              <div className="h-96 rounded-2xl bg-white/5" />
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Handle Timeout/Pending state (when loading is false but review is not ready)
  if (!review && pendingMessage) {
    // Reuse the instant board logic if available
    if (instantPgn) {
      try {
        const tempChess = new Chess();
        tempChess.loadPgn(instantPgn);
        const history = tempChess.history({ verbose: true });

        return (
          <div className="px-6 py-10 sm:px-12 lg:px-16">
            <div className="mx-auto max-w-6xl space-y-6">
              <div className="rounded-2xl border border-amber-500/30 bg-amber-500/10 p-4">
                <div className="flex items-center gap-3">
                  <div className="w-2 h-2 rounded-full bg-amber-400" />
                  <div className="flex-1">
                    <div className="text-sm font-semibold text-white">Analysis taking longer than expected</div>
                    <div className="text-xs text-white/70">
                      {pendingMessage}. You can refresh later to check for results.
                    </div>
                  </div>
                </div>
              </div>

              <div className="grid gap-6 lg:grid-cols-2">
                <BoardCard
                  fen={tempChess.fen()}
                  evalPercent={undefined}
                  judgementBadge={undefined}
                />
                <div className="space-y-4">
                  <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                    <h3 className="text-sm font-semibold text-white/80 mb-3">Game Moves</h3>
                    <div className="max-h-96 overflow-y-auto text-sm text-white/70">
                      {history.map((move, idx) => (
                        <span key={idx} className="mr-2">
                          {idx % 2 === 0 && `${Math.floor(idx / 2) + 1}. `}
                          {move.san}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        );
      } catch { }
    }

    return (
      <div className="px-6 py-10 sm:px-12 lg:px-16">
        <div className="mx-auto max-w-3xl rounded-2xl border border-white/10 bg-white/5 p-6">
          <h2 className="text-xl font-semibold text-white">Analyzing...</h2>
          <p className="text-sm text-white/80">Job {reviewId} is still processing. You can refresh later.</p>
          <p className="mt-2 text-xs text-white/60">
            Set NEXT_PUBLIC_REVIEW_API_BASE for the API base URL, or open `/review/sample` to view sample data.
          </p>
        </div>
      </div>
    );
  }

  if (error || !review) {
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

  return (
    <div className="px-6 py-10 sm:px-12 lg:px-16">
      <div className="mx-auto flex max-w-6xl flex-col gap-6">
        <SummaryHero timeline={enhancedTimeline} critical={review.critical ?? []} review={review} />
        <div className="flex flex-col gap-2">
          <p className="text-xs uppercase tracking-[0.2em] text-white/60">Review</p>
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <h1 className="font-display text-3xl text-white">Game analysis</h1>
            <div className="flex flex-wrap gap-2 text-xs">
              <span className="rounded-full bg-white/10 px-3 py-1">PGN timeline</span>
              <span className="rounded-full bg-white/10 px-3 py-1">Stockfish shallow/deep</span>
              <span className="rounded-full bg-white/10 px-3 py-1">Concept scores</span>
              {review.studyChapters && review.studyChapters.length ? (
                <span className="rounded-full bg-accent-teal/15 px-3 py-1 text-accent-teal/80">Study chapters</span>
              ) : null}
            </div>
          </div>
          <div className="flex flex-wrap gap-2 text-xs">
            <button
              onClick={toggleAnalysis}
              className={`rounded-full border px-3 py-1 transition ${isAnalyzing
                ? "border-accent-teal bg-accent-teal/10 text-accent-teal"
                : "border-white/20 text-white/60 hover:border-white/40 hover:text-white"
                }`}
            >
              {isAnalyzing ? "Stop Analysis" : "Analyze"}
            </button>
            <button
              onClick={handleSaveGame}
              className="rounded-full border border-white/20 px-3 py-1 text-white/60 hover:border-white/40 hover:text-white"
            >
              Save
            </button>
            {previewFen ? (
              <button
                onClick={clearPreview}
                className="rounded-full border border-amber-400/60 px-3 py-1 text-amber-100 hover:border-amber-300/80"
              >
                Exit preview
              </button>
            ) : null}
          </div>
          <div className="text-sm text-white/70">
            Opening: {review.opening?.name ?? "Unknown"} {review.opening?.eco ? `(${review.opening.eco})` : ""}
          </div>
        </div>

        <div className="grid gap-5 lg:grid-cols-[auto_300px_1fr] xl:grid-cols-[auto_360px_1fr]">
          <div className="space-y-4 lg:sticky lg:top-4 lg:self-start">
            <div className="group relative">
              <BoardCard
                fen={previewFen || activeMove?.fen}
                squareStyles={boardSquareStyles}
                arrows={arrows}
                evalPercent={activeMove?.winPctAfterForPlayer}
                judgementBadge={judgementBadge}
                moveSquare={activeMove?.uci?.slice(2, 4)} // Extract target square from UCI (e.g. "e2e4" -> "e4")
                onDrop={handleBoardDrop}
              />
              <DrawingTools
                selectedColor={drawingColor}
                onSelectColor={setDrawingColor}
                onClear={() => {
                  setCustomArrows([]);
                }}
              />
            </div>
            {previewLabel ? (
              <div className="text-xs text-amber-100">Previewing line: {previewLabel}</div>
            ) : null}
            {showAdvanced ? (
              <EvalSparkline
                timeline={enhancedTimeline}
                spikePlys={conceptSpikes.map((s) => ({ ply: s.ply, concept: s.concept }))}
              />
            ) : null}
            <MoveControls timeline={enhancedTimeline} selected={selected?.ply} onSelect={setSelectedPly} />
            <div className="text-xs text-white/60">
              Drag on board to add a variation at the selected ply (server merges/dedupes).
              {branchSaving ? <span className="ml-2 text-accent-teal">Saving…</span> : null}
              {branchError ? <span className="ml-2 text-rose-200">{branchError}</span> : null}
            </div>
          </div>

          <div className="flex flex-col gap-4 lg:h-[calc(100vh-2rem)] lg:sticky lg:top-4">
            <CompressedMoveList
              timeline={enhancedTimeline}
              currentPly={selectedPly}
              onSelectPly={setSelectedPly}
            />
            <QuickJump timeline={enhancedTimeline} onSelect={(ply) => setSelectedPly(ply)} />
          </div>

          <div className="flex flex-col gap-4 lg:h-[calc(100vh-2rem)] lg:sticky lg:top-4">
            <ConceptCards
              concepts={activeMove?.concepts}
              prevConcepts={enhancedTimeline.find(t => t.ply === (activeMove?.ply ?? 0) - 1)?.concepts}
            />
            <AnalysisPanel activeTab={activeTab} onTabChange={setActiveTab}>
              {activeTab === "engine" && (
                <BestAlternatives
                  lines={engineLines}
                  isAnalyzing={isAnalyzing}
                  onToggleAnalysis={toggleAnalysis}
                  onPreviewLine={(pv) => {
                    // Reuse existing preview logic
                    if (!activeMove?.fenBefore || !pv) return;
                    try {
                      const chess = new Chess(activeMove.fenBefore);
                      const arrows: Array<[string, string, string?]> = [];
                      pv.split(" ").slice(0, 8).forEach((mv) => {
                        try {
                          const move = (chess as any).move(mv, { sloppy: true });
                          if (move?.from && move?.to) arrows.push([move.from, move.to, "#10b981"]);
                        } catch {
                          // ignore
                        }
                      });
                      setPreviewFen(chess.fen());
                      setPreviewArrows(arrows);
                      setPreviewLabel("Engine Line");
                    } catch {
                      // ignore
                    }
                  }}
                />
              )}

              {activeTab === "opening" && (
                <OpeningExplorerTab
                  stats={openingLookup}
                  loading={lookupLoading}
                  error={lookupError}
                />
              )}

              {activeTab === "study" && (
                <StudyTab
                  chapters={review.studyChapters}
                  onSelectChapter={(ply) => {
                    setSelectedPly(ply);
                    setSelectedVariation(null);
                  }}
                  onStartGuess={(chapter) => {
                    setSelectedPly(chapter.anchorPly);
                    setIsGuessing(true);
                    setGuessState("waiting");
                    setGuessFeedback(undefined);
                  }}
                />
              )}

              {activeTab === "concepts" && (
                <ConceptsTab
                  review={review}
                  currentConcepts={activeMove?.concepts}
                />
              )}

              {isGuessing && activeMove && (
                <div className="absolute inset-0 z-20 bg-black/80 p-4 backdrop-blur-sm">
                  <GuessTheMove
                    targetPly={activeMove.ply}
                    fenBefore={activeMove.fenBefore || ""}
                    bestMoveSan={
                      activeMove.evalBeforeDeep?.lines?.[0]?.move
                        ? uciToSan(activeMove.fenBefore || "", activeMove.evalBeforeDeep.lines[0].move)
                        : "Unknown"
                    }
                    playedMoveSan={activeMove.san}
                    guessState={guessState}
                    feedbackMessage={guessFeedback}
                    onSolve={() => {
                      setIsGuessing(false);
                      // Maybe move to next chapter?
                    }}
                    onGiveUp={() => {
                      setGuessState("giveup");
                      // Show best move on board?
                      const best = activeMove.evalBeforeDeep?.lines?.[0]?.move;
                      if (best) {
                        const from = best.slice(0, 2);
                        const to = best.slice(2, 4);
                        setPreviewArrows([[from, to, "#22c55e"]]);
                      }
                    }}
                    onClose={() => setIsGuessing(false)}
                  />
                </div>
              )}
            </AnalysisPanel>
          </div>
        </div>

        <BlunderTimeline timeline={enhancedTimeline} selected={selected?.ply} onSelect={setSelectedPly} />
      </div>
    </div>
  );
}
// ... existing code ...
