import type { CevalEngine, Work } from 'lib/ceval';
import type AnalyseCtrl from '../ctrl';
import type { EvalVariation, ProbeRequest, ProbeResult } from './types';

type SessionActive = (session: number) => boolean;

export type ProbeOrchestrator = {
  stop: () => void;
  evalToVariations: (ceval: any, maxPvs: number) => EvalVariation[] | null;
  runPositionEval: (
    fen: string,
    depth: number,
    timeoutMs: number,
    multiPv: number,
    session: number,
  ) => Promise<Tree.LocalEval | null>;
  runProbeEval: (
    fen: string,
    move: string,
    depth: number,
    timeoutMs: number,
    multiPv: number,
    session: number,
  ) => Promise<Tree.LocalEval | null>;
  runProbes: (probeRequests: ProbeRequest[], baselineEvalCp: number, session: number) => Promise<ProbeResult[]>;
};

export function createProbeOrchestrator(ctrl: AnalyseCtrl | undefined, isSessionActive: SessionActive): ProbeOrchestrator {
  let probeEngine: CevalEngine | undefined;

  const ensureProbeEngine = (): CevalEngine | undefined => {
    if (!ctrl) return;
    try {
      probeEngine ??= ctrl.ceval.engines.make({ variant: ctrl.data.game.variant.key });
      return probeEngine;
    } catch {
      return;
    }
  };

  const stop = () => {
    try {
      probeEngine?.stop();
    } catch { }
  };

  const workPlyAfterMove = (fen: string): number => (fen.includes(' w ') ? 1 : 0);
  const workPlyAtFen = (fen: string): number => (fen.includes(' w ') ? 0 : 1);
  const variant = () => ctrl?.data.game.variant.key ?? 'standard';

  const runEval = async (
    path: string,
    fen: string,
    moves: string[],
    depth: number,
    timeoutMs: number,
    multiPv: number,
    ply: number,
    session: number,
    complete: (ev: Tree.LocalEval) => boolean,
  ): Promise<Tree.LocalEval | null> => {
    const engine = ensureProbeEngine();
    if (!engine) return null;

    engine.stop();

    return await new Promise<Tree.LocalEval | null>(resolve => {
      let best: Tree.LocalEval | null = null;
      let done = false;
      const finish = () => {
        if (done) return;
        done = true;
        clearTimeout(timer);
        try {
          engine.stop();
        } catch { }
        resolve(best);
      };
      const timer = setTimeout(finish, timeoutMs);
      const work: Work = {
        variant: variant(),
        threads: 1,
        hashSize: 16,
        gameId: undefined,
        stopRequested: false,
        path,
        search: { depth },
        multiPv,
        ply,
        threatMode: false,
        initialFen: fen,
        currentFen: fen,
        moves,
        emit: (ev: Tree.LocalEval) => {
          if (!isSessionActive(session)) return finish();
          best = ev;
          if (complete(ev)) finish();
        },
      };
      engine.start(work);
    });
  };

  const evalToVariations = (ceval: any, maxPvs: number): EvalVariation[] | null => {
    if (!ceval || !Array.isArray(ceval.pvs)) return null;
    return ceval.pvs
      .filter((pv: any) => Array.isArray(pv?.moves) && pv.moves.length)
      .slice(0, maxPvs)
      .map((pv: any) => ({
        moves: pv.moves.slice(0, 40),
        scoreCp: typeof pv.cp === 'number' ? pv.cp : 0,
        mate: typeof pv.mate === 'number' ? pv.mate : null,
        depth: typeof pv.depth === 'number' ? pv.depth : typeof ceval.depth === 'number' ? ceval.depth : 0,
      }));
  };

  const runPositionEval = async (
    fen: string,
    depth: number,
    timeoutMs: number,
    multiPv: number,
    session: number,
  ): Promise<Tree.LocalEval | null> =>
    runEval(
      `bookmaker-eval:${session}`,
      fen,
      [],
      depth,
      timeoutMs,
      multiPv,
      workPlyAtFen(fen),
      session,
      ev => {
        const pvCount = Array.isArray(ev.pvs) ? ev.pvs.filter(pv => Array.isArray(pv?.moves) && pv.moves.length).length : 0;
        return ev.depth >= depth && pvCount >= multiPv;
      },
    );

  const runProbeEval = async (
    fen: string,
    move: string,
    depth: number,
    timeoutMs: number,
    multiPv: number,
    session: number,
  ): Promise<Tree.LocalEval | null> =>
    runEval(
      `bookmaker-probe:${session}:${move}`,
      fen,
      [move],
      depth,
      timeoutMs,
      multiPv,
      workPlyAfterMove(fen),
      session,
      ev => ev.depth >= depth && Array.isArray(ev.pvs) && Boolean(ev.pvs[0]?.moves?.length),
    );

  const runProbes = async (
    probeRequests: ProbeRequest[],
    baselineEvalCp: number,
    session: number,
  ): Promise<ProbeResult[]> => {
    const highEffort =
      probeRequests.some(pr => typeof pr.purpose === 'string' && pr.purpose.length) ||
      probeRequests.some(pr => typeof pr.multiPv === 'number' && pr.multiPv >= 3) ||
      probeRequests.some(pr => typeof pr.depth === 'number' && pr.depth >= 20);

    const maxEffort =
      probeRequests.some(pr => pr.purpose === 'free_tempo_branches') ||
      probeRequests.some(pr => pr.purpose === 'latent_plan_refutation') ||
      probeRequests.some(pr => pr.purpose === 'recapture_branches') ||
      probeRequests.some(pr => pr.purpose === 'keep_tension_branches') ||
      probeRequests.some(pr => pr.purpose === 'convert_reply_multipv') ||
      probeRequests.some(pr => pr.purpose === 'defense_reply_multipv');

    const maxProbeMoves = maxEffort ? 16 : highEffort ? 10 : 6;
    const totalBudgetMs = maxEffort ? 35000 : highEffort ? 20000 : 8000;
    const flattened: { pr: ProbeRequest; move?: string }[] = probeRequests
      .flatMap(pr => {
        if (Array.isArray(pr.moves) && pr.moves.length > 0) {
          return pr.moves.map(move => ({ pr, move: move as string | undefined }));
        }
        return [{ pr, move: undefined as string | undefined }];
      })
      .slice(0, maxProbeMoves);

    if (!flattened.length) return [];

    const perMoveBudget = Math.max(
      highEffort ? 1000 : 700,
      Math.min(highEffort ? 5000 : 2000, Math.floor(totalBudgetMs / flattened.length)),
    );
    const results: ProbeResult[] = [];

    for (const { pr, move } of flattened) {
      if (!isSessionActive(session)) break;

      const baseCp = typeof pr.baselineEvalCp === 'number' ? pr.baselineEvalCp : baselineEvalCp;
      const depth = typeof pr.depth === 'number' && pr.depth > 0 ? pr.depth : 20;
      const multiPv = typeof pr.multiPv === 'number' && pr.multiPv > 0 ? pr.multiPv : 2;

      const ev = move
        ? await runProbeEval(pr.fen, move, depth, perMoveBudget, multiPv, session)
        : await runPositionEval(pr.fen, depth, perMoveBudget, multiPv, session);

      if (!ev || !isSessionActive(session)) continue;

      const replyPvs = Array.isArray(ev.pvs)
        ? ev.pvs
          .filter((pv: any) => Array.isArray(pv?.moves) && pv.moves.length)
          .slice(0, Math.max(1, Math.min(4, multiPv)))
          .map((pv: any) => pv.moves.slice(0, 12))
        : [];
      const evalCp = typeof ev.cp === 'number' ? ev.cp : 0;

      results.push({
        id: pr.id,
        fen: pr.fen,
        evalCp,
        bestReplyPv: replyPvs[0] ?? [],
        replyPvs: replyPvs.length ? replyPvs : undefined,
        deltaVsBaseline: evalCp - (typeof baseCp === 'number' ? baseCp : 0),
        keyMotifs: [],
        purpose: typeof pr.purpose === 'string' ? pr.purpose : undefined,
        questionId: typeof pr.questionId === 'string' ? pr.questionId : undefined,
        questionKind: typeof pr.questionKind === 'string' ? pr.questionKind : undefined,
        probedMove: move,
        mate: typeof ev.mate === 'number' ? ev.mate : undefined,
        depth: typeof ev.depth === 'number' ? ev.depth : undefined,
      });
    }

    return results;
  };

  return { stop, evalToVariations, runPositionEval, runProbeEval, runProbes };
}
