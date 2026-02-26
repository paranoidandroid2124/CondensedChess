import type { CevalEngine, Work } from 'lib/ceval';
import type AnalyseCtrl from '../ctrl';
import type { EvalVariation, ProbeRequest, ProbeResult, L1DeltaSnapshot, FutureSnapshot, TargetsDelta } from './types';

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
  let purposeSignalSamples = 0;
  const PurposeSignalLogEvery = 20;
  const purposeSignalStats = new Map<
    string,
    { probes: number; requiredSlots: number; generatedRequired: number; compatFallbackUses: number }
  >();
  type MotifInference = {
    keyMotifs: string[];
    requiredSignals: string[];
    generatedRequiredSignals: string[];
    compatFallbackUsed: boolean;
  };

  const parseMove = (uci?: string): { fromFile: string; toFile: string; isPawnPush: boolean; isRookPawnPush: boolean } | null => {
    if (!uci || !/^[a-h][1-8][a-h][1-8][qrbn]?$/i.test(uci)) return null;
    const fromFile = uci[0]!;
    const toFile = uci[2]!;
    const fromRank = Number(uci[1]!);
    const toRank = Number(uci[3]!);
    const rankDelta = Math.abs(toRank - fromRank);
    const isPawnPush = rankDelta === 1 || rankDelta === 2;
    const isRookPawnPush = isPawnPush && (fromFile === 'a' || fromFile === 'h');
    return { fromFile, toFile, isPawnPush, isRookPawnPush };
  };

  const purposeRequiredSignals = (purpose: string | undefined, requestRequiredSignals: string[] | undefined): string[] => {
    const required = new Set<string>();
    if (Array.isArray(requestRequiredSignals)) {
      requestRequiredSignals.map(s => (typeof s === 'string' ? s.trim() : '')).filter(Boolean).forEach(s => required.add(s));
    }
    if (purpose?.includes('recapture')) required.add('recapture_branching');
    if (purpose?.includes('tension')) required.add('tension_branching');
    if (purpose?.includes('convert')) required.add('conversion_route');
    if (purpose?.includes('latent_plan_refutation')) required.add('latent_plan_refutation');
    if (purpose?.includes('free_tempo')) required.add('free_tempo_trajectory');
    if (purpose?.includes('defense_reply')) required.add('defense_reply_branching');
    return Array.from(required);
  };

  const inferPurposeMotifs = (
    move: string | undefined,
    purpose: string | undefined,
    replyPvs: string[][],
  ): string[] => {
    const motifs = new Set<string>();
    const parsed = parseMove(move);
    if (purpose?.includes('recapture')) motifs.add('recapture_branching');
    if (purpose?.includes('tension')) motifs.add('tension_branching');
    if (purpose?.includes('convert')) motifs.add('conversion_route');
    if (purpose?.includes('latent_plan_refutation')) motifs.add('latent_plan_refutation');
    if (purpose?.includes('free_tempo')) motifs.add('free_tempo_trajectory');
    if (purpose?.includes('defense_reply')) motifs.add('defense_reply_branching');
    if (purpose?.includes('free_tempo') && parsed?.isRookPawnPush) motifs.add('rook_pawn_march_candidate');
    if (purpose?.includes('convert') && (replyPvs[0] ?? []).length >= 3) motifs.add('multi_ply_sequence');
    return Array.from(motifs);
  };

  const inferCompatFallbackMotifs = (
    move: string | undefined,
    purpose: string | undefined,
    replyPvs: string[][],
  ): string[] => {
    const motifs = new Set<string>();
    const parsed = parseMove(move);
    if (parsed?.isRookPawnPush) motifs.add('rook_pawn_march_candidate');
    if (parsed?.fromFile !== parsed?.toFile) motifs.add('pawn_lever_or_capture');
    if (purpose?.includes('recapture')) motifs.add('recapture_branching');
    if (purpose?.includes('tension')) motifs.add('tension_branching');
    if (purpose?.includes('convert')) motifs.add('conversion_route');
    if (purpose?.includes('latent_plan_refutation')) motifs.add('latent_plan_refutation');
    if (purpose?.includes('free_tempo')) motifs.add('free_tempo_trajectory');
    const firstPv = replyPvs[0] ?? [];
    if (firstPv.length >= 3) motifs.add('multi_ply_sequence');
    if (firstPv.some(m => /^[a-h][1-8][a-h][1-8]/i.test(m) && m[0] !== m[2])) motifs.add('forcing_exchange_pattern');
    return Array.from(motifs);
  };

  const isCompatMotifFallbackEnabled = (): boolean => {
    const g = globalThis as Record<string, unknown>;
    const runtimeFlag = g['__bookmakerCompatMotifFallback'];
    if (typeof runtimeFlag === 'boolean') return runtimeFlag;
    try {
      const stored = globalThis.localStorage?.getItem('bookmaker.compatMotifFallback');
      return stored === '1' || stored === 'true';
    } catch {
      return false;
    }
  };

  const computeMotifInference = (
    move: string | undefined,
    purpose: string | undefined,
    replyPvs: string[][],
    requestRequiredSignals: string[] | undefined,
  ): MotifInference => {
    const requiredSignals = purposeRequiredSignals(purpose, requestRequiredSignals);
    const motifs = new Set<string>(inferPurposeMotifs(move, purpose, replyPvs));
    const compatFallbackUsed = isCompatMotifFallbackEnabled();
    if (compatFallbackUsed) inferCompatFallbackMotifs(move, purpose, replyPvs).forEach(m => motifs.add(m));
    const generatedRequiredSignals = requiredSignals.filter(s => motifs.has(s));
    return {
      keyMotifs: Array.from(motifs),
      requiredSignals,
      generatedRequiredSignals,
      compatFallbackUsed,
    };
  };

  const trackPurposeSignalCoverage = (
    purpose: string | undefined,
    requiredSignals: string[],
    generatedRequiredSignals: string[],
    compatFallbackUsed: boolean,
  ): void => {
    if (!purpose || !requiredSignals.length) return;
    const current = purposeSignalStats.get(purpose) ?? {
      probes: 0,
      requiredSlots: 0,
      generatedRequired: 0,
      compatFallbackUses: 0,
    };
    current.probes += 1;
    current.requiredSlots += requiredSignals.length;
    current.generatedRequired += generatedRequiredSignals.length;
    if (compatFallbackUsed) current.compatFallbackUses += 1;
    purposeSignalStats.set(purpose, current);

    purposeSignalSamples += 1;
    if (purposeSignalSamples % PurposeSignalLogEvery !== 0) return;
    const coverage = current.requiredSlots > 0 ? current.generatedRequired / current.requiredSlots : 1;
    const compatRate = current.probes > 0 ? current.compatFallbackUses / current.probes : 0;
    console.info(
      `[bookmaker.probe.signals] purpose=${purpose} probes=${current.probes} required_slots=${current.requiredSlots} generated=${current.generatedRequired} coverage=${coverage.toFixed(3)} compat_fallback_rate=${compatRate.toFixed(3)}`,
    );
  };

  const buildL1Delta = (
    deltaVsBaseline: number,
    keyMotifs: string[],
    purpose: string | undefined,
  ): L1DeltaSnapshot => {
    const clamp = (v: number, lo: number, hi: number): number => Math.max(lo, Math.min(hi, v));
    const swing = clamp(Math.round(deltaVsBaseline / 60), -6, 6);
    const kingSafetyDelta = purpose?.includes('refutation') ? -Math.abs(swing) : swing >= 0 ? 1 : -1;
    const centerControlDelta = keyMotifs.includes('pawn_lever_or_capture') ? swing : Math.sign(swing);
    const openFilesDelta = keyMotifs.includes('forcing_exchange_pattern') ? Math.sign(swing) : 0;
    const mobilityDelta = clamp(Math.round(deltaVsBaseline / 25), -8, 8);
    const collapseReason =
      deltaVsBaseline <= -120
        ? 'Structure or king safety deteriorates under accurate defense'
        : deltaVsBaseline >= 80
          ? 'Plan preconditions are reinforced with practical upside'
          : undefined;
    return {
      materialDelta: 0,
      kingSafetyDelta,
      centerControlDelta,
      openFilesDelta,
      mobilityDelta,
      collapseReason,
    };
  };

  const buildFutureSnapshot = (
    purpose: string | undefined,
    keyMotifs: string[],
    deltaVsBaseline: number,
  ): FutureSnapshot => {
    const tacticalAdded =
      keyMotifs.includes('forcing_exchange_pattern') ? ['forcing exchanges'] : [];
    const strategicAdded = keyMotifs.includes('rook_pawn_march_candidate')
      ? ['rook-pawn space gain']
      : keyMotifs.includes('conversion_route')
        ? ['conversion route']
        : [];
    const targetsDelta: TargetsDelta = {
      tacticalAdded,
      tacticalRemoved: deltaVsBaseline > 20 ? ['immediate tactical liability'] : [],
      strategicAdded,
      strategicRemoved: deltaVsBaseline < -80 ? ['stable plan trajectory'] : [],
    };
    const prereqs = [
      ...(keyMotifs.includes('rook_pawn_march_candidate') ? ['flank space expansion'] : []),
      ...(purpose?.includes('convert') ? ['simplification channel'] : []),
      ...(purpose?.includes('tension') ? ['tension handling branch'] : []),
    ];
    const blockersRemoved =
      deltaVsBaseline > 40 ? ['coordination bottleneck'] : [];
    return {
      resolvedThreatKinds: deltaVsBaseline > 30 ? ['Counterplay'] : [],
      newThreatKinds: deltaVsBaseline < -90 ? ['KingSafety'] : [],
      targetsDelta,
      planBlockersRemoved: blockersRemoved,
      planPrereqsMet: prereqs,
    };
  };

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
      const deltaVsBaseline = evalCp - (typeof baseCp === 'number' ? baseCp : 0);
      const purpose = typeof pr.purpose === 'string' ? pr.purpose : undefined;
      const motifInference = computeMotifInference(
        move,
        purpose,
        replyPvs,
        Array.isArray(pr.requiredSignals) ? pr.requiredSignals : undefined,
      );
      const keyMotifs = motifInference.keyMotifs;
      trackPurposeSignalCoverage(
        purpose,
        motifInference.requiredSignals,
        motifInference.generatedRequiredSignals,
        motifInference.compatFallbackUsed,
      );
      const l1Delta = purpose ? buildL1Delta(deltaVsBaseline, keyMotifs, purpose) : undefined;
      const futureSnapshot =
        purpose && (purpose.includes('latent') || purpose.includes('convert') || purpose.includes('tension') || purpose.includes('free_tempo'))
          ? buildFutureSnapshot(purpose, keyMotifs, deltaVsBaseline)
          : undefined;
      results.push({
        id: pr.id,
        fen: pr.fen,
        evalCp,
        bestReplyPv: replyPvs[0] ?? [],
        replyPvs: replyPvs.length ? replyPvs : undefined,
        deltaVsBaseline,
        keyMotifs,
        purpose,
        questionId: typeof pr.questionId === 'string' ? pr.questionId : undefined,
        questionKind: typeof pr.questionKind === 'string' ? pr.questionKind : undefined,
        probedMove: move,
        mate: typeof ev.mate === 'number' ? ev.mate : undefined,
        depth: typeof ev.depth === 'number' ? ev.depth : undefined,
        l1Delta,
        futureSnapshot,
        objective: typeof pr.objective === 'string' ? pr.objective : undefined,
        seedId: typeof pr.seedId === 'string' ? pr.seedId : undefined,
        requiredSignals: motifInference.requiredSignals.length ? motifInference.requiredSignals : undefined,
        generatedRequiredSignals: motifInference.generatedRequiredSignals.length
          ? motifInference.generatedRequiredSignals
          : undefined,
        motifInferenceMode: motifInference.compatFallbackUsed ? 'purpose_plus_compat' : 'purpose_only',
      });
    }

    return results;
  };

  return { stop, evalToVariations, runPositionEval, runProbeEval, runProbes };
}
