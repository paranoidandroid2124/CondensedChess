import { prop, type Prop } from 'lib';
import { pubsub } from 'lib/pubsub';
import type AnalyseCtrl from '../ctrl';
import { storedBooleanProp, tempStorage } from 'lib/storage';
import * as pgnExport from '../pgnExport';
import type { CevalEngine, Work } from 'lib/ceval';
import type { BoardPreview } from 'lib/view/boardPreview';
import { createProbeOrchestrator } from '../bookmaker/probeOrchestrator';
import type { StrategicPlanExperiment } from '../bookmaker/types';
import {
    buildProbeResultsByPlyEntries,
    collectGameArcProbeMomentBundles,
    countSurfacedEvidenceMoments,
    validateProbeResultAgainstRequest,
    type ProbeResultsByPlyEntry,
} from './probePlanning';
import { buildFullAnalysisRequestPayload, type FullAnalysisRequestPayload } from './requestPayload';
import type { NarrativeSignalDigest, StrategicIdeaGroup, StrategicIdeaKind } from '../chesstory/signalTypes';
export type { NarrativeSignalDigest, StrategicIdeaGroup, StrategicIdeaKind } from '../chesstory/signalTypes';

export const MIN_GAME_CHRONICLE_PLY = 9;

export function moveNumberFromPly(ply: number): number {
    return Math.max(1, Math.ceil(ply / 2));
}

export function totalMainlinePly(ctrl: AnalyseCtrl): number {
    return ctrl.mainline.length ? ctrl.mainline[ctrl.mainline.length - 1].ply : 0;
}

export function gameChronicleShortGameMessage(totalPly: number): string {
    const startMove = moveNumberFromPly(MIN_GAME_CHRONICLE_PLY);
    return totalPly <= 2
        ? `Game Chronicle opens from move ${startMove}. Let the opening develop a little more first.`
        : `Game Chronicle opens from move ${startMove}. A few more moves will give the review enough material to work with.`;
}

interface VariationLine {
    moves: string[];
    scoreCp: number;
    mate?: number | null;
    depth?: number;
    tags?: string[];
}

export interface ActivePlanRef {
    themeL1: string;
    subplanId?: string;
    phase?: string;
    commitmentScore?: number;
}

export interface StrategicPlanSummary {
    planId: string;
    planName: string;
    rank: number;
    score: number;
    themeL1?: string;
    subplanId?: string | null;
}

export interface LatentPlanSummary {
    seedId: string;
    planName: string;
    viabilityScore: number;
    whyAbsentFromTopMultiPv: string;
}

export interface EngineAlternative {
    uci: string;
    san?: string;
    cpAfterAlt?: number;
    cpLossVsPlayed?: number;
    pv?: string[];
}

export interface CollapseAnalysis {
    interval: string;
    rootCause: string;
    earliestPreventablePly: number;
    patchLineUci: string[];
    recoverabilityPlies: number;
}

export interface StrategyPieceRouteSummary {
    ownerSide: string;
    piece: string;
    from: string;
    route: string[];
    purpose: string;
    strategicFit: number;
    tacticalSafety: number;
    surfaceConfidence: number;
    surfaceMode: 'exact' | 'toward' | 'hidden' | string;
    evidence?: string[];
}

export interface StrategyPieceMoveRefSummary {
    ownerSide: string;
    piece: string;
    from: string;
    target: string;
    idea: string;
    tacticalTheme?: string | null;
    evidence?: string[];
}

export interface StrategyIdeaSignalSummary {
    ideaId: string;
    ownerSide: string;
    kind: StrategicIdeaKind | string;
    group: StrategicIdeaGroup | string;
    readiness: 'ready' | 'build' | 'premature' | 'blocked' | string;
    focusSquares?: string[];
    focusFiles?: string[];
    focusDiagonals?: string[];
    focusZone?: string | null;
    beneficiaryPieces?: string[];
    confidence: number;
    evidenceRefs?: string[];
}

export interface StrategyPackSummary {
    schema: string;
    sideToMove: string;
    strategicIdeas: StrategyIdeaSignalSummary[];
    pieceRoutes: StrategyPieceRouteSummary[];
    pieceMoveRefs: StrategyPieceMoveRefSummary[];
    directionalTargets: StrategyDirectionalTarget[];
    longTermFocus: string[];
    signalDigest?: NarrativeSignalDigest | null;
}

