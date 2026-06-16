import type { GameData, Player } from './interfaces';

export type * from './interfaces';
export * from './chess';

export type { StatusName, Status } from './status';

const abortedStatusId = 25;
const mateStatusId = 30;

export const playedTurns = (data: GameData): number => data.game.turns - (data.game.startedAtTurn || 0);

export const playable = (data: GameData): boolean =>
  data.game.status.id < abortedStatusId && data.game.source !== 'import';

export const replayable = (data: GameData): boolean =>
  data.game.source === 'import' ||
  data.game.status.id >= mateStatusId ||
  (data.game.status.id === abortedStatusId && playedTurns(data) > 1);

export const getPlayer = (data: GameData, color: Color): Player =>
  data.player.color === color ? data.player : data.opponent;
