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
    className?: string; // For sizing
    shapes?: DrawShape[];
    viewOnly?: boolean;
}

export default function InteractiveBoard({
    fen,
    orientation = 'white',
    onMove,
    className,
    shapes = [],
    viewOnly = false
}: InteractiveBoardProps) {
    const ref = useRef<HTMLDivElement>(null);
    const [api, setApi] = useState<Api | null>(null);
    const game = useRef(new Chess(fen));

    // Initialize Chessground
    useEffect(() => {
        if (!ref.current) return;

        const chess = game.current; // Stable reference

        const config: Config = {
            fen: fen,
            orientation: orientation,
            viewOnly: viewOnly,
            animation: { enabled: true, duration: 200 },
            movable: {
                color: 'both', // controlled by game logic
                free: false,
                dests: toDests(chess),
                events: {
                    after: (orig: Key, dest: Key) => {
                        // Apply move to internal logic
                        try {
                            const move = chess.move({ from: orig, to: dest });
                            if (move) {
                                // Valid move
                                if (onMove) onMove(orig, dest, chess.fen());
                                // Update dests for next turn
                                api?.set({
                                    turnColor: toColor(chess),
                                    movable: {
                                        color: toColor(chess),
                                        dests: toDests(chess)
                                    },
                                    check: chess.inCheck()
                                });
                            } else {
                                console.warn("Invalid move attempted", orig, dest);
                            }
                        } catch (e) {
                            console.error(e);
                            // Snapback handled by chessground if we don't update state
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

        // Cleanup
        return () => cgApi.destroy();
    }, []); // Run once on mount

    // Sync FEN changes from props
    useEffect(() => {
        if (!api) return;

        // Check if external FEN is different from internal state
        if (fen !== game.current.fen()) {
            try {
                game.current.load(fen);
                api.set({
                    fen: fen,
                    turnColor: toColor(game.current),
                    movable: {
                        color: toColor(game.current),
                        dests: toDests(game.current)
                    },
                    check: game.current.inCheck(),
                    lastMove: undefined
                });
            } catch (e) {
                console.error("Invalid FEN passed to InteractiveBoard:", fen);
            }
        }
    }, [fen, api]);

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
            style={{ width: '100%', height: '100%' }} // Ensure it accepts parent size
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
