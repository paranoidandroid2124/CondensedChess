export declare const jsonHeader: {
    Accept: string;
};
export declare const defaultInit: RequestInit;
export declare const xhrHeader: {
    'X-Requested-With': string;
};
export declare const ensureOk: (res: Response) => Response;
export declare const jsonSimple: (url: string, init?: RequestInit) => Promise<any>;
export declare const json: (url: string, init?: RequestInit) => Promise<any>;
export declare const jsonAnyResponse: (url: string, init?: RequestInit) => Promise<any>;
export declare const text: (url: string, init?: RequestInit) => Promise<string>;
export declare const textRaw: (url: string, init?: RequestInit) => Promise<Response>;
export declare const script: (src: string) => Promise<void>;
export declare const form: (data: any) => FormData;
export declare const url: (path: string, params: {
    [k: string]: string | number | boolean | undefined;
}) => string;
export declare const formToXhr: (el: HTMLFormElement, submitter?: HTMLButtonElement) => Promise<string>;
export type ProcessLine<T> = (line: T) => void;
export declare const readNdJson: <T>(response: Response, processLine: ProcessLine<T>) => Promise<void>;
export declare function writeTextClipboard(url: string, callbackOnSuccess?: (() => void) | undefined): Promise<void>;
