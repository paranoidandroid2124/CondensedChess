"use client";

import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Chessboard } from "react-chessboard";
import { Chess } from "chess.js";
import { fetchOpeningLookup, fetchReview } from "../../../lib/review";
import type { Concepts, CriticalNode, OpeningStats, Review, ReviewTreeNode, TimelineNode } from "../../../types/review";

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

function uciToSanWithFen(fen: string | undefined, uci: string): string {
  if (!fen) return uci;
  try {
    const chess = new Chess(fen);
    const from = uci.slice(0, 2);
    const to = uci.slice(2, 4);
    const promotion = uci.length > 4 ? uci.slice(4) : undefined;
    const move = chess.move({ from, to, promotion });
    return move?.san ?? uci;
  } catch {
    return uci;
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
          {name} {Math.round((score ?? 0) * 100) / 100}
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
      .filter((v): v is number => typeof v === "number");
    if (!vals.length) return { poly: "", values: [], w: width, h: height, min: 0, max: 0, coords: [] as Array<[number, number]> };
    const minVal = Math.min(...vals);
    const maxVal = Math.max(...vals);
    const range = maxVal - minVal || 1;
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
        {[0.25, 0.5, 0.75].map((p) => (
          <line key={p} x1={0} x2={w} y1={h * p} y2={h * p} stroke="rgba(255,255,255,0.08)" strokeWidth={1} />
        ))}
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
  onDrop
}: {
  fen?: string;
  squareStyles?: Record<string, React.CSSProperties>;
  arrows?: Array<[string, string, string?]>;
  evalPercent?: number;
  judgementBadge?: string;
  onDrop?: (args: PieceDropArgs) => boolean;
}) {
  const [width, setWidth] = useState(420);

  useEffect(() => {
    const update = () => {
      const w = typeof window !== "undefined" ? window.innerWidth : 480;
      setWidth(Math.max(260, Math.min(520, w - 64)));
    };
    update();
    window.addEventListener("resize", update);
    return () => window.removeEventListener("resize", update);
  }, []);

  return (
    <div className="glass-card rounded-2xl p-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-white/80">Board</h3>
        <span className="text-xs text-white/60">{fen ? "Selected ply" : "Starting position"}</span>
      </div>
      <div className="relative mt-3 flex gap-3">
        <div className="flex w-4 flex-col items-center">
          <div className="relative h-full w-2 overflow-hidden rounded-full bg-white/10">
            {typeof evalPercent === "number" ? (
              <div
                className="absolute left-0 w-full rounded-full bg-accent-teal"
                style={{ height: `${Math.max(0, Math.min(100, evalPercent))}%`, bottom: 0 }}
                title={`Win chance ${evalPercent.toFixed(1)}%`}
              />
            ) : null}
          </div>
          <div className="mt-1 text-[10px] text-white/70">{evalPercent != null ? `${evalPercent.toFixed(1)}%` : "—"}</div>
        </div>
        <div className="relative overflow-hidden rounded-xl border border-white/10">
          <div className="absolute right-2 top-2 z-10 flex items-center gap-2">
            {judgementBadge ? (
              <span className="rounded-full bg-black/50 px-2 py-1 text-xs text-white">{judgementBadge}</span>
            ) : null}
          </div>
          <Chessboard
            id="review-board"
            position={fen || "start"}
            boardWidth={width}
            arePiecesDraggable={true}
            onPieceDrop={(sourceSquare, targetSquare, piece) =>
              onDrop
                ? onDrop({ sourceSquare, targetSquare, piece })
                : false
            }
            customLightSquareStyle={{ backgroundColor: "#e7ecff" }}
            customDarkSquareStyle={{ backgroundColor: "#5b8def" }}
            customSquareStyles={squareStyles}
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
  showAdvanced
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
                      onSelect={onSelect}
                      variationCount={row.white?.ply != null ? variations?.[row.white.ply]?.length ?? 0 : 0}
                      showAdvanced={!!showAdvanced}
                    />
                    <MoveCell
                      move={row.black}
                      selected={selected}
                      onSelect={onSelect}
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
                                  {showAdvanced && group.vars[0].depth ? <span>Engine depth {group.vars[0].depth}</span> : null}
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
                                    className={`w-full rounded-lg px-2 py-1 text-left transition ${
                                      selected === v.node.ply ? "bg-white/10 ring-1 ring-accent-teal/50" : "hover:bg-white/5"
                                    }`}
                                  >
                                    <button
                                      onClick={() => {
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
                                          <div className="text-[11px] text-white/60 flex flex-wrap gap-1">
                                            {convertPvToSan(v.parentFenBefore, v.node.pv).map((m, idxPv) => (
                                              <span
                                                key={`${group.ply}-${vidx}-pv-${idxPv}`}
                                                className={`rounded-full px-2 py-0.5 ${
                                                  idxPv === 0 ? "bg-white/15 text-white" : "bg-white/10 text-white/70"
                                                }`}
                                              >
                                                {formatSanHuman(m)}
                                              </span>
                                            ))}
                                          </div>
                                        ) : null}
                                        <div className="text-[11px] text-white/50 flex flex-wrap items-center gap-2">
                                          <span>{v.turn === "white" ? "White" : "Black"} perspective</span>
                                          {showAdvanced && v.depth ? (
                                            <span className="rounded-full bg-white/5 px-2 py-0.5">Engine depth {v.depth}</span>
                                          ) : null}
                                          <span className="rounded-full bg-white/5 px-2 py-0.5">Line {v.pvIndex ?? vidx + 1}</span>
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
        className={`w-full rounded-lg border border-white/5 px-2 py-2 text-left transition ${
          selected === move.ply ? "bg-white/10 ring-1 ring-accent-teal/60" : "bg-transparent hover:bg-white/5"
        }`}
        title={move.shortComment}
      >
        <div className="flex flex-col gap-0.5">
          <div className="flex items-center justify-between gap-2">
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-sm font-semibold text-white">{sanDisplay}</span>
              {!isBook && mark ? (
                <span
                  className={`rounded-full px-2 py-0.5 text-[11px] ${
                    judgementColors[judgement] ?? "bg-white/10 text-white"
                  }`}
                >
                  {mark}
                </span>
              ) : null}
              {variationCount ? (
                <span className="rounded-full bg-white/10 px-2 py-0.5 text-[10px] text-white/70">Variations {variationCount}</span>
              ) : null}
            </div>
            {showDelta && !isBook ? <span className={`text-[11px] ${delta.tone}`}>{deltaText}</span> : null}
          </div>
            <div className="flex flex-wrap items-center gap-2 text-[11px] text-white/60">
              {showAdvanced && depth ? <span className="rounded-full bg-white/5 px-2 py-0.5 text-white/70">Engine depth {depth}</span> : null}
              {showAdvanced && multiPv ? <span className="rounded-full bg-white/5 px-2 py-0.5 text-white/70">Candidates {multiPv}</span> : null}
            </div>
          {move.shortComment ? <div className="text-[11px] text-white/70">{move.shortComment}</div> : null}
        </div>
      </button>
    </td>
  );
}

