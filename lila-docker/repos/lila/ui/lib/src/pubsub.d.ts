import type { Data as WatchersData } from '@/view/watchers';
export type PubsubEventKey = keyof PubsubEvents;
export interface PubsubEvents {
    'ab.rep': (data: 'kbc') => void;
    'analysis.closeAll': () => void;
    'analysis.change': (fen: FEN, path: string) => void;
    'analysis.chart.click': (index: number) => void;
    'analysis.comp.toggle': (enabled: boolean) => void;
    'analysis.server.progress': (analyseData: any) => void;
    'analysis.bookmaker.hover': (data: {
        fen: string;
        color: string;
        lastmove: string | undefined;
    } | null) => void;
    'analysis.bookmaker.move': (data: {
        uci: string;
        san: string | undefined;
    }) => void;
    'board.change': (is3d: boolean) => void;
    'challenge-app.open': () => void;
    'chart.panning': () => void;
    'content-loaded': (el?: HTMLElement) => void;
    flip: (flip: boolean) => void;
    jump: (ply: string) => void;
    'botdev.import.book': (key: string, oldKey?: string) => void;
    'notify-app.set-read': (user: string) => void;
    ply: (ply: number, isMainline?: boolean) => void;
    'ply.trigger': () => void;
    'round.suggestion': (text: string | null) => void;
    'socket.close': () => void;
    'socket.in.blockedBy': (userId: string) => void;
    'socket.in.challenges': (data: any) => void;
    'socket.in.crowd': (data: {
        nb: number;
        users?: string[];
        anons?: number;
        watchers?: WatchersData;
        streams?: [UserId, {
            name: string;
            lang: string;
        }][];
    }) => void;
    'socket.in.announce': (data: {
        msg?: string;
        date?: string;
    }) => void;
    'socket.in.endData': (data: any) => void;
    'socket.in.fen': (data: {
        id: string;
        fen: FEN;
        lm: Uci;
        wc?: number;
        bc?: number;
    }) => void;
    'socket.in.finish': (data: {
        id: string;
        win?: 'b' | 'w';
    }) => void;
    'socket.in.following_enters': (titleName: string, msg: {
        playing: boolean;
        patronColor?: PatronColor;
    }) => void;
    'socket.in.following_leaves': (titleName: string) => void;
    'socket.in.following_onlines': (friends: string[], msg: {
        playing: string[];
        patronColors: PatronColor[];
    }) => void;
    'socket.in.following_playing': (titleName: string) => void;
    'socket.in.following_stopped_playing': (titleName: string) => void;
    'socket.in.mlat': (millis: number) => void;
    'socket.in.notifications': (data: {
        notifications: Paginator<any>;
        unread: number;
    }) => void;
    'socket.in.redirect': (d: RedirectTo) => void;
    'socket.in.reload': (data: any) => void;
    'socket.in.sk1': (signed: string) => void;
    'socket.in.tournamentReminder': (data: {
        id: string;
        name: string;
    }) => void;
    'socket.in.unblockedBy': (userId: string) => void;
    'socket.in.serverRestart': () => void;
    'socket.lag': (lag: number) => void;
    'socket.open': () => void;
    'socket.send': (event: string, d?: any, o?: any) => void;
    theme: (theme: string) => void;
    zen: () => void;
}
export interface OneTimeEvents {
    'polyfill.dialog': ((dialog: HTMLElement) => void) | undefined;
    'socket.hasConnected': void;
    'botdev.images.ready': void;
}
export declare class Pubsub {
    private allSubs;
    private oneTimeEvents;
    on<K extends keyof PubsubEvents>(name: K, cb: PubsubEvents[K]): void;
    off<K extends keyof PubsubEvents>(name: K, cb: PubsubEvents[K]): void;
    emit<K extends keyof PubsubEvents>(name: K, ...args: Parameters<PubsubEvents[K]>): void;
    after<K extends OneTimeKey>(event: K): Promise<OneTimeEvents[K]>;
    complete<K extends OneTimeKey>(event: K, value?: OneTimeEvents[K]): void;
    past(event: OneTimeKey): boolean;
}
export declare const pubsub: Pubsub;
type OneTimeKey = keyof OneTimeEvents;
export {};
