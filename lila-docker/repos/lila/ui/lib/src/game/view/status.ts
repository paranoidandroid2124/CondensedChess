import type { GameData } from '../index';

function bishopOnColor(expandedFen: string, offset: 0 | 1): boolean {
  if (expandedFen.length !== 64) throw new Error('Expanded FEN expected to be 64 characters');

  for (let row = 0; row < 8; row++) {
    for (let col = row % 2 === offset ? 0 : 1; col < 8; col += 2) {
      const piece = expandedFen[row * 8 + col];
      if (piece === 'b' || piece === 'B') return true;
    }
  }
  return false;
}

function expandFen(fullFen: FEN): string {
  return fullFen
    .split(' ')[0]
    .replace(/\d/g, n => '1'.repeat(+n))
    .replace(/\//g, '');
}

function insufficientMaterial(fullFen: FEN): boolean {
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

export default function status(d: GameData): string {
  const game = d.game;
  const winnerSuffix = game.winner
    ? ' • ' + (game.winner === 'white' ? 'White is victorious' : 'Black is victorious')
    : '';
  switch (game.status.name) {
    case 'started':
      return 'Playing right now';
    case 'aborted':
      return 'Game aborted' + winnerSuffix;
    case 'mate':
      return 'Checkmate' + winnerSuffix;
    case 'resign':
      return (game.winner === 'white' ? 'Black resigned' : 'White resigned') + winnerSuffix;
    case 'stalemate':
      return 'Stalemate' + winnerSuffix;
    case 'timeout':
      switch (game.winner) {
        case 'white':
          return 'Black left the game' + winnerSuffix;
        case 'black':
          return 'White left the game' + winnerSuffix;
        default:
          return `${game.turns % 2 === 0 ? 'White left the game' : 'Black left the game'} • Draw`;
      }
    case 'draw': {
      if (game.fiftyMoves || game.fen.split(' ')[4] === '100') return 'Fifty moves without progress • Draw';
      if (game.threefold) return 'Threefold repetition • Draw';
      if (insufficientMaterial(game.fen)) return 'Insufficient material • Draw';
      if (game.drawOffers?.some(turn => turn >= game.turns)) return 'Draw by mutual agreement';
      return 'Draw';
    }
    case 'insufficientMaterialClaim':
      return 'Draw claimed • Insufficient material';
    case 'outoftime':
      return `${game.turns % 2 === 0 ? 'White ran out of time' : 'Black ran out of time'}${winnerSuffix || ' • Draw'}`;
    case 'noStart':
      return (game.winner === 'white' ? "Black didn't move" : "White didn't move") + winnerSuffix;
    case 'cheat':
      return 'Cheat detected' + winnerSuffix;
    case 'unknownFinish':
      return game.winner ? (game.winner === 'white' ? 'White is victorious' : 'Black is victorious') : 'Finished';
    default:
      return game.status.name + winnerSuffix;
  }
}
