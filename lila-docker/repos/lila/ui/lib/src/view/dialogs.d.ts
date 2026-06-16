export declare function alert(msg: string): Promise<void>;
export declare function confirm(msg: string, ok?: string, cancel?: string): Promise<boolean>;
export declare function prompt(msg: string, def?: string, valid?: (text: string) => boolean): Promise<string | null>;