export interface GameChronicleMoment {
    momentId?: string;
    ply: number;
    moveNumber?: number;
    side?: 'white' | 'black';
    moveClassification?: string;
    momentType: string;
    fen: string;
    narrative: string;
    selectionKind?: 'key' | 'opening' | 'thread_bridge' | string;
    selectionLabel?: string;
    selectionReason?: string;
    concepts: string[];
    variations: VariationLine[];
    cpBefore?: number;
    cpAfter?: number;
    mateBefore?: number;
    mateAfter?: number;
    wpaSwing?: number;
    strategicSalience?: 'High' | 'Low';
    transitionType?: string;
    transitionConfidence?: number;
    activePlan?: ActivePlanRef;
    topEngineMove?: EngineAlternative;
    collapse?: CollapseAnalysis;
    strategyPack?: StrategyPackSummary;
    signalDigest?: NarrativeSignalDigest;
    probeRequests?: ProbeRequest[];
    probeRefinementRequests?: ProbeRequest[];
    authorQuestions?: AuthorQuestionSummary[];
    authorEvidence?: AuthorEvidenceSummary[];
    mainStrategicPlans?: StrategicPlanSummary[];
    strategicPlanExperiments?: StrategicPlanExperiment[];
    latentPlans?: LatentPlanSummary[];
    whyAbsentFromTopMultiPV?: string[];
    strategicBranch?: boolean;
    activeStrategicNote?: string;
    activeStrategicSourceMode?: string;
    activeStrategicIdeas?: ActiveStrategicIdeaRef[];
    activeStrategicRoutes?: ActiveStrategicRouteRef[];
    activeStrategicMoves?: ActiveStrategicMoveRef[];
    activeDirectionalTargets?: StrategyDirectionalTarget[];
    activeBranchDossier?: ActiveBranchDossier;
    strategicThread?: ActiveStrategicThreadRef;
}

export interface ProbeRequest {
    id: string;
    fen: string;
    moves: string[];
    depth: number;
    purpose?: string;
    questionId?: string;
    questionKind?: string;
    multiPv?: number;
    planId?: string;
    planName?: string;
    planScore?: number;
    baselineEvalCp?: number;
    baselineMove?: string;
    baselineMate?: number | null;
    baselineDepth?: number;
    objective?: string;
    seedId?: string;
    requiredSignals?: string[];
    horizon?: 'short' | 'medium' | 'long' | string;
    maxCpLoss?: number;
}

export interface AuthorQuestionSummary {
    id: string;
    kind: string;
    priority: number;
    question: string;
    why?: string | null;
    anchors?: string[];
    confidence: string;
    latentPlanName?: string | null;
}

export interface EvidenceBranchSummary {
    keyMove: string;
    line: string;
    evalCp?: number | null;
    mate?: number | null;
    depth?: number | null;
}

export interface AuthorEvidenceSummary {
    questionId: string;
    questionKind: string;
    question: string;
    why?: string | null;
    status: string;
    purposes?: string[];
    branchCount: number;
    branches?: EvidenceBranchSummary[];
    pendingProbeCount: number;
    probeObjectives?: string[];
    linkedPlans?: string[];
}

export interface ActiveStrategicRouteRef {
    routeId: string;
    ownerSide: string;
    piece: string;
    route: string[];
    purpose: string;
    strategicFit: number;
    tacticalSafety: number;
    surfaceConfidence: number;
    surfaceMode: 'exact' | 'toward' | 'hidden';
}

export interface StrategyDirectionalTarget {
    targetId: string;
    ownerSide: string;
    piece: string;
    from: string;
    targetSquare: string;
    readiness: 'build' | 'premature' | 'blocked' | 'contested';
    strategicReasons?: string[];
    prerequisites?: string[];
    evidence?: string[];
}

export interface ActiveStrategicIdeaRef {
    ideaId: string;
    ownerSide: string;
    kind: StrategicIdeaKind | string;
    group: StrategicIdeaGroup | string;
    readiness: 'ready' | 'build' | 'premature' | 'blocked';
    focusSummary: string;
    confidence: number;
}

export interface ActiveStrategicMoveRef {
    label: string;
    source: string;
    uci: string;
    san?: string;
    fenAfter?: string;
}

export interface ActiveBranchRouteCue {
    routeId: string;
    ownerSide: string;
    piece: string;
    route: string[];
    purpose: string;
    strategicFit: number;
    tacticalSafety: number;
    surfaceConfidence: number;
    surfaceMode: 'exact' | 'toward' | 'hidden';
}

export interface ActiveBranchMoveCue {
    label: string;
    uci: string;
    san?: string;
    source: string;
}

export interface ActiveBranchDossier {
    dominantLens: string;
    chosenBranchLabel: string;
    engineBranchLabel?: string;
    deferredBranchLabel?: string;
    whyChosen?: string;
    whyDeferred?: string;
    opponentResource?: string;
    routeCue?: ActiveBranchRouteCue;
    moveCue?: ActiveBranchMoveCue;
    evidenceCue?: string;
    continuationFocus?: string;
    practicalRisk?: string;
    comparisonGapCp?: number;
    threadLabel?: string;
    threadStage?: string;
    threadSummary?: string;
    threadOpponentCounterplan?: string;
}

export interface ActiveStrategicThreadRef {
    threadId: string;
    themeKey: string;
    themeLabel: string;
    stageKey: string;
    stageLabel: string;
}

export interface ActiveStrategicThread {
    threadId: string;
    side: 'white' | 'black' | string;
    themeKey: string;
    themeLabel: string;
    summary: string;
    seedPly: number;
    lastPly: number;
    representativePlies: number[];
    opponentCounterplan?: string;
    continuityScore: number;
}

