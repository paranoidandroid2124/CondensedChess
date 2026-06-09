import type { EvalGetData, EvalPutData } from './interfaces';

export interface AnaMove { [key: string]: any }
export interface AnaDrop { [key: string]: any }
export interface AnaDests { [key: string]: any }

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

export interface ReqPosition {
  ch: string;
  path: string;
}

export interface StudySocketSendParams {
  anaMove: (d: AnaMove & MoveOpts) => void;
  anaDrop: (d: AnaDrop & MoveOpts) => void;
  anaDests: (d: AnaDestsReq) => void;
}

export interface EvalCacheSocketParams {
  evalPut: (d: EvalPutData) => void;
  evalGet: (d: EvalGetData) => void;
}

export type AnalyseSocketSendParams = StudySocketSendParams &
  EvalCacheSocketParams & { startWatching: (gameId: string) => void };

export type StudySocketSend = <K extends keyof StudySocketSendParams>(
  event: K,
  ...args: Parameters<StudySocketSendParams[K]>
) => void;
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
