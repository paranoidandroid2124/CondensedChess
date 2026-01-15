import { initial as initialBoardFEN } from '@lichess-org/chessground/fen';
import { ops as treeOps } from 'lib/tree/tree';
import type AnalyseCtrl from './ctrl';
import type { EvalGetData, EvalPutData, ServerEvalData } from './interfaces';

export interface AnaMove { [key: string]: any }
export interface AnaDrop { [key: string]: any }
export interface AnaDests { [key: string]: any }

interface DestsCache {
  [fen: string]: AnaDests;
}

interface AnaDestsReq {
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

interface GameUpdate {
  id: string;
  fen: FEN;
  lm: Uci;
  wc?: number;
  bc?: number;
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

export function make(send: AnalyseSocketSend, ctrl: AnalyseCtrl): Socket {
  let anaMoveTimeout: number | undefined;
  let anaDestsTimeout: number | undefined;

  let anaDestsCache: DestsCache = {};

  function clearCache() {
    anaDestsCache =
      ctrl.data.game.variant.key === 'standard' && ctrl.tree.root.fen.split(' ', 1)[0] === initialBoardFEN
        ? {
          '': {
            path: '',
            dests: 'iqy muC gvx ltB bqs pxF jrz nvD ksA owE',
          },
        }
        : {};
  }
  clearCache();

  // forecast mode: reload when opponent moves
  if (!ctrl.synthetic)
    setTimeout(function () {
      send('startWatching', ctrl.data.game.id);
    }, 1000);

  function currentChapterId(): string | undefined {
    return undefined;
  }

  function addStudyData(_req: { ch?: string } & MoveOpts, _isWrite = false) {
  }

  const handlers = {
    node(data: { ch?: string; node: Tree.Node; path: string }) {
      clearTimeout(anaMoveTimeout);
      if (data.ch === currentChapterId()) ctrl.addNode(data.node, data.path);
      else console.log('socket handler node got wrong chapter id', data);
    },
    stepFailure() {
      clearTimeout(anaMoveTimeout);
      ctrl.reset();
    },
    dests(data: AnaDests) {
      clearTimeout(anaDestsTimeout);
      if (!data.ch || data.ch === currentChapterId()) {
        anaDestsCache[data.path] = data;
        ctrl.addDests(data.dests, data.path);
      } else console.log('socket handler node got wrong chapter id', data);
    },
    destsFailure(data: any) {
      console.log(data);
      clearTimeout(anaDestsTimeout);
    },
    fen(e: GameUpdate) {
      if (
        ctrl.forecast &&
        e.id === ctrl.data.game.id &&
        treeOps.last(ctrl.mainline)!.fen.indexOf(e.fen) !== 0
      )
        ctrl.forecast.reloadToLastPly();
    },
    analysisProgress(data: ServerEvalData) {
      ctrl.mergeAnalysisData(data);
    },
    evalHit: ctrl.evalCache.onCloudEval,
  };

  function withoutStandardVariant(obj: { variant?: VariantKey }) {
    if (obj.variant === 'standard') delete obj.variant;
  }

  function sendAnaDests(req: AnaDestsReq) {
    clearTimeout(anaDestsTimeout);
    if (anaDestsCache[req.path]) setTimeout(() => handlers.dests(anaDestsCache[req.path]), 300);
    else {
      withoutStandardVariant(req);
      addStudyData(req);
      send('anaDests', req);
      anaDestsTimeout = setTimeout(function () {
        console.log(req, 'resendAnaDests');
        sendAnaDests(req);
      }, 3000);
    }
  }

  function sendAnaMove(req: AnaMove) {
    clearTimeout(anaMoveTimeout);
    withoutStandardVariant(req);
    addStudyData(req, true);
    send('anaMove', req);
    anaMoveTimeout = setTimeout(() => sendAnaMove(req), 3000);
  }

  function sendAnaDrop(req: AnaDrop) {
    clearTimeout(anaMoveTimeout);
    withoutStandardVariant(req);
    addStudyData(req, true);
    send('anaDrop', req);
    anaMoveTimeout = setTimeout(() => sendAnaDrop(req), 3000);
  }

  return {
    receive(type, data) {
      const handler = (handlers as SocketHandlers)[type];
      if (handler) {
        handler(data);
        return true;
      }
      return false;
    },
    sendAnaMove,
    sendAnaDrop,
    sendAnaDests,
    clearCache,
    send,
  };
}