export interface GameChronicleReview {
    schemaVersion?: number;
    reviewPerspective?: string;
    totalPlies: number;
    evalCoveredPlies: number;
    evalCoveragePct: number;
    selectedMoments: number;
    selectedMomentPlies: number[];
    internalMomentCount?: number;
    visibleMomentCount?: number;
    polishedMomentCount?: number;
    visibleStrategicMomentCount?: number;
    visibleBridgeMomentCount?: number;
    blundersCount?: number;
    missedWinsCount?: number;
    brilliantMovesCount?: number;
    momentTypeCounts?: Record<string, number>;
}

export interface GameChronicleResponse {
    schema: string;
    intro: string;
    moments: GameChronicleMoment[];
    conclusion: string;
    themes: string[];
    review?: GameChronicleReview;
    sourceMode?: string;
    model?: string | null;
    planTier?: string;
    llmLevel?: string;
    strategicThreads?: ActiveStrategicThread[];
    ccaEnabled?: boolean;
}

interface AsyncGameChronicleSubmitResponse {
    jobId: string;
    status: string;
    statusToken?: string;
    refineToken?: string;
    durability?: string;
    expiresAtMs?: number;
}

interface AsyncGameChronicleStatusResponse {
    jobId: string;
    status: string;
    createdAtMs?: number;
    updatedAtMs?: number;
    expiresAtMs?: number;
    durability?: string;
    result?: GameChronicleResponse | null;
    error?: string | null;
    msg?: string | null;
    ccaEnabled?: boolean;
}

interface GameChronicleEnvelope extends GameChronicleResponse {
    refineToken?: string | null;
}

export interface DefeatDnaReport {
    userId: string;
    totalGamesAnalyzed: number;
    rootCauseDistribution: Record<string, number>;
    avgRecoverabilityPlies: number;
    mostCommonPatchLines: string[];
    recentCollapses: CollapseAnalysis[];
}

const AUTO_EVAL_DEPTH = 12;
const AUTO_EVAL_MULTI_PV = 2;
const AUTO_EVAL_PER_PLY_TIMEOUT_MS = 450;
const AUTO_EVAL_MAX_BUDGET_MS = 25000;
const AUTO_EVAL_MAX_PLY_SCAN = 120;
const ASYNC_NARRATIVE_POLL_INTERVAL_MS = 1200;
const ASYNC_NARRATIVE_POLL_TIMEOUT_MS = 180000;
const COLLAPSE_OVERLAY_GLOBAL_KEY = '__chesstoryCollapseOverlays';
const GAME_ARC_REFINE_HEADER = 'X-Chesstory-GameArc-Refine';
const GAME_ARC_REFINE_TOKEN_HEADER = 'X-Chesstory-GameArc-Refine-Token';
const ASYNC_STATUS_TOKEN_HEADER = 'X-Chesstory-Async-Status-Token';
const ASYNC_DURABILITY_EPHEMERAL = 'ephemeral_memory';
const NARRATIVE_SESSION_STORAGE_KEY = 'analyse.game-chronicle.session.v2';
const MAX_PERSISTED_NARRATIVES = 4;
type BetaFeedbackChoice = 'would_pay' | 'maybe' | 'not_now';

interface BetaFeedbackResponse {
    ok: boolean;
    waitlist?: string;
    message?: string;
    storedEmail?: string | null;
}

interface PersistedNarrativeSnapshot {
    key: string;
    response: GameChronicleResponse;
    savedAt: number;
}

function magicLinkHref(): string {
    return `/auth/magic-link?referrer=${encodeURIComponent(location.pathname + location.search)}`;
}

function formatSeconds(totalSeconds: number): string {
    const seconds = Math.max(0, Math.floor(totalSeconds));
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    if (hours > 0) return `${hours}h ${mins}m`;
    if (minutes > 0) return `${minutes}m`;
    return `${seconds}s`;
}

function hashNarrativeContext(source: string): string {
    let hash = 2166136261;
    for (let i = 0; i < source.length; i++) {
        hash ^= source.charCodeAt(i);
        hash = Math.imul(hash, 16777619);
    }
    return (hash >>> 0).toString(36);
}

export class NarrativeCtrl {
    enabled: Prop<boolean>;
    loading: Prop<boolean> = prop(false);
    data: Prop<GameChronicleResponse | null> = prop(null);
    error: Prop<string | null> = prop(null);
    needsLogin: Prop<boolean> = prop(false);
    loadingDetail: Prop<string | null> = prop(null);