function CriticalList({
  critical,
  fenBeforeByPly
}: {
  critical: CriticalNode[];
  fenBeforeByPly: Record<number, string | undefined>;
}) {
  const humanReason = (r: string) => {
    const low = r.toLowerCase();
    if (low.includes("blunder")) return "Blunder: big swing";
    if (low.includes("mistake")) return "Mistake: swing down";
    if (low.includes("swing")) return "Big eval swing";
    return "Concept change";
  };

  return (
    <div className="glass-card rounded-2xl p-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-white/80">Critical moments</h3>
        <span className="text-xs text-white/60">Key swings & ideas</span>
      </div>
      <div className="mt-3 space-y-3">
        {critical.map((c) => (
          <div key={c.ply} className="rounded-xl border border-white/10 bg-white/5 p-3">
            <div className="flex items-center justify-between">
              <div className="text-sm font-semibold text-white">
                Ply {c.ply}
                <span className="ml-2 text-xs text-white/60">{humanReason(c.reason)}</span>
              </div>
              <div className="text-xs font-semibold text-rose-200">{formatDelta(c.deltaWinPct)}</div>
            </div>
            <div className="mt-2 flex flex-wrap gap-2 text-[11px] text-white/70">
              {c.mistakeCategory ? (
                <span className="rounded-full bg-rose-500/15 px-2 py-1 text-rose-100">{c.mistakeCategory}</span>
              ) : null}
              {c.tags?.slice(0, 5).map((t) => (
                <span key={t} className="rounded-full bg-white/10 px-2 py-1">{t}</span>
              ))}
            </div>
            {c.branches?.length ? (
              <div className="mt-2 grid gap-2">
                {c.branches.slice(0, 3).map((b) => (
                  <div key={b.move} className="flex items-center justify-between rounded-lg bg-white/5 px-3 py-2">
                    <div className="text-sm text-white">
                      <span className="mr-2 rounded-full bg-white/10 px-2 py-0.5 text-[11px] uppercase tracking-wide text-white/70">
                        {b.label}
                      </span>
                      {uciToSanWithFen(fenBeforeByPly[c.ply], b.move)}
                    </div>
                    <div className="text-xs text-accent-teal">{b.winPct.toFixed(1)}%</div>
                  </div>
                ))}
              </div>
            ) : null}
            {c.comment ? (
              <p className="mt-2 rounded-lg bg-white/5 px-3 py-2 text-sm text-white/80">{c.comment}</p>
            ) : (
              <p className="mt-2 rounded-lg bg-white/5 px-3 py-2 text-xs text-white/50">No LLM comment</p>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}

function GuessTheMove({
  critical,
  fenBeforeByPly
}: {
  critical: CriticalNode[];
  fenBeforeByPly: Record<number, string | undefined>;
}) {
  const target = critical.find((c) => c.branches?.length);
  const [guess, setGuess] = useState<string | null>(null);
  const [revealed, setRevealed] = useState<boolean>(false);

  if (!target || !target.branches?.length) return null;
  const options = target.branches.slice(0, Math.min(3, target.branches.length));
  const correct = options[0]?.move;
  const fen = fenBeforeByPly[target.ply];

  const status =
    revealed && guess
      ? guess === correct
        ? { tone: "text-emerald-200", text: "Spot on." }
        : { tone: "text-rose-200", text: `Better was ${uciToSanWithFen(fen, correct ?? "")}.` }
      : guess
      ? { tone: "text-white/70", text: "Lock in or reveal to check." }
      : { tone: "text-white/60", text: "Pick the best move." };

  return (
    <div className="glass-card rounded-2xl p-4">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-xs uppercase tracking-[0.14em] text-white/60">Guess the move</p>
          <h3 className="text-sm font-semibold text-white">Critical ply {target.ply}</h3>
        </div>
        {target.mistakeCategory ? (
          <span className="rounded-full bg-white/10 px-2 py-1 text-[11px] text-white/70">{target.mistakeCategory}</span>
        ) : null}
      </div>
      <p className="mt-2 text-xs text-white/60">Choose the best move (PV1) before revealing.</p>
      <div className="mt-3 grid gap-2">
        {options.map((b) => (
          <button
            key={b.move}
            onClick={() => setGuess(b.move)}
            className={`flex items-center justify-between rounded-lg px-3 py-2 text-left text-sm ${
              guess === b.move ? "bg-accent-teal/20 ring-1 ring-accent-teal/60 text-white" : "bg-white/5 hover:bg-white/10 text-white/80"
            }`}
          >
            <span>
              <span className="mr-2 rounded-full bg-white/10 px-2 py-0.5 text-[10px] uppercase tracking-wide text-white/60">
                {b.label}
              </span>
              {uciToSanWithFen(fen, b.move)}
            </span>
            <span className="text-xs text-white/60">{b.winPct.toFixed(1)}%</span>
          </button>
        ))}
      </div>
      <div className="mt-3 flex items-center justify-between text-xs text-white/70">
        <span className={status.tone}>{status.text}</span>
        <button
          onClick={() => setRevealed(true)}
          className="rounded-lg border border-white/10 px-3 py-1 text-white/80 hover:border-white/30"
        >
          Reveal
        </button>
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
  if (!stats) return null;
  const hasMoves = stats.topMoves && stats.topMoves.length > 0;
  const hasGames = stats.topGames && stats.topGames.length > 0;
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
          <div className="text-xs uppercase tracking-[0.14em] text-white/60">다음 수 분포</div>
          {stats.topMoves?.map((m) => {
            const gameCounts = (stats.topMoves ?? []).map((tm) => tm.games ?? 0);
            const fallbackTotal = gameCounts.length ? Math.max(...gameCounts) : 1;
            const total = stats.games ?? fallbackTotal;
            const pct = total ? Math.max(4, ((m.games ?? 0) / total) * 100) : 0;
            return (
              <div key={m.san} className="space-y-1">
                <div className="flex items-center justify-between text-xs text-white/70">
                  <span className="font-semibold text-white">{m.san}</span>
                  <span>
                    {m.games} games{m.winPct != null ? ` · W ${(m.winPct * 100).toFixed(1)}%` : ""}
                  </span>
                </div>
                <div className="h-2 overflow-hidden rounded-full bg-white/10">
                  <div className="h-full bg-gradient-to-r from-indigo-400 to-emerald-400" style={{ width: `${Math.min(100, pct)}%` }} />
                </div>
              </div>
            );
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
        <span className="text-xs text-white/60">blunder/miss/brilliant at a glance</span>
      </div>
      <div className="mt-3 flex items-center gap-2 overflow-x-auto py-2">
        {timeline.map((t) => (
          <button
            key={t.ply}
            onClick={() => onSelect(t.ply)}
            className={`relative flex h-10 w-8 flex-col items-center justify-center rounded-lg transition ${
              selected === t.ply ? "bg-white/10" : "hover:bg-white/5"
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
          className={`group flex w-full items-center gap-2 rounded-lg px-2 py-1 text-left ${
            isSelected ? "bg-white/10 ring-1 ring-accent-teal/60" : "hover:bg-white/5"
          }`}
        >
          <span className="text-[10px] text-white/50" style={{ width: 34, visibility: isMainline ? "visible" : "hidden" }}>
            {node.ply}.
          </span>
          <div
            className={`flex flex-1 items-center gap-2 ${
              isMainline ? "font-semibold text-white" : "text-white/80"
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
                  className={`flex flex-col rounded-lg px-2 py-1 text-left ${
                    selected === v.ply ? "bg-white/10 ring-1 ring-accent-teal/50" : "hover:bg-white/5"
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
  openingTrend
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
                    >
                      {s.concept} +{s.delta.toFixed(2)} @ {s.label}
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

function SummaryHero({
  timeline,
  critical
}: {
  timeline: (TimelineNode & { label?: string })[];
  critical: CriticalNode[];
}) {
  type SummaryStats = {
    total: number;
    counts: Record<string, number>;
    worst: { ply: number; delta: number; label: string } | null;
    topCritical?: CriticalNode;
  };
  const stats = useMemo<SummaryStats>(() => {
    const total = timeline.length;
    const counts: Record<string, number> = {
      blunder: 0,
      mistake: 0,
      inaccuracy: 0,
      good: 0,
      best: 0
    };
    let worst: { ply: number; delta: number; label: string } | null = null;
    timeline.forEach((t) => {
      const j = t.judgement ?? "good";
      if (counts[j] != null) counts[j] += 1;
      if (worst == null || (t.deltaWinPct ?? 0) < (worst.delta ?? 0)) {
        worst = { ply: t.ply, delta: t.deltaWinPct ?? 0, label: t.label ?? t.san };
      }
    });
    const topCritical = critical[0];
    return {
      total,
      counts,
      worst,
      topCritical
    };
  }, [timeline, critical]);

  return (
    <div className="glass-card mb-4 rounded-3xl p-5 md:p-6">
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
      <div className="mt-4 grid gap-3 md:grid-cols-3">
        <div className="rounded-2xl border border-white/10 bg-white/5 p-3">
          <p className="text-xs uppercase tracking-[0.14em] text-white/60">Biggest drop</p>
          {stats.worst ? (
            <>
              <div className="text-sm font-semibold text-white">{stats.worst.label}</div>
              <div className="text-xs text-rose-200">{formatDelta(stats.worst.delta)}</div>
            </>
          ) : (
            <p className="text-xs text-white/60">No data</p>
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
        <div className="rounded-2xl border border-white/10 bg-white/5 p-3">
          <p className="text-xs uppercase tracking-[0.14em] text-white/60">Accurate moves</p>
          <div className="text-sm font-semibold text-white">
            {stats.counts.best + stats.counts.good} Good/Best
          </div>
          <div className="text-xs text-white/60">The rest need improvement</div>
        </div>
      </div>
    </div>
  );
}

export default function ReviewClient({ reviewId }: { reviewId: string }) {
  const [review, setReview] = useState<Review | null>(null);
  const [selectedPly, setSelectedPly] = useState<number | null>(null);
  const [scratchChess] = useState(() => new Chess());
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [pendingMessage, setPendingMessage] = useState<string | null>(null);
  const [showAdvanced, setShowAdvanced] = useState<boolean>(false);
  const [openingLookup, setOpeningLookup] = useState<OpeningStats | null>(null);
  const [lookupKey, setLookupKey] = useState<string>("");
  const [lookupError, setLookupError] = useState<string | null>(null);
  const [lookupLoading, setLookupLoading] = useState<boolean>(false);

  useEffect(() => {
    let mounted = true;
    let timer: NodeJS.Timeout | null = null;

    const poll = async (attempt: number) => {
      if (!mounted) return;
      try {
        const res = await fetchReview(reviewId);
        if (res.status === "pending") {
          setPendingMessage(res.message ?? "processing");
          setLoading(true);
          // poll up to ~80 attempts (~120s)
          if (attempt < 80) {
            timer = setTimeout(() => poll(attempt + 1), 1500);
          } else {
            setError("Result not ready yet. Please refresh later.");
            setLoading(false);
          }
          return;
        }
        // ready
        setPendingMessage(null);
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
    poll(0);

    return () => {
      mounted = false;
      if (timer) clearTimeout(timer);
    };
  }, [reviewId]);

  const enhancedTimeline = useMemo(() => {
    if (!review?.timeline?.length) return [];
    const sorted = [...review.timeline].sort((a, b) => a.ply - b.ply);
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
      if (!prev.concepts || !cur.concepts) continue;
      Object.keys(cur.concepts).forEach((k) => {
        const key = k as keyof Concepts;
        const curVal = cur.concepts?.[key];
        const prevVal = prev.concepts?.[key];
        if (typeof curVal === "number" && typeof prevVal === "number") {
          const delta = curVal - prevVal;
          if (delta >= 0.25) {
            spikes.push({ ply: cur.ply, concept: key, delta, label: cur.label ?? cur.san });
          }
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

  const handleBoardDrop = useCallback(
    ({ sourceSquare, targetSquare }: PieceDropArgs) => {
      try {
        scratchChess.load(selected?.fen ?? "start");
      } catch {
        return false;
      }
      const prevFen = scratchChess.fen();
      const move = scratchChess.move({ from: sourceSquare, to: targetSquare, promotion: "q" });
      if (!move) return false;
      const newFen = scratchChess.fen();
      // create a synthetic ply entry
      const nextPly = (selected?.ply ?? 0) + 1;
      const san = move.san;
      const uci = `${sourceSquare}${targetSquare}${move.promotion ?? ""}`;
      const synthetic: TimelineNode & { label: string; fenBefore: string } = {
        ply: nextPly,
        turn: move.color === "w" ? "white" : "black",
        san,
        uci,
        fen: newFen,
        fenBefore: prevFen,
        features: selected?.features ?? ({} as any),
        judgement: "good",
        winPctAfterForPlayer: selected?.winPctAfterForPlayer,
        winPctBefore: selected?.winPctAfterForPlayer,
        deltaWinPct: 0,
        epBefore: 0,
        epAfter: 0,
        epLoss: 0,
        special: undefined,
        concepts: selected?.concepts,
        label: `${Math.ceil(nextPly / 2)}${nextPly % 2 === 1 ? "." : "..."} ${san}`
      };
      const mergedTimeline = [...enhancedTimeline.filter((t) => t.ply < nextPly), synthetic];
      // update san sequence and lookup
      const seq = mergedTimeline.sort((a, b) => a.ply - b.ply).map((t) => t.san);
      setSelectedPly(nextPly);
      setLookupLoading(true);
      fetchOpeningLookup(seq)
        .then(setOpeningLookup)
        .catch((err) => setLookupError(err instanceof Error ? err.message : "Lookup failed"))
        .finally(() => setLookupLoading(false));
      return true;
    },
    [scratchChess, selected, enhancedTimeline]
  );

  useEffect(() => {
    const key = sanSequence.join(" ");
    if (!key || key === lookupKey) return;
    setLookupKey(key);
    setLookupError(null);
    setLookupLoading(true);
    fetchOpeningLookup(sanSequence)
      .then((data) => {
        setOpeningLookup(data);
      })
      .catch((err) => {
        setLookupError(err instanceof Error ? err.message : "Lookup failed");
      })
      .finally(() => setLookupLoading(false));
  }, [sanSequence, lookupKey]);

  const evalPercent = selected
    ? selected.turn === "white"
      ? selected.winPctAfterForPlayer ?? selected.winPctBefore
      : selected.winPctAfterForPlayer != null
      ? 100 - selected.winPctAfterForPlayer
      : selected.winPctBefore != null
      ? 100 - selected.winPctBefore
      : undefined
    : undefined;
  const judgementBadge =
    selected?.special === "brilliant"
      ? "!!"
      : selected?.special === "great"
      ? "!"
      : selected?.judgement === "blunder"
      ? "??"
      : selected?.judgement === "mistake"
      ? "?"
      : selected?.judgement === "inaccuracy"
      ? "?!"
      : selected?.judgement === "book"
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
    if (selected) {
      const bad = selected.judgement === "inaccuracy" || selected.judgement === "mistake" || selected.judgement === "blunder";
      highlight(selected.uci, bad ? "red" : "purple");
      const best = selected.evalBeforeDeep?.lines?.[0]?.move;
      if (best && best !== selected.uci) {
        highlight(best, "green");
      }
    }
    return styles;
  }, [selected]);

  const arrows = useMemo(() => {
    const arr: Array<[string, string, string?]> = [];
    if (selected) {
      const from = selected.uci.slice(0, 2);
      const to = selected.uci.slice(2, 4);
      const bad = selected.judgement === "inaccuracy" || selected.judgement === "mistake" || selected.judgement === "blunder";
      arr.push([from, to, bad ? "#f87171" : "#818cf8"]);
      const best = selected.evalBeforeDeep?.lines?.[0]?.move;
      if (best && best !== selected.uci) {
        const bFrom = best.slice(0, 2);
        const bTo = best.slice(2, 4);
        arr.push([bFrom, bTo, "#4ade80"]);
      }
    }
    return arr;
  }, [selected]);

  if (loading) {
    return (
      <div className="px-6 py-10 sm:px-12 lg:px-16">
        <div className="mx-auto max-w-6xl animate-pulse space-y-4">
          <div className="h-10 rounded-2xl bg-white/5" />
          <div className="grid gap-4 md:grid-cols-2">
            <div className="h-96 rounded-2xl bg-white/5" />
            <div className="h-96 rounded-2xl bg-white/5" />
          </div>
          {pendingMessage ? (
            <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/80">
              Analyzing... ({pendingMessage})
            </div>
          ) : null}
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
        <SummaryHero timeline={enhancedTimeline} critical={review.critical ?? []} />
        <div className="flex flex-col gap-2">
          <p className="text-xs uppercase tracking-[0.2em] text-white/60">Review</p>
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <h1 className="font-display text-3xl text-white">Game analysis</h1>
            <div className="flex flex-wrap gap-2 text-xs">
              <span className="rounded-full bg-white/10 px-3 py-1">PGN timeline</span>
              <span className="rounded-full bg-white/10 px-3 py-1">Stockfish shallow/deep</span>
              <span className="rounded-full bg-white/10 px-3 py-1">Concept scores</span>
            </div>
          </div>
          <div className="flex flex-wrap gap-2 text-xs">
            <button
              onClick={() => setShowAdvanced((p) => !p)}
              className="rounded-full border border-white/15 px-3 py-1 text-white/80 hover:border-white/35"
            >
              {showAdvanced ? "Hide advanced" : "Show advanced"}
            </button>
          </div>
          <div className="text-sm text-white/70">
            Opening: {review.opening?.name ?? "Unknown"} {review.opening?.eco ? `(${review.opening.eco})` : ""}
          </div>
        </div>

        <div className="grid gap-5 lg:grid-cols-[1.3fr_1fr]">
          <div className="space-y-4 lg:sticky lg:top-4 lg:self-start">
            <BoardCard
              fen={selected?.fen}
              squareStyles={boardSquareStyles}
              arrows={arrows}
              evalPercent={evalPercent}
              judgementBadge={judgementBadge}
              onDrop={handleBoardDrop}
            />
            {showAdvanced ? (
              <EvalSparkline
                timeline={enhancedTimeline}
                spikePlys={conceptSpikes.map((s) => ({ ply: s.ply, concept: s.concept }))}
              />
            ) : null}
            <MoveControls timeline={enhancedTimeline} selected={selected?.ply} onSelect={setSelectedPly} />
          </div>

          <div className="flex flex-col gap-4">
            <SummaryPanel
              opening={review.opening}
              openingStats={review.openingStats}
              oppositeColorBishops={review.oppositeColorBishops}
              concepts={selected?.concepts}
              conceptSpikes={conceptSpikes}
              showAdvanced={showAdvanced}
              summaryText={review.summaryText}
              openingSummary={review.openingSummary}
              bookExitComment={review.bookExitComment}
              openingTrend={review.openingTrend}
            />
            <OpeningLookupPanel stats={openingLookup} loading={lookupLoading} error={lookupError} />
            <GuessTheMove critical={review.critical ?? []} fenBeforeByPly={fenBeforeByPly} />
            <div className="rounded-2xl">
              <div className="max-h-[70vh] overflow-y-auto pr-1">
                <MoveTimeline
                  rows={moveRows}
                  selected={selected?.ply}
                  onSelect={setSelectedPly}
                  variations={variationMap}
                  showAdvanced={showAdvanced}
                />
              </div>
            </div>
            {showAdvanced ? (
              <CriticalList
                critical={review.critical ?? []}
                fenBeforeByPly={fenBeforeByPly}
              />
            ) : null}
          </div>
        </div>

        <BlunderTimeline timeline={enhancedTimeline} selected={selected?.ply} onSelect={setSelectedPly} />
      </div>
    </div>
  );
}
