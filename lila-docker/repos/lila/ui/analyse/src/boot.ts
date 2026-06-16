import type { AnalyseOpts } from './interfaces';
import type { AnalyseSocketSend } from './socket';

export default function (start: (opts: AnalyseOpts) => void) {
  return function (cfg: AnalyseOpts) {
    cfg.socketSend = ((_: any, ..._args: any[]) => { }) as AnalyseSocketSend;
    start(cfg);
  };
}
