 
import { type VNode, type Attrs, type LooseVNodes } from './snabbdom';
export interface Dialog {
    readonly view: HTMLElement;
    readonly dialog: HTMLDialogElement;
    readonly returnValue?: 'ok' | 'cancel' | string;
    show(): Promise<Dialog>;
    updateActions(actions?: Action | Action[]): void;
    close(returnValue?: string): void;
}
export interface DialogOpts {
    class?: string;
    css?: ({
        url: string;
    } | {
        hashed: string;
    })[];
    htmlText?: string;
    cash?: Cash;
    htmlUrl?: string;
    append?: {
        node: HTMLElement;
        where?: string;
        how?: 'after' | 'before' | 'child';
    }[];
    attrs?: {
        dialog?: Attrs;
        view?: Attrs;
    };
    focus?: string;
    actions?: Action | Action[];
    onClose?: (dialog: Dialog) => void;
    noCloseButton?: boolean;
    noClickAway?: boolean;
    noScrollable?: boolean;
    modal?: boolean;
}
export interface DomDialogOpts extends DialogOpts {
    parent?: Element;
    show?: boolean;
}
export interface SnabDialogOpts extends DialogOpts {
    vnodes?: LooseVNodes;
    onInsert?: (dialog: Dialog) => void;
}
export type ActionListener = (e: Event, dialog: Dialog, action: Action) => void;
export type Action = {
    selector?: string;
    event?: string | string[];
    listener: ActionListener;
} | {
    selector?: string;
    event?: string | string[];
    result: string;
};
export declare function domDialog(o: DomDialogOpts): Promise<Dialog>;
export declare function snabDialog(o: SnabDialogOpts): VNode;
