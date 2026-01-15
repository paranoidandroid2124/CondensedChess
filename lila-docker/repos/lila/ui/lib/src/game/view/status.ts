import type { GameData, Source, StatusName } from '../index';

export function bishopOnColor(expandedFen: string, offset: 0 | 1): boolean {
  if (expandedFen.length !== 64) throw new Error('Expanded FEN expected to be 64 characters');

  for (let row = 0; row < 8; row++) {
    for (let col = row % 2 === offset ? 0 : 1; col < 8; col += 2) {
      if (/[bB]/.test(expandedFen[row * 8 + col])) return true;
    }
  }
  return false;
}

export function expandFen(fullFen: FEN): string {
  return fullFen
    .split(' ')[0]
    .replace(/\d/g, n => '1'.repeat(+n))
    .replace(/\//g, '');
}

export function insufficientMaterial(variant: VariantKey, fullFen: FEN): boolean {
  // TODO: atomic, antichess, threeCheck
  if (
    variant === 'horde' ||
    variant === 'kingOfTheHill' ||
    variant === 'racingKings' ||
    variant === 'crazyhouse' ||
    variant === 'atomic' ||
    variant === 'antichess' ||
    variant === 'threeCheck'
  )
    return false;
  const pieces = fullFen.split(' ')[0].replace(/[^a-z]/gi, '');
  if (/^[Kk]{2}$/.test(pieces)) return true;
  if (/[prq]/i.test(pieces)) return false;
  if (/^[KkNn]{3}$/.test(pieces)) return true;
  if (/b/i.test(pieces)) {
    const expandedFen = expandFen(fullFen);
    return (!bishopOnColor(expandedFen, 0) || !bishopOnColor(expandedFen, 1)) && !/[nN]/.test(pieces);
  }
  return false;
}

export interface StatusData {
  winner: Color | undefined;
  status: StatusName;
  ply: Ply;
  fen: FEN;
  variant: VariantKey;
  fiftyMoves?: boolean;
  threefold?: boolean;
  drawOffers?: number[];
  source?: Source;
}

export default function status(d: GameData): string {
  return statusOf({
    winner: d.game.winner,
    status: d.game.status.name,
    ply: d.game.turns,
    fen: d.game.fen,
    variant: d.game.variant.key,
    fiftyMoves: d.game.fiftyMoves,
    threefold: d.game.threefold,
    drawOffers: d.game.drawOffers,
    source: d.game.source,
  });
}
export function statusOf(d: StatusData): string {
  const winnerSuffix = d.winner
    ? ' • ' + (d.winner === 'white' ? 'White is victorious' : 'Black is victorious')
    : '';
  switch (d.status) {
    case 'started':
      return 'Playing right now';
    case 'aborted':
      return 'Game aborted' + winnerSuffix;
    case 'mate':
      return 'Checkmate' + winnerSuffix;
    case 'resign':
      return (d.winner === 'white' ? 'Black resigned' : 'White resigned') + winnerSuffix;
    case 'stalemate':
      return 'Stalemate' + winnerSuffix;
    case 'timeout':
      switch (d.winner) {
        case 'white':
          return 'Black left the game' + winnerSuffix;
        case 'black':
          return 'White left the game' + winnerSuffix;
        default:
          return `${d.ply % 2 === 0 ? 'White left the game' : 'Black left the game'} • Draw`;
      }
    case 'draw': {
      if (d.fiftyMoves || d.fen.split(' ')[4] === '100')
        return 'Fifty moves without progress • Draw';
      if (d.threefold) return 'Threefold repetition • Draw';
      if (insufficientMaterial(d.variant, d.fen))
        return 'Insufficient material • Draw';
      if (d.drawOffers?.some(turn => turn >= d.ply)) return 'Draw by mutual agreement';
      return 'Draw';
    }
    case 'insufficientMaterialClaim':
      return 'Draw claimed • Insufficient material';
    case 'outoftime':
      return `${d.ply % 2 === 0 ? 'White ran out of time' : 'Black ran out of time'}${winnerSuffix || ' • Draw'
        }`;
    case 'noStart':
      return (d.winner === 'white' ? "Black didn't move" : "White didn't move") + winnerSuffix;
    case 'cheat':
      return 'Cheat detected' + winnerSuffix;
    case 'variantEnd':
      switch (d.variant) {
        case 'kingOfTheHill':
          return 'King in the center' + winnerSuffix;
        case 'threeCheck':
          return 'Three checks' + winnerSuffix;
      }
      return 'Variant ending' + winnerSuffix;
    case 'unknownFinish':
      return d.winner
        ? (d.winner === 'white' ? 'White is victorious' : 'Black is victorious')
        : 'Finished';
    default:
      return d.status + winnerSuffix;
  }
}
