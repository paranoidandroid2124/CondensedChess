export declare const prefersLightThemeQuery: () => MediaQueryList;
export declare function displayColumns(): number;
export declare const isTouchDevice: () => boolean;
export declare const isMobile: () => boolean;
export declare const isAndroid: () => boolean;
export declare const isIos: () => boolean;
export declare const isIPad: () => boolean;
type VersionConstraint = {
    atLeast?: string;
    below?: string;
};
export declare const isChrome: (constraint?: VersionConstraint) => boolean;
export declare const isSafari: (constraint?: VersionConstraint) => boolean;
export declare const isWebkit: (constraint?: VersionConstraint) => boolean;
export type Feature = 'wasm' | 'sharedMem' | 'simd' | 'dynamicImportFromWorker' | 'bigint' | 'structuredClone';
export declare const features: () => readonly Feature[];
export {};
