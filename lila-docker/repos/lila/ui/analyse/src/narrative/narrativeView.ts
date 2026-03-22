import type { VNode } from 'snabbdom';
import {
    MIN_GAME_CHRONICLE_PLY,
    gameChronicleShortGameMessage,
    moveNumberFromPly,
    totalMainlinePly,
    type NarrativeCtrl,
    type DefeatDnaReport,
} from './narrativeCtrl';
import { hl, bind, dataIcon, onInsert } from 'lib/view';
import * as licon from 'lib/licon';
import { renderEval } from 'lib/ceval/util';
import { type BoardPreview, renderBoardPreview } from 'lib/view/boardPreview';
import { initMiniBoards } from 'lib/view/miniBoard';
import { makeBoardFen, parseFen } from 'chessops/fen';
import { setupPosition } from 'chessops/variant';
import { lichessRules } from 'chessops/compat';
import { makeSanAndPlay } from 'chessops/san';
import { parseUci } from 'chessops/util';
import type { DrawShape } from '@lichess-org/chessground/draw';
import { plyToTurn } from 'lib/game/chess';
import type { StrategicPlanExperiment } from '../bookmaker/types';
import { formatStrategicPlanText, strategicPlanExperimentIndex } from '../bookmaker/planSupportSurface';
import {
    buildDecisionComparisonSurface,
} from '../decisionComparison';
import {
    buildPlayerFacingSupportOptions,
    filterPlayerFacingValues,
    filterPlayerFacingRows,
    formatDeploymentSummary,
    formatEvidenceStatus,
    cleanNarrativeSurfaceLabel,
    cleanNarrativeProseText,
    cleanStrategicNoteText,
    humanizeToken,
    rewritePlayerFacingSupportText,
    summarizeReviewMomentProse,
} from '../chesstory/signalFormatting';
import { buildCompactSupportSurface } from '../chesstory/compactSupportSurface';
import type { DecisionComparisonDigest, NarrativeSignalDigest, StrategicIdeaGroup, StrategicIdeaKind } from '../chesstory/signalTypes';
export type { DecisionComparisonDigest, NarrativeSignalDigest, StrategicIdeaGroup, StrategicIdeaKind } from '../chesstory/signalTypes';

type VariationLine = { moves: string[]; scoreCp: number; mate?: number | null; depth?: number; tags?: string[] };
type RenderedMoveLine = {
    nodes: Array<VNode | string>;
    preview?: {
        board: string;
        state: string;
        plies: number;
    };
};

const NARRATIVE_INTRO_ANCHOR_ID = 'narrative-anchor-intro';
const NARRATIVE_CONCLUSION_ANCHOR_ID = 'narrative-anchor-conclusion';

export type CollapseAnalysis = {
    interval: string;
    rootCause: string;
    earliestPreventablePly: number;
    patchLineUci: string[];
    recoverabilityPlies: number;
};

export type ActivePlanRef = {
    themeL1: string;
    subplanId?: string;
    phase?: string;
    commitmentScore?: number;
};

export type StrategicPlanSummary = {
    planId: string;
    planName: string;
    rank: number;
    score: number;
    themeL1?: string;
    subplanId?: string | null;
};

export type LatentPlanSummary = {
    seedId: string;
    planName: string;
    viabilityScore: number;
    whyAbsentFromTopMultiPv: string;
};

export type EngineAlternative = {
    uci: string;
    san?: string;
    cpAfterAlt?: number;
    cpLossVsPlayed?: number;
    pv?: string[];
};

export type ActiveStrategicRouteRef = {
    routeId: string;
    ownerSide: string;
    piece: string;
    route: string[];
    purpose: string;
    strategicFit: number;
    tacticalSafety: number;
    surfaceConfidence: number;
    surfaceMode: 'exact' | 'toward' | 'hidden';
};

export type StrategyDirectionalTarget = {
    targetId: string;
    ownerSide: string;
    piece: string;
    from: string;
    targetSquare: string;
    readiness: 'build' | 'premature' | 'blocked' | 'contested';
    strategicReasons?: string[];
    prerequisites?: string[];
    evidence?: string[];
};

export type ActiveStrategicIdeaRef = {
    ideaId: string;
    ownerSide: string;
    kind: StrategicIdeaKind | string;
    group: StrategicIdeaGroup | string;
    readiness: 'ready' | 'build' | 'premature' | 'blocked';
    focusSummary: string;
    confidence: number;
};

export type ActiveStrategicMoveRef = {
    label: string;
    source: string;
    uci: string;
    san?: string;
    fenAfter?: string;
};

export type ActiveBranchRouteCue = {
    routeId: string;
    ownerSide: string;
    piece: string;
    route: string[];
    purpose: string;
    strategicFit: number;
    tacticalSafety: number;
    surfaceConfidence: number;
    surfaceMode: 'exact' | 'toward' | 'hidden';
};

export type ActiveBranchMoveCue = {
    label: string;
    uci: string;
    san?: string;
    source: string;
};

export type ActiveBranchDossier = {
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
};

export type ActiveStrategicThreadRef = {
    threadId: string;
    themeKey: string;
    themeLabel: string;
    stageKey: string;
    stageLabel: string;
};

export type ActiveStrategicThread = {
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
};

export type ProbeRequest = {
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
    objective?: string;
    requiredSignals?: string[];
};

export type AuthorQuestionSummary = {
    id: string;
    kind: string;
    priority: number;
    question: string;
    why?: string | null;
    anchors?: string[];
    confidence: string;
    latentPlanName?: string | null;
};

export type EvidenceBranchSummary = {
    keyMove: string;
    line: string;
    evalCp?: number | null;
    mate?: number | null;
    depth?: number | null;
};

export type AuthorEvidenceSummary = {
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
};

type GameChronicleMoment = {
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
    strategyPack?: {
        schema: string;
        sideToMove: string;
        strategicIdeas: Array<{
            ideaId: string;
            ownerSide: string;
            kind: StrategicIdeaKind | string;
            group: StrategicIdeaGroup | string;
            readiness: string;
            focusSquares?: string[];
            focusFiles?: string[];
            focusDiagonals?: string[];
            focusZone?: string | null;
            beneficiaryPieces?: string[];
            confidence: number;
        }>;
        pieceRoutes: Array<{
            ownerSide: string;
            piece: string;
            from: string;
            route: string[];
            purpose: string;
            strategicFit: number;
            tacticalSafety: number;
            surfaceConfidence: number;
            surfaceMode: string;
        }>;
        pieceMoveRefs: Array<{
            ownerSide: string;
            piece: string;
            from: string;
            target: string;
            idea: string;
        }>;
        directionalTargets: StrategyDirectionalTarget[];
        longTermFocus: string[];
    };
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
};
type GameChronicleReview = {
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
};
type GameChronicleResponse = {
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
};

const reviewCardInteractiveSelector = [
    'button',
    'a[href]',
    'input',
    'select',
    'textarea',
    'summary',
    '[role="button"]',
    '[role="link"]',
    '[role="tab"]',
    '[data-board]',
    '[data-route]',
    '[data-route-fen]',
    '[contenteditable="true"]',
].join(',');

function narrativeMomentAnchorId(moment: GameChronicleMoment, index: number): string {
    const raw = moment.momentId?.trim() || `${moment.ply}-${index}`;
    const normalized = raw
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '');
    return `narrative-anchor-${normalized || `moment-${index}`}`;
}

function moveRefLabel(ply: number, side?: 'white' | 'black', moveNumber?: number): string {
    const turn = moveNumber ?? plyToTurn(Math.max(1, ply));
    const resolvedSide = side || (ply % 2 === 1 ? 'white' : 'black');
    return `${turn}${resolvedSide === 'white' ? '.' : '...'}`;
}

function moveLabel(ply: number, side?: 'white' | 'black', moveNumber?: number): string {
    return `Move ${moveRefLabel(ply, side, moveNumber)}`;
}

function moveSpanLabel(startPly: number, endPly = startPly): string {
    if (startPly === endPly) return moveLabel(startPly);
    return `Moves ${moveRefLabel(startPly)} to ${moveRefLabel(endPly)}`;
}

function collapseIntervalLabel(interval: string): string {
    const parts = interval.split('-').map(part => parseInt(part, 10)).filter(Number.isFinite);
    const start = parts[0];
    const end = parts[1] ?? start;
    if (!start) return interval;
    return moveSpanLabel(start, end);
}

function outlineSummary(bits: Array<string | null | undefined>, limit = 2): string | null {
    const seen = new Set<string>();
    const summary = bits
        .map(bit => bit?.trim())
        .filter((bit): bit is string => !!bit)
        .filter(bit => {
            const key = bit.toLowerCase();
            if (seen.has(key)) return false;
            seen.add(key);
            return true;
        })
        .slice(0, limit);
    return summary.length ? summary.join(' · ') : null;
}

function narrativeMomentOutlineCopy(moment: GameChronicleMoment) {
    const classification = moment.moveClassification ? humanizeToken(moment.moveClassification) : null;
    const momentType = moment.momentType ? humanizeToken(moment.momentType) : null;
    const selectionTitle =
        moment.selectionKind === 'thread_bridge'
            ? moment.selectionLabel || 'Campaign Bridge'
            : null;
    const title = classification || selectionTitle || momentType || 'Key Moment';
    const detail = outlineSummary(
        [
            selectionTitle && selectionTitle !== title ? selectionTitle : null,
            momentType && momentType !== title ? momentType : null,
            ...((moment.concepts || []).slice(0, 2)),
            moment.transitionType ? humanizeToken(moment.transitionType) : null,
        ],
        3,
    );
    return {
        eyebrow: moveLabel(moment.ply, moment.side, moment.moveNumber),
        title,
        detail,
    };
}

function isCriticalMoment(moment: GameChronicleMoment): boolean {
    const classification = (moment.moveClassification || '').trim().toLowerCase();
    return !!moment.collapse || ['blunder', 'mistake', 'missed win', 'critical'].includes(classification);
}

