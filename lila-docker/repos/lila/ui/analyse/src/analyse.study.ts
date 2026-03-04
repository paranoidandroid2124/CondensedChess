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
  const useExplorerProxy = document.body.dataset.brandExplorerProxy !== '0';
  if (useExplorerProxy && cfg?.explorer) cfg.explorer.endpoint = `${location.origin}/api/explorer`;
  cfg.socketSend = ((_: any, ..._args: any[]) => {}) as AnalyseSocketSend;
  start(cfg);
}

export default initModule;
