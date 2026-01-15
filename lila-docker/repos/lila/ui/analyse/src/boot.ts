import type { AnalyseApi, AnalyseOpts } from './interfaces';
import type { AnalyseSocketSend } from './socket';

export default function (start: (opts: AnalyseOpts) => AnalyseApi) {
  return function (cfg: AnalyseOpts) {
    cfg.$side = $('.analyse__side').clone();
    cfg.$underboard = $('.analyse__underboard').clone();
    cfg.socketSend = ((_: any, ..._args: any[]) => { }) as AnalyseSocketSend;
    start(cfg);
  };
}
