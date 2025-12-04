import { Chess } from "chess.js";

export function formatSanHuman(san: string): string {
  const trimmed = san.trim();
  const first = trimmed.charAt(0);
  const pieceEmoji: Record<string, string> = {
    K: "♔",
    Q: "♕",
    R: "♖",
    B: "♗",
    N: "♘"
  };
  if (pieceEmoji[first]) {
    return `${pieceEmoji[first]}${trimmed.slice(1)}`;
  }
  return trimmed; // pawns or already symbolic
}

export function formatPvList(pv?: string[]) {
  if (!pv?.length) return "";
  return pv.map((m) => formatSanHuman(m)).join(" ");
}

export function convertPvToSan(fen: string | undefined, pv?: string[]) {
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

export function normalizeEvalKind(kind?: string, value?: number) {
  const lower = kind?.toLowerCase();
  if (!lower) return "win%";
  if (lower === "cp") {
    if (value != null && Math.abs(value) > 10) return "win%";
    return "cp";
  }
  if (lower.includes("win")) return "win%";
  return lower;
}

export function formatEvalValue(value?: number, evalKind?: string, turn?: "white" | "black") {
  const kind = normalizeEvalKind(evalKind, value);
  if (value === undefined || Number.isNaN(value)) return "Eval —";
  if (kind === "cp") {
    const sign = value > 0 ? "+" : "";
    return `Eval ${sign}${Math.round(value)}cp`;
  }
  const sideLabel = turn === "white" ? "White" : turn === "black" ? "Black" : "Side";
  return `${sideLabel} eval ${value.toFixed(1)}%`;
}

export function formatDeltaWithSide(value?: number, turn?: "white" | "black") {
  if (value === undefined || Number.isNaN(value)) return { text: "No eval change", tone: "text-white/60" };
  const sign = value > 0 ? "+" : "";
  const tone = value > 0 ? "text-accent-teal" : value < 0 ? "text-rose-300" : "text-white/70";
  const verb = value > 0 ? "improved" : value < 0 ? "worsened" : "no change";
  return { text: `Move ${verb}: ${sign}${value.toFixed(1)}%`, tone };
}

export function formatDelta(value?: number): string {
  if (value === undefined || Number.isNaN(value)) return "–";
  const prefix = value > 0 ? "+" : "";
  return `${prefix}${value.toFixed(1)}%`;
}
