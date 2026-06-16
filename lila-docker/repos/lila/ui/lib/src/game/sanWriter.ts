/* eslint no-restricted-syntax:"error" */ // no side effects allowed due to re-export by index.ts

import { charToRole } from 'chessops';

const nvui = {
  sanTakes: 'takes',
  sanCheck: 'check',
  sanCheckmate: 'checkmate',
  sanPromotesTo: 'promotes to',
  sanLongCastling: 'long castling',
  sanShortCastling: 'short castling',
  gameStart: 'game start',
  king: 'King',
  queen: 'Queen',
  rook: 'Rook',
  bishop: 'Bishop',
  knight: 'Knight',
  pawn: 'Pawn',
};

const sanToWords = (san: string): string =>
  san
    .split('')
    .map(c => {
      if (c === 'x') return nvui.sanTakes;
      if (c === '+') return nvui.sanCheck;
      if (c === '#') return nvui.sanCheckmate;
      if (c === '=') return nvui.sanPromotesTo;
      const code = c.charCodeAt(0);
      if (code > 48 && code < 58) return c; // 1-8
      if (code > 96 && code < 105) return c.toUpperCase(); // a-h
      const role = charToRole(c);
      return role ? transRole(role) : c;
    })
    .join(' ')
    .replace('O - O - O', nvui.sanLongCastling)
    .replace('O - O', nvui.sanShortCastling);

const transRole = (role: Role): string =>
  (nvui[role as keyof typeof nvui] as string) || (role as string);

export function speakable(san?: San): string {
  const text = !san
    ? nvui.gameStart
    : sanToWords(san)
      .replace(/^A /, '"A"') // "A takes" & "A 3" are mispronounced
      .replace(/(\d) E (\d)/, '$1,E $2') // Strings such as 1E5 are treated as scientific notation
      .replace(/C /, 'c ') // Capital C is pronounced as "degrees celsius" when it comes after a number (e.g. R8c3)
      .replace(/F /, 'f ') // Capital F is pronounced as "degrees fahrenheit" when it comes after a number (e.g. R8f3)
      .replace(/(\d) H (\d)/, '$1H$2'); // "H" is pronounced as "hour" when it comes after a number with a space (e.g. Rook 5 H 3)
  return text;
}
