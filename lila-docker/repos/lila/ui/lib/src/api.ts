import { type PubsubEvents, pubsub } from './pubsub';
import { domDialog } from './view/dialog';
import { alert, confirm, prompt } from './view/dialogs';

const publicEvents = ['ply', 'analysis.change', 'analysis.closeAll'] as const;
type PublicEventKey = (typeof publicEvents)[number] & keyof PubsubEvents;

export interface Api {
  initializeDom: (root?: HTMLElement) => void;
  events: {
    on<K extends PublicEventKey>(name: K, cb: PubsubEvents[K]): void;
    off<K extends PublicEventKey>(name: K, cb: PubsubEvents[K]): void;
  };
  dialog: {
    alert: typeof alert;
    confirm: typeof confirm;
    prompt: typeof prompt;
    domDialog: typeof domDialog;
  };
  analysis?: any;
}

// This object is available to extensions as window.chesstory.
export const api: Api = {
  initializeDom: (root?: HTMLElement) => {
    pubsub.emit('content-loaded', root);
  },
  events: {
    on<K extends PublicEventKey>(name: K, cb: PubsubEvents[K]): void {
      if (!publicEvents.includes(name)) throw 'This event is not part of the public API';
      pubsub.on(name, cb);
    },
    off<K extends PublicEventKey>(name: K, cb: PubsubEvents[K]): void {
      pubsub.off(name, cb);
    },
  },
  dialog: {
    alert,
    confirm,
    prompt,
    domDialog,
  },
};