function scrollToNarrativeAnchor(anchorId: string): void {
    const el = document.getElementById(anchorId);
    if (!el) return;
    window.requestAnimationFrame(() => {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
}

function jumpToNarrativePly(ctrl: NarrativeCtrl, ply: number, anchorId?: string): void {
    ctrl.root.jumpToMain(ply);
    ctrl.root.redraw();
    if (anchorId) scrollToNarrativeAnchor(anchorId);
}

function routeSurfaceText(route: { piece: string; route: string[]; surfaceMode: 'exact' | 'toward' | 'hidden' }): string {
    const squares = (route.route || []).filter(Boolean);
    const destination = squares[squares.length - 1] || 'the target square';
    return route.surfaceMode === 'exact' ? `${route.piece} ${squares.join('-')}` : `${route.piece} toward ${destination}`;
}

function humanizeIdeaKind(kind: string): string {
    return kind
        .replace(/[_-]+/g, ' ')
        .trim()
        .split(/\s+/)
        .filter(Boolean)
        .map(word => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
}

function ideaSurfaceText(idea: ActiveStrategicIdeaRef): string {
    const focus = (idea.focusSummary || '').trim();
    const readiness = idea.readiness === 'ready' ? '' : humanizeToken(idea.readiness);
    return [humanizeIdeaKind(idea.kind), focus, readiness].filter(Boolean).join(' · ');
}

function objectiveSurfaceText(target: StrategyDirectionalTarget): string {
    return `work toward making ${target.targetSquare} available`;
}

function routeOwnerLabel(side: string): string {
    return side === 'black' ? 'Black' : 'White';
}

type NarrativeStrategySurface = {
    dominantIdea: ActiveStrategicIdeaRef | null;
    secondaryIdea: ActiveStrategicIdeaRef | null;
    campaignOwner: string | null;
    ownerMismatch: boolean;
    executionRoute: ActiveStrategicRouteRef | ActiveBranchRouteCue | null;
    objectiveTargets: StrategyDirectionalTarget[];
    focus: string | null;
};

function strategyIdeaFromPack(moment: GameChronicleMoment, index: number): ActiveStrategicIdeaRef | null {
    const idea = moment.strategyPack?.strategicIdeas?.[index];
    if (!idea) return null;
    const focusSummary = [
        ...(idea.focusSquares || []),
        ...(idea.focusFiles || []),
        ...(idea.focusDiagonals || []),
        idea.focusZone || '',
    ]
        .filter(Boolean)
        .join(', ');
    return {
        ideaId: idea.ideaId,
        ownerSide: idea.ownerSide,
        kind: idea.kind,
        group: idea.group,
        readiness: idea.readiness as ActiveStrategicIdeaRef['readiness'],
        focusSummary,
        confidence: idea.confidence,
    };
}

function strategyExecutionFromPack(
    moment: GameChronicleMoment,
    owner: string | null,
): ActiveStrategicRouteRef | null {
    const route =
        moment.strategyPack?.pieceRoutes?.find(value => value.surfaceMode !== 'hidden' && (!owner || value.ownerSide === owner)) ||
        moment.strategyPack?.pieceRoutes?.find(value => value.surfaceMode !== 'hidden');
    if (route) {
        return {
            routeId: `${route.piece.toLowerCase()}-${route.route.join('-')}`,
            ownerSide: route.ownerSide,
            piece: route.piece,
            route: route.route,
            purpose: route.purpose,
            strategicFit: 0,
            tacticalSafety: 0,
            surfaceConfidence: 0,
            surfaceMode: (route.surfaceMode as 'exact' | 'toward' | 'hidden') || 'toward',
        };
    }
    const moveRef =
        moment.strategyPack?.pieceMoveRefs?.find(value => !owner || value.ownerSide === owner) ||
        moment.strategyPack?.pieceMoveRefs?.[0];
    if (!moveRef) return null;
    return {
        routeId: `${moveRef.piece.toLowerCase()}-${moveRef.from}-${moveRef.target}`,
        ownerSide: moveRef.ownerSide,
        piece: moveRef.piece,
        route: [moveRef.from, moveRef.target],
        purpose: moveRef.idea,
        strategicFit: 0,
        tacticalSafety: 0,
        surfaceConfidence: 0,
        surfaceMode: 'toward' as const,
    };
}

function narrativeStrategySurface(moment: GameChronicleMoment): NarrativeStrategySurface {
    const dominantIdea = moment.activeStrategicIdeas?.[0] || strategyIdeaFromPack(moment, 0);
    const secondaryIdea = moment.activeStrategicIdeas?.[1] || strategyIdeaFromPack(moment, 1);
    const campaignOwner = dominantIdea?.ownerSide || moment.strategyPack?.sideToMove || null;
    const ownerMismatch = !!campaignOwner && !!moment.side && campaignOwner !== moment.side;
    const executionRoute =
        (moment.activeBranchDossier?.routeCue && (!campaignOwner || moment.activeBranchDossier.routeCue.ownerSide === campaignOwner)
            ? moment.activeBranchDossier.routeCue
            : null) ||
        moment.activeStrategicRoutes?.find(route => route.surfaceMode !== 'hidden' && (!campaignOwner || route.ownerSide === campaignOwner)) ||
        moment.activeStrategicRoutes?.find(route => route.surfaceMode !== 'hidden') ||
        strategyExecutionFromPack(moment, campaignOwner);
    const objectiveTargets =
        (moment.activeDirectionalTargets || []).filter(target => !campaignOwner || target.ownerSide === campaignOwner) ||
        [];
    const fallbackTargets =
        objectiveTargets.length
            ? objectiveTargets
            : (moment.strategyPack?.directionalTargets || []).filter(target => !campaignOwner || target.ownerSide === campaignOwner);
    return {
        dominantIdea,
        secondaryIdea,
        campaignOwner,
        ownerMismatch,
        executionRoute,
        objectiveTargets: fallbackTargets,
        focus: moment.strategyPack?.longTermFocus?.find(Boolean)?.trim() || null,
    };
}

function eventTargetElement(target: EventTarget | null): Element | null {
    const candidate = target as Partial<Node> | null;
    if (!candidate || typeof candidate !== 'object' || typeof candidate.nodeType !== 'number') return null;
    return candidate.nodeType === 1 ? candidate as Element : (candidate as Node).parentElement;
}

export function shouldIgnoreReviewCardClick(target: EventTarget | null): boolean {
    return !!eventTargetElement(target)?.closest(reviewCardInteractiveSelector);
}

export function narrativeView(ctrl: NarrativeCtrl): VNode | null {
    if (!ctrl.enabled()) return null;

    const ccaEnabled = ctrl.data()?.ccaEnabled;
    const activeTab = ctrl.dnaTab();
    const hasCollapses = ctrl.data()?.moments?.some(m => m.collapse);

    return hl('div.narrative-box', {
        hook: {
            insert: vnode => ((vnode.elm as HTMLElement).scrollTop = 0)
        }
    }, [
        hl('div.narrative-header', [
            hl('h2', [
                hl('span.icon', { attrs: { ...dataIcon(licon.Book) } }),
                'Game Chronicle'
            ]),
            hl('button.button.button-empty.text', {
                attrs: { 'aria-label': 'Close' },
                hook: bind('click', ctrl.toggle, ctrl.root.redraw)
            }, hl('span', { attrs: { ...dataIcon(licon.X) } }))
        ]),
        // 3-tab bar (visible when CCA is enabled and data exists)
        ccaEnabled && ctrl.data() ? hl('div.narrative-tabs', [
            hl('button.narrative-tab' + (activeTab === 'narrative' ? '.active' : ''), {
                hook: bind('click', () => ctrl.switchTab('narrative'))
            }, 'Narrative'),
            hasCollapses ? hl('button.narrative-tab' + (activeTab === 'collapse' ? '.active' : ''), {
                hook: bind('click', () => ctrl.switchTab('collapse'))
            }, 'Collapse') : null,
            hl('button.narrative-tab' + (activeTab === 'dna' ? '.active' : ''), {
                hook: bind('click', () => ctrl.switchTab('dna'))
            }, 'Defeat DNA'),
        ]) : null,
        // Content area routed by active tab
        activeTab === 'dna' && ccaEnabled
            ? defeatDnaContentView(ctrl)
            : activeTab === 'collapse' && ccaEnabled
                ? collapseTabView(ctrl)
                : hl('div.narrative-content', [
                    ctrl.loading() ? hl('div.loader', ctrl.loadingDetail() || 'Game Chronicle in progress...') :
                        ctrl.error() ? hl('div.error', [
                            hl('div', ctrl.error()),
                            ctrl.needsLogin() ? hl('a.button', { attrs: { href: ctrl.loginHref() } }, 'Sign in') : null
                        ]) : ctrl.data() ? narrativeDocView(ctrl, ctrl.data()!) : hl('div.narrative-empty', 'No narrative generated yet.'),
                ]),
        !ctrl.loading() && !ctrl.data() && (() => {
            const totalPly = totalMainlinePly(ctrl.root);
            const chronicleEligible = totalPly >= MIN_GAME_CHRONICLE_PLY;
            return hl('div.actions', [
                hl(
                    'div.narrative-disclosure',
                    chronicleEligible
                        ? 'Game Chronicle runs a deeper on-device WASM scan and may take longer on large PGNs.'
                        : gameChronicleShortGameMessage(totalPly),
                ),
                hl(`button.button.action${chronicleEligible ? '' : '.disabled'}`, {
                    attrs: { type: 'button', disabled: !chronicleEligible },
                    hook: chronicleEligible
                        ? bind('click', () => {
                            void ctrl.fetchNarrative();
                        }, ctrl.root.redraw)
                        : undefined,
                }, chronicleEligible ? 'Run Game Chronicle' : `Game Chronicle opens from move ${moveNumberFromPly(MIN_GAME_CHRONICLE_PLY)}`)
            ]);
        })()
    ]);
}

// ── Collapse Tab ──────────────────────────────────────────────────────

export function collapseTabView(ctrl: NarrativeCtrl, activeCollapseId?: string | null): VNode {
    const data = ctrl.data();
    const moments = (data?.moments || []).filter(m => m.collapse);
    if (!moments.length) {
        return hl('div.narrative-content.collapse-tab-empty', [
            hl('p', 'No causal collapse detected in this game.'),
        ]);
    }
    return hl('div.narrative-content.collapse-tab', [
        hl('h3.dna-section-title', `${moments.length} Collapse${moments.length > 1 ? 's' : ''} Detected`),
        collapseTimelineView(ctrl, moments),
        ...moments.map(m => narrativeCollapseCardView(ctrl, m, {
            selected: !!activeCollapseId && m.collapse?.interval === activeCollapseId,
        })),
    ]);
}

export function collapseTimelineView(ctrl: NarrativeCtrl, moments: GameChronicleMoment[]): VNode {
    const totalPlies = ctrl.root.mainline.length > 0
        ? ctrl.root.mainline[ctrl.root.mainline.length - 1].ply
        : 1;

    const segments: VNode[] = [];
    const markers: VNode[] = [];

    for (const m of moments) {
        const c = m.collapse!;
        // Parse interval "22-27" → [22, 27]
        const parts = c.interval.split('-').map(Number);
        const start = parts[0] || 0;
        const end = parts[1] || start;
        const color = CAUSE_COLORS[c.rootCause] || DEFAULT_CAUSE_COLOR;

        const leftPct = (start / totalPlies) * 100;
        const widthPct = Math.max(((end - start + 1) / totalPlies) * 100, 1.5);

        // Collapse interval segment
        segments.push(
            hl('div.timeline-segment', {
                style: { left: `${leftPct}%`, width: `${widthPct}%`, background: color },
                attrs: { title: `${c.rootCause} (${collapseIntervalLabel(c.interval)})` },
                hook: bind('click', () => jumpToNarrativePly(ctrl, start)),
            }),
        );

        // Earliest preventable ply marker (diamond)
        const preventPct = (c.earliestPreventablePly / totalPlies) * 100;
        markers.push(
            hl('div.timeline-marker', {
                style: { left: `${preventPct}%` },
                attrs: { title: `Preventable at ${moveLabel(c.earliestPreventablePly)}` },
                hook: bind('click', () => jumpToNarrativePly(ctrl, c.earliestPreventablePly)),
            }),
        );
    }

    return hl('div.collapse-timeline', [
        hl('div.timeline-track', [...segments, ...markers]),
        hl('div.timeline-labels', [
            hl('span', moveRefLabel(1)),
            hl('span', moveRefLabel(totalPlies)),
        ]),
    ]);
}

// ── Defeat DNA Dashboard ──────────────────────────────────────────────

const CAUSE_COLORS: Record<string, string> = {
    'Tactical Miss': 'hsl(0, 70%, 55%)',
    'Plan Deviation': 'hsl(35, 80%, 55%)',
    'King Safety': 'hsl(280, 60%, 55%)',
    'Time Pressure': 'hsl(200, 70%, 50%)',
    'Positional Error': 'hsl(160, 55%, 45%)',
};
const DEFAULT_CAUSE_COLOR = 'hsl(220, 40%, 55%)';

export function defeatDnaContentView(ctrl: NarrativeCtrl): VNode {
    if (ctrl.dnaLoading()) {
        return hl('div.narrative-content.dna-loading', hl('div.loader', 'Loading Defeat DNA...'));
    }
    if (ctrl.dnaError()) {
        return hl('div.narrative-content.dna-error', hl('div.error', ctrl.dnaError()));
    }
    const report = ctrl.dnaData();
    if (!report || report.totalGamesAnalyzed === 0) {
        return hl('div.narrative-content.dna-empty', [
            hl('div.dna-empty-icon', '🧬'),
            hl('p', 'No Defeat DNA data yet.'),
            hl('p.dna-empty-hint', 'Run a few game analyses to build your profile.'),
        ]);
    }
    return hl('div.defeat-dna-dashboard', [
        defeatDnaStatCards(report),
        defeatDnaBarChart(report),
        defeatDnaRecentTable(ctrl),
    ]);
}

function defeatDnaStatCards(report: DefeatDnaReport): VNode {
    return hl('div.dna-stat-row', [
        hl('div.dna-stat-card', [
            hl('div.dna-stat-value', String(report.totalGamesAnalyzed)),
            hl('div.dna-stat-label', 'Games Analyzed'),
        ]),
        hl('div.dna-stat-card', [
            hl('div.dna-stat-value', report.avgRecoverabilityPlies.toFixed(1)),
            hl('div.dna-stat-label', 'Avg Recovery (steps)'),
        ]),
        hl('div.dna-stat-card', [
            hl('div.dna-stat-value', String(Object.keys(report.rootCauseDistribution).length)),
            hl('div.dna-stat-label', 'Cause Types'),
        ]),
    ]);
}

function defeatDnaBarChart(report: DefeatDnaReport): VNode {
    const dist = report.rootCauseDistribution;
    const entries = Object.entries(dist).sort((a, b) => b[1] - a[1]);
    const maxVal = entries.length ? entries[0][1] : 1;

    return hl('div.dna-bar-chart', [
        hl('h3.dna-section-title', 'Root Cause Distribution'),
        ...entries.map(([cause, count]) => {
            const pct = Math.round((count / maxVal) * 100);
            const color = CAUSE_COLORS[cause] || DEFAULT_CAUSE_COLOR;
            return hl('div.dna-bar-row', [
                hl('span.dna-bar-label', cause),
                hl('div.dna-bar-track', [
                    hl('div.dna-bar-fill', {
                        style: { width: `${pct}%`, background: color },
                    }),
                ]),
                hl('span.dna-bar-count', String(count)),
            ]);
        }),
    ]);
}

function defeatDnaRecentTable(ctrl: NarrativeCtrl): VNode {
    const report = ctrl.dnaData();
    const allCollapses = report?.recentCollapses || [];
    const showAll = ctrl.showAllCollapses();
    const visible = showAll ? allCollapses.slice(0, 10) : allCollapses.slice(0, 5);
    const hasMore = !showAll && allCollapses.length > 5;

    if (!visible.length) {
        return hl('div.dna-recent-empty', 'No collapse history yet. Analyze more games to build your profile.');
    }

    return hl('div.dna-recent-collapses', [
        hl('h3.dna-section-title', 'Recent Collapses'),
        hl('table.dna-collapse-table', [
            hl('thead', hl('tr', [
                hl('th', 'Interval'),
                hl('th', 'Root Cause'),
                hl('th', 'Recovery'),
                hl('th', 'Preventable'),
            ])),
            hl('tbody', visible.map(c =>
                hl('tr', [
                    hl('td.dna-cell-interval', collapseIntervalLabel(c.interval)),
                    hl('td.dna-cell-cause', {
                        style: { color: CAUSE_COLORS[c.rootCause] || DEFAULT_CAUSE_COLOR }
                    }, c.rootCause),
                    hl('td.dna-cell-recov', `${c.recoverabilityPlies} steps`),
                    hl('td.dna-cell-prevent', moveLabel(c.earliestPreventablePly)),
                ])
            )),
        ]),
        hasMore ? hl('button.button.button-empty.dna-show-more', {
            hook: bind('click', () => {
                ctrl.showAllCollapses(true);
                ctrl.root.redraw();
            })
        }, `Show ${allCollapses.length - 5} more`) : null,
    ]);
}

// ── Story View ────────────────────────────────────────────────────────

function narrativeDocView(ctrl: NarrativeCtrl, doc: GameChronicleResponse): VNode {
    const threadSummaries = new Map<number, ActiveStrategicThread>();
    (doc.strategicThreads || []).forEach(thread => {
        const firstRepresentative = (thread.representativePlies || []).find(Boolean);
        if (typeof firstRepresentative === 'number' && !threadSummaries.has(firstRepresentative)) {
            threadSummaries.set(firstRepresentative, thread);
        }
    });
    return hl('div.narrative-doc', {
        hook: {
            insert: vnode => {
                const el = vnode.elm as HTMLElement;
                bindPreviewHover(ctrl, el);
                initMiniBoards(el);
            },
            postpatch: (_, vnode) => {
                initMiniBoards(vnode.elm as HTMLElement);
            },
        },
    }, [
        hl('div.narrative-preview', [
            ctrl.pvBoard()
                ? renderBoardPreview(ctrl.pvBoard() as BoardPreview, ctrl.root.getOrientation())
                : hl('div.narrative-preview-empty', 'Hover a move to preview'),
        ]),
        narrativeReviewView(doc, ctrl),
        narrativeOutlineView(ctrl, doc),
        doc.sourceMode || doc.model || doc.planTier || doc.llmLevel
            ? hl('div.narrative-review-metrics', [
                doc.sourceMode ? hl('span.narrative-review-metric', `Source: ${doc.sourceMode}`) : null,
                doc.model ? hl('span.narrative-review-metric', `Model: ${doc.model}`) : null,
                doc.planTier ? hl('span.narrative-review-metric', `Plan: ${doc.planTier}`) : null,
                doc.llmLevel ? hl('span.narrative-review-metric', `Level: ${doc.llmLevel}`) : null,
            ])
            : null,
        hl('section.narrative-intro', {
            attrs: { id: NARRATIVE_INTRO_ANCHOR_ID },
        }, [
            hl(
                'div.narrative-themes',
                doc.themes?.length ? doc.themes.map(t => hl('span.narrative-theme', cleanNarrativeSurfaceLabel(t))) : null,
            ),
            hl('pre.narrative-prose', cleanNarrativeProseText(doc.intro)),
        ]),
        ...(doc.moments || []).map((moment, index) => narrativeMomentView(ctrl, moment, {
            anchorId: narrativeMomentAnchorId(moment, index),
            threadSummary: threadSummaries.get(moment.ply),
        })),
        hl('section.narrative-conclusion', {
            attrs: { id: NARRATIVE_CONCLUSION_ANCHOR_ID },
        }, [hl('pre.narrative-prose', cleanNarrativeProseText(doc.conclusion))]),
        narrativeBetaFeedbackView(ctrl),
    ]);
}

function narrativeBetaFeedbackView(ctrl: NarrativeCtrl): VNode {
    const submitted = ctrl.betaFeedbackSubmitted();
    const loading = ctrl.betaFeedbackLoading();
    const message = ctrl.betaFeedbackMessage();
    return hl('section.narrative-feedback', [
        hl('div.narrative-feedback__copy', [
            hl('span.narrative-feedback__eyebrow', 'Open beta signal'),
            hl('strong.narrative-feedback__title', 'Would you pay for deeper full-PGN analysis if this stayed useful?'),
            hl(
                'p.narrative-feedback__body',
                submitted
                    ? 'Your answer was stored for this open beta. If you want a launch email for paid plans later, join the waitlist below.'
                    : 'We ask this after Game Chronicle finishes so pricing feedback comes after a real run, not before.',
            ),
        ]),
        hl(
            'div.narrative-feedback__actions',
            submitted
                ? [
                    hl('span.narrative-feedback__saved', humanizeToken(submitted)),
                    hl(
                        'a.button.button-empty.narrative-feedback__waitlist',
                        { attrs: { href: ctrl.betaFeedbackHref(true) } },
                        'Join paid-plan waitlist',
                    ),
                ]
                : ([
                    ['would_pay', 'Would pay'],
                    ['maybe', 'Maybe'],
                    ['not_now', 'Not for now'],
                ] as const).map(([value, label]) =>
                    hl(
                        'button.button.button-empty.narrative-feedback__choice',
                        {
                            attrs: { type: 'button', disabled: loading },
                            hook: bind('click', () => {
                                void ctrl.submitBetaFeedback(value);
                            }, ctrl.root.redraw),
                        },
                        label,
                    ),
                ),
        ),
        message ? hl('p.narrative-feedback__message', message) : null,
    ]);
}

function narrativeOutlineView(ctrl: NarrativeCtrl, doc: GameChronicleResponse): VNode | null {
    const moments = doc.moments || [];
    if (!moments.length) return null;

    const items = [
        hl('button.narrative-outline-item.section', {
            attrs: {
                type: 'button',
                title: 'Jump to introduction',
                'aria-label': 'Jump to introduction',
            },
            hook: bind('click', () => scrollToNarrativeAnchor(NARRATIVE_INTRO_ANCHOR_ID)),
        }, [
            hl('span.narrative-outline-item-eyebrow', 'Overview'),
            hl('strong.narrative-outline-item-title', 'Opening frame'),
            doc.themes?.length
              ? hl(
                  'span.narrative-outline-item-detail',
                  doc.themes.slice(0, 3).map(cleanNarrativeSurfaceLabel).join(' · '),
                )
              : hl('span.narrative-outline-item-detail', 'Themes and starting ideas'),
        ]),
        ...moments.map((moment, index) => {
            const anchorId = narrativeMomentAnchorId(moment, index);
            const copy = narrativeMomentOutlineCopy(moment);
            return hl(`button.narrative-outline-item${isCriticalMoment(moment) ? '.critical' : ''}`, {
                key: `outline-${anchorId}`,
                attrs: {
                    type: 'button',
                    title: `Jump to ${copy.eyebrow}`,
                    'aria-label': `Jump to ${copy.eyebrow}`,
                },
                hook: bind('click', () => jumpToNarrativePly(ctrl, moment.ply, anchorId)),
            }, [
                hl('span.narrative-outline-item-eyebrow', copy.eyebrow),
                hl('strong.narrative-outline-item-title', copy.title),
                copy.detail ? hl('span.narrative-outline-item-detail', copy.detail) : null,
            ]);
        }),
        hl('button.narrative-outline-item.section', {
            attrs: {
                type: 'button',
                title: 'Jump to conclusion',
                'aria-label': 'Jump to conclusion',
            },
            hook: bind('click', () => scrollToNarrativeAnchor(NARRATIVE_CONCLUSION_ANCHOR_ID)),
        }, [
            hl('span.narrative-outline-item-eyebrow', 'Wrap-up'),
            hl('strong.narrative-outline-item-title', 'Final takeaway'),
            hl('span.narrative-outline-item-detail', 'Closing evaluation and practical summary'),
        ]),
    ];

    return hl('section.narrative-outline', [
        hl('div.narrative-outline-header', [
            hl('h3.narrative-outline-title', 'Story Outline'),
            hl('span.narrative-outline-count', `${moments.length} key moment${moments.length === 1 ? '' : 's'}`),
        ]),
        hl('div.narrative-outline-list', items),
    ]);
}

export function narrativeReviewView(doc: GameChronicleResponse, ctrl?: NarrativeCtrl): VNode | null {
    const review = doc.review;
    if (!review) return null;

    const totalPlies = Math.max(0, review.totalPlies || 0);
    const evalCoveredPlies = Math.max(0, review.evalCoveredPlies || 0);
    const evalCoveragePct = Math.max(0, Math.min(100, review.evalCoveragePct || 0));
    const selectedMoments = Math.max(0, review.selectedMoments || 0);
    const internalMomentCount = Math.max(0, review.internalMomentCount || 0);
    const visibleMomentCount = Math.max(0, review.visibleMomentCount || selectedMoments);
    const polishedMomentCount = Math.max(0, review.polishedMomentCount || 0);
    const visibleStrategicMomentCount = Math.max(0, review.visibleStrategicMomentCount || 0);
    const visibleBridgeMomentCount = Math.max(0, review.visibleBridgeMomentCount || 0);
    const totalMoves = Math.max(1, Math.ceil(totalPlies / 2));
    const selectedMomentPlies = (review.selectedMomentPlies || [])
        .map(p => Math.trunc(p))
        .filter(p => p > 0 && (totalPlies <= 0 || p <= totalPlies));
    const momentsByPly = new Map((doc.moments || []).map((moment, index) => [moment.ply, { moment, anchorId: narrativeMomentAnchorId(moment, index) }]));

    const summary =
        totalPlies > 0
            ? `Game span ${totalMoves} moves. Engine coverage ${evalCoveragePct}% across ${evalCoveredPlies} evaluated positions.`
            : `Engine eval coverage ${evalCoveragePct}%.`;

    return hl('section.narrative-review', [
        hl('div.narrative-review-summary', summary),
        hl('div.narrative-review-metrics', [
            review.blundersCount !== undefined ? hl('span.narrative-review-metric.blunder', `Blunders: ${review.blundersCount}`) : null,
            review.missedWinsCount !== undefined ? hl('span.narrative-review-metric.missed', `Missed Wins: ${review.missedWinsCount}`) : null,
            hl('span.narrative-review-metric', `Selected moments: ${selectedMoments}`),
            internalMomentCount > 0 ? hl('span.narrative-review-metric', `Internal coverage: ${internalMomentCount}`) : null,
            review.visibleMomentCount !== undefined ? hl('span.narrative-review-metric', `Visible moments: ${visibleMomentCount}`) : null,
            review.polishedMomentCount !== undefined ? hl('span.narrative-review-metric', `Polish targets: ${polishedMomentCount}`) : null,
            review.visibleStrategicMomentCount !== undefined
                ? hl('span.narrative-review-metric', `Strategic reps: ${visibleStrategicMomentCount}`)
                : null,
            review.visibleBridgeMomentCount !== undefined
                ? hl('span.narrative-review-metric', `Visible bridges: ${visibleBridgeMomentCount}`)
                : null,
        ]),
        totalPlies > 0
            ? hl('div.narrative-review-timeline', [
                hl('div.narrative-review-track'),
                ...selectedMomentPlies.map((ply, idx) => {
                    const ratio = totalPlies <= 1 ? 0 : (ply - 1) / (totalPlies - 1);
                    const left = Math.max(0, Math.min(100, Math.round(ratio * 1000) / 10));
                    const target = momentsByPly.get(ply);
                    const moment = target?.moment;
                    const label = moment ? moveLabel(moment.ply, moment.side, moment.moveNumber) : moveLabel(ply);
                    const titleParts = outlineSummary([
                        label,
                        moment?.moveClassification ? humanizeToken(moment.moveClassification) : null,
                        moment?.concepts?.[0],
                    ], 3) || label;
                    return ctrl
                        ? hl(`button.narrative-review-marker${moment && isCriticalMoment(moment) ? '.critical' : ''}`, {
                            key: `moment-${ply}-${idx}`,
                            attrs: {
                                type: 'button',
                                style: `left:${left}%;`,
                                title: titleParts,
                                'aria-label': titleParts,
                            },
                            hook: bind('click', () => jumpToNarrativePly(ctrl, ply, target?.anchorId)),
                        })
                        : hl(`span.narrative-review-marker${moment && isCriticalMoment(moment) ? '.critical' : ''}`, {
                            key: `moment-${ply}-${idx}`,
                            attrs: {
                                style: `left:${left}%;`,
                                title: titleParts,
                                'aria-label': titleParts,
                            },
                        });
                }),
                hl('div.narrative-review-labels', [
                    hl('span', moveRefLabel(1)),
                    hl('span', moveRefLabel(totalPlies)),
                ]),
            ])
            : null,
    ]);
}

export function narrativeMomentView(
    ctrl: NarrativeCtrl,
    moment: GameChronicleMoment,
    opts: { selected?: boolean; onSelect?: () => void; anchorId?: string; threadSummary?: ActiveStrategicThread; compact?: boolean } = {},
): VNode {
    const title = moveLabel(moment.ply, moment.side, moment.moveNumber);
    const variations = (moment.variations || []).filter(v => Array.isArray(v.moves) && v.moves.length);
    const hasStrategicBlock =
        !!opts.threadSummary ||
        !!moment.activeStrategicNote ||
        !!moment.activeBranchDossier ||
        !!moment.activeStrategicIdeas?.length ||
        !!moment.activeStrategicMoves?.length ||
        !!moment.activeStrategicRoutes?.length ||
        !!moment.activeDirectionalTargets?.length ||
        !!moment.strategicThread;
    const compactOpeningMoment =
        !!opts.compact &&
        (moment.selectionKind === 'opening' ||
            /opening/i.test(moment.momentType || '') ||
            /opening/i.test(moment.moveClassification || '') ||
            moment.ply <= 10);
    const prose = opts.compact
        ? summarizeReviewMomentProse(moment.narrative, compactOpeningMoment)
        : cleanNarrativeProseText(moment.narrative);
    const advancedDetails = narrativeAdvancedDetailsView(ctrl, moment, hasStrategicBlock);

    return hl('section.narrative-moment', {
        attrs: opts.anchorId ? { 'data-ply': moment.ply, id: opts.anchorId } : { 'data-ply': moment.ply },
        class: { active: !!opts.selected },
        hook: opts.onSelect
            ? bind('click', e => {
                if (!shouldIgnoreReviewCardClick(e.target)) opts.onSelect?.();
            })
            : undefined,
    }, [
        hl('header.narrative-moment-header', [
            hl('div.narrative-moment-title-box', [
                hl('button.button.button-empty.narrative-jump', {
                    hook: bind(
                        'click',
                        () => {
                            opts.onSelect?.();
                            jumpToNarrativePly(ctrl, moment.ply, opts.anchorId);
                        },
                        undefined,
                    ),
                }, title),
                moment.side ? hl(`span.narrative-side.${moment.side}`, moment.side) : null,
                moment.moveClassification ? narrativeBadgeView(moment.moveClassification, 'classification') : null,
                moment.selectionKind === 'thread_bridge' && moment.selectionLabel
                    ? narrativeBadgeView(moment.selectionLabel, 'selection')
                    : null,
                moment.momentType ? narrativeBadgeView(moment.momentType, 'type') : null,
                moment.strategicSalience ? narrativeBadgeView(moment.strategicSalience, 'salience') : null,
                moment.strategicBranch ? narrativeBadgeView('Strategic Branch', 'branch') : null,
            ]),
            moment.concepts?.length
                ? hl(
                    'div.narrative-concepts',
                    moment.concepts.map(c => hl('span.narrative-concept', cleanNarrativeSurfaceLabel(c))),
                )
                : null,
        ]),
        opts.threadSummary ? narrativeThreadSummaryView(opts.threadSummary) : null,
        moment.selectionKind === 'thread_bridge' && moment.selectionReason
            ? hl('div.narrative-selection-reason', cleanNarrativeProseText(moment.selectionReason))
            : null,
        narrativeProseView(ctrl, moment, prose, 'moment'),
        narrativeSignalSummaryView(ctrl, moment),
        advancedDetails,
        moment.collapse ? narrativeCollapseCardView(ctrl, moment, {
            selected: !!opts.selected,
            onSelect: opts.onSelect,
            anchorId: opts.anchorId,
        }) : null,
        variations.length
            ? hl('div.narrative-variations', [
                hl('h3', 'Variations'),
                hl('div.narrative-variation-list', variations.map((v, i) => narrativeVariationView(ctrl, moment.fen, v, i))),
            ])
            : null,
    ]);
}

function narrativeSignalSummaryView(ctrl: NarrativeCtrl, moment: GameChronicleMoment): VNode | null {
    const digest = moment.signalDigest;
    const canonicalDecisionComparison = digest?.decisionComparison;
    const fallbackDecisionComparison = !canonicalDecisionComparison
        ? narrativeFallbackDecisionComparison(moment)
        : undefined;
    const decisionComparison = canonicalDecisionComparison || fallbackDecisionComparison;
    const moveRefs = buildInlineMoveRefMap(moment, ctrl.root.data.game.variant.key);
    const mainPlans = (moment.mainStrategicPlans || []).slice(0, 2);
    const experimentIndex = strategicPlanExperimentIndex(moment.strategicPlanExperiments || []);
    const holdReasons = (moment.whyAbsentFromTopMultiPV || []).filter(Boolean).slice(0, 2);
    const deploymentSummary = digest ? formatDeploymentSummary(digest) : null;
    const decisionSurface = buildDecisionComparisonSurface(decisionComparison, {
        includeEngineLine: true,
        includeEvidence: true,
    });
    const compactSurface = buildCompactSupportSurface({
        signalDigest: digest,
        mainPlanTexts: mainPlans.map(plan => formatStrategicPlanText(plan, experimentIndex, { includeRank: true })),
        holdReasons,
        deploymentSummary,
    });

    if (
        !compactSurface.mainPlanTexts.length &&
        !compactSurface.holdReasons.length &&
        !decisionSurface.headline &&
        !decisionSurface.secondary &&
        !decisionSurface.engineLine &&
        !decisionSurface.evidence &&
        !compactSurface.rows.length
    ) return null;

    return hl('div.narrative-signal-box', [
        hl('h3.narrative-signal-title', 'Support'),
        compactSurface.mainPlanTexts.length
            ? hl('div.narrative-signal-group', [
                hl('span.narrative-signal-label', 'Main plans'),
                hl('div.narrative-signal-chip-list', compactSurface.mainPlanTexts.map((planText, idx) =>
                    hl('span.narrative-signal-chip', {
                        key: `plan-${idx}`,
                    }, planText),
                )),
            ])
            : null,
        narrativeDecisionComparisonView(decisionComparison, decisionSurface, moveRefs),
        compactSurface.holdReasons.length
            ? hl('div.narrative-signal-list', compactSurface.holdReasons.map((reason, idx) =>
                hl('div.narrative-signal-row', { key: `hold-${idx}` }, [
                    hl('span.narrative-signal-row-label', `${idx === 0 ? 'Why it stayed conditional' : 'Also'}:`),
                    hl('span.narrative-signal-row-value', reason),
                ]),
            ))
            : null,
        compactSurface.rows.length
            ? hl('div.narrative-signal-list', compactSurface.rows.map(([label, value], idx) =>
                hl('div.narrative-signal-row', { key: `${label}-${idx}` }, [
                    hl('span.narrative-signal-row-label', `${label}:`),
                    hl('span.narrative-signal-row-value', value),
                ]),
            ))
            : null,
    ]);
}

function narrativeAdvancedDetailsView(
    ctrl: NarrativeCtrl,
    moment: GameChronicleMoment,
    hasStrategicBlock: boolean,
): VNode | null {
    const signalDetails = narrativeAdvancedSignalDetailsView(moment);
    const evidence = narrativeEvidenceSummaryView(ctrl, moment);
    const strategicNote = hasStrategicBlock ? narrativeStrategicNoteView(ctrl, moment) : null;
    const activePlan = moment.activePlan ? narrativeActivePlanView(moment.activePlan) : null;
    const blocks = [signalDetails, evidence, strategicNote, activePlan].filter(Boolean) as VNode[];
    if (!blocks.length) return null;
    return hl('details.narrative-advanced-details', [
        hl('summary.narrative-advanced-details__summary', 'Advanced details'),
        hl('div.narrative-advanced-details__body', blocks),
    ]);
}

function narrativeAdvancedSignalDetailsView(moment: GameChronicleMoment): VNode | null {
    const digest = moment.signalDigest;
    const supportOpts = buildPlayerFacingSupportOptions(digest);
    const strategySurface = narrativeStrategySurface(moment);
    const practicalFactors = filterPlayerFacingValues((digest?.practicalFactors || []).filter(Boolean).slice(0, 2), supportOpts);
    const structureProfileBits = filterPlayerFacingValues([
        digest?.structureProfile,
        digest?.centerState ? `${digest.centerState.toLowerCase()} center` : null,
    ], supportOpts);

    const signalRows = filterPlayerFacingRows([
        digest?.decision ? ['Decision', digest.decision] : null,
    ], supportOpts);
    const structureRows = [
        rewritePlayerFacingSupportText(digest?.structuralCue, supportOpts)
            ? ['Summary', rewritePlayerFacingSupportText(digest?.structuralCue, supportOpts)!]
            : null,
        structureProfileBits.length ? ['Profile', structureProfileBits.join(' · ')] : null,
    ].filter(Boolean) as [string, string][];
    const prophylaxisRows = filterPlayerFacingRows([
        digest?.prophylaxisThreat ? ['Prophylaxis', `stops ${digest.prophylaxisThreat}`] : null,
        digest?.prophylaxisPlan ? ['Prophylaxis', `slows down ${digest.prophylaxisPlan}`] : null,
    ], supportOpts);
    const compensationRows = filterPlayerFacingRows([
        digest?.compensation ? ['Compensation', digest.compensation] : null,
    ], supportOpts);
    const practicalRows = practicalFactors.length ? [['Practical', practicalFactors.join('; ')]] as [string, string][] : [];
    const strategyRows = filterPlayerFacingRows([
        strategySurface.dominantIdea ? ['Idea', `Dominant ${ideaSurfaceText(strategySurface.dominantIdea)}`] : null,
        strategySurface.secondaryIdea ? ['Idea', `Secondary ${ideaSurfaceText(strategySurface.secondaryIdea)}`] : null,
        strategySurface.executionRoute ? ['Execution', routeSurfaceText(strategySurface.executionRoute)] : null,
        strategySurface.objectiveTargets.length ? ['Objective', objectiveSurfaceText(strategySurface.objectiveTargets[0])] : null,
        strategySurface.focus ? ['Focus', strategySurface.focus] : null,
    ], supportOpts);

    if (
        !signalRows.length &&
        !structureRows.length &&
        !strategyRows.length &&
        !prophylaxisRows.length &&
        !practicalRows.length &&
        !compensationRows.length
    ) return null;

    return hl('div.narrative-signal-box.narrative-signal-box--advanced', [
        hl('h3.narrative-signal-title', 'Signal details'),
        signalRows.length ? narrativeSignalGroupView('Signals', signalRows) : null,
        strategyRows.length ? narrativeSignalGroupView('Strategy', strategyRows) : null,
        structureRows.length ? narrativeSignalGroupView('Structure', structureRows) : null,
        prophylaxisRows.length ? narrativeSignalGroupView('Prophylaxis', prophylaxisRows) : null,
        practicalRows.length ? narrativeSignalGroupView('Practical', practicalRows) : null,
        compensationRows.length ? narrativeSignalGroupView('Compensation', compensationRows) : null,
    ]);
}

function narrativeFallbackDecisionComparison(
    moment: GameChronicleMoment,
): DecisionComparisonDigest | undefined {
    const alt = moment.topEngineMove;
    if (!alt) return undefined;

    const engineBestMove = (alt.san || alt.uci || '').trim();
    if (!engineBestMove) return undefined;

    return {
        engineBestMove,
        engineBestPv: (alt.pv || []).filter(Boolean),
        cpLossVsChosen: typeof alt.cpLossVsPlayed === 'number' ? alt.cpLossVsPlayed : undefined,
        chosenMatchesBest: false,
    };
}

function narrativeDecisionComparisonView(
    comparison: DecisionComparisonDigest | undefined,
    surface: ReturnType<typeof buildDecisionComparisonSurface>,
    moveRefs: Map<string, string>,
): VNode | null {
    const detailRows: Array<[string, string]> = [];

    if (surface.engineLine) detailRows.push(['Engine line', surface.engineLine]);
    if (surface.evidence) detailRows.push(['Evidence', surface.evidence]);

    const headline = surface.headline || null;
    const secondary = surface.secondary || null;

    if (!headline && !secondary && !detailRows.length) return null;

    const moveStrip = narrativeDecisionMoveStrip(comparison, moveRefs);

    const containerSelector = detailRows.length
        ? 'details.narrative-decision-compare'
        : 'div.narrative-decision-compare.narrative-decision-compare--static';
    const summaryChildren = [
        hl('span.narrative-decision-compare__kicker', 'Decision compare'),
        hl('div.narrative-decision-compare__copy', [
            moveStrip,
            headline ? hl('div.narrative-decision-compare__headline', headline) : null,
            secondary ? hl('div.narrative-decision-compare__secondary', secondary) : null,
        ]),
        surface.gap ? hl('span.narrative-decision-compare__gap', surface.gap) : null,
    ];

    return hl(containerSelector, [
        detailRows.length
            ? hl('summary.narrative-decision-compare__summary', summaryChildren)
            : hl('div.narrative-decision-compare__summary.narrative-decision-compare__summary--static', summaryChildren),
        detailRows.length
            ? hl('div.narrative-decision-compare__details', detailRows.map(([label, value], idx) =>
                hl('div.narrative-decision-compare__detail', { key: `${label}-${idx}` }, [
                    hl('span.narrative-decision-compare__detail-label', `${label}:`),
                    hl('span.narrative-decision-compare__detail-value', value),
                ]),
            ))
            : null,
    ]);
}

function narrativeDecisionMoveStrip(
    comparison: DecisionComparisonDigest | undefined,
    moveRefs: Map<string, string>,
): VNode | null {
    if (!comparison) return null;

    const chips = [
        narrativeDecisionMoveChip('Chosen', comparison.chosenMove, moveRefs, 'chosen'),
        !comparison.chosenMatchesBest
            ? narrativeDecisionMoveChip('Engine', comparison.engineBestMove, moveRefs, 'engine')
            : null,
        comparison.deferredMove
            ? narrativeDecisionMoveChip(
                comparison.practicalAlternative ? 'Practical' : 'Deferred',
                comparison.deferredMove,
                moveRefs,
                'deferred',
            )
            : null,
    ].filter(Boolean) as VNode[];

    if (!chips.length) return null;
    return hl('div.narrative-decision-compare__moves', chips);
}

function narrativeDecisionMoveChip(
    label: string,
    move: string | undefined,
    moveRefs: Map<string, string>,
    tone: 'chosen' | 'engine' | 'deferred',
): VNode | null {
    const normalized = normalizeSanToken(move);
    if (!normalized) return null;
    const payload = moveRefs.get(normalized);
    return hl(`span.narrative-decision-compare__move.narrative-decision-compare__move--${tone}`, [
        hl('span.narrative-decision-compare__move-label', label),
        hl(`span.narrative-decision-compare__move-chip${payload ? '.narrative-move' : ''}`, payload ? {
            attrs: { 'data-board': payload, title: `${label}: ${move}` },
        } : undefined, move || normalized),
    ]);
}

function narrativeSignalGroupView(title: string, rows: [string, string][]): VNode | null {
    if (!rows.length) return null;
    return hl('div.narrative-signal-group', [
        hl('span.narrative-signal-label', title),
        hl('div.narrative-signal-list', rows.map(([label, value], idx) =>
            hl('div.narrative-signal-row', { key: `${title}-${label}-${idx}` }, [
                hl('span.narrative-signal-row-label', `${label}:`),
                hl('span.narrative-signal-row-value', value),
            ]),
        )),
    ]);
}

function narrativeEvidenceSummaryView(ctrl: NarrativeCtrl, moment: GameChronicleMoment): VNode | null {
    const probeRequests = (moment.probeRequests || []).slice(0, 1);
    const authorEvidence = (moment.authorEvidence || []).slice(0, 2);
    const authorQuestions = (moment.authorQuestions || []).slice(0, 2);
    const moveRefs = buildInlineMoveRefMap(moment, ctrl.root.data.game.variant.key);
    const supportOpts = buildPlayerFacingSupportOptions(moment.signalDigest);

    if (!probeRequests.length && !authorEvidence.length && !authorQuestions.length) return null;

    const questionById = new Map(authorQuestions.map(question => [question.id, question]));

    return hl('div.narrative-evidence-box', [
        hl('h3.narrative-evidence-title', 'Authoring Evidence'),
        probeRequests.length
            ? hl('div.narrative-evidence-group', [
                hl('span.narrative-signal-label', 'Evidence Probes'),
                hl('div.narrative-signal-list', probeRequests.map((probe, idx) => {
                    const primary =
                        (probe.planName || '').trim() ||
                        (probe.questionKind || '').trim() ||
                        (probe.objective || '').trim() ||
                        (probe.purpose || '').trim() ||
                        `probe ${idx + 1}`;
                    const details = [
                        probe.purpose && probe.purpose !== primary ? probe.purpose : '',
                        probe.objective && probe.objective !== primary ? probe.objective : '',
                    ];
                    const detailText = filterPlayerFacingValues(details, supportOpts).join(' · ');
                    return hl('div.narrative-signal-row', { key: `probe-${probe.id}-${idx}` }, [
                        hl('span.narrative-signal-row-label', `${primary}:`),
                        hl('span.narrative-signal-row-value', detailText),
                    ]);
                })),
            ])
            : null,
        authorEvidence.length
            ? hl('div.narrative-evidence-group', authorEvidence.map(summary => {
                const question = questionById.get(summary.questionId);
                const statusKey = (summary.status || 'question_only').trim().toLowerCase();
                const why = (summary.why || question?.why || '').trim();
                const anchors = (question?.anchors || []).filter(Boolean).slice(0, 2);
                const purposes = (summary.purposes || []).filter(Boolean).slice(0, 2);
                const objectives = (summary.probeObjectives || []).filter(Boolean).slice(0, 2);
                const linkedPlans = (summary.linkedPlans || []).filter(Boolean).slice(0, 2);
                const branches = (summary.branches || []).slice(0, 2);
                const meta = filterPlayerFacingValues([
                    linkedPlans.length ? `plans ${linkedPlans.join(', ')}` : '',
                    purposes.length ? `focus ${purposes.join('; ')}` : '',
                    objectives.length ? `objective ${objectives.join('; ')}` : '',
                    anchors.length ? `anchors ${anchors.join(', ')}` : '',
                ], supportOpts);
                return hl('div.narrative-evidence-card', { key: `evidence-${summary.questionId}` }, [
                    hl('div.narrative-evidence-card-head', [
                        hl('strong', humanizeToken(summary.questionKind || question?.kind || 'Authoring')),
                        hl(`span.narrative-evidence-status.narrative-evidence-status--${statusKey}`, formatEvidenceStatus(statusKey)),
                    ]),
                    hl('div.narrative-evidence-question', summary.question),
                    why ? hl('div.narrative-evidence-why', why) : null,
                    meta.length ? hl('div.narrative-evidence-meta', meta.join(' · ')) : null,
                    branches.length
                        ? hl('div.narrative-evidence-branches', branches.map((branch, idx) =>
                            hl('div.narrative-evidence-branch', { key: `${summary.questionId}-branch-${idx}` }, [
                                narrativeEvidenceMoveChip(branch.keyMove, moveRefs),
                                hl(
                                    'span',
                                    filterPlayerFacingValues([branch.line], supportOpts)
                                        .filter(Boolean)
                                        .join(' · '),
                                ),
                            ]),
                        ))
                        : null,
                ]);
            }))
            : null,
        !authorEvidence.length && authorQuestions.length
            ? hl('div.narrative-evidence-group', authorQuestions.map(question => {
                const why = (question.why || '').trim();
                const anchors = (question.anchors || []).filter(Boolean).slice(0, 2);
                const meta = filterPlayerFacingValues([
                    anchors.length ? `anchors ${anchors.join(', ')}` : '',
                ], supportOpts);
                return hl('div.narrative-evidence-card', { key: `question-${question.id}` }, [
                    hl('div.narrative-evidence-card-head', [
                        hl('strong', humanizeToken(question.kind || 'Authoring')),
                        hl('span.narrative-evidence-status.narrative-evidence-status--question_only', 'Heuristic'),
                    ]),
                    hl('div.narrative-evidence-question', question.question),
                    why ? hl('div.narrative-evidence-why', why) : null,
                    meta.length ? hl('div.narrative-evidence-meta', meta.join(' · ')) : null,
                ]);
            }))
            : null,
    ]);
}

function narrativeEvidenceMoveChip(keyMove: string, moveRefs: Map<string, string>): VNode {
    const normalized = normalizeSanToken(keyMove);
    const payload = normalized ? moveRefs.get(normalized) : null;
    if (!payload) return hl('code', keyMove);
    return hl('span.narrative-strategic-chip.narrative-evidence-move', {
        attrs: {
            'data-board': payload,
            title: `Preview ${keyMove}`,
        },
    }, keyMove);
}

function narrativeStrategicNoteView(ctrl: NarrativeCtrl, moment: GameChronicleMoment): VNode {
    const note = cleanStrategicNoteText(moment.activeStrategicNote || '');
    const dossier = moment.activeBranchDossier;
    const strategySurface = narrativeStrategySurface(moment);
    const threadLabel = dossier?.threadLabel || moment.strategicThread?.themeLabel;
    const threadStage = dossier?.threadStage || moment.strategicThread?.stageLabel;
    const strategicMoves = (moment.activeStrategicMoves || []).filter(m => typeof m.uci === 'string' && m.uci.length >= 4);
    const strategicIdeas = [strategySurface.dominantIdea, strategySurface.secondaryIdea].filter(Boolean) as ActiveStrategicIdeaRef[];
    const objectiveTargets = strategySurface.objectiveTargets;
    const executionRoute = strategySurface.executionRoute;

    return hl('div.narrative-strategic-note-box', [
        hl('div.narrative-strategic-note-head', [
            hl('h3.narrative-strategic-note-title', 'Strategic Note'),
            threadLabel ? narrativeBadgeView(threadLabel, 'theme') : null,
            threadStage ? narrativeBadgeView(threadStage, 'stage') : null,
            strategySurface.ownerMismatch && strategySurface.campaignOwner
                ? narrativeBadgeView(`${routeOwnerLabel(strategySurface.campaignOwner)} Campaign`, 'selection')
                : null,
        ]),
        note ? narrativeProseView(ctrl, moment, note, 'note') : null,
        strategicIdeas.length ? narrativeStrategicIdeasSurface(strategicIdeas) : null,
        executionRoute ? narrativeStrategicExecutionSurface(moment, executionRoute) : null,
        objectiveTargets.length
            ? narrativeStrategicObjectiveSurface(objectiveTargets)
            : strategySurface.focus
                ? hl('div.narrative-strategic-surface.narrative-strategic-surface--objective', [
                    hl('div.narrative-strategic-surface__label', 'Objective'),
                    hl('div.narrative-strategic-surface__body', [
                        hl('span.narrative-strategic-chip.narrative-strategic-chip--objective', strategySurface.focus),
                    ]),
                ])
                : null,
        dossier ? narrativeBranchDossierView(moment, strategicMoves) : null,
    ]);
}

function narrativeStrategicIdeasSurface(ideas: ActiveStrategicIdeaRef[]): VNode {
    return hl('div.narrative-strategic-surface.narrative-strategic-surface--idea', [
        hl('div.narrative-strategic-surface__label', 'Idea'),
        hl('div.narrative-strategic-surface__body', ideas.map((idea, idx) =>
            hl('span.narrative-strategic-chip.narrative-strategic-chip--idea', {
                key: idea.ideaId,
                attrs: {
                    title: `${idx === 0 ? 'Dominant' : 'Secondary'} · ${humanizeToken(idea.group)}`,
                },
            }, `${idx === 0 ? 'Dominant' : 'Secondary'}: ${ideaSurfaceText(idea)}`),
        )),
    ]);
}

function narrativeStrategicExecutionSurface(
    moment: GameChronicleMoment,
    route: ActiveStrategicRouteRef | ActiveBranchRouteCue,
): VNode {
    const routeText = route.route.join('-');
    const attrs: Record<string, string> =
        route.surfaceMode === 'exact'
            ? {
                'data-route': routeText,
                'data-route-fen': moment.fen,
                title: route.purpose,
            }
            : { title: route.purpose };
    return hl('div.narrative-strategic-surface.narrative-strategic-surface--execution', [
        hl('div.narrative-strategic-surface__label', 'Execution'),
        hl('div.narrative-strategic-surface__body', [
            hl(`span.narrative-strategic-chip${route.surfaceMode === 'exact' ? '.route' : ''}.narrative-strategic-chip--execution`, {
                attrs,
            }, routeSurfaceText(route)),
        ]),
    ]);
}

function narrativeStrategicObjectiveSurface(targets: StrategyDirectionalTarget[]): VNode {
    return hl('div.narrative-strategic-surface.narrative-strategic-surface--objective', [
        hl('div.narrative-strategic-surface__label', 'Objective'),
        hl('div.narrative-strategic-surface__body', targets.map(target =>
            hl('span.narrative-strategic-chip.narrative-strategic-chip--objective', {
                key: target.targetId,
                attrs: {
                    title: [
                        ...((target.strategicReasons || []).slice(0, 2)),
                        ...((target.prerequisites || []).slice(0, 2)),
                    ].join(' · '),
                },
            }, `${objectiveSurfaceText(target)} · ${humanizeToken(target.readiness)}`),
        )),
    ]);
}

function narrativeBranchDossierView(
    moment: GameChronicleMoment,
    strategicMoves: ActiveStrategicMoveRef[],
): VNode {
    const dossier = moment.activeBranchDossier!;
    const moveCue = dossier.moveCue;
    const matchedMove =
        moveCue &&
        strategicMoves.find(move =>
            move.label === moveCue.label ||
            move.uci.toLowerCase() === moveCue.uci.toLowerCase(),
        );
    const routeCue = dossier.routeCue;
    const detailRows: Array<[string, string | VNode]> = [
        dossier.whyDeferred ? ['Why deferred', dossier.whyDeferred] : null,
        dossier.opponentResource ? ['Opponent resource', dossier.opponentResource] : null,
        dossier.threadSummary ? ['Thread', dossier.threadSummary] : null,
        dossier.threadOpponentCounterplan ? ['Thread counterplan', dossier.threadOpponentCounterplan] : null,
        dossier.evidenceCue ? ['Evidence', dossier.evidenceCue] : null,
        dossier.continuationFocus ? ['Continuation', dossier.continuationFocus] : null,
        dossier.practicalRisk ? ['Practical risk', dossier.practicalRisk] : null,
    ].filter(Boolean) as Array<[string, string | VNode]>;

    const summaryRows: VNode[] = [
        hl('div.narrative-branch-dossier__row', [
            hl('span.narrative-branch-dossier__label', 'Chosen'),
            hl('span.narrative-branch-dossier__value', dossier.chosenBranchLabel),
        ]),
        dossier.engineBranchLabel
            ? hl('div.narrative-branch-dossier__row', [
                hl('span.narrative-branch-dossier__label', 'Engine'),
                hl('span.narrative-branch-dossier__value', dossier.engineBranchLabel),
            ])
            : null,
        dossier.deferredBranchLabel
            ? hl('div.narrative-branch-dossier__row', [
                hl('span.narrative-branch-dossier__label', 'Deferred'),
                hl('span.narrative-branch-dossier__value', dossier.deferredBranchLabel),
            ])
            : null,
        dossier.threadLabel
            ? hl('div.narrative-branch-dossier__row', [
                hl('span.narrative-branch-dossier__label', 'Thread'),
                hl('span.narrative-branch-dossier__value', dossier.threadStage ? `${dossier.threadLabel} (${dossier.threadStage})` : dossier.threadLabel),
            ])
            : null,
        routeCue
            ? hl('div.narrative-branch-dossier__row', [
                hl('span.narrative-branch-dossier__label', 'Route'),
                narrativeBranchRouteChip(moment, routeCue),
            ])
            : null,
        moveCue
            ? hl('div.narrative-branch-dossier__row', [
                hl('span.narrative-branch-dossier__label', 'Move ref'),
                narrativeBranchMoveChip(moveCue, matchedMove),
            ])
            : null,
    ].filter(Boolean) as VNode[];

    const summary = [
        hl('span.narrative-branch-dossier__title', 'Branch Dossier'),
        dossier.comparisonGapCp !== undefined && dossier.comparisonGapCp !== null
            ? hl('span.narrative-branch-dossier__gap', `${dossier.comparisonGapCp}cp`)
            : null,
    ];

    const body = hl('div.narrative-branch-dossier__body', [
        ...summaryRows,
        detailRows.length
            ? hl('div.narrative-branch-dossier__details', detailRows.map(([label, value]) =>
                hl('div.narrative-branch-dossier__detail', { key: label }, [
                    hl('span.narrative-branch-dossier__detail-label', `${label}:`),
                    typeof value === 'string'
                        ? hl('span.narrative-branch-dossier__detail-value', value)
                        : value,
                ]),
            ))
            : null,
    ]);

    return detailRows.length
        ? hl('details.narrative-branch-dossier', [
            hl('summary.narrative-branch-dossier__summary', summary),
            body,
        ])
        : hl('div.narrative-branch-dossier', [
            hl('div.narrative-branch-dossier__summary', summary),
            body,
        ]);
}

function narrativeBranchRouteChip(moment: GameChronicleMoment, cue: ActiveBranchRouteCue): VNode {
    const routeText = cue.route.join('-');
    const text = `${routeOwnerLabel(cue.ownerSide)} ${routeSurfaceText(cue)}`;
    const attrs: Record<string, string> =
        cue.surfaceMode === 'exact'
            ? {
                  'data-route': routeText,
                  'data-route-fen': moment.fen,
                  title: `${cue.routeId} · ${cue.purpose}`,
              }
            : { title: `${cue.routeId} · ${cue.purpose}` };
    return hl(`span.narrative-strategic-chip${cue.surfaceMode === 'exact' ? '.route' : ''}.narrative-branch-dossier__chip`, {
        attrs,
    }, text);
}

function narrativeBranchMoveChip(
    cue: ActiveBranchMoveCue,
    matchedMove?: ActiveStrategicMoveRef,
): VNode {
    const uci = matchedMove?.uci || cue.uci;
    const san = matchedMove?.san || cue.san || cue.uci;
    const fenAfter = matchedMove?.fenAfter || '';
    const preview = fenAfter && uci.length >= 4 ? `${fenAfter}|${uci.toLowerCase()}` : null;
    return hl('span.narrative-strategic-chip.narrative-branch-dossier__chip', {
        attrs: preview ? { 'data-board': preview, title: cue.label } : { title: cue.label },
    }, `${cue.label}: ${san}`);
}

function narrativeProseView(
    ctrl: NarrativeCtrl,
    moment: GameChronicleMoment,
    prose: string,
    scope: 'moment' | 'note',
): VNode {
    const moveRefs = buildInlineMoveRefMap(moment, ctrl.root.data.game.variant.key);
    const chunks = prose.split(/(\s+)/);
    const nodes: Array<VNode | string> = [];

    chunks.forEach((chunk, idx) => {
        if (!chunk || /^\s+$/.test(chunk)) {
            nodes.push(chunk);
            return;
        }
        const rendered = renderInlineToken(chunk, idx, moment, moveRefs, scope);
        if (rendered.length) nodes.push(...rendered);
        else nodes.push(chunk);
    });

    return hl('pre.narrative-prose', nodes);
}

function buildInlineMoveRefMap(moment: GameChronicleMoment, variantKey: string): Map<string, string> {
    const refs = new Map<string, string>();

    const addRef = (sanRaw: string | undefined, boardPayload: string | null): void => {
        const san = normalizeSanToken(sanRaw);
        if (!san || !boardPayload || refs.has(san)) return;
        refs.set(san, boardPayload);
    };

    (moment.activeStrategicMoves || []).forEach(move => {
        const uci = typeof move.uci === 'string' ? move.uci.trim().toLowerCase() : '';
        const fenAfter = typeof move.fenAfter === 'string' ? move.fenAfter.trim() : '';
        if (!uci || !fenAfter) return;
        addRef(move.san, `${fenAfter}|${uci}`);
    });

    const addFromVariation = (moves: string[]): void => {
        const setup = parseFen(moment.fen);
        if (!setup.isOk) return;
        const pos = setupPosition(lichessRules(variantKey as any), setup.value);
        if (!pos.isOk) return;
        for (const rawUci of moves.slice(0, 20)) {
            const uci = (rawUci || '').trim().toLowerCase();
            const parsed = parseUci(uci);
            if (!parsed) break;
            const san = makeSanAndPlay(pos.value, parsed);
            const afterFen = makeBoardFen(pos.value.board);
            if (san === '--') break;
            addRef(san, `${afterFen}|${uci}`);
        }
    };

    const topLines = (moment.variations || [])
        .filter(v => Array.isArray(v.moves) && v.moves.length)
        .slice(0, 2);
    topLines.forEach(v => addFromVariation(v.moves));

    return refs;
}

function normalizeSanToken(raw: string | undefined): string {
    return (raw || '')
        .trim()
        .replace(/^[\(\[\{'"“”‘’]+/, '')
        .replace(/[\)\]\}'"“”‘’]+$/, '')
        .replace(/[!?]+$/g, '')
        .trim();
}

function renderInlineToken(
    token: string,
    idx: number,
    moment: GameChronicleMoment,
    moveRefs: Map<string, string>,
    scope: 'moment' | 'note',
): Array<VNode | string> {
    const leading = token.match(/^[^A-Za-z0-9O]+/)?.[0] || '';
    const trailing = token.match(/[^A-Za-z0-9O#+=\-!?]+$/)?.[0] || '';
    let core = token.slice(leading.length, token.length - trailing.length);

    const nodes: Array<VNode | string> = [];
    if (leading) nodes.push(leading);
    if (!core) {
        if (trailing) nodes.push(trailing);
        return nodes;
    }

    const movePrefix = core.match(/^(\d+\.(?:\.\.)?)(.+)$/);
    if (movePrefix) {
        nodes.push(movePrefix[1]);
        core = movePrefix[2];
    }

    const routeMatch = core.match(/^([KQRBN]?)([a-h][1-8](?:-[a-h][1-8]){1,6})([+#!?]*)$/);
    if (routeMatch) {
        const routeSquares = routeMatch[2].split('-').map(s => s.toLowerCase());
        nodes.push(
            hl('span.narrative-strategic-chip.route.narrative-inline-route', {
                key: `${scope}-route-${idx}-${routeMatch[2]}`,
                attrs: {
                    'data-route': routeSquares.join('-'),
                    'data-route-fen': moment.fen,
                    title: 'Route preview',
                },
            }, core),
        );
        if (trailing) nodes.push(trailing);
        return nodes;
    }

    const normalizedCore = normalizeSanToken(core);
    const boardPayload = moveRefs.get(normalizedCore);
    if (boardPayload) {
        nodes.push(
            hl('span.narrative-move', {
                key: `${scope}-move-${idx}-${normalizedCore}`,
                attrs: { 'data-board': boardPayload },
            }, core),
        );
    } else {
        nodes.push(core);
    }

    if (trailing) nodes.push(trailing);
    return nodes;
}

function narrativeActivePlanView(plan: ActivePlanRef): VNode {
    return hl('div.narrative-active-plan-box', [
        hl('div.narrative-active-plan-theme', [
            hl('span.narrative-plan-label', 'Active Plan:'),
            hl('span.narrative-plan-theme-text', plan.themeL1),
        ]),
        plan.subplanId || plan.phase ? hl('div.narrative-active-plan-details', [
            plan.subplanId ? hl('span.narrative-plan-detail', plan.subplanId) : null,
            plan.phase ? hl('span.narrative-plan-detail.phase', plan.phase) : null,
        ]) : null,
    ]);
}

export function narrativeCollapseCardView(
    ctrl: NarrativeCtrl,
    moment: GameChronicleMoment,
    opts: { selected?: boolean; onSelect?: () => void; anchorId?: string } = {},
): VNode | null {
    const collapse = moment.collapse;
    if (!collapse) return null;

    return hl('div.narrative-collapse-card', {
        attrs: { 'data-collapse-id': collapse.interval },
        class: { active: !!opts.selected },
        hook: opts.onSelect
            ? bind('click', e => {
                if (!shouldIgnoreReviewCardClick(e.target)) opts.onSelect?.();
            })
            : undefined,
    }, [
        hl('h3.narrative-collapse-title', [
            hl('span.icon', { attrs: { ...dataIcon(licon.Target) } }),
            ' Causal Collapse Analyzer'
        ]),
        hl('div.narrative-collapse-body', [
            hl('div.narrative-collapse-row', [
                hl('span.narrative-collapse-label', 'Collapse Interval:'),
                hl('span.narrative-collapse-value', collapseIntervalLabel(collapse.interval))
            ]),
            hl('div.narrative-collapse-row', [
                hl('span.narrative-collapse-label', 'Root Cause:'),
                hl('span.narrative-collapse-value.cause', collapse.rootCause)
            ]),
            hl('div.narrative-collapse-row', [
                hl('span.narrative-collapse-label', 'Earliest Preventable:'),
                hl('button.button.button-empty.narrative-jump', {
                    hook: bind('click', () => {
                        opts.onSelect?.();
                        jumpToNarrativePly(ctrl, collapse.earliestPreventablePly, opts.anchorId);
                    })
                }, moveLabel(collapse.earliestPreventablePly))
            ]),
            hl('div.narrative-collapse-row', [
                hl('span.narrative-collapse-label', 'Recoverability Window:'),
                hl('span.narrative-collapse-value', `${collapse.recoverabilityPlies} steps`)
            ]),
            patchReplayPanel(ctrl, moment),
        ])
    ]);
}

function patchReplayPanel(ctrl: NarrativeCtrl, moment: GameChronicleMoment): VNode {
    const collapse = moment.collapse!;
    const collapseId = collapse.interval;
    const patchMoves = collapse.patchLineUci || [];
    const replayState = ctrl.patchReplay();
    const isActive = replayState?.collapseId === collapseId;

    if (!patchMoves.length) {
        return hl('div.narrative-collapse-row', [
            hl('span.narrative-collapse-label', 'Patch Line:'),
            hl('span.narrative-collapse-value', 'N/A'),
        ]);
    }

    if (!isActive) {
        return hl('div.patch-replay-closed', [
            hl('span.narrative-collapse-label', 'Patch Line:'),
            hl('button.button.button-empty.patch-replay-open-btn', {
                hook: bind('click', () => ctrl.patchOpen(collapseId))
            }, `▶ Replay ${patchMoves.length} steps`),
        ]);
    }

    // Get FEN at the earliest preventable ply from mainline
    const ply = collapse.earliestPreventablePly;
    const mainline = ctrl.root.mainline;
    const node = mainline.find(n => n.ply === ply);
    const fen = node?.fen || 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';

    // Determine which moves to show: original (game continuation) or patch
    const mode = replayState!.mode;
    let movesToShow: string[];
    if (mode === 'patch') {
        movesToShow = patchMoves;
    } else {
        // Original = game moves from this ply onward
        const startIdx = mainline.findIndex(n => n.ply === ply);
        movesToShow = startIdx >= 0
            ? mainline.slice(startIdx + 1, startIdx + 1 + patchMoves.length)
                .map(n => n.uci || '')
                .filter(u => u.length > 0)
            : [];
    }

    const totalSteps = movesToShow.length;
    const step = replayState!.step;

    return hl('div.patch-replay-panel', {
        hook: onInsert((el: HTMLElement) => bindPreviewHover(ctrl, el)),
    }, [
        hl('div.patch-replay-header', [
            hl('span.patch-replay-title', mode === 'patch' ? '✨ Improved Line' : '📋 Original Line'),
            hl('button.button.button-empty.patch-replay-close', {
                hook: bind('click', () => ctrl.patchClose())
            }, '✕'),
        ]),
        hl('div.patch-replay-moves', renderMoves(ctrl, fen, step > 0 ? movesToShow.slice(0, step) : [])),
        hl('div.patch-replay-controls', [
            hl('button.button.button-empty.patch-ctrl-btn', {
                attrs: { disabled: step === 0 },
                hook: bind('click', () => ctrl.patchStep(-step, totalSteps))
            }, '⏮'),
            hl('button.button.button-empty.patch-ctrl-btn', {
                attrs: { disabled: step === 0 },
                hook: bind('click', () => ctrl.patchStep(-1, totalSteps))
            }, '◀'),
            hl('span.patch-replay-step', `${step}/${totalSteps}`),
            hl('button.button.button-empty.patch-ctrl-btn', {
                attrs: { disabled: step >= totalSteps },
                hook: bind('click', () => ctrl.patchStep(1, totalSteps))
            }, '▶'),
            hl('button.button.button-empty.patch-ctrl-btn', {
                attrs: { disabled: step >= totalSteps },
                hook: bind('click', () => ctrl.patchStep(totalSteps - step, totalSteps))
            }, '⏭'),
        ]),
        hl('div.patch-replay-toggle', [
            hl('button.button.button-empty.patch-toggle-btn' + (mode === 'patch' ? '.active' : ''), {
                hook: bind('click', () => { if (mode !== 'patch') ctrl.patchToggle(); })
            }, 'Improved'),
            hl('button.button.button-empty.patch-toggle-btn' + (mode === 'original' ? '.active' : ''), {
                hook: bind('click', () => { if (mode !== 'original') ctrl.patchToggle(); })
            }, 'Original'),
        ]),
        hl('button.button.button-empty.patch-jump-btn', {
            hook: bind('click', () => {
                jumpToNarrativePly(ctrl, ply);
            })
        }, `↗ Jump to ${moveLabel(ply)}`),
    ]);
}

function narrativeThreadSummaryView(thread: ActiveStrategicThread): VNode {
    const meta = [
        thread.opponentCounterplan ? `Counterplan: ${thread.opponentCounterplan}` : '',
        Number.isFinite(thread.continuityScore) ? `Continuity ${Math.round(thread.continuityScore * 100)}%` : '',
    ].filter(Boolean).join(' · ');
    return hl('div.narrative-thread-summary', [
        hl('span.narrative-thread-summary__eyebrow', 'Campaign Thread'),
        hl('div.narrative-thread-summary__body', [
            hl('strong.narrative-thread-summary__title', thread.themeLabel),
            hl('span.narrative-thread-summary__copy', thread.summary),
            meta ? hl('span.narrative-thread-summary__meta', meta) : null,
        ]),
    ]);
}

function narrativeBadgeView(text: string, kind: 'classification' | 'type' | 'salience' | 'branch' | 'theme' | 'stage' | 'selection'): VNode {
    const label = cleanNarrativeSurfaceLabel(text);
    const cls = label.toLowerCase().replace(/\s+/g, '-');
    return hl(`span.narrative-badge.${kind}.${cls}`, label);
}

function narrativeVariationView(ctrl: NarrativeCtrl, fen: string, line: VariationLine, index: number): VNode {
    const label = String.fromCharCode('A'.charCodeAt(0) + (index % 26));
    const score = typeof line.mate === 'number' ? `#${line.mate}` : renderEval(line.scoreCp);
    const tags = Array.isArray(line.tags) && line.tags.length ? line.tags : null;
    const moveLine = renderMovesSurface(ctrl, fen, line.moves);
    const preview = moveLine.preview;

    return hl('div.narrative-variation', preview ? { attrs: { 'data-board': preview.board } } : undefined, [
        preview
            ? hl('div.narrative-variation-board-wrap', [
                hl('div.narrative-variation-board-copy', [
                    hl('span.narrative-variation-board-label', `PV ${label}`),
                    hl('span.narrative-variation-board-meta', `${preview.plies}-step line`),
                ]),
                hl('div.narrative-variation-board-shell', {
                    attrs: {
                        'data-board': preview.board,
                        tabindex: '0',
                        role: 'button',
                        'aria-label': `Preview variation ${label}`,
                        title: `Preview variation ${label}`,
                    },
                }, [
                    hl('div.mini-board.mini-board--init.narrative-variation-board', {
                        attrs: {
                            'data-state': preview.state,
                            'data-board': preview.board,
                        },
                    }),
                ]),
            ])
            : null,
        hl('div.narrative-variation-main', [
            hl('div.narrative-variation-meta', [
                hl('span.narrative-variation-label', label),
                hl('span.narrative-variation-score', score),
                tags ? hl('span.narrative-variation-tags', tags.join(', ')) : null,
            ]),
            hl('div.narrative-variation-moves', moveLine.nodes),
        ]),
    ]);
}

function renderMoves(ctrl: NarrativeCtrl, fen: string, moves: string[]): Array<VNode | string> {
    return renderMovesSurface(ctrl, fen, moves).nodes;
}

function renderMovesSurface(ctrl: NarrativeCtrl, fen: string, moves: string[]): RenderedMoveLine {
    const setup = parseFen(fen);
    if (!setup.isOk) return { nodes: ['(invalid FEN)'] };

    const pos = setupPosition(lichessRules(ctrl.root.data.game.variant.key), setup.value);
    if (!pos.isOk) return { nodes: ['(invalid position)'] };

    const vnodes: Array<VNode | string> = [];
    let key = makeBoardFen(pos.value.board);
    let lastBoard: string | undefined;
    let lastUci: string | undefined;

    for (let i = 0; i < moves.length; i++) {
        let text: string | undefined;
        if (pos.value.turn === 'white') text = `${pos.value.fullmoves}.`;
        else if (i === 0) text = `${pos.value.fullmoves}...`;
        if (text) vnodes.push(hl('span.narrative-move-number', { key: `${key}|${text}` }, text));

        const uci = moves[i];
        const parsed = parseUci(uci);
        if (!parsed) break;

        const san = makeSanAndPlay(pos.value, parsed);
        const afterFen = makeBoardFen(pos.value.board);
        if (san === '--') break;
        key += '|' + uci;
        lastBoard = afterFen;
        lastUci = uci;

        vnodes.push(
            hl('span.narrative-move', { key, attrs: { 'data-board': `${afterFen}|${uci}` } }, san),
        );
    }

    return {
        nodes: vnodes,
        preview:
            lastBoard && lastUci
                ? {
                    board: `${lastBoard}|${lastUci}`,
                    state: `${lastBoard},${ctrl.root.getOrientation()},${lastUci}`,
                    plies: moves.length,
                }
                : undefined,
    };
}

function routePreviewFromDataset(el: HTMLElement): BoardPreview | null {
    const fen = el.dataset.routeFen;
    const squares = routeSquaresFromDataset(el);
    if (!fen || !squares) return null;
    const shapes = routeShapesFromSquares(squares);
    const fallbackUci = `${squares[0]}${squares[1]}`;
    return { fen, uci: fallbackUci, shapes } as BoardPreview;
}

function routeSquaresFromDataset(el: HTMLElement): string[] | null {
    const routeRaw = el.dataset.route;
    if (!routeRaw) return null;
    const squares = routeRaw
        .split('-')
        .map(s => s.trim().toLowerCase())
        .filter(s => /^[a-h][1-8]$/.test(s));
    if (squares.length < 2) return null;
    return squares;
}

function routeShapesFromSquares(squares: string[]) {
    const shapes: DrawShape[] = [];
    for (let i = 0; i < squares.length - 1; i++) {
        shapes.push({
            orig: squares[i] as any,
            dest: squares[i + 1] as any,
            brush: 'paleBlue',
            modifiers: i === squares.length - 2 ? { hilite: 'white' } : undefined,
        });
    }
    return shapes;
}

export function bindPreviewHover(ctrl: NarrativeCtrl, root: HTMLElement): void {
    const anyRoot: any = root;
    if (anyRoot._chesstoryNarrativeBound) return;
    anyRoot._chesstoryNarrativeBound = true;

    const updatePreview = (target: HTMLElement | null) => {
        const routeEl = target?.closest?.('[data-route][data-route-fen]') as HTMLElement | null;
        if (routeEl) {
            const routePreview = routePreviewFromDataset(routeEl);
            if (routePreview) {
                const routeSquares = routeSquaresFromDataset(routeEl);
                ctrl.pvBoard(routePreview);
                ctrl.root.setNarrativeRouteOverlay({
                    fen: routePreview.fen,
                    shapes: routeSquares ? routeShapesFromSquares(routeSquares) : [],
                });
                ctrl.root.redraw();
                return true;
            }
        }

        const el = target?.closest?.('[data-board]') as HTMLElement | null;
        const board = el?.dataset?.board;
        if (!board || !board.includes('|')) return false;
        const [fen, uci] = board.split('|');
        if (!fen || !uci) return false;
        ctrl.root.setNarrativeRouteOverlay(null);
        ctrl.pvBoard({ fen, uci });
        ctrl.root.redraw();
        return true;
    };

    const clearPreview = () => {
        ctrl.root.setNarrativeRouteOverlay(null);
        ctrl.pvBoard(null);
        ctrl.root.redraw();
    };

    root.addEventListener('mouseover', (e: MouseEvent) => {
        updatePreview(e.target as HTMLElement | null);
    });

    root.addEventListener('focusin', (e: FocusEvent) => {
        updatePreview(e.target as HTMLElement | null);
    });

    root.addEventListener('focusout', () => {
        setTimeout(() => {
            if (root.contains(document.activeElement)) return;
            clearPreview();
        }, 0);
    });

    root.addEventListener('mouseleave', () => {
        clearPreview();
    });
}
