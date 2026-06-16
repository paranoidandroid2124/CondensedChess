import { type Hooks, type VNode, type Attrs } from 'snabbdom';
export interface ToggleSettings {
    name: string;
    title?: string;
    id: string;
    checked: boolean;
    disabled?: boolean;
    cls?: string;
    change(v: boolean): void;
}
export declare function toggle(t: ToggleSettings, redraw: () => void): VNode;
export declare function toggleBoxInit(): void;
export declare function rangeConfig(read: () => number, write: (value: number) => void): Hooks;
export declare function stepwiseScroll(inner: (e: WheelEvent, scroll: boolean) => void): (e: WheelEvent) => void;
export declare function copyMeInput(content: string, inputAttrs?: Attrs): VNode;
export declare const spinnerHtml: string;
export declare const spinnerVdom: (box?: string) => VNode;