    pvBoard: Prop<BoardPreview | null> = prop(null);
    dnaTab: Prop<'narrative' | 'collapse' | 'dna'> = prop('narrative' as 'narrative' | 'collapse' | 'dna');
    dnaData: Prop<DefeatDnaReport | null> = prop(null);
    dnaLoading: Prop<boolean> = prop(false);
    dnaError: Prop<string | null> = prop(null);
    betaFeedbackLoading: Prop<boolean> = prop(false);
    betaFeedbackSubmitted: Prop<BetaFeedbackChoice | null> = prop(null);
    betaFeedbackMessage: Prop<string | null> = prop(null);
    showAllCollapses: Prop<boolean> = prop(false);
    patchReplay: Prop<{ collapseId: string; step: number; mode: 'original' | 'patch' } | null> = prop(null);
    private narrativeProbeSession = 0;
    private activeRefineController: AbortController | null = null;
    private readonly narrativeProbes: ReturnType<typeof createProbeOrchestrator>;

    constructor(readonly root: AnalyseCtrl) {
        this.enabled = storedBooleanProp('analyse.narrative.enabled', false);
        this.narrativeProbes = createProbeOrchestrator(root, session => session === this.narrativeProbeSession);
    }

    loginHref = () => magicLinkHref();
    betaFeedbackHref = (notify = false) => {
        const params = new URLSearchParams({
            surface: 'game_chronicle',
            feature: 'full_pgn_analysis',
            entrypoint: 'game_chronicle_completion',
            returnTo: `${location.pathname}${location.search}${location.hash}`,
        });
        if (notify) params.set('notify', 'true');
        return `/beta-feedback?${params.toString()}`;
    };

    toggle = () => {
        this.enabled(!this.enabled());
        if (this.enabled() && !this.data() && !this.loading()) {
            this.fetchNarrative();
        }
    };

    patchOpen = (collapseId: string) => {
        this.patchReplay({ collapseId, step: 0, mode: 'patch' });
        this.root.redraw();
    };

    patchClose = () => {
        this.patchReplay(null);
        this.root.redraw();
    };

    patchStep = (delta: number, maxSteps: number) => {
        const state = this.patchReplay();
        if (!state) return;
        const next = Math.max(0, Math.min(maxSteps, state.step + delta));
        this.patchReplay({ ...state, step: next });
        this.root.redraw();
    };

    patchToggle = () => {
        const state = this.patchReplay();
        if (!state) return;
        this.patchReplay({
            ...state,
            step: 0,
            mode: state.mode === 'patch' ? 'original' : 'patch',
        });
        this.root.redraw();
    };

    switchTab = (tab: 'narrative' | 'collapse' | 'dna') => {
        this.dnaTab(tab);
        if (tab === 'dna' && !this.dnaData() && !this.dnaLoading()) {
            this.fetchDefeatDna();
        }
        this.showAllCollapses(false);
        this.root.redraw();
    };

    fetchDefeatDna = async () => {
        this.dnaLoading(true);
        this.dnaError(null);
        this.root.redraw();
        try {
            const res = await fetch('/api/llm/defeat-dna');
            if (res.ok) {
                this.dnaData(await res.json() as DefeatDnaReport);
            } else if (res.status === 404) {
                this.dnaError('Defeat DNA not available for your account.');
            } else {
                this.dnaError('Failed to load Defeat DNA.');
            }
        } catch {
            this.dnaError('Network error loading Defeat DNA.');
        } finally {
            this.dnaLoading(false);
            this.root.redraw();
        }
    };

