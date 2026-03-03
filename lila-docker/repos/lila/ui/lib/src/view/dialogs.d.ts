 
import { type Dialog } from './dialog';
export declare function alert(msg: string): Promise<void>;
export declare function alerts(msgs: string[]): Promise<void>;
export declare function info(msg: string, autoDismiss?: Millis): Promise<Dialog>;
export declare function confirm(msg: string, ok?: string, cancel?: string): Promise<boolean>;
export declare function prompt(msg: string, def?: string, valid?: (text: string) => boolean): Promise<string | null>;
export declare function choose(msg: string, options: string[], initial?: string, mustChoose?: boolean): Promise<string | undefined>;
export declare const makeLinkPopups: (dom: HTMLElement | Cash, selector?: string) => void;
