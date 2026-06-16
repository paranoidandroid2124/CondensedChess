/* eslint no-restricted-syntax:"error" */ // no side effects allowed due to re-export by index.ts

import type { Status } from './status';

export interface GameData {
  game: Game;
  player: Player;
  opponent: Player;
}

export interface Game {
  id: string;
  status: Status;
  turns: number;
  fen: FEN;
  startedAtTurn?: number;
  source?: string;
  variant: Variant;
  winner?: Color;
  drawOffers?: number[];
  moveCentis?: number[];
  initialFen?: string;
  threefold?: boolean;
  fiftyMoves?: boolean;
}

export interface Player {
  name?: string | null;
  user?: PlayerUser;
  color: Color;
  ai?: number;
  ratingDiff?: number;
  checks?: number;
}

export interface PlayerUser {
  username: string;
}
