interface PermaLog {
    (...args: any[]): Promise<number | void>;
    clear(): Promise<void>;
    get(): Promise<string>;
}
export declare const log: PermaLog;
export {};
