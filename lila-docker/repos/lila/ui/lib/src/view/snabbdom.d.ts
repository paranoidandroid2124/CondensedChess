 
import { type VNode, type VNodeData, type VNodeChildElement, type VNodeChildren, type Hooks, type Attrs, type Classes, thunk } from 'snabbdom';
export type { Attrs, Hooks, Classes, VNode, VNodeData, VNodeChildElement, VNodeChildren };
export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[];
export { thunk };
export declare function onInsert<A extends HTMLElement>(f: (element: A) => void): Hooks;
export declare function bind<K extends keyof GlobalEventHandlersEventMap>(eventName: K, f: (ev: GlobalEventHandlersEventMap[K]) => any, redraw?: Redraw, passive?: boolean): Hooks;
export declare const bindNonPassive: <K extends keyof GlobalEventHandlersEventMap>(eventName: K, f: (ev: GlobalEventHandlersEventMap[K]) => any, redraw?: Redraw) => Hooks;
export declare function bindSubmit(f: (e: SubmitEvent) => unknown, redraw?: () => void): Hooks;
export declare const dataIcon: (icon: string) => Attrs;
export declare const iconTag: (name: string) => VNode;
import { icons } from '../icons';
export declare function icon(name: keyof typeof icons, className?: string): VNode;
export type LooseVNode = VNodeChildElement | boolean;
export type LooseVNodes = LooseVNode | LooseVNodes[];
export declare function hl(sel: string, dataOrKids?: VNodeData | LooseVNodes, kids?: LooseVNodes): VNode;
export declare const noTrans: (s: string) => VNode;
