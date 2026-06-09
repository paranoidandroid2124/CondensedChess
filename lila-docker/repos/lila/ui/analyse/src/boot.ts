import type { AnalyseApi, AnalyseOpts } from './interfaces';
import type { AnalyseSocketSend } from './socket';

export default function (start: (opts: AnalyseOpts) => AnalyseApi) {
  return function (cfg: AnalyseOpts) {
    cfg.socketSend = ((_: any, ..._args: any[]) => { }) as AnalyseSocketSend;
    start(cfg);
  };
}
