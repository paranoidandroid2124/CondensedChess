 
export declare const defined: <T>(value: T | undefined) => value is T;
export declare const notNull: <T>(value: T | null | undefined) => value is T;
export declare const isEmpty: <T>(a: T[] | undefined) => boolean;
export declare const notEmpty: <T>(a: T[] | undefined) => boolean;
export interface Prop<T> {
    (): T;
    (v: T): T;
}
export interface PropWithEffect<T> extends Prop<T> {
}
export declare const prop: <A>(initialValue: A) => Prop<A>;
export declare const propWithEffect: <A>(initialValue: A, effect: (value: A) => void) => PropWithEffect<A>;
export declare const withEffect: <T>(prop: Prop<T>, effect: (v: T) => void) => PropWithEffect<T>;
export interface Toggle extends PropWithEffect<boolean> {
    toggle(): void;
    effect(value: boolean): void;
}
export declare const toggle: (initialValue: boolean, effect?: (value: boolean) => void) => Toggle;
export declare const toggleWithConstraint: (value: boolean, constraint: () => boolean) => Toggle;
export declare const memoize: <A>(compute: () => A) => (() => A);
export declare const scrollToInnerSelector: (el: HTMLElement, selector: string, horiz?: boolean, behavior?: ScrollBehavior) => void;
export declare const scrollTo: (el: HTMLElement, target: HTMLElement | null, horiz?: boolean, behavior?: ScrollBehavior) => void;
export declare const onClickAway: (f: () => void) => (el: HTMLElement) => void;
export declare function hyphenToCamel(str: string): string;
export declare const requestIdleCallback: (f: () => void, timeout?: number) => void;
export declare function escapeHtml(str: string): string;
export declare function frag<T extends Node = Node>(html: string): T;
export declare function scopedQuery(scope: Element): <T extends Element = HTMLElement>(sel: string) => T | null;
export declare function myUserId(): string | undefined;
export declare function myUsername(): string | undefined;
export declare function repeater(f: () => void, additionalStopCond?: () => boolean): void;
