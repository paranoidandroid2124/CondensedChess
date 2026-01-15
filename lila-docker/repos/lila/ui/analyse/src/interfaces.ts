import type { Player, Status, Source, Clock } from 'lib/game';
import type { ForecastData } from './forecast/interfaces';


import type { ExplorerOpts } from './explorer/interfaces';

import type { AnalyseSocketSend } from './socket';
import type { ExternalEngineInfo } from 'lib/ceval';
import type { Coords, MoveEvent } from 'lib/prefs';
import type { EnhanceOpts } from 'lib/richText';




export interface AnalyseApi {
  socketReceive(type: string, data: any): boolean;
  path(): Tree.Path;
  setChapter(id: string): void;
}

export interface OpeningPuzzle {
  key: string;
  name: string;
  count: number;
}

// similar, but not identical, to game/GameData
export interface AnalyseData {
  game: Game;
  player: Player;
  opponent: Player;
  orientation: Color;
  spectator?: boolean; // for compat with GameData, for game functions
  takebackable: boolean;
  moretimeable: boolean;
  analysis?: Analysis;
  userAnalysis: boolean;
  forecast?: ForecastData;
  sidelines?: Tree.Node[][];
  treeParts: Tree.NodeOptionalChildren[];
  practiceGoal?: any;
  clock?: Clock;
  pref: AnalysePref;
  userTv?: {
    id: string;
  };
  puzzle?: OpeningPuzzle;
  externalEngines?: ExternalEngineInfo[];
}

export interface AnalysePref {
  coords: Coords;
  is3d?: boolean;
  showDests?: boolean;
  rookCastle?: boolean;
  destination?: boolean;
  highlight?: boolean;
  showCaptured?: boolean;
  animationDuration?: number;
  moveEvent: MoveEvent;
}

export interface ServerEvalData {
  ch: string;
  analysis?: Analysis;
  tree: Tree.Node;
  division?: Division;
}

export interface EvalHit {
  fen: FEN;
  knodes: number;
  depth: number;
  pvs: Tree.PvDataServer[];
  path: string;
}

export interface EvalHitMulti extends EvalScore {
  fen: FEN;
}

export interface EvalHitMultiArray {
  multi: EvalHitMulti[];
}

// similar, but not identical, to game/Game
export interface Game {
  id: string;
  status: Status;
  player: Color;
  turns: number;
  fen: FEN;
  startedAtTurn?: number;
  source: Source;
  speed: Speed;
  variant: Variant;
  winner?: Color;
  moveCentis?: number[];
  initialFen?: string;
  importedBy?: string;
  division?: Division;
  opening?: Opening;
  perf: string;
  rated?: boolean;
  threefold?: boolean;
}

export interface Opening {
  name: string;
  eco: string;
  ply: number;
}

export interface Division {
  middle?: number;
  end?: number;
}

export interface Analysis {
  id: string;
  white: AnalysisSide;
  black: AnalysisSide;
  partial?: boolean;
}

export interface AnalysisSide {
  acpl: number;
  inaccuracy: number;
  mistake: number;
  blunder: number;
  accuracy: number;
}

export interface AnalyseOpts {
  element: HTMLElement;
  data: AnalyseData;
  userId?: string;
  hunter: boolean;
  explorer: ExplorerOpts;
  socketSend: AnalyseSocketSend;
  study?: any;
  tagTypes?: string;
  practice?: any;
  relay?: any;
  $side?: Cash;
  $underboard?: Cash;
  chat: any & {
    enhance: EnhanceOpts;
    instance?: any;
  };
  bookmaker?: boolean;
  inlinePgn?: string;
  externalEngineEndpoint: string;
  embed?: boolean;
  socketUrl?: string;
  socketVersion?: number;
}

export interface JustCaptured extends Piece {
  promoted?: boolean;
}

export interface EvalGetData {
  fen: FEN;
  path: string;
  variant?: VariantKey;
  mpv?: number;
  up?: boolean;
}

export interface EvalPutData extends Tree.ServerEval {
  variant?: VariantKey;
}

export type Conceal = false | 'conceal' | 'hide' | null;
export type ConcealOf = (isMainline: boolean) => (path: Tree.Path, node: Tree.Node) => Conceal;
