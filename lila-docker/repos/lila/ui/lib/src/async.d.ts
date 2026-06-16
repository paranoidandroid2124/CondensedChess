/**
 * Ensures calls to the wrapped function are spaced by the given delay.
 * Any extra calls are dropped, except the last one, which waits for the delay.
 */
export declare function throttle<T extends (...args: any) => void>(delay: number, wrapped: T): (...args: Parameters<T>) => void;
export declare function debounce<T extends (...args: any) => void>(f: T, wait: number, immediate?: boolean): (...args: Parameters<T>) => void;
