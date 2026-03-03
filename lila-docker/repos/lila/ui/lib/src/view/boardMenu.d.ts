 
import { type Toggle } from '@/index';
import { type MaybeVNode, type MaybeVNodes, type VNode } from './snabbdom';
export declare const toggleButton: (toggle: Toggle, title: string) => VNode;
export declare const boardMenu: (redraw: Redraw, toggle: Toggle, content: (menu: BoardMenu) => MaybeVNodes) => MaybeVNode;
export declare class BoardMenu {
    readonly redraw: Redraw;
    anonymous: boolean;
    constructor(redraw: Redraw);
    flip: (name: string, active: boolean, onChange: () => void) => VNode;
    zenMode: (enabled?: boolean) => VNode;
    voiceInput: (toggle: Toggle, enabled?: boolean) => VNode;
    keyboardInput: (toggle: Toggle, enabled?: boolean) => VNode;
    blindfold: (toggle: Toggle, enabled?: boolean) => VNode;
    confirmMove: (toggle: Toggle, enabled?: boolean) => VNode;
    private cmnToggle;
}
