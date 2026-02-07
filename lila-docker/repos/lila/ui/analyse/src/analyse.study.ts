import { patch } from './view/util';
import makeStart from './start';
import type { AnalyseSocketSend } from './socket';

export { patch };

const start = makeStart(patch as any);

export async function initModule({ cfg }: { cfg: any }) {
  try {
    await site.asset.loadPieces;
  } catch (e) {
    console.warn('loadPieces failed', e);
  }
  cfg.socketSend = ((_: any, ..._args: any[]) => {}) as AnalyseSocketSend;
  start(cfg);
}

export default initModule;