    submitBetaFeedback = async (willingness: BetaFeedbackChoice) => {
        if (this.betaFeedbackLoading()) return;
        this.betaFeedbackLoading(true);
        this.betaFeedbackMessage(null);
        this.root.redraw();
        try {
            const res = await fetch('/api/beta-feedback', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    surface: 'game_chronicle',
                    feature: 'full_pgn_analysis',
                    entrypoint: 'game_chronicle_completion',
                    willingness,
                    notify: false,
                }),
            });
            const data = (await res.json().catch(() => null)) as BetaFeedbackResponse | null;
            if (!res.ok || !data?.ok) {
                this.betaFeedbackMessage(data?.message || 'We could not save that beta response.');
                return;
            }
            this.betaFeedbackSubmitted(willingness);
            this.betaFeedbackMessage(data.message || 'Thanks. We saved your beta feedback.');
        } catch {
            this.betaFeedbackMessage('Network error while saving beta feedback.');
        } finally {
            this.betaFeedbackLoading(false);
            this.root.redraw();
        }
    };

    openAndFetch = async (pgnOverride?: string | null) => {
        if (!this.enabled()) this.enabled(true);
        if (this.loading()) {
            this.root.redraw();
            return;
        }
        try {
            await this.fetchNarrative(pgnOverride);
        } finally {
            this.root.redraw();
        }
    };

    private publishCollapseOverlay = (response: GameChronicleResponse | null): void => {
        const collapses = (response?.moments || [])
            .flatMap(m => (m.collapse
                ? [{
                    interval: m.collapse.interval,
                    earliestPreventablePly: m.collapse.earliestPreventablePly,
                    rootCause: m.collapse.rootCause,
                }]
                : []));
        (window as any)[COLLAPSE_OVERLAY_GLOBAL_KEY] = collapses;
        pubsub.emit('analysis.collapse.update', collapses);
    };

    private cancelNarrativeRefinement = (): void => {
        this.activeRefineController?.abort();
        this.activeRefineController = null;
        this.narrativeProbeSession++;
        this.narrativeProbes.stop();
    };

    private narrativeContextKey = (pgnOverride?: string | null): string | null => {
        const pgn = (pgnOverride ?? pgnExport.renderFullTxt(this.root)).trim();
        if (!pgn) return null;
        return `${location.pathname}|${this.root.data.game.variant.key}|${hashNarrativeContext(pgn)}`;
    };

    private readPersistedNarratives = (): PersistedNarrativeSnapshot[] => {
        const raw = tempStorage.get(NARRATIVE_SESSION_STORAGE_KEY);
        if (!raw) return [];
        try {
            const parsed = JSON.parse(raw);
            if (!Array.isArray(parsed)) return [];
            return parsed.filter((entry): entry is PersistedNarrativeSnapshot => {
                return (
                    !!entry &&
                    typeof entry === 'object' &&
                    typeof entry.key === 'string' &&
                    typeof entry.savedAt === 'number' &&
                    !!entry.response &&
                    typeof entry.response === 'object' &&
                    typeof entry.response.schema === 'string' &&
                    typeof entry.response.intro === 'string' &&
                    Array.isArray(entry.response.moments) &&
                    typeof entry.response.conclusion === 'string' &&
                    Array.isArray(entry.response.themes)
                );
            });
        } catch (_) {
            tempStorage.remove(NARRATIVE_SESSION_STORAGE_KEY);
            return [];
        }
    };

    private writePersistedNarratives = (entries: PersistedNarrativeSnapshot[]): void => {
        try {
            tempStorage.set(NARRATIVE_SESSION_STORAGE_KEY, JSON.stringify(entries));
        } catch (_) {
            tempStorage.remove(NARRATIVE_SESSION_STORAGE_KEY);
        }
    };

    private persistNarrativeResponse = (response: GameChronicleResponse, pgnOverride?: string | null): void => {
        const key = this.narrativeContextKey(pgnOverride);
        if (!key) return;
        const entries = [
            {
                key,
                response,
                savedAt: Date.now(),
            },
            ...this.readPersistedNarratives().filter(entry => entry.key !== key),
        ].slice(0, MAX_PERSISTED_NARRATIVES);
        this.writePersistedNarratives(entries);
    };

    syncPersistedNarrative = (): void => {
        const key = this.narrativeContextKey();
        const snapshot = key ? this.readPersistedNarratives().find(entry => entry.key === key) : undefined;
        this.data(snapshot?.response || null);
        this.error(null);
        this.needsLogin(false);
        this.loading(false);
        this.loadingDetail(null);
        this.publishCollapseOverlay(snapshot?.response || null);
        this.root.refreshReviewShellState();
        this.root.redraw();
    };

    private applyNarrativeResponse = (response: GameChronicleResponse, pgnOverride?: string | null): void => {
        this.data(response);
        this.persistNarrativeResponse(response, pgnOverride);
        this.publishCollapseOverlay(response);
        this.root.refreshReviewShellState();
    };

    private fetchRefinedNarrativeResponse = async (
        payload: FullAnalysisRequestPayload,
        probeResultsByPly: ProbeResultsByPlyEntry[],
        fallbackResponse: GameChronicleResponse,
        refineToken: string | null | undefined,
        isCurrentSession: () => boolean,
    ): Promise<GameChronicleResponse> => {
        if (!refineToken) return fallbackResponse;
        try {
            this.loadingDetail('Probe refinement: rebuilding Game Chronicle...');
            this.root.redraw();
            this.activeRefineController = new AbortController();
            const res = await fetch('/api/llm/game-analysis-local', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    [GAME_ARC_REFINE_HEADER]: '1',
                    [GAME_ARC_REFINE_TOKEN_HEADER]: refineToken,
                },
                body: JSON.stringify({
                    ...payload,
                    probeResultsByPly,
                }),
                signal: this.activeRefineController.signal,
            });
            this.activeRefineController = null;
            if (!isCurrentSession()) return fallbackResponse;
            if (!res.ok) {
                console.warn(`[gamearc.probes] refinement_request_failed status=${res.status}`);
                return fallbackResponse;
            }

            const refined = (await res.json()) as GameChronicleResponse;
            if (typeof fallbackResponse.ccaEnabled === 'boolean' && typeof refined.ccaEnabled !== 'boolean') {
                refined.ccaEnabled = fallbackResponse.ccaEnabled;
            }
            return refined;
        } catch (err) {
            if (!(err instanceof DOMException && err.name === 'AbortError') && isCurrentSession()) {
                console.warn('[gamearc.probes] refinement_request_error', err);
            }
            return fallbackResponse;
        } finally {
            this.activeRefineController = null;
        }
    };

    private refineNarrativeWithProbes = async (
        payload: FullAnalysisRequestPayload,
        response: GameChronicleResponse,
        refineToken?: string | null,
    ): Promise<GameChronicleResponse> => {
        const probeBundles = collectGameArcProbeMomentBundles(response);
        if (!probeBundles.length) return response;

        const requestsBeforeDedupe = (response.moments || []).reduce((sum, moment) => {
            const internalCount = Array.isArray(moment.probeRefinementRequests) ? moment.probeRefinementRequests.length : 0;
            const surfacedCount = Array.isArray(moment.probeRequests) ? moment.probeRequests.length : 0;
            return sum + (internalCount || surfacedCount);
        }, 0);
        const requestsAfterDedupe = probeBundles.reduce((sum, bundle) => sum + bundle.requests.length, 0);

        this.narrativeProbeSession++;
        this.narrativeProbes.stop();
        const probeSession = this.narrativeProbeSession;
        const isCurrentSession = () => probeSession === this.narrativeProbeSession;

        this.loadingDetail(
            `Probe refinement: validating ${probeBundles.length} selected moment${probeBundles.length === 1 ? '' : 's'}...`,
        );
        this.root.redraw();

        let successCount = 0;
        let timeoutCount = 0;
        let contractDropCount = 0;
        const probeResultsByPly: ProbeResultsByPlyEntry[] = [];

        for (const bundle of probeBundles) {
            if (!isCurrentSession()) return response;
            const rawResults = await this.narrativeProbes.runProbes(bundle.requests, bundle.baselineCp, probeSession);
            if (!isCurrentSession()) return response;

            timeoutCount += Math.max(0, bundle.requests.length - rawResults.length);
            const validResults = rawResults.filter(result => {
                const request = bundle.requests.find(candidate => candidate.id === result.id);
                return request ? validateProbeResultAgainstRequest(request, result) : false;
            });
            contractDropCount += Math.max(0, rawResults.length - validResults.length);
            successCount += validResults.length;

            if (validResults.length) {
                probeResultsByPly.push({
                    ply: bundle.ply,
                    results: validResults,
                });
            }
        }

        const sanitizedProbeResults = buildProbeResultsByPlyEntries(probeResultsByPly);
        console.info(
            `[gamearc.probes] selected_moments=${probeBundles.length} requests_before=${requestsBeforeDedupe} requests_after=${requestsAfterDedupe} success=${successCount} timeout=${timeoutCount} contract_drop=${contractDropCount} probe_backed_rerender_moments=${sanitizedProbeResults.length} surfaced_evidence_moments=${countSurfacedEvidenceMoments(response)}`,
        );

        if (!sanitizedProbeResults.length) return response;

        return this.fetchRefinedNarrativeResponse(payload, sanitizedProbeResults, response, refineToken, isCurrentSession);
    };

    fetchNarrative = async (pgnOverride?: string | null) => {
        this.cancelNarrativeRefinement();
        this.loading(true);
        this.error(null);
        this.needsLogin(false);
        this.betaFeedbackLoading(false);
        this.betaFeedbackSubmitted(null);
        this.betaFeedbackMessage(null);
        this.pvBoard(null);
        this.root.setNarrativeRouteOverlay(null);
        this.publishCollapseOverlay(null);
        this.root.redraw();
        try {
            const currentPgn = pgnExport.renderFullTxt(this.root);
            const stagedPgn = pgnOverride?.trim();
            const pgn = stagedPgn && stagedPgn !== currentPgn ? stagedPgn : currentPgn;
            const usesCurrentTree = pgn === currentPgn;
            const currentTreePly = totalMainlinePly(this.root);

            if (usesCurrentTree && currentTreePly < MIN_GAME_CHRONICLE_PLY) {
                this.error(gameChronicleShortGameMessage(currentTreePly));
                return;
            }

            this.loadingDetail(
                usesCurrentTree
                    ? 'Deep analysis prep: collecting PGN and existing eval...'
                    : 'Deep analysis prep: staging the imported PGN...',
            );
            this.root.redraw();

            const evals = usesCurrentTree
                ? await extractMoveEvals(this.root, detail => {
                    this.loadingDetail(detail);
                    this.root.redraw();
                })
                : [];
            this.loadingDetail(
                usesCurrentTree
                    ? 'Submitting async deep analysis job. Keep this tab open until it completes...'
                    : 'Submitting async deep analysis job for imported PGN. Keep this tab open until it completes...',
            );
            this.root.redraw();

            const payload = buildFullAnalysisRequestPayload({
                pgn,
                evals,
                variant: this.root.data.game.variant.key || 'standard',
            });

            const submitRes = await fetch('/api/llm/game-analysis-async', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            if (submitRes.ok) {
                const submit = (await submitRes.json()) as AsyncGameChronicleSubmitResponse;
                await this.pollAsyncNarrative(
                    submit.jobId,
                    submit.statusToken || '',
                    submit.refineToken || null,
                    submit.durability || null,
                    payload,
                );
            } else if (submitRes.status === 400) {
                const data = await submitRes.json().catch(() => null as { msg?: string } | null);
                this.error(data?.msg || 'Game Chronicle request is invalid.');
            } else if (submitRes.status === 404 || submitRes.status === 405 || submitRes.status === 501) {
                await this.fetchNarrativeSyncFallback(payload);
            } else if (submitRes.status === 401) {
                this.needsLogin(true);
                this.error('Sign in to run Game Chronicle.');
            } else if (submitRes.status === 429) {
                try {
                    const data = await submitRes.json();
                    const seconds = data?.ratelimit?.seconds;
                    if (typeof seconds === 'number') this.error(`Game Chronicle quota reached. Try again in ${formatSeconds(seconds)}.`);
                    else this.error('Game Chronicle quota reached.');
                } catch {
                    this.error('Game Chronicle quota reached.');
                }
            } else {
                const txt = await submitRes.text();
                this.error("Error submitting async narrative: " + submitRes.status + " " + txt);
            }
        } catch (e) {
            console.error(e);
            this.error("Error: " + e);
        } finally {
            this.loading(false);
            this.loadingDetail(null);
            this.root.redraw();
        }
    };

    private fetchNarrativeSyncFallback = async (payload: FullAnalysisRequestPayload): Promise<void> => {
        this.loadingDetail('Async endpoint unavailable. Falling back to local full analysis...');
        this.root.redraw();

        const res = await fetch('/api/llm/game-analysis-local', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });

              if (res.ok) {
                  const data = (await res.json()) as GameChronicleEnvelope;
                  const finalResponse = await this.refineNarrativeWithProbes(payload, data, data.refineToken);
                  this.applyNarrativeResponse(finalResponse, payload.pgn);
                  return;
              }

        if (res.status === 400) {
            const data = await res.json().catch(() => null as { msg?: string } | null);
            this.error(data?.msg || 'Game Chronicle request is invalid.');
            return;
        }

        if (res.status === 401) {
            this.needsLogin(true);
            this.error('Sign in to run Game Chronicle.');
            return;
        }

        if (res.status === 429) {
            try {
                const data = await res.json();
                const seconds = data?.ratelimit?.seconds;
                if (typeof seconds === 'number') this.error(`Game Chronicle quota reached. Try again in ${formatSeconds(seconds)}.`);
                else this.error('Game Chronicle quota reached.');
            } catch {
                this.error('Game Chronicle quota reached.');
            }
            return;
        }

        const txt = await res.text();
        this.error('Error fetching narrative: ' + res.status + ' ' + txt);
    };

    private pollAsyncNarrative = async (
        jobId: string,
        statusToken: string,
        refineToken: string | null,
        durability: string | null,
        payload: FullAnalysisRequestPayload,
    ): Promise<void> => {
        if (!statusToken) {
            this.error('Async analysis token is missing. Please retry Game Chronicle.');
            return;
        }
        const startedAt = Date.now();
        while (Date.now() - startedAt < ASYNC_NARRATIVE_POLL_TIMEOUT_MS) {
            this.loadingDetail('Deep analysis in progress...');
            this.root.redraw();

            const res = await fetch(`/api/llm/game-analysis-async/${encodeURIComponent(jobId)}`, {
                headers: {
                    [ASYNC_STATUS_TOKEN_HEADER]: statusToken,
                },
            });
            if (!res.ok) {
                if (res.status === 404 || res.status === 410) {
                    const data = await res.json().catch(() => null as AsyncGameChronicleStatusResponse | null);
                    const contractHint =
                        data?.msg ||
                        (durability === ASYNC_DURABILITY_EPHEMERAL
                            ? 'Async Game Chronicle jobs are temporary. Keep this tab open while the job runs and retry if it becomes unavailable.'
                            : 'Async analysis is no longer available. Please retry.');
                    this.error(contractHint);
                    return;
                }
                const txt = await res.text();
                this.error('Async analysis polling failed: ' + res.status + ' ' + txt);
                return;
            }

            const status = (await res.json()) as AsyncGameChronicleStatusResponse;
            const state = (status.status || '').toLowerCase();
            if (state === 'completed') {
                if (status.result) {
                    // Merge ccaEnabled from the polling envelope into the result
                      if (typeof status.ccaEnabled === 'boolean') {
                          status.result.ccaEnabled = status.ccaEnabled;
                      }
                      const finalResponse = await this.refineNarrativeWithProbes(payload, status.result, refineToken);
                      this.applyNarrativeResponse(finalResponse, payload.pgn);
                  }
                else this.error('Async analysis completed without result.');
                return;
            }
            if (state === 'failed') {
                this.error(status.error || 'Async analysis failed.');
                return;
            }

            await new Promise(resolve => setTimeout(resolve, ASYNC_NARRATIVE_POLL_INTERVAL_MS));
        }

        this.error('Async analysis timed out. Please try again.');
    };
}

