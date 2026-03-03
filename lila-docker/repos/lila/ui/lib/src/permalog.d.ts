import { type DbInfo } from './objectStorage';
export interface PermaLog {
    (...args: any[]): Promise<number | void>;
    clear(): Promise<void>;
    get(): Promise<string>;
}
export declare const log: PermaLog;
export declare function makeLog(dbInfo: DbInfo, windowSize: number): PermaLog;
