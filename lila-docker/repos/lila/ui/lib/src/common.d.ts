export declare const defined: <T>(value: T | undefined) => value is T;
export declare const notNull: <T>(value: T | null | undefined) => value is T;
export interface Prop<T> {
    (): T;
    (v: T): T;
}
export declare const prop: <A>(initialValue: A) => Prop<A>;
export declare const propWithEffect: <A>(initialValue: A, effect: (value: A) => void) => Prop<A>;
export declare const withEffect: <T>(prop: Prop<T>, effect: (v: T) => void) => Prop<T>;
export interface Toggle extends Prop<boolean> {
    toggle(): void;
}
export declare const toggle: (initialValue: boolean, effect?: (value: boolean) => void) => Toggle;
export declare const memoize: <A>(compute: () => A) => (() => A);
export declare const scrollToInnerSelector: (el: HTMLElement, selector: string, horiz?: boolean, behavior?: ScrollBehavior) => void;
export declare const onClickAway: (f: () => void) => (el: HTMLElement) => void;
export declare const requestIdleCallback: (f: () => void, timeout?: number) => void;
export declare function escapeHtml(str: string): string;
export declare function frag<T extends Node = Node>(html: string): T;
export declare function myUserId(): string | undefined;
export declare function myUsername(): string | undefined;
export declare function repeater(f: () => void, additionalStopCond?: () => boolean): void;