export function make(root: AnalyseCtrl): NarrativeCtrl {
    return new NarrativeCtrl(root);
}

async function extractMoveEvals(
    ctrl: AnalyseCtrl,
    onProgress?: (detail: string) => void,
): Promise<any[]> {
    const evals: any[] = [];
    const byPly = new Map<number, any>();
    const nodes = ctrl.mainline.filter(node => node.ply >= 1);

    for (const node of nodes) {
        const raw = node.ceval || node.eval;
        if (!raw) continue;
        const normalized = normalizeEval(node.ply, raw);
        byPly.set(node.ply, normalized);
    }

    const missing = nodes.filter(node => !byPly.has(node.ply));
    if (missing.length) {
        const budgetMs = Math.min(AUTO_EVAL_MAX_BUDGET_MS, Math.max(7000, missing.length * 260));
        const missingSlice = missing.slice(0, AUTO_EVAL_MAX_PLY_SCAN);
        onProgress?.(`Deep scan: evaluating ${missingSlice.length} missing plies (may take up to ${(budgetMs / 1000).toFixed(0)}s)...`);
        const enriched = await enrichMissingEvalsWithWasm(ctrl, missingSlice, budgetMs, onProgress);
        for (const item of enriched) byPly.set(item.ply, item.eval);
    }

    for (const node of nodes) {
        const ev = byPly.get(node.ply);
        if (ev) evals.push(ev);
    }

    onProgress?.(`Deep scan complete: eval coverage ${byPly.size}/${nodes.length} positions.`);
    return evals;
}

