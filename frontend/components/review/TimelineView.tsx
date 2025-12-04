import React, { useState } from "react";
import type { ReviewTreeNode, TimelineNode } from "../../types/review";
import { displayTag } from "../../lib/review-tags";
import { convertPvToSan, formatDeltaWithSide, formatEvalValue, formatSanHuman } from "../../lib/review-format";

export type VariationEntry = {
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

const practicalityBg: Record<string, string> = {
  "Human-Friendly": "bg-emerald-500/10 hover:bg-emerald-500/15",
  "Challenging": "bg-amber-500/10 hover:bg-amber-500/15",
  "Engine-Like": "bg-orange-500/10 hover:bg-orange-500/15",
  "Computer-Only": "bg-rose-500/10 hover:bg-rose-500/15"
};

const mistakeIcons: Record<string, string> = {
  tactical_miss: "🎯",
  greedy: "💰",
  positional_trade_error: "⚖️",
  ignored_threat: "🚨",
  conversion_difficulty: "🧗",
  king_exposed: "👑",
  fortress_building: "🏰"
};

const conceptIcons: Record<string, string> = {
  dynamic: "⚡",
  drawish: "😴",
  blunderRisk: "⚠️",
  tacticalDepth: "⚔️",
  pawnStorm: "♟️",
  sacrificeQuality: "💣"
};

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
  if (!move) {
    return (
      <td className="px-3 py-2 text-xs text-white/40" colSpan={showAdvanced ? 1 : 2}>
        —
      </td>
    );
  }
  const sanDisplay = formatSanHuman(move.san); // avoid repeating move number; column already shows move #
  const judgement = (move.special as keyof typeof judgementMarks) ?? move.judgement ?? "good";
  const mark = judgementMarks[judgement] ?? move.judgement?.toUpperCase();
  const phase = move.phaseLabel || move.label;
  const delta = formatDeltaWithSide(move.deltaWinPct, move.turn);
  const showDelta = !["book", "best"].includes(move.judgement ?? "");
  const isBook = move.judgement === "book";
  const deltaText = showDelta ? delta.text.replace(/\(.*?\)/, "").trim() : "";

  // Get practicality background color
  const practicalityCategory = move.practicality?.categoryPersonal || move.practicality?.categoryGlobal || move.practicality?.category;
  const practicalityBgClass = practicalityCategory && selected !== move.ply ? practicalityBg[practicalityCategory] : "";

  return (
    <td className="px-3 py-2 align-top">
      <button
        onClick={() => onSelect(move.ply)}
        className={`w-full rounded-lg border border-white/5 px-2 py-2 text-left transition ${selected === move.ply
          ? "bg-white/10 ring-1 ring-accent-teal/60"
          : `${practicalityBgClass || "bg-transparent"} hover:bg-white/5`
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
              {move.mistakeCategory && mistakeIcons[move.mistakeCategory] ? (
                <span className="text-sm" title={displayTag(move.mistakeCategory)}>
                  {mistakeIcons[move.mistakeCategory]}
                </span>
              ) : null}
              {phase ? (
                <span className="rounded-full bg-accent-teal/15 px-2 py-0.5 text-[10px] text-accent-teal/80">
                  {displayTag(phase)}
                </span>
              ) : null}
              {variationCount ? (
                <span className="rounded-full bg-white/10 px-2 py-0.5 text-[10px] text-white/70">
                  Variations {variationCount}
                </span>
              ) : null}
            </div>
            {showDelta && !isBook ? <span className={`text-[11px] ${delta.tone}`}>{deltaText}</span> : null}
          </div>
          {/* Engine depth/multipv are global; avoid repeating per-move badges */}
          {move.shortComment ? <div className="text-[11px] text-white/70">{move.shortComment}</div> : null}

          {/* Concept Deltas & Semantic Tags */}
          {(move.semanticTags?.length || move.conceptDelta) ? (
            <div className="mt-1 flex flex-wrap gap-1">
              {/* Concept Deltas */}
              {move.conceptDelta && Object.entries(move.conceptDelta).map(([key, val]) => {
                if (val && val > 0.2 && conceptIcons[key]) {
                  return (
                    <span key={key} className="text-xs" title={`${key} increased`}>
                      {conceptIcons[key]}
                    </span>
                  );
                }
                return null;
              })}

              {/* Semantic Tags */}
              {move.semanticTags?.slice(0, 3).map((tag) => (
                <span
                  key={tag}
                  className="text-[9px] rounded-full bg-cyan-500/10 px-1.5 py-0.5 text-cyan-300 border border-cyan-500/20"
                >
                  {displayTag(tag)}
                </span>
              ))}
            </div>
          ) : null}
        </div>
      </button>
    </td>
  );
}

interface TimelineViewProps {
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
}

export function TimelineView({
  rows,
  selected,
  onSelect,
  variations,
  showAdvanced,
  onSelectVariation,
  onPreviewLine
}: TimelineViewProps) {
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
                                    <span
                                      className={`h-2 w-2 rounded-full ${group.vars[0].turn === "white" ? "bg-white" : "bg-black"
                                        } border border-white/20`}
                                    />
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
                                        <span className="text-xs text-accent-teal">
                                          {formatEvalValue(v.node.eval, v.evalKind, v.turn)}
                                        </span>
                                        <span className="text-xs text-white/50">
                                          {expandedVariation === `${group.ply}-${vidx}` ? "Hide" : "Show"}
                                        </span>
                                      </div>
                                    </button>
                                    {expandedVariation === `${group.ply}-${vidx}` ? (
                                      <div className="mt-1 space-y-1 rounded-md bg-white/5 p-2">
                                        {v.node.pv?.length ? (
                                          <div className="flex flex-wrap items-center gap-2 text-[11px] text-white/60">
                                            <button
                                              className="rounded-full bg-accent-teal/20 px-2 py-0.5 text-white"
                                              onClick={() => {
                                                onPreviewLine?.(
                                                  v.parentFenBefore,
                                                  v.node.pv,
                                                  `Preview: ${v.node.san} (${v.parentLabel})`
                                                );
                                              }}
                                            >
                                              Preview line
                                            </button>
                                            <span className="text-white/80">
                                              {convertPvToSan(v.parentFenBefore, v.node.pv).map((m, idxPv) => (
                                                <span key={`${m}-${idxPv}`} className="mr-2">
                                                  {m}
                                                </span>
                                              ))}
                                            </span>
                                          </div>
                                        ) : null}
                                        {v.node.comment ? (
                                          <div className="text-[11px] text-white/70">{v.node.comment}</div>
                                        ) : null}
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
