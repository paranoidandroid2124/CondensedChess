import { type Prop } from '@/index';
export type UpdateImageHook = {
    markdown: Prop<string>;
} | {
    url: (img: HTMLElement, newUrl: string, width: number) => void;
};
export type ResizeArgs = {
    root: HTMLElement;
    update: UpdateImageHook;
    origin: string;
    designWidth: number;
};
export declare function wireMarkdownImgResizers({ root, update, designWidth, origin, }: ResizeArgs): Promise<void>;
export declare function wrapImg(arg: {
    img: HTMLImageElement;
} | {
    src: string;
    alt: string;
}): HTMLElement;
export declare function naturalSize(image: Blob): Promise<{
    width: number;
    height: number;
}>;
export declare function markdownPicfitRegex(origin?: string): RegExp;