function normalizeEval(ply: number, raw: any): any {
    const cp = typeof raw?.cp === 'number' ? raw.cp : 0;
    const mate = typeof raw?.mate === 'number' ? raw.mate : null;
    const depth = typeof raw?.depth === 'number' ? raw.depth : 0;

    const pvs: any[] = Array.isArray(raw?.pvs) ? raw.pvs : [];
    const variations = pvs
        .map(pv => {
            const moves = Array.isArray(pv?.moves)
                ? pv.moves
                : typeof pv?.moves === 'string'
                    ? pv.moves.trim().split(/\s+/).filter(Boolean)
                    : [];
            if (!moves.length) return null;
            return {
                moves,
                scoreCp: typeof pv?.cp === 'number' ? pv.cp : cp,
                mate: typeof pv?.mate === 'number' ? pv.mate : mate,
                depth: typeof pv?.depth === 'number' ? pv.depth : depth,
            };
        })
        .filter(Boolean);

    return {
        ply,
        cp,
        mate,
        pv: variations?.[0]?.moves ?? [],
        variations,
    };
}

async function enrichMissingEvalsWithWasm(
    ctrl: AnalyseCtrl,
    missingNodes: Tree.Node[],
    totalBudgetMs: number,
    onProgress?: (detail: string) => void,
): Promise<Array<{ ply: number; eval: any }>> {
    const enriched: Array<{ ply: number; eval: any }> = [];
    if (!missingNodes.length) return enriched;

    let engine: CevalEngine | undefined;
    try {
        engine = ctrl.ceval.engines.make({ variant: ctrl.data.game.variant.key });
    } catch {
        return enriched;
    }
    if (!engine) return enriched;

    const startedAt = Date.now();
    try {
        for (const [idx, node] of missingNodes.entries()) {
            if (Date.now() - startedAt >= totalBudgetMs) break;
            if (!node?.fen || typeof node.fen !== 'string') continue;
            const ev = await runNodeEval(engine, ctrl, node, AUTO_EVAL_DEPTH, AUTO_EVAL_MULTI_PV);
            if (!ev) continue;
            enriched.push({ ply: node.ply, eval: normalizeEval(node.ply, ev) });
            if ((idx + 1) % 6 === 0 || idx + 1 === missingNodes.length) {
                onProgress?.(`Deep scan progress: ${idx + 1}/${missingNodes.length} plies checked.`);
            }
        }
    } finally {
        try {
            engine.stop();
        } catch { }
        try {
            engine.destroy();
        } catch { }
    }

    return enriched;
}

async function runNodeEval(
    engine: CevalEngine,
    ctrl: AnalyseCtrl,
    node: Tree.Node,
    depth: number,
    multiPv: number,
): Promise<Tree.LocalEval | null> {
    try {
        engine.stop();
    } catch { }

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
        const timer = setTimeout(finish, AUTO_EVAL_PER_PLY_TIMEOUT_MS);

        const work: Work = {
            variant: ctrl.data.game.variant.key,
            threads: 1,
            hashSize: 16,
            gameId: undefined,
            stopRequested: false,
            path: `narrative-auto:${node.ply}`,
            search: { depth },
            multiPv,
            ply: node.ply,
            threatMode: false,
            initialFen: node.fen,
            currentFen: node.fen,
            moves: [],
            emit: (ev: Tree.LocalEval) => {
                best = ev;
                const pvCount = Array.isArray(ev.pvs)
                    ? ev.pvs.filter(pv => Array.isArray(pv?.moves) && pv.moves.length).length
                    : 0;
                if (ev.depth >= depth && pvCount >= 1) finish();
            },
        };

        try {
            engine.start(work);
        } catch {
            finish();
        }
    });
}
