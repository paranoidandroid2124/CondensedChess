// chess.js dependency removed
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

  // Sort by ply to ensure correct order
  const sorted = [...review.timeline].sort((a, b) => a.ply - b.ply);

  return sorted.map((t) => {
    const moveNumber = Math.ceil(t.ply / 2);
    const turnPrefix = t.ply % 2 === 1 ? "." : "...";
    const label = `${moveNumber}${turnPrefix} ${t.san}`;
    // Backend/TimelineBuilder guarantees fenBefore is populated.
    // We fall back to fen (current) only if absolutely necessary, though it shouldn't happen for valid moves.
    const fenBefore = t.fenBefore || t.fen;

    return { ...t, label, fenBefore };
  });
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
      // We rely solely on the backend-provided conceptDelta.
      // If the backend didn't compute a delta, we assume it's not a significant spike.
      const delta = typeof deltaFromEngine === "number" ? deltaFromEngine : null;
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


export function findPathToNode(root: ReviewTreeNode, targetNode: ReviewTreeNode): ReviewTreeNode[] | null {
  // Try reference equality first
  if (root === targetNode) return [root];
  // Fallback to strict unique ID check if refs change (UCI + Ply + FEN)
  const isMatch = root.ply === targetNode.ply && root.uci === targetNode.uci && root.fen === targetNode.fen;
  if (isMatch) return [root];

  if (!root.children) return null;
  for (const child of root.children) {
    const path = findPathToNode(child, targetNode);
    if (path) {
      return [root, ...path];
    }
  }
  return null;
}

export function convertPathToTimeline(path: ReviewTreeNode[]): EnhancedTimelineNode[] {
  // Skip root if it's ply 0 (start position) with no SAN
  const moves = path.filter(n => n.ply > 0);

  return moves.map(node => {
    const moveNumber = Math.ceil(node.ply / 2);
    const turnPrefix = node.ply % 2 === 1 ? "." : "...";
    const label = `${moveNumber}${turnPrefix} ${node.san}`;

    // For variation nodes, infer fenBefore from the previous node in the path
    const parent = path.find(p => p.ply === node.ply - 1);
    const fenBefore = parent ? parent.fen : node.fen;

    // Determine turn from ply (odd = white moved, even = black moved)
    const turn: "white" | "black" = node.ply % 2 === 1 ? "white" : "black";

    // Build EnhancedTimelineNode with required fields
    // ReviewTreeNode has: ply, san, uci, fen, eval, evalType, judgement, glyph, tags, pv, children
    // TimelineNode requires: ply, turn, san, uci, fen, features
    return {
      ply: node.ply,
      turn,
      san: node.san,
      uci: node.uci,
      fen: node.fen,
      features: node.features ?? { pawnIslands: 0, isolatedPawns: 0, doubledPawns: 0, passedPawns: 0, rookOpenFiles: 0, rookSemiOpenFiles: 0, bishopPair: false, kingRingPressure: 0, spaceControl: 0 },
      concepts: node.concepts,
      label,
      fenBefore,
    } as EnhancedTimelineNode;
  });
}

