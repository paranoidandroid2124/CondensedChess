import { patch } from './view/util';
import makeBoot from './boot';
import makeStart from './start';
import type { AnalyseSocketSend } from './socket';

export { patch };

const start = makeStart(patch);
const boot = makeBoot(start);

export async function initModule({ mode, cfg }: { mode: 'userAnalysis' | 'replay'; cfg: any }) {
  try {
    await site.asset.loadPieces;
  } catch (e) {
    console.warn('loadPieces failed', e);
  }
  if (mode === 'replay') boot(cfg);
  else userAnalysis(cfg);
}

function userAnalysis(cfg: any) {
  cfg.$side = $('.analyse__side').clone();
  cfg.socketSend = ((_: any, ..._args: any[]) => { }) as AnalyseSocketSend;
  start(cfg);
}
