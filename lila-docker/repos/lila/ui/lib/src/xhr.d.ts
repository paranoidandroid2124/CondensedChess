export declare const jsonHeader: {
    Accept: string;
};
export declare const defaultInit: RequestInit;
export declare const xhrHeader: {
    'X-Requested-With': string;
};
export declare const ensureOk: (res: Response) => Response;
export declare const json: (url: string, init?: RequestInit) => Promise<any>;
export declare const text: (url: string, init?: RequestInit) => Promise<string>;
export declare const script: (src: string) => Promise<void>;
export declare const url: (path: string, params: {
    [k: string]: string | number | boolean | undefined;
}) => string;
type ProcessLine<T> = (line: T) => void;
export declare const readNdJson: <T>(response: Response, processLine: ProcessLine<T>) => Promise<void>;
export declare function writeTextClipboard(url: string, callbackOnSuccess?: (() => void) | undefined): Promise<void>;
export {};
