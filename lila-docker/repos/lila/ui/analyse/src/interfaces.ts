import type { Player, Status } from 'lib/game';

import type { ExplorerOpts } from './explorer/interfaces';

import type { AnalyseSocketSend } from './socket';
import type { ExternalEngineInfo } from 'lib/ceval';
import type { Coords, MoveEvent } from 'lib/prefs';

// similar, but not identical, to game/GameData
export interface AnalyseData {
  game: Game;
  player: Player;
  opponent: Player;
  orientation: Color;
  analysis?: Analysis;
  userAnalysis: boolean;
  sidelines?: Tree.Node[][];
  treeParts: Tree.NodeOptionalChildren[];
  pref: AnalysePref;
  externalEngines?: ExternalEngineInfo[];
}

export interface AnalysePref {
  coords: Coords;
  is3d?: boolean;
  rookCastle?: boolean;
  destination?: boolean;
  highlight?: boolean;
  showCaptured?: boolean;
  animationDuration?: number;
  moveEvent: MoveEvent;
}

export interface ImportHistoryAccount {
  provider: string;
  providerLabel: string;
  username: string;
  href: string;
  analysisCount: number;
  activityAt: string;
  lastAnalysedAt?: string;
}

export interface ImportHistoryAnalysis {
  id: string;
  title: string;
  provider?: string;
  providerLabel?: string;
  username?: string;
  href: string;
  openedAt: string;
  sourceType: string;
  result?: string;
  speed?: string;
  playedAtLabel?: string;
  variant?: string;
  opening?: string;
}

export interface ImportHistoryView {
  currentAnalysisId?: string;
  recentAccounts: ImportHistoryAccount[];
  recentAnalyses: ImportHistoryAnalysis[];
}

export interface StudyChapterSummary {
  id: string;
  url?: string;
}

export interface StudyView {
  id: string;
  chapterId: string;
  name: string;
  chapterName: string;
  canWrite: boolean;
  chapters: StudyChapterSummary[];
  url?: string;
  visibility?: string;
}

export interface ServerEvalData {
  ch: string;
  analysis?: Analysis;
  tree: Tree.Node;
}

export interface EvalHit {
  fen: FEN;
  knodes: number;
  depth: number;
  pvs: Tree.PvDataServer[];
  path: string;
}

// similar, but not identical, to game/Game
export interface Game {
  id: string;
  status: Status;
  turns: number;
  fen: FEN;
  startedAtTurn?: number;
  source?: string;
  variant: Variant;
  winner?: Color;
  moveCentis?: number[];
  initialFen?: string;
  opening?: Opening;
  threefold?: boolean;
}

export interface Opening {
  name: string;
  eco: string;
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
  study?: StudyView;
  inlinePgn?: string;
  importHistory?: ImportHistoryView;
  externalEngineEndpoint: string;
  embed?: boolean;
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
