import React, { useMemo } from "react";
import { Chess } from "chess.js";

interface MiniBoardProps {
    fen: string;
    orientation?: "white" | "black";
    lastMove?: string; // uci e.g. "e2e4"
    markers?: Array<{ square: string; type: "arrow" | "circle"; color: string }>;
    className?: string;
}

const PIECE_IMAGES: Record<string, string> = {
    p: "https://upload.wikimedia.org/wikipedia/commons/c/c7/Chess_pdt45.svg",
    n: "https://upload.wikimedia.org/wikipedia/commons/e/ef/Chess_ndt45.svg",
    b: "https://upload.wikimedia.org/wikipedia/commons/9/98/Chess_bdt45.svg",
    r: "https://upload.wikimedia.org/wikipedia/commons/f/ff/Chess_rdt45.svg",
    q: "https://upload.wikimedia.org/wikipedia/commons/4/47/Chess_qdt45.svg",
    k: "https://upload.wikimedia.org/wikipedia/commons/f/f0/Chess_kdt45.svg",
    P: "https://upload.wikimedia.org/wikipedia/commons/4/45/Chess_plt45.svg",
    N: "https://upload.wikimedia.org/wikipedia/commons/7/70/Chess_nlt45.svg",
    B: "https://upload.wikimedia.org/wikipedia/commons/b/b1/Chess_blt45.svg",
    R: "https://upload.wikimedia.org/wikipedia/commons/7/72/Chess_rlt45.svg",
    Q: "https://upload.wikimedia.org/wikipedia/commons/1/15/Chess_qlt45.svg",
    K: "https://upload.wikimedia.org/wikipedia/commons/1/10/Chess_klt45.svg",
};

export function MiniBoard({ fen, orientation = "white", lastMove, markers, className }: MiniBoardProps) {
    const chess = useMemo(() => new Chess(fen), [fen]);
    const board = chess.board(); // 8x8 array

    const isFlipped = orientation === "black";

    // Helper to get coordinates
    const getCoords = (square: string) => {
        const file = square.charCodeAt(0) - 97; // 'a' -> 0
        const rank = 8 - parseInt(square[1]); // '8' -> 0
        return { x: isFlipped ? 7 - file : file, y: isFlipped ? 7 - rank : rank };
    };

    return (
        <div className={`relative aspect-square select-none overflow-hidden rounded bg-[#dedede] ${className}`}>
            <svg viewBox="0 0 100 100" className="absolute inset-0 h-full w-full">
                {/* Board Squares */}
                {Array.from({ length: 64 }).map((_, i) => {
                    const x = i % 8;
                    const y = Math.floor(i / 8);
                    const isDark = (x + y) % 2 === 1;
                    return (
                        <rect
                            key={i}
                            x={x * 12.5}
                            y={y * 12.5}
                            width="12.5"
                            height="12.5"
                            fill={isDark ? "#8ca2ad" : "#dee3e6"} // Lichess-like colors
                        />
                    );
                })}

                {/* Last Move Highlight */}
                {lastMove && (
                    <>
                        {[lastMove.slice(0, 2), lastMove.slice(2, 4)].map((sq) => {
                            const { x, y } = getCoords(sq);
                            return (
                                <rect
                                    key={sq}
                                    x={x * 12.5}
                                    y={y * 12.5}
                                    width="12.5"
                                    height="12.5"
                                    fill="rgba(155, 199, 0, 0.41)" // Lichess highlight color
                                />
                            );
                        })}
                    </>
                )}

                {/* Pieces */}
                {board.map((row, y) =>
                    row.map((piece, x) => {
                        if (!piece) return null;
                        const renderX = isFlipped ? 7 - x : x;
                        const renderY = isFlipped ? 7 - y : y;
                        return (
                            <image
                                key={`${x}-${y}`}
                                href={PIECE_IMAGES[piece.color === "w" ? piece.type.toUpperCase() : piece.type]}
                                x={renderX * 12.5}
                                y={renderY * 12.5}
                                width="12.5"
                                height="12.5"
                            />
                        );
                    })
                )}
            </svg>
        </div>
    );
}
