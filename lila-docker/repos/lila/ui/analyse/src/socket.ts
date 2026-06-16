import type { EvalGetData, EvalPutData } from './interfaces';

export interface AnaMove { [key: string]: any }
export interface AnaDrop { [key: string]: any }

export interface AnaDestsReq {
  fen: FEN;
  path: string;
  ch?: string;
  variant?: VariantKey;
}

interface MoveOpts {
  write?: false;
  sticky?: false;
}

interface StudySocketSendParams {
  anaMove: (d: AnaMove & MoveOpts) => void;
  anaDrop: (d: AnaDrop & MoveOpts) => void;
  anaDests: (d: AnaDestsReq) => void;
}

interface EvalCacheSocketParams {
  evalPut: (d: EvalPutData) => void;
  evalGet: (d: EvalGetData) => void;
}

type AnalyseSocketSendParams = StudySocketSendParams &
  EvalCacheSocketParams & { startWatching: (gameId: string) => void };

export type AnalyseSocketSend = <K extends keyof AnalyseSocketSendParams>(
  event: K,
  ...args: Parameters<AnalyseSocketSendParams[K]>
) => void;

export interface Socket {
  send: AnalyseSocketSend;
  receive(type: string, data: any): boolean;
  sendAnaMove(d: AnaMove): void;
  sendAnaDrop(d: AnaDrop): void;
  sendAnaDests(d: AnaDestsReq): void;
  clearCache(): void;
}
