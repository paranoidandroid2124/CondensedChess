import { type Prop } from './common';
export declare const storage: ChesstoryStorageHelper;
export declare const tempStorage: ChesstoryStorageHelper;
export interface StoredProp<V> extends Prop<V> {
    (replacement?: V): V;
}
export declare function storedProp<V>(key: string, defaultValue: V, fromStr: (str: string) => V, toStr: (v: V) => string): StoredProp<V>;
export declare const storedStringProp: (k: string, defaultValue: string) => StoredProp<string>;
export declare const storedBooleanProp: (k: string, defaultValue: boolean) => StoredProp<boolean>;
export declare const storedBooleanPropWithEffect: (k: string, defaultValue: boolean, effect: (v: boolean) => void) => Prop<boolean>;
export declare const storedIntProp: (k: string, defaultValue: number) => StoredProp<number>;
export declare function once(key: string, every?: {
    seconds?: number;
    hours?: number;
    days?: number;
}): boolean;
export interface ChesstoryStorage {
    get(): string | null;
    set(v: any): void;
    remove(): void;
    listen(f: (e: ChesstoryStorageEvent) => void): void;
    fire(v?: string): void;
}
export interface ChesstoryBooleanStorage {
    get(): boolean;
    getOrDefault(defaultValue: boolean): boolean;
    set(v: boolean): void;
    toggle(): void;
}
interface ChesstoryStorageHelper {
    make(k: string, ttl?: number): ChesstoryStorage;
    boolean(k: string): ChesstoryBooleanStorage;
    get(k: string): string | null;
    set(k: string, v: string): void;
    fire(k: string, v?: string): void;
    remove(k: string): void;
}
interface ChesstoryStorageEvent {
    sri: string;
    nonce: number;
    value?: string;
}
export {};
