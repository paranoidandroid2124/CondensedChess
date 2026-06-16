import { fenToEpd } from 'lib/game/chess';

export const nextGlyphSymbol = (
  color: Color,
  symbol: string,
  mainline: Tree.Node[],
  fromPly: number,
): Tree.Node | undefined =>
  mainline
    .map((_, i) => mainline[(fromPly - mainline[0].ply + i + 1) % mainline.length])
    .find(n => n.ply % 2 === (color === 'white' ? 1 : 0) && n.glyphs?.some(g => g.symbol === symbol));

// can be 3fold or 5fold
export function add3or5FoldGlyphs(mainlineNodes: Tree.Node[]): boolean {
  // Only the final position can trigger a threefold or fivefold repetition.
  const lastEpd = fenToEpd(mainlineNodes[mainlineNodes.length - 1].fen);
  const repetitions = mainlineNodes.filter(n => fenToEpd(n.fen) === lastEpd);
  if (repetitions.length > 2) {
    const unicodeList = ['①', '②', '③', '④', '⑤'];
    for (const [i, node] of repetitions.slice(0, unicodeList.length).entries()) {
      const unicode = unicodeList[i];
      const glyph = { symbol: unicode, name: `repetition number ${i + 1}`, id: 9 };
      if (!node.glyphs) node.glyphs = [glyph];
      else node.glyphs.push(glyph);
    }
    return true;
  }
  return false;
}
