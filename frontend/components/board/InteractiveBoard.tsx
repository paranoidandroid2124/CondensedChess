import React, { useEffect, useRef, useState } from 'react';
import { Chess } from 'chess.js'; // Removed unused Move import
import { Chessground } from 'chessground';
import { Api } from 'chessground/api';
import { Config } from 'chessground/config';
import { Color, Key } from 'chessground/types';
import { DrawShape } from 'chessground/draw';

interface InteractiveBoardProps {
    fen: string;
    orientation?: 'white' | 'black';
    onMove?: (orig: Key, dest: Key, fen: string) => void;
    className?: string;
    shapes?: DrawShape[];
    viewOnly?: boolean;
    lastMove?: [Key, Key]; // Sync external last move
    check?: boolean | Color; // Sync external check
}

export default function InteractiveBoard({
    fen,
    orientation = 'white',
    onMove,
    className,
    shapes = [],
    viewOnly = false,
    lastMove,
    check
}: InteractiveBoardProps) {
    const ref = useRef<HTMLDivElement>(null);
    const [api, setApi] = useState<Api | null>(null);
    // Safe initialization
    const safeGame = (f: string) => {
        try {
            return new Chess(f);
        } catch {
            return new Chess("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        }
    };
    const game = useRef(safeGame(fen));

    // Initialize Chessground
    useEffect(() => {
        if (!ref.current) return;

        const chess = game.current;

        const config: Config = {
            fen: fen,
            orientation: orientation,
            viewOnly: viewOnly,
            animation: { enabled: true, duration: 200 },
            movable: {
                color: 'both',
                free: false,
                dests: toDests(chess),
                events: {
                    after: (orig: Key, dest: Key) => {
                        try {
                            const move = chess.move({ from: orig, to: dest });
                            if (move) {
                                if (onMove) onMove(orig, dest, chess.fen());
                                api?.set({
                                    turnColor: toColor(chess),
                                    movable: {
                                        color: toColor(chess),
                                        dests: toDests(chess)
                                    },
                                    check: chess.inCheck()
                                });
                            }
                        } catch (e) {
                            console.error(e);
                        }
                    },
                },
            },
            drawable: {
                enabled: true,
                shapes: shapes,
            },
            premovable: { enabled: true },
            draggable: {
                enabled: true,
                showGhost: true,
            },
            highlight: {
                lastMove: true,
                check: true,
            },
        };

        const cgApi = Chessground(ref.current, config);
        setApi(cgApi);

        // ResizeObserver
        const ro = new ResizeObserver(() => {
            cgApi.redrawAll();
        });
        ro.observe(ref.current);

        return () => {
            cgApi.destroy();
            ro.disconnect();
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // Sync FEN & External State
    useEffect(() => {
        if (!api) return;

        // Load FEN if changed
        if (fen !== game.current.fen()) {
            try {
                game.current.load(fen);
            } catch {
                console.error("Invalid FEN:", fen);
            }
        }

        // Apply state to Chessground
        api.set({
            fen: fen,
            turnColor: toColor(game.current),
            movable: {
                color: toColor(game.current),
                dests: toDests(game.current)
            },
            // Use external props if provided, otherwise internal
            lastMove: lastMove ?? undefined,
            check: check ?? game.current.inCheck()
        });
    }, [fen, api, lastMove, check]);

    // Sync Orientation
    useEffect(() => {
        api?.set({ orientation });
    }, [orientation, api]);

    // Sync Shapes
    useEffect(() => {
        api?.set({ drawable: { shapes } });
    }, [shapes, api]);

    // Sync ViewOnly
    useEffect(() => {
        api?.set({ viewOnly });
    }, [viewOnly, api]);

    return (
        <div
            ref={ref}
            className={`is2d blues ${className}`}
            style={{
                width: '100%',
                height: '100%',
                touchAction: 'none' // Prevent scrolling on mobile
            }}
        />
    );
}

// Helpers
function toDests(chess: Chess): Map<Key, Key[]> {
    const dests = new Map<Key, Key[]>();
    chess.moves({ verbose: true }).forEach((m) => {
        const from = m.from as Key;
        const to = m.to as Key;
        if (!dests.has(from)) dests.set(from, []);
        dests.get(from)?.push(to);
    });
    return dests;
}

function toColor(chess: Chess): Color {
    return chess.turn() === 'w' ? 'white' : 'black';
}
