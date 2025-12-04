import { Chess } from "chess.js";

export function formatDelta(value?: number): string {
  if (value === undefined || Number.isNaN(value)) return "–";
  const prefix = value > 0 ? "+" : "";
  return `${prefix}${value.toFixed(1)}%`;
}

export function uciToSan(fen: string | undefined, uci: string): string {
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

export function moveNumber(ply: number): number {
  if (!Number.isFinite(ply)) return 0;
  return Math.max(1, Math.ceil(ply / 2));
}

export function turnPrefix(ply: number): string {
  if (!Number.isFinite(ply)) return ".";
  return ply % 2 === 1 ? "." : "...";
}
