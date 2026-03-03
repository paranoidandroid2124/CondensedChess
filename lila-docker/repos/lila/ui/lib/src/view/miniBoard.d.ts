 
import { type VNode } from 'snabbdom';
import { Chessground as makeChessground } from '@lichess-org/chessground';
export declare const initMiniBoard: (node: HTMLElement) => void;
export declare const initMiniBoardWith: (node: HTMLElement, config: CgConfig) => void;
export declare const initMiniBoards: (parent?: HTMLElement) => void;
export declare const renderClock: (color: Color, time: number) => VNode;
export declare const initMiniGame: (node: Element, withCg?: typeof makeChessground) => string | null;
export declare const getChessground: (node: HTMLElement) => CgApi;
export declare const initMiniGames: (parent?: HTMLElement) => void;
export declare const updateMiniGame: (node: HTMLElement, data: MiniGameUpdateData) => void;
export declare const finishMiniGame: (node: HTMLElement, win?: "b" | "w") => void;
interface MiniGameUpdateData {
    fen: FEN;
    lm: Uci;
    wc?: number;
    bc?: number;
}
export {};
