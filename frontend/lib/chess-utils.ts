import { Chess } from "chess.js";

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

/** Converts a sequence of UCI moves to SAN, chaining the FEN after each move. */
export function pvToSan(fen: string | undefined, uciMoves: string[]): string[] {
  if (!fen || !uciMoves.length) return uciMoves;
  try {
    const chess = new Chess(fen);
    const result: string[] = [];
    for (const uci of uciMoves) {
      const from = uci.slice(0, 2);
      const to = uci.slice(2, 4);
      const promotion = uci.length > 4 ? uci.slice(4) : undefined;
      const move = chess.move({ from, to, promotion });
      if (move) {
        result.push(move.san);
      } else {
        result.push(uci); // Fallback if move is invalid
      }
    }
    return result;
  } catch {
    return uciMoves;
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
