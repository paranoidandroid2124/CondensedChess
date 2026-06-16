import type { Hooks } from 'snabbdom';
export declare const innerHTML: <A>(a: A, toHtml: (a: A) => string) => Hooks;
export declare function enrichText(text: string, allowNewlines?: boolean): string;
