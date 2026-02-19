import type { OpeningReferencePayload } from './types';

const openingPhase = (ply: number): string => {
  if (ply <= 16) return 'opening';
  if (ply <= 60) return 'middlegame';
  return 'endgame';
};

export async function fetchOpeningReferenceViaProxy(
  analysisFen: string,
  ply: number,
  useExplorerProxy: boolean,
): Promise<OpeningReferencePayload | null> {
  if (!useExplorerProxy) return null;
  if (ply < 1 || ply > 30) return null;
  if (openingPhase(ply) !== 'opening') return null;

  try {
    const explorerRes = await fetch(`/api/llm/opening/masters?fen=${encodeURIComponent(analysisFen)}`);
    if (!explorerRes.ok) return null;

    const raw = (await explorerRes.json()) as any;
    const topMoves = (raw.topMoves || []).map((m: any) => ({
      uci: m.uci,
      san: m.san,
      total: m.total || 0,
      white: m.white || 0,
      draws: m.draws || 0,
      black: m.black || 0,
      performance: m.performance || 0,
    }));

    const sampleGames = await Promise.all(
      (raw.sampleGames || []).slice(0, 3).map(async (g: any) => {
        let pgn = g.pgn || null;
        if (!pgn && g.id) {
          try {
            const pgnRes = await fetch(`/api/llm/opening/master-pgn/${encodeURIComponent(g.id)}`);
            if (pgnRes.ok) pgn = await pgnRes.text();
          } catch {}
        }
        return {
          id: g.id,
          winner: g.winner,
          white: { name: g.white?.name || '?', rating: g.white?.rating || 0 },
          black: { name: g.black?.name || '?', rating: g.black?.rating || 0 },
          year: g.year || 0,
          month: g.month || 1,
          event: g.event,
          pgn,
        };
      }),
    );

    return {
      eco: raw.eco,
      name: raw.name,
      totalGames: raw.totalGames || 0,
      topMoves,
      sampleGames,
    };
  } catch (e) {
    console.warn('Bookmaker: failed to fetch opening reference via proxy', e);
    return null;
  }
}
