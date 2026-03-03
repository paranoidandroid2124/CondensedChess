/***
 * Wraps an asynchronous function to ensure only one call at a time is in
 * flight. Any extra calls are dropped, except the last one, which waits for
 * the previous call to complete.
 */
 
export declare function throttlePromiseWithResult<R, T extends (...args: any) => Promise<R>>(wrapped: T): (...args: Parameters<T>) => Promise<R>;
export declare function throttlePromise<T extends (...args: any) => Promise<void>>(wrapped: T): (...args: Parameters<T>) => Promise<void>;
/**
 * Wraps an asynchronous function to return a promise that resolves
 * after completion plus a delay (regardless if the wrapped function resolves
 * or rejects).
 */
export declare function finallyDelay<T extends (...args: any) => Promise<any>>(delay: (...args: Parameters<T>) => number, wrapped: T): (...args: Parameters<T>) => Promise<void>;
/**
 * Wraps an asynchronous function to ensure only one call at a time is in flight. Any extra calls
 * are dropped, except the last one, which waits for the previous call to complete plus a delay.
 */
export declare function throttlePromiseDelay<T extends (...args: any) => Promise<any>>(delay: (...args: Parameters<T>) => number, wrapped: T): (...args: Parameters<T>) => Promise<void>;
/**
 * Ensures calls to the wrapped function are spaced by the given delay.
 * Any extra calls are dropped, except the last one, which waits for the delay.
 */
export declare function throttle<T extends (...args: any) => void>(delay: number, wrapped: T): (...args: Parameters<T>) => void;
export interface Sync<T> {
    promise: Promise<T>;
    sync: T | undefined;
}
export declare function sync<T>(promise: Promise<T>): Sync<T>;
export declare function promiseTimeout<A>(asyncPromise: Promise<A>, timeLimit: number): Promise<A>;
export declare function debounce<T extends (...args: any) => void>(f: T, wait: number, immediate?: boolean): (...args: Parameters<T>) => void;
export interface Deferred<A> {
    promise: Promise<A>;
    resolve(a: A | PromiseLike<A>): void;
    reject(err: unknown): void;
}
export declare function defer<A>(): Deferred<A>;
