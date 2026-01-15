import { patch } from './view/util';
import makeStart from './start';
import type { AnalyseOpts } from './interfaces';
import type { AnalyseSocketSend } from './socket';

export { patch };

const start = makeStart(patch as any);

export async function initModule(cfg: AnalyseOpts) {
  try {
    await site.asset.loadPieces;
  } catch (e) {
    console.warn('loadPieces failed', e);
  }
  cfg.socketSend = ((_: any, ..._args: any[]) => {}) as AnalyseSocketSend;
  start(cfg);
}
