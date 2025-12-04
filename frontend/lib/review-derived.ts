import { Chess } from "chess.js";
import { normalizeEvalKind } from "./review-format";
import type { Review, ReviewTreeNode, TimelineNode } from "../types/review";
import type { VariationEntry } from "../components/review/TimelineView";

export type EnhancedTimelineNode = TimelineNode & { label: string; fenBefore: string };

export interface MoveRow {
  moveNumber: number;
  white?: EnhancedTimelineNode;
  black?: EnhancedTimelineNode;
}

export function buildEnhancedTimeline(review?: Review | null): EnhancedTimelineNode[] {
  if (!review?.timeline?.length) return [];
  const sorted = [...review.timeline].sort((a, b) => a.ply - b.ply);
  const chess = new Chess();
  const result: EnhancedTimelineNode[] = [];
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
}

export function buildMoveRows(enhancedTimeline: EnhancedTimelineNode[]): MoveRow[] {
  const rows: MoveRow[] = [];
  enhancedTimeline.forEach((t) => {
    const moveNum = Math.ceil(t.ply / 2);
    const isWhite = t.ply % 2 === 1;
    const existing = rows.find((r) => r.moveNumber === moveNum);
    const row = existing ?? { moveNumber: moveNum };
    if (isWhite) row.white = t;
    else row.black = t;
    if (!existing) rows.push(row);
  });
  return rows;
}

export function buildConceptSpikes(enhancedTimeline: EnhancedTimelineNode[]) {
  const spikes: Array<{ ply: number; concept: string; delta: number; label: string }> = [];
  for (let i = 1; i < enhancedTimeline.length; i++) {
    const prev = enhancedTimeline[i - 1];
    const cur = enhancedTimeline[i];
    if (!cur.concepts) continue;
    Object.keys(cur.concepts).forEach((k) => {
      const key = k as keyof TimelineNode["concepts"];
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
}

export function buildFenBeforeMap(enhancedTimeline: EnhancedTimelineNode[]) {
  return Object.fromEntries(enhancedTimeline.map((t) => [t.ply, t.fenBefore as string | undefined]));
}

export function findSelected(enhancedTimeline: EnhancedTimelineNode[], selectedPly: number | null) {
  if (!enhancedTimeline.length) return null;
  if (selectedPly === null) return enhancedTimeline[enhancedTimeline.length - 1];
  return enhancedTimeline.find((t) => t.ply === selectedPly) ?? enhancedTimeline[enhancedTimeline.length - 1];
}

function collectVariations(
  node: ReviewTreeNode,
  timelineByPly: Map<number, EnhancedTimelineNode>,
  map: Record<number, VariationEntry[]>
) {
  const vars = node.children?.filter((c) => c.judgement === "variation") ?? [];
  if (vars.length) {
    const parentTimeline = timelineByPly.get(node.ply);
    const parentLabel =
      parentTimeline?.label ?? `${Math.ceil(node.ply / 2)}${node.ply % 2 === 1 ? "." : "..."} ${node.san}`;
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
  (node.children ?? []).filter((c) => c.judgement !== "variation").forEach((c) => collectVariations(c, timelineByPly, map));
}

export function buildVariationMap(review: Review | null, enhancedTimeline: EnhancedTimelineNode[]) {
  const map: Record<number, VariationEntry[]> = {};
  const rootNode = review?.root;
  if (!rootNode) return map;
  const timelineByPly = new Map(enhancedTimeline.map((t) => [t.ply, t]));
  collectVariations(rootNode, timelineByPly, map);
  return map;
}

