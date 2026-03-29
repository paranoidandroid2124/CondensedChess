import { storedBooleanPropWithEffect } from 'lib/storage';
import type AnalyseCtrl from './ctrl';
import { treePath } from 'lib/tree';
import { fetchOpeningReferenceViaProxy } from './bookmaker/openingProxy';
import { initBookmakerHandlers, setBookmakerRefs } from './bookmaker/interactionHandlers';
import { createProbeOrchestrator } from './bookmaker/probeOrchestrator';
import { clearBookmakerPanel, renderBookmakerPanel, restoreBookmakerPanel, syncBookmakerEvalDisplay } from './bookmaker/rendering';
import { buildBookmakerRequest, deriveAfterVariations, toBaselineCp, toEvalData } from './bookmaker/requestPayload';
import { blockedHtmlFromErrorResponse, bookmakerIdleHtml, bookmakerRetryHtml, bookmakerTooEarlyHtml } from './bookmaker/blockingState';
import {
    buildStoredBookmakerEntry,
    listStudyBookmakerSnapshots,
    persistSessionBookmakerSnapshot,
    persistStudyBookmakerSnapshot,
    readSessionBookmakerSnapshot,
    readStudyBookmakerSnapshot,
    type StoredBookmakerEntry,
    type StudyBookmakerRef,
} from './bookmaker/studyPersistence';
import { flushBookmakerStudySyncQueue, rememberBookmakerStudySync } from './bookmaker/studySyncQueue';
import { buildDecisionComparisonSurface } from './decisionComparison';
import {
    buildPlayerFacingSupportOptions,
    filterPlayerFacingValues,
    formatDeploymentSummary,
    formatEvidenceStatus,
    humanizeToken,
    rewritePlayerFacingSupportText,
} from './chesstory/signalFormatting';
import {
    decodeBookmakerResponse,
    type DecodedBookmakerResponse,
    type BookmakerStrategicLedgerV1,
    type BookmakerRefsV1,
    type NarrativeSignalDigest,
    type PolishMetaV1,
    type StrategyPackV1,
    variationLinesFromResponse,
} from './bookmaker/responsePayload';
import {
    bookmakerLedgerRootAttrs,
    renderBookmakerLedgerProbeRows,
} from './bookmaker/ledgerSurface';
import { formatStrategicPlanText, strategicPlanExperimentIndex } from './bookmaker/planSupportSurface';
import { escapeHtml, normalizeSanToken, renderInteractiveSanChip } from './bookmaker/surfaceShared';
import { restoreStoredBookmakerTokens } from './bookmaker/stateContinuity';
import { buildCompactSupportSurface } from './chesstory/compactSupportSurface';
import type {
    AuthorEvidenceSummary,
    AuthorQuestionSummary,
    EndgameStateToken,
    PlanHypothesis,
    PlanStateToken,
    ProbeRequest,
    StrategicPlanExperiment,
} from './bookmaker/types';

export type BookmakerNarrative = (nodes: Tree.Node[]) => void;

type TriggerBookmakerRequest = (opts?: { force?: boolean }) => void;

type BookmakerCacheEntry = StoredBookmakerEntry;

let requestsBlocked = false;
let blockedHtml: string | null = null;
let lastRequestedFen: string | null = null;
let lastShownHtml = '';
let activeProbeSession = 0;
let bookmakerRequestTrigger: TriggerBookmakerRequest | null = null;
const MIN_BOOKMAKER_PLY = 5;

type LoadingStage = 'position' | 'lines' | 'compose' | 'polish';

const loadingStageOrder: Record<LoadingStage, number> = {
    position: 1,
    lines: 2,
    compose: 3,
    polish: 4,
};

const loadingStageTitle: Record<LoadingStage, string> = {
    position: 'Position analysis',
    lines: 'Line analysis',
    compose: 'Draft generation',
    polish: 'Language polish',
};

const loadingStageMessages: Record<LoadingStage, string[]> = {
    position: [
        'Reading the position...',
        'Checking king safety and piece activity...',
        'Dusting off the board for a clean read...',
    ],
    lines: [
        'Calculating principal variations...',
        'Comparing tactical and positional routes...',
        'Following forcing lines one by one...',
    ],
    compose: [
        'Building a concise explanation...',
        'Aligning plans with the current structure...',
        'Turning engine signals into human guidance...',
    ],
    polish: [
        'Polishing the final commentary...',
        'Ensuring move order and notation stay exact...',
        'Preparing a clean final render...',
    ],
};

function renderLoadingHud(stage: LoadingStage, message: string, streamPreview?: string): string {
    const step = loadingStageOrder[stage];
    const title = loadingStageTitle[stage];
    const isReveal = typeof streamPreview === 'string';
    const safeMessage = escapeHtml(message);
    const safeStream = isReveal ? escapeHtml(streamPreview || '') : '';

    return `
      <div class="bookmaker-thinking-hud glass${isReveal ? ' bookmaker-thinking-hud--reveal' : ''}">
        <div class="hud-aura"></div>
        <div class="hud-content">
          <span class="hud-stage">Step ${step}/4 · ${escapeHtml(title)}</span>
          <i data-icon="L" class="hud-icon pulse"></i>
          <span class="hud-text">${safeMessage}</span>
          ${isReveal ? `<span class="hud-stream">${safeStream}<span class="hud-caret">|</span></span>` : ''}
        </div>
        <div class="hud-shimmer"></div>
      </div>
    `;
}

function splitWords(text: string): string[] {
    return text
        .split(/\s+/)
        .map(t => t.trim())
        .filter(Boolean);
}

function currentStudyBookmakerRef(ctrl?: AnalyseCtrl): StudyBookmakerRef | null {
    const study = ctrl?.opts?.study as { id?: string; chapterId?: string } | undefined;
    if (!study?.id || !study?.chapterId) return null;
    return { studyId: study.id, chapterId: study.chapterId };
}

function currentBookmakerSessionScope(): string {
    return `${location.pathname}${location.search}`;
}

function findSavedStudyAiComment(node: Tree.Node | undefined): string | null {
    if (!node?.comments?.length) return null;
    const comment = node.comments.find(entry => {
        const author = entry?.by;
        return !!author && typeof author === 'object' && 'name' in author && (author as { name?: string }).name === 'Chesstory AI';
    });
    const text = typeof comment?.text === 'string' ? comment.text.trim() : '';
    return text || null;
}

function markerForPly(ply: number): string {
    const moveNo = Math.floor((ply + 1) / 2);
    return ply % 2 === 1 ? `${moveNo}.` : `${moveNo}...`;
}

function buildSavedStudyRefs(ctrl: AnalyseCtrl | undefined, originPath: string, commentPath: string): BookmakerRefsV1 | null {
    if (!ctrl) return null;
    const originNode = ctrl.tree.nodeAtPath(originPath);
    if (!originNode) return null;
    const relative = commentPath.slice(originPath.length);
    const chosenChildId = relative ? treePath.head(relative) : null;
    const candidateChildren = originNode.children
        .filter(child => !child.comp)
        .filter(child => !chosenChildId || child.id !== chosenChildId)
        .slice(0, 3);
    if (!candidateChildren.length) return null;

    const variations = candidateChildren
        .map((child, lineIdx) => {
            const moves: BookmakerRefsV1['variations'][number]['moves'] = [];
            let current: Tree.Node | undefined = child;
            while (current && moves.length < 12) {
                const san = typeof current.san === 'string' ? current.san.trim() : '';
                const uci = typeof current.uci === 'string' ? current.uci.trim() : '';
                const fenAfter = typeof current.fen === 'string' ? current.fen : '';
                if (!san || !uci || !fenAfter) break;
                moves.push({
                    refId: `study:${commentPath}:${lineIdx}:${moves.length}`,
                    san,
                    uci,
                    fenAfter,
                    ply: current.ply,
                    moveNo: Math.floor((current.ply + 1) / 2),
                    marker: markerForPly(current.ply),
                });
                current = current.children.find(next => !next.comp && !next.forceVariation);
            }
            return moves.length
                ? {
                      lineId: `study:${commentPath}:${lineIdx}`,
                      scoreCp: 0,
                      mate: null,
                      depth: 0,
                      moves,
                  }
                : null;
        })
        .filter(Boolean) as BookmakerRefsV1['variations'];

    if (!variations.length) return null;
    return {
        schema: 'chesstory.refs.v1',
        startFen: originNode.fen,
        startPly: originNode.ply + 1,
        variations,
    };
}

function renderSavedStudyFallbackHtml(commentary: string, refs: BookmakerRefsV1 | null): string {
    const paragraphs = commentary
        .replace(/\r\n/g, '\n')
        .split(/\n\n+/)
        .map(chunk => chunk.trim())
        .filter(Boolean)
        .map(chunk => `<p>${escapeHtml(chunk).replace(/\n/g, '<br/>')}</p>`)
        .join('');

    const alternatives =
        refs && refs.variations.length
            ? `
      <div class="alternatives">
        <h3>Saved Alternatives</h3>
        ${refs.variations
            .map(variation => {
                const moves = variation.moves
                    .map(
                        move => `
              <span class="pv-move-no">${escapeHtml(move.marker || markerForPly(move.ply))}</span>
              <span
                class="pv-san move-chip move-chip--interactive"
                data-ref-id="${escapeHtml(move.refId)}"
                data-uci="${escapeHtml(move.uci)}"
                data-board="${escapeHtml(`${move.fenAfter}|${move.uci}`)}"
                tabindex="0"
                role="button"
                aria-label="Preview move ${escapeHtml(move.san)}"
              >${escapeHtml(move.san)}</span>
            `,
                    )
                    .join(' ');
                return `
          <div class="variation-item variation-item--saved">
            <div class="pv-line">${moves}</div>
          </div>
        `;
            })
            .join('')}
      </div>
    `
            : '';

    return `
      <div class="bookmaker-content bookmaker-content--saved">
        <div class="bookmaker-toolbar">
          <span class="bookmaker-saved-pill">Saved in study</span>
        </div>
        <div class="bookmaker-pv-preview"></div>
        <div class="commentary">${paragraphs}</div>
        ${alternatives}
      </div>
    `;
}

function studyNodeLabel(ctrl: AnalyseCtrl | undefined, path: string): string {
    if (!ctrl) return path || 'Root';
    const node = ctrl.tree.nodeAtPath(path);
    if (!node || !path) return 'Initial position';
    const san = typeof node.san === 'string' ? node.san.trim() : '';
    if (!san) return 'Initial position';
    return `${markerForPly(node.ply)} ${san}`;
}

function summarizeCommentary(text: string | null | undefined): string {
    const normalized = (text || '').replace(/\s+/g, ' ').trim();
    if (!normalized) return 'Saved commentary';
    if (normalized.length <= 140) return normalized;
    return `${normalized.slice(0, 137).trimEnd()}...`;
}

function renderStudyReadingSurface(ctrl: AnalyseCtrl | undefined, ref: StudyBookmakerRef, currentPath: string): string | null {
    const snapshots = listStudyBookmakerSnapshots(ref)
        .filter(snapshot => snapshot.commentPath !== currentPath)
        .slice(0, 8);
    if (!snapshots.length) return null;

    const items = snapshots
        .map(snapshot => {
            const label = studyNodeLabel(ctrl, snapshot.commentPath);
            const excerpt = summarizeCommentary(snapshot.commentary);
            return `
              <button
                type="button"
                class="bookmaker-study-reading__item"
                data-bookmaker-study-path="${escapeHtml(snapshot.commentPath)}"
              >
                <span class="bookmaker-study-reading__label">${escapeHtml(label)}</span>
                <span class="bookmaker-study-reading__excerpt">${escapeHtml(excerpt)}</span>
              </button>
            `;
        })
        .join('');

    return `
      <div class="bookmaker-study-reading">
        <div class="bookmaker-study-reading__title">Saved study commentary</div>
        <div class="bookmaker-study-reading__list">${items}</div>
      </div>
    `;
}

type BookmakerMoveRef = {
    refId: string;
    san: string;
    uci: string;
    fenAfter: string;
};

type BookmakerRefIndex = {
    firstBySan: Map<string, BookmakerMoveRef>;
    anyBySan: Map<string, BookmakerMoveRef>;
};

function buildBookmakerRefIndex(refs: BookmakerRefsV1 | null): BookmakerRefIndex {
    const firstBySan = new Map<string, BookmakerMoveRef>();
    const anyBySan = new Map<string, BookmakerMoveRef>();
    if (!refs) return { firstBySan, anyBySan };

    refs.variations.forEach(variation => {
        variation.moves.forEach((move, idx) => {
            const normalized = normalizeSanToken(move.san);
            if (!normalized) return;
            const ref: BookmakerMoveRef = {
                refId: move.refId,
                san: move.san,
                uci: move.uci,
                fenAfter: move.fenAfter,
            };
            if (idx === 0 && !firstBySan.has(normalized)) firstBySan.set(normalized, ref);
            if (!anyBySan.has(normalized)) anyBySan.set(normalized, ref);
        });
    });

    return { firstBySan, anyBySan };
}

function renderBookmakerMoveChip(
    label: string,
    move: string | null | undefined,
    refIndex: Map<string, BookmakerMoveRef>,
    tone: 'chosen' | 'engine' | 'deferred',
): string | null {
    const normalized = normalizeSanToken(move);
    const raw = move?.trim() || normalized;
    if (!normalized || !raw) return null;
    const ref = refIndex.get(normalized);
    const chip = renderInteractiveSanChip(raw, ref || null, {
        interactiveClasses: 'bookmaker-decision-compare__move-chip move-chip move-chip--interactive',
        fallbackTag: 'span',
        fallbackClasses: 'bookmaker-decision-compare__move-chip',
    });

    return `
      <span class="bookmaker-decision-compare__move bookmaker-decision-compare__move--${tone}">
        <span class="bookmaker-decision-compare__move-label">${escapeHtml(label)}</span>
        ${chip}
      </span>
    `;
}

function renderDecisionCompareStrip(
    comparison: NarrativeSignalDigest['decisionComparison'],
    refIndex: BookmakerRefIndex,
): string | null {
    const surface = buildDecisionComparisonSurface(comparison, {
        includeEngineLine: false,
        includeEvidence: false,
    });
    const chosen = comparison?.chosenMove?.trim() || '';
    const best = comparison?.engineBestMove?.trim() || '';
    const deferred = comparison?.deferredMove?.trim() || '';
    const secondary = surface.secondary;

    const moveBits = [
        renderBookmakerMoveChip('Chosen', chosen, refIndex.firstBySan, 'chosen'),
        !surface.chosenMatchesBest ? renderBookmakerMoveChip('Engine', best, refIndex.firstBySan, 'engine') : null,
        deferred ? renderBookmakerMoveChip(comparison?.practicalAlternative ? 'Practical' : 'Deferred', deferred, refIndex.firstBySan, 'deferred') : null,
    ].filter(Boolean);

    if (!moveBits.length && !secondary) return null;

    const classes = [
        'bookmaker-decision-compare',
        surface.chosenMatchesBest ? 'bookmaker-decision-compare--match' : '',
        !surface.headline ? 'bookmaker-decision-compare--fallback' : '',
    ]
        .filter(Boolean)
        .join(' ');
    const kicker = !surface.headline ? 'Alternative context' : 'Decision compare';

    return `
      <div class="${classes}">
        <div class="bookmaker-decision-compare__topline">
          <span class="bookmaker-decision-compare__kicker">${escapeHtml(kicker)}</span>
          ${surface.gap ? `<span class="bookmaker-decision-compare__gap">${escapeHtml(surface.gap)}</span>` : ''}
        </div>
        ${moveBits.length ? `<div class="bookmaker-decision-compare__moves">${moveBits.join('')}</div>` : ''}
        ${secondary ? `<div class="bookmaker-decision-compare__secondary">${escapeHtml(secondary)}</div>` : ''}
      </div>
    `;
}

type BookmakerStrategySurface = {
    idea: string | null;
    campaign: string | null;
    execution: string | null;
    objective: string | null;
};

function bookmakerIdeaText(kind: string | null | undefined, focus: string | null | undefined): string | null {
    const base = typeof kind === 'string' && kind.trim() ? humanizeToken(kind.replace(/_/g, ' ')) : '';
    const suffix = typeof focus === 'string' && focus.trim() ? focus.trim() : '';
    const text = [base, suffix].filter(Boolean).join(' · ').trim();
    return text || null;
}

function bookmakerRouteText(strategyPack: StrategyPackV1 | null, owner: string | null): string | null {
    const route =
        strategyPack?.pieceRoutes?.find(r => r.surfaceMode !== 'hidden' && (!owner || r.ownerSide === owner)) ||
        strategyPack?.pieceRoutes?.find(r => r.surfaceMode !== 'hidden');
    if (!route) return null;
    const destination = Array.isArray(route.route) && route.route.length ? route.route[route.route.length - 1] : '';
    const piece = humanizeToken(route.piece);
    if (route.surfaceMode === 'exact' && route.route.length >= 2) {
        return [(`${piece} via ${route.route.join('-')}`).trim(), route.purpose || ''].filter(Boolean).join(' · ');
    }
    return [(`${piece} toward ${destination}`).trim(), route.purpose || ''].filter(Boolean).join(' · ');
}

function bookmakerMoveRefText(strategyPack: StrategyPackV1 | null, owner: string | null): string | null {
    const moveRef =
        strategyPack?.pieceMoveRefs?.find(ref => !owner || ref.ownerSide === owner) ||
        strategyPack?.pieceMoveRefs?.[0];
    if (!moveRef) return null;
    return [humanizeToken(moveRef.piece), `toward ${moveRef.target}`, moveRef.idea || ''].filter(Boolean).join(' · ');
}

function bookmakerObjectiveText(strategyPack: StrategyPackV1 | null, owner: string | null): string | null {
    const target =
        strategyPack?.directionalTargets?.find(value => !owner || value.ownerSide === owner) ||
        strategyPack?.directionalTargets?.[0];
    if (target) return `make ${target.targetSquare} available for the ${humanizeToken(target.piece)}`;
    const focus = strategyPack?.longTermFocus?.find(Boolean)?.trim();
    return focus || null;
}

function buildBookmakerStrategySurface(
    strategyPack: StrategyPackV1 | null,
    signalDigest: NarrativeSignalDigest | null,
): BookmakerStrategySurface {
    const dominantIdea = strategyPack?.strategicIdeas?.[0] || null;
    const secondaryIdea = strategyPack?.strategicIdeas?.[1] || null;
    const owner = dominantIdea?.ownerSide || strategyPack?.sideToMove || null;
    const sideToMove = strategyPack?.sideToMove || null;
    const dominantText =
        bookmakerIdeaText(signalDigest?.dominantIdeaKind, signalDigest?.dominantIdeaFocus) ||
        bookmakerIdeaText(dominantIdea?.kind || null, [
            ...(dominantIdea?.focusSquares || []),
            ...(dominantIdea?.focusFiles || []),
            ...(dominantIdea?.focusDiagonals || []),
            dominantIdea?.focusZone || '',
        ].filter(Boolean).join(', '));
    const secondaryText =
        bookmakerIdeaText(signalDigest?.secondaryIdeaKind, signalDigest?.secondaryIdeaFocus) ||
        bookmakerIdeaText(secondaryIdea?.kind || null, [
            ...(secondaryIdea?.focusSquares || []),
            ...(secondaryIdea?.focusFiles || []),
            ...(secondaryIdea?.focusDiagonals || []),
            secondaryIdea?.focusZone || '',
        ].filter(Boolean).join(', '));

    return {
        idea: [dominantText ? `Dominant ${dominantText}` : '', secondaryText ? `Secondary ${secondaryText}` : '']
            .filter(Boolean)
            .join(' · ') || null,
        campaign: owner && sideToMove && owner !== sideToMove ? `${humanizeToken(owner)} campaign` : null,
        execution: bookmakerRouteText(strategyPack, owner) || bookmakerMoveRefText(strategyPack, owner),
        objective: bookmakerObjectiveText(strategyPack, owner),
    };
}

function decorateBookmakerHtml(
    html: string,
    refs: BookmakerRefsV1 | null,
    ledger: BookmakerStrategicLedgerV1 | null,
    strategyPack: StrategyPackV1 | null,
    signalDigest: NarrativeSignalDigest | null,
    mainPlans: PlanHypothesis[],
    strategicPlanExperiments: StrategicPlanExperiment[],
    probeRequests: ProbeRequest[],
    authorQuestions: AuthorQuestionSummary[],
    authorEvidence: AuthorEvidenceSummary[],
): string {
    const rows: string[] = [];
    const advancedRows: string[] = [];
    const probeRows: string[] = [];
    const authorRows: string[] = [];
    const decisionComparison = signalDigest?.decisionComparison;
    const strategySurface = buildBookmakerStrategySurface(strategyPack, signalDigest);
    const refIndex = buildBookmakerRefIndex(refs);
    const resolveLedgerRef = (san: string) => refIndex.anyBySan.get(normalizeSanToken(san)) || null;
    const authorQuestionById = new Map(authorQuestions.map(question => [question.id, question]));
    const experimentIndex = strategicPlanExperimentIndex(strategicPlanExperiments);
    const supportOpts = buildPlayerFacingSupportOptions(signalDigest);
    const mainPlanTexts = mainPlans
        .slice(0, 2)
        .map(plan => formatStrategicPlanText(plan, experimentIndex))
        .filter(Boolean);
    const deploymentSummary = signalDigest ? formatDeploymentSummary(signalDigest) : null;
    const compactSurface = buildCompactSupportSurface({
        signalDigest,
        mainPlanTexts,
        deploymentSummary,
    });
    const compactPlanText = compactSurface.mainPlanTexts.map(escapeHtml).join(' · ');
    const pushAdvancedRow = (label: string, value: string | null | undefined) => {
        const cleaned = rewritePlayerFacingSupportText(value, supportOpts);
        if (!cleaned) return;
        advancedRows.push(`<div class="bookmaker-strategic-summary__row"><strong>${escapeHtml(label)}:</strong> ${escapeHtml(cleaned)}</div>`);
    };

    if (compactPlanText)
        rows.push(`<div class="bookmaker-strategic-summary__row"><strong>Main plans:</strong> ${compactPlanText}</div>`);
    const decisionStrip = renderDecisionCompareStrip(decisionComparison, refIndex);
    if (decisionStrip) rows.push(decisionStrip);
    compactSurface.rows.forEach(([label, value]) => {
        rows.push(`<div class="bookmaker-strategic-summary__row"><strong>${escapeHtml(label)}:</strong> ${escapeHtml(value)}</div>`);
    });

    const structureBits = filterPlayerFacingValues([
        signalDigest?.structureProfile || '',
        signalDigest?.centerState ? `${signalDigest.centerState.toLowerCase()} center` : '',
    ], supportOpts);
    pushAdvancedRow('Idea', strategySurface.idea);
    pushAdvancedRow('Execution', strategySurface.execution);
    pushAdvancedRow('Objective', strategySurface.objective);
    if (structureBits.length)
        advancedRows.push(`<div class="bookmaker-strategic-summary__row"><strong>Structure:</strong> ${escapeHtml(structureBits.join(' · '))}</div>`);
    if (signalDigest?.prophylaxisPlan || signalDigest?.prophylaxisThreat || typeof signalDigest?.counterplayScoreDrop === 'number') {
        const prophylaxisDetails = filterPlayerFacingValues([
            signalDigest?.prophylaxisThreat ? `stops ${signalDigest.prophylaxisThreat}` : '',
            signalDigest?.prophylaxisPlan ? `slows down ${signalDigest.prophylaxisPlan}` : '',
        ], supportOpts);
        if (prophylaxisDetails.length)
            advancedRows.push(`<div class="bookmaker-strategic-summary__row"><strong>Prophylaxis:</strong> ${escapeHtml(prophylaxisDetails.join(' · '))}</div>`);
    }
    const compensationDetails = filterPlayerFacingValues([signalDigest?.compensation || ''], supportOpts);
    if (compensationDetails.length)
        advancedRows.push(`<div class="bookmaker-strategic-summary__row"><strong>Compensation:</strong> ${escapeHtml(compensationDetails.join(' · '))}</div>`);
    probeRows.push(...renderBookmakerLedgerProbeRows(ledger, resolveLedgerRef));
    probeRequests
        .slice(0, 2)
        .forEach((probe, idx) => {
            const planName = typeof probe.planName === 'string' ? probe.planName.trim() : '';
            const questionKind = typeof probe.questionKind === 'string' ? probe.questionKind.trim() : '';
            const purpose = typeof probe.purpose === 'string' ? probe.purpose.trim() : '';
            const objective = typeof probe.objective === 'string' ? probe.objective.trim() : '';
            const primary =
                planName ||
                questionKind ||
                objective ||
                purpose ||
                `probe ${idx + 1}`;
            const details = [
                purpose && purpose !== primary ? purpose : '',
                objective && objective !== primary ? objective : '',
            ];
            const movePreview =
                Array.isArray(probe.moves) && probe.moves.length
                    ? probe.moves.slice(0, 2).map((move: string) => escapeHtml(move)).join(' / ')
                    : '';
            const detailText = filterPlayerFacingValues(details, supportOpts).join(' | ');
            probeRows.push(`
              <div class="bookmaker-probe-summary__row">
                <strong>${escapeHtml(primary)}:</strong>
                <span>${escapeHtml(detailText)}</span>
                ${movePreview ? `<code>${movePreview}</code>` : ''}
              </div>
            `);
        });

    authorEvidence.slice(0, 2).forEach(summary => {
        const question = authorQuestionById.get(summary.questionId);
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
        const branchMarkup = branches
            .map(branch => {
                const details = filterPlayerFacingValues([branch.line], supportOpts);
                const normalizedKeyMove = normalizeSanToken(branch.keyMove);
                const moveRef = refIndex.anyBySan.get(normalizedKeyMove);
                const branchMove = moveRef
                    ? `<span class="bookmaker-authoring-summary__branch-move move-chip move-chip--interactive" data-ref-id="${escapeHtml(moveRef.refId)}" data-uci="${escapeHtml(moveRef.uci)}" data-san="${escapeHtml(moveRef.san)}" tabindex="0">${escapeHtml(branch.keyMove)}</span>`
                    : `<code>${escapeHtml(branch.keyMove)}</code>`;
                return `
                  <div class="bookmaker-authoring-summary__branch">
                    ${branchMove}
                    <span>${escapeHtml(details.join(' · '))}</span>
                  </div>
                `;
            })
            .join('');
        authorRows.push(`
          <div class="bookmaker-authoring-summary__card">
            <div class="bookmaker-authoring-summary__head">
              <strong>${escapeHtml(humanizeToken(summary.questionKind || question?.kind || 'Authoring'))}</strong>
              <span class="bookmaker-authoring-summary__status bookmaker-authoring-summary__status--${escapeHtml(statusKey)}">${escapeHtml(formatEvidenceStatus(statusKey))}</span>
            </div>
            <div class="bookmaker-authoring-summary__question">${escapeHtml(summary.question)}</div>
            ${why ? `<div class="bookmaker-authoring-summary__why">${escapeHtml(why)}</div>` : ''}
            ${meta.length ? `<div class="bookmaker-authoring-summary__meta">${escapeHtml(meta.join(' · '))}</div>` : ''}
            ${branchMarkup ? `<div class="bookmaker-authoring-summary__branches">${branchMarkup}</div>` : ''}
          </div>
        `);
    });

    if (!authorRows.length) {
        authorQuestions.slice(0, 2).forEach(question => {
        const why = (question.why || '').trim();
        const anchors = (question.anchors || []).filter(Boolean).slice(0, 2);
        const meta = filterPlayerFacingValues([
            anchors.length ? `anchors ${anchors.join(', ')}` : '',
        ], supportOpts);
            authorRows.push(`
              <div class="bookmaker-authoring-summary__card">
                <div class="bookmaker-authoring-summary__head">
                  <strong>${escapeHtml(humanizeToken(question.kind || 'Authoring'))}</strong>
                  <span class="bookmaker-authoring-summary__status bookmaker-authoring-summary__status--question_only">Heuristic</span>
                </div>
                <div class="bookmaker-authoring-summary__question">${escapeHtml(question.question)}</div>
                ${why ? `<div class="bookmaker-authoring-summary__why">${escapeHtml(why)}</div>` : ''}
                ${meta.length ? `<div class="bookmaker-authoring-summary__meta">${escapeHtml(meta.join(' · '))}</div>` : ''}
              </div>
            `);
        });
    }

    if (!rows.length && !advancedRows.length && !probeRows.length && !authorRows.length) return html;

    return `
      <div class="bookmaker-strategic-summary">
        <div class="bookmaker-strategic-summary__title">Support</div>
        ${rows.join('')}
        ${
            advancedRows.length || probeRows.length || authorRows.length
                ? `
          <details class="bookmaker-strategic-summary__details">
            <summary class="bookmaker-strategic-summary__details-summary">Advanced details</summary>
            ${advancedRows.join('')}
            ${
                probeRows.length
                    ? `
              <div class="bookmaker-probe-summary">
                <div class="bookmaker-probe-summary__title">Evidence Probes</div>
                ${probeRows.join('')}
              </div>
            `
                    : ''
            }
            ${
                authorRows.length
                    ? `
              <div class="bookmaker-authoring-summary">
                <div class="bookmaker-probe-summary__title">Authoring Evidence</div>
                ${authorRows.join('')}
              </div>
            `
                    : ''
            }
          </details>
        `
                : ''
        }
      </div>
      ${html}
    `;
}

function decorateDecodedBookmakerHtml(decoded: DecodedBookmakerResponse): string {
    return decorateBookmakerHtml(
        decoded.html,
        decoded.refs,
        decoded.bookmakerLedger,
        decoded.strategyPack,
        decoded.signalDigest,
        decoded.mainStrategicPlans,
        decoded.strategicPlanExperiments,
        decoded.probeRequests,
        decoded.authorQuestions,
        decoded.authorEvidence,
    );
}

export function flushBookmakerStudySync(ctrl: AnalyseCtrl): void {
    flushBookmakerStudySyncQueue(ctrl);
}

const bookmakerEvalDisplay = storedBooleanPropWithEffect('analyse.bookmaker.showEval', true, value => {
    syncBookmakerEvalDisplay(value);
});

export function requestBookmakerCurrent(opts?: { force?: boolean }): void {
    bookmakerRequestTrigger?.(opts);
}

export function bookmakerToggleBox(ctrl?: AnalyseCtrl) {
    initBookmakerHandlers(() => bookmakerEvalDisplay(!bookmakerEvalDisplay()));

    $('#bookmaker-field').each(function (this: HTMLElement) {
        const box = this;
        if (box.dataset.toggleBoxInit) return;
        box.dataset.toggleBoxInit = '1';

        const state = storedBooleanPropWithEffect('analyse.bookmaker.display', true, value =>
            box.classList.toggle('toggle-box--toggle-off', !value),
        );

        const toggle = () => state(!state());

        if (!state()) box.classList.add('toggle-box--toggle-off');

        $(box)
            .children('legend')
            .on('click', toggle)
            .on('keypress', e => e.key === 'Enter' && toggle());
    });

    syncBookmakerEvalDisplay(bookmakerEvalDisplay());
    bookmakerRestore(ctrl);

    const body = document.body;
    if (!body.dataset.bookmakerRequestInit) {
        body.dataset.bookmakerRequestInit = '1';
        $(body).on('click.bookmaker-request', '[data-bookmaker-request]', function (this: HTMLElement, e) {
            e.preventDefault();
            requestBookmakerCurrent({ force: this.dataset.bookmakerForce === '1' });
        });
        $(body).on('click.bookmaker-study-nav', '[data-bookmaker-study-path]', function (this: HTMLElement, e) {
            e.preventDefault();
            const path = this.dataset.bookmakerStudyPath;
            if (path && ctrl?.userJumpIfCan) ctrl.userJumpIfCan(path);
        });
    }
}

export default function bookmakerNarrative(ctrl?: AnalyseCtrl): BookmakerNarrative {
    const cache = new Map<string, BookmakerCacheEntry>();
    const planStateByPath = new Map<string, PlanStateToken | null>();
    const endgameStateByPath = new Map<string, EndgameStateToken | null>();
    const probes = createProbeOrchestrator(ctrl, session => session === activeProbeSession);
    const bookmakerEndpoint = '/api/llm/bookmaker-position';
    let loadingTicker: number | null = null;
    let activeOpeningFetchController: AbortController | null = null;
    let activeInitialFetchController: AbortController | null = null;
    let activeRefinedFetchController: AbortController | null = null;
    let activeRequestKey: string | null = null;

    type CurrentBookmakerContext = {
        nodes: Tree.Node[];
        node: Tree.Node;
        fen: string;
        playedMove: string | null;
        analysisFen: string;
        analysisCeval: any;
        commentPath: string;
        originPath: string;
        stateKey: string;
        requestToken: PlanStateToken | null;
        requestEndgameToken: EndgameStateToken | null;
        cacheKey: string;
    };

    let currentContext: CurrentBookmakerContext | null = null;

    const canonicalize = (value: unknown): string => {
        if (Array.isArray(value)) return `[${value.map(canonicalize).join(',')}]`;
        if (value && typeof value === 'object') {
            const entries = Object.entries(value as Record<string, unknown>).sort(([a], [b]) => a.localeCompare(b));
            return `{${entries.map(([k, v]) => `${JSON.stringify(k)}:${canonicalize(v)}`).join(',')}}`;
        }
        return JSON.stringify(value);
    };

    const tokenHash = (token: unknown): string => {
        if (!token) return '-';
        const str = canonicalize(token);
        let h = 2166136261;
        for (let i = 0; i < str.length; i++) {
            h ^= str.charCodeAt(i);
            h = Math.imul(h, 16777619);
        }
        return (h >>> 0).toString(16);
    };

    const cacheKeyOf = (
        fen: string,
        originPath: string,
        planToken: PlanStateToken | null,
        endgameToken: EndgameStateToken | null,
    ): string => `${fen}|${originPath}|${tokenHash(planToken)}|${tokenHash(endgameToken)}`;
    const stateKeyOf = (originPath: string, analysisFen: string): string =>
        `${originPath}|${analysisFen}`;

    const show = (html: string, remember = true) => {
        if (remember) lastShownHtml = html;
        renderBookmakerPanel(html, ctrl?.getOrientation() ?? 'white', bookmakerEvalDisplay());
    };

    const applyMetaToRoot = (
        sourceMode: string | null,
        model: string | null,
        cacheHit: boolean | null,
        polishMeta: PolishMetaV1 | null,
        bookmakerLedger: BookmakerStrategicLedgerV1 | null,
    ) => {
        const root = document.querySelector('.analyse__bookmaker-text');
        if (!root) return;
        if (sourceMode) root.setAttribute('data-llm-source-mode', sourceMode);
        else root.removeAttribute('data-llm-source-mode');
        if (model) root.setAttribute('data-llm-model', model);
        else root.removeAttribute('data-llm-model');
        if (cacheHit !== null) root.setAttribute('data-llm-cache-hit', String(cacheHit));
        else root.removeAttribute('data-llm-cache-hit');
        if (polishMeta) {
            root.setAttribute('data-llm-polish-provider', polishMeta.provider);
            root.setAttribute('data-llm-polish-phase', polishMeta.validationPhase);
            if (polishMeta.model) root.setAttribute('data-llm-polish-model', polishMeta.model);
            else root.removeAttribute('data-llm-polish-model');
            root.setAttribute('data-llm-polish-source', polishMeta.sourceMode);
            root.setAttribute('data-llm-polish-cache-hit', String(polishMeta.cacheHit));
            if (polishMeta.validationReasons.length)
                root.setAttribute('data-llm-polish-reasons', polishMeta.validationReasons.join(','));
            else root.removeAttribute('data-llm-polish-reasons');
            if (polishMeta.strategyCoverage) {
                const s = polishMeta.strategyCoverage;
                root.setAttribute('data-llm-strategy-mode', s.mode);
                root.setAttribute('data-llm-strategy-score', s.coverageScore.toFixed(2));
                root.setAttribute('data-llm-strategy-covered', String(s.coveredCategories));
                root.setAttribute('data-llm-strategy-required', String(s.requiredCategories));
                root.setAttribute('data-llm-strategy-pass', String(s.passesThreshold));
                root.setAttribute('data-llm-strategy-plan', `${s.planHits}/${s.planSignals}`);
                root.setAttribute('data-llm-strategy-route', `${s.routeHits}/${s.routeSignals}`);
                root.setAttribute('data-llm-strategy-focus', `${s.focusHits}/${s.focusSignals}`);
            } else {
                root.removeAttribute('data-llm-strategy-mode');
                root.removeAttribute('data-llm-strategy-score');
                root.removeAttribute('data-llm-strategy-covered');
                root.removeAttribute('data-llm-strategy-required');
                root.removeAttribute('data-llm-strategy-pass');
                root.removeAttribute('data-llm-strategy-plan');
                root.removeAttribute('data-llm-strategy-route');
                root.removeAttribute('data-llm-strategy-focus');
            }
        } else {
            root.removeAttribute('data-llm-polish-provider');
            root.removeAttribute('data-llm-polish-phase');
            root.removeAttribute('data-llm-polish-model');
            root.removeAttribute('data-llm-polish-source');
            root.removeAttribute('data-llm-polish-cache-hit');
            root.removeAttribute('data-llm-polish-reasons');
            root.removeAttribute('data-llm-strategy-mode');
            root.removeAttribute('data-llm-strategy-score');
            root.removeAttribute('data-llm-strategy-covered');
            root.removeAttribute('data-llm-strategy-required');
            root.removeAttribute('data-llm-strategy-pass');
            root.removeAttribute('data-llm-strategy-plan');
            root.removeAttribute('data-llm-strategy-route');
            root.removeAttribute('data-llm-strategy-focus');
        }
        const ledgerAttrs = bookmakerLedgerRootAttrs(bookmakerLedger);
        ['data-llm-motif', 'data-llm-stage', 'data-llm-carry-over'].forEach(attr => {
            const value = ledgerAttrs[attr];
            if (typeof value === 'string') root.setAttribute(attr, value);
            else root.removeAttribute(attr);
        });
    };

    const applyStrategicMetaToRoot = (mainPlansCount: number) => {
        const root = document.querySelector('.analyse__bookmaker-text');
        if (!root) return;
        root.setAttribute('data-llm-main-plans-count', String(mainPlansCount));
        root.removeAttribute('data-llm-latent-plans-count');
        root.removeAttribute('data-llm-hold-reasons-count');
    };

    const resetMetaOnRoot = () => {
        applyMetaToRoot(null, null, null, null, null);
        applyStrategicMetaToRoot(0);
    };

    const applyCachedEntry = (entry: BookmakerCacheEntry) => {
        setBookmakerRefs(entry.refs);
        show(entry.html);
        applyMetaToRoot(entry.sourceMode, entry.model, entry.cacheHit, entry.polishMeta, entry.bookmakerLedger || null);
        applyStrategicMetaToRoot(entry.mainPlansCount);
    };

    const entryTokenContext = (context: CurrentBookmakerContext) => ({
        stateKey: context.stateKey,
        analysisFen: context.analysisFen,
        originPath: context.originPath,
    });

    const restoreStoredEntryForContext = (context: CurrentBookmakerContext, entry: BookmakerCacheEntry) => {
        const restored = restoreStoredBookmakerTokens(entry, entryTokenContext(context), planStateByPath, endgameStateByPath);
        const restoredCacheKey = cacheKeyOf(
            context.fen,
            context.originPath,
            restored.planStateToken,
            restored.endgameStateToken,
        );
        cache.set(context.cacheKey, entry);
        if (restoredCacheKey !== context.cacheKey) cache.set(restoredCacheKey, entry);
        if (currentContext?.commentPath === context.commentPath) {
            currentContext = {
                ...context,
                requestToken: restored.planStateToken,
                requestEndgameToken: restored.endgameStateToken,
                cacheKey: restoredCacheKey,
            };
        }
        applyCachedEntry(entry);
    };

    const restoreStudySnapshotForContext = (context: CurrentBookmakerContext): boolean => {
        const ref = currentStudyBookmakerRef(ctrl);
        if (!ref) return false;
        const snapshot = readStudyBookmakerSnapshot(ref, context.commentPath);
        if (!snapshot?.entry?.html) return false;
        restoreStoredEntryForContext(context, snapshot.entry);
        return true;
    };

    const restoreSessionSnapshotForContext = (context: CurrentBookmakerContext): boolean => {
        const snapshot = readSessionBookmakerSnapshot(currentBookmakerSessionScope(), context.commentPath);
        if (!snapshot?.entry?.html) return false;
        restoreStoredEntryForContext(context, snapshot.entry);
        return true;
    };

    const restoreStudyFallbackForContext = (context: CurrentBookmakerContext): boolean => {
        const ref = currentStudyBookmakerRef(ctrl);
        if (!ref) return false;
        const commentary = findSavedStudyAiComment(context.node);
        if (!commentary) return false;
        const refs = buildSavedStudyRefs(ctrl, context.originPath, context.commentPath);
        const entry: BookmakerCacheEntry = {
            html: renderSavedStudyFallbackHtml(commentary, refs),
            refs,
            polishMeta: null,
            sourceMode: 'study_saved',
            model: null,
            cacheHit: true,
            mainPlansCount: 0,
            bookmakerLedger: null,
            planStateToken: null,
            endgameStateToken: null,
            tokenContext: entryTokenContext(context),
        };
        cache.set(context.cacheKey, entry);
        persistStudyBookmakerSnapshot(ref, context.commentPath, context.originPath, commentary, entry);
        applyCachedEntry(entry);
        return true;
    };

    const persistBookmakerSnapshot = (
        context: CurrentBookmakerContext,
        commentary: string | null,
        entry: BookmakerCacheEntry,
    ) => {
        persistSessionBookmakerSnapshot(
            currentBookmakerSessionScope(),
            context.commentPath,
            context.originPath,
            commentary,
            entry,
        );
        const studyRef = currentStudyBookmakerRef(ctrl);
        if (studyRef)
            persistStudyBookmakerSnapshot(studyRef, context.commentPath, context.originPath, commentary, entry);
    };

    const abortNetwork = () => {
        activeOpeningFetchController?.abort();
        activeInitialFetchController?.abort();
        activeRefinedFetchController?.abort();
        activeOpeningFetchController = null;
        activeInitialFetchController = null;
        activeRefinedFetchController = null;
        activeRequestKey = null;
    };

    const stopLoadingTicker = () => {
        if (loadingTicker !== null) {
            window.clearInterval(loadingTicker);
            loadingTicker = null;
        }
    };

    const setLoadingStage = (stage: LoadingStage, isCurrentSession: () => boolean) => {
        stopLoadingTicker();
        const messages = loadingStageMessages[stage];
        let index = 0;
        const draw = () => {
            if (!isCurrentSession()) {
                stopLoadingTicker();
                return;
            }
            const message = messages[index % messages.length];
            show(renderLoadingHud(stage, message), false);
            index++;
        };
        draw();
        if (messages.length > 1) loadingTicker = window.setInterval(draw, 1150);
    };

    const streamReveal = async (commentary: string, isCurrentSession: () => boolean) => {
        const words = splitWords(commentary);
        if (words.length < 16 || !isCurrentSession()) return;
        const steps = Math.max(6, Math.min(18, Math.floor(words.length / 8)));
        const totalMs = Math.max(420, Math.min(1200, words.length * 10));
        const stepMs = Math.max(30, Math.floor(totalMs / steps));

        for (let step = 1; step <= steps; step++) {
            if (!isCurrentSession()) return;
            const take = Math.max(1, Math.floor((words.length * step) / steps));
            const preview = words.slice(0, take).join(' ');
            show(renderLoadingHud('polish', 'Streaming final commentary...', preview), false);
            await new Promise<void>(resolve => window.setTimeout(resolve, stepMs));
        }
    };

    const phaseOf = (ply: number): string => {
        if (ply <= 16) return 'opening';
        if (ply <= 60) return 'middlegame';
        return 'endgame';
    };

    const loginHref = () =>
        `/auth/magic-link?referrer=${encodeURIComponent(location.pathname + location.search)}`;

    const suspiciousFallback = (text: string): boolean =>
        /under strict evidence mode|probe evidence pending|engine-coupled continuation|theme:|subplan:|\{seed\}|PlayableByPV|PlayedPV|return vector|cash out/i.test(
            text,
        );

    const showIdle = () => {
        setBookmakerRefs(null);
        resetMetaOnRoot();
        const ref = currentStudyBookmakerRef(ctrl);
        const readingSurface =
            ref && currentContext ? renderStudyReadingSurface(ctrl, ref, currentContext.commentPath) : null;
        const baseState =
            currentContext && currentContext.node.ply < MIN_BOOKMAKER_PLY
                ? bookmakerTooEarlyHtml(MIN_BOOKMAKER_PLY)
                : bookmakerIdleHtml();
        show(`${baseState}${readingSurface || ''}`);
    };

    const showRetry = (message?: string) => {
        setBookmakerRefs(null);
        resetMetaOnRoot();
        show(bookmakerRetryHtml(message));
    };

    const syncStudy = (commentPath: string, originPath: string, commentary: string, lines: any[]) => {
        if (!commentary) return;
        const payload = { commentPath, originPath, commentary, variations: lines };
        rememberBookmakerStudySync(payload);
        if (ctrl?.canWriteStudy()) ctrl.syncBookmaker(payload);
    };

    const runCurrentRequest = async (opts?: { force?: boolean }) => {
        const context = currentContext;
        if (!context) return;
        if (context.node.ply < MIN_BOOKMAKER_PLY) {
            setBookmakerRefs(null);
            resetMetaOnRoot();
            show(bookmakerTooEarlyHtml(MIN_BOOKMAKER_PLY));
            return;
        }
        const force = !!opts?.force;
        if (requestsBlocked) {
            if (blockedHtml) show(blockedHtml);
            setBookmakerRefs(null);
            return;
        }

        if (force) cache.delete(context.cacheKey);
        else {
            const cached = cache.get(context.cacheKey);
            if (cached) {
                applyCachedEntry(cached);
                return;
            }
            if (restoreStudySnapshotForContext(context)) return;
            if (restoreSessionSnapshotForContext(context)) return;
            if (restoreStudyFallbackForContext(context)) return;
            if (activeRequestKey === context.cacheKey) return;
        }

        abortNetwork();
        activeProbeSession++;
        probes.stop();
        const probeSession = activeProbeSession;
        const requestKey = context.cacheKey;
        activeRequestKey = requestKey;
        lastRequestedFen = context.fen;
        const isCurrentSession = () =>
            probeSession === activeProbeSession && lastRequestedFen === context.fen && activeRequestKey === requestKey;

        try {
            setLoadingStage('position', isCurrentSession);

            const targetDepth = 20;
            const targetMultiPv = 5;
            const analysisTimeoutMs = 15000;

            let analysisEval: any = context.analysisCeval;
            let variations = probes.evalToVariations(analysisEval, targetMultiPv);
            if ((!variations || variations.length < targetMultiPv) && ctrl) {
                setLoadingStage('lines', isCurrentSession);
                analysisEval = await probes.runPositionEval(
                    context.analysisFen,
                    targetDepth,
                    analysisTimeoutMs,
                    targetMultiPv,
                    probeSession,
                );
                if (!isCurrentSession()) {
                    stopLoadingTicker();
                    return;
                }
                variations = probes.evalToVariations(analysisEval, targetMultiPv);
            }

            if (
                context.playedMove &&
                variations &&
                !variations.some(v => Array.isArray(v.moves) && v.moves[0] === context.playedMove) &&
                ctrl
            ) {
                setLoadingStage('lines', isCurrentSession);
                const playedEv = await probes.runProbeEval(context.analysisFen, context.playedMove, targetDepth, 5000, 1, probeSession);
                if (!isCurrentSession()) {
                    stopLoadingTicker();
                    return;
                }
                if (playedEv) {
                    const replyPv = Array.isArray(playedEv.pvs) ? playedEv.pvs[0]?.moves : null;
                    const playedVar = {
                        moves: [context.playedMove, ...(Array.isArray(replyPv) ? replyPv.slice(0, 28) : [])],
                        scoreCp: typeof playedEv.cp === 'number' ? playedEv.cp : 0,
                        mate: typeof playedEv.mate === 'number' ? playedEv.mate : null,
                        depth: typeof playedEv.depth === 'number' ? playedEv.depth : targetDepth,
                    };
                    variations = [...variations, playedVar].slice(0, targetMultiPv + 1);
                }
            }

            const afterFen = context.playedMove ? context.fen : null;
            let afterVariations = afterFen ? probes.evalToVariations(context.playedMove ? context.node.ceval : null, 1) : null;
            afterVariations = deriveAfterVariations(afterFen, afterVariations, context.playedMove, variations);
            const evalData = toEvalData(variations);

            const useAnalysisSurfaceV3 = document.body.dataset.brandV3AnalysisSurface !== '0';
            const useExplorerProxy = useAnalysisSurfaceV3 && document.body.dataset.brandExplorerProxy !== '0';
            setLoadingStage('compose', isCurrentSession);
            activeOpeningFetchController = new AbortController();
            const openingData = await fetchOpeningReferenceViaProxy(
                context.analysisFen,
                context.node.ply,
                useExplorerProxy,
                activeOpeningFetchController.signal,
            );
            activeOpeningFetchController = null;
            if (!isCurrentSession()) {
                stopLoadingTicker();
                return;
            }
            const initialPayload = buildBookmakerRequest({
                fen: context.analysisFen,
                lastMove: context.playedMove || null,
                variations,
                probeResults: null,
                openingData,
                afterFen,
                afterVariations,
                phase: phaseOf(context.node.ply),
                ply: context.node.ply,
                variant: ctrl?.data.game.variant.key ?? 'standard',
                planStateToken: context.requestToken,
                endgameStateToken: context.requestEndgameToken,
            });

            setLoadingStage('polish', isCurrentSession);
            activeInitialFetchController = new AbortController();
            const res = await fetch(bookmakerEndpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(initialPayload),
                signal: activeInitialFetchController.signal,
            });
            activeInitialFetchController = null;

            if (!isCurrentSession()) {
                stopLoadingTicker();
                return;
            }
            stopLoadingTicker();

            if (!res.ok) {
                const blocked = await blockedHtmlFromErrorResponse(res, loginHref());
                if (!blocked) {
                    activeRequestKey = null;
                    return showRetry();
                }
                blockedHtml = blocked;
                requestsBlocked = true;
                setBookmakerRefs(null);
                show(blockedHtml);
                activeRequestKey = null;
                return;
            }

            const data = await res.json();
            const decoded = decodeBookmakerResponse(data);
            const emittedToken = decoded.planStateToken;
            const emittedEndgameToken = decoded.endgameStateToken;
            if (emittedToken) planStateByPath.set(context.stateKey, emittedToken);
            else planStateByPath.delete(context.stateKey);
            if (emittedEndgameToken) endgameStateByPath.set(context.stateKey, emittedEndgameToken);
            else endgameStateByPath.delete(context.stateKey);
            const html = decoded.html;
            const commentary = decoded.commentary;
            if (
                (decoded.sourceMode?.startsWith('fallback_rule') || decoded.sourceMode === 'rule_circuit_open') &&
                suspiciousFallback(commentary)
            ) {
                activeRequestKey = null;
                return showRetry('Commentary timed out before polish completed. Retry for a clean explanation.');
            }
            const decoratedHtml = decorateDecodedBookmakerHtml(decoded);
            const shouldStream = decoded.sourceMode === 'llm_polished' && commentary.length > 0;

            if (shouldStream) {
                await streamReveal(commentary, isCurrentSession);
                if (!isCurrentSession()) {
                    stopLoadingTicker();
                    return;
                }
            }

            const initialEntry: BookmakerCacheEntry = buildStoredBookmakerEntry(
                decoded,
                decoratedHtml,
                entryTokenContext(context),
            );
            cache.set(context.cacheKey, initialEntry);
            persistBookmakerSnapshot(context, commentary, initialEntry);
            applyCachedEntry(initialEntry);

            const vLines = variationLinesFromResponse(data, variations);
            syncStudy(context.commentPath, context.originPath, commentary, vLines);

            const baselineCp = toBaselineCp(variations, evalData);

            if (decoded.probeRequests.length && ctrl) {
                void (async () => {
                    const probeResults = await probes.runProbes(decoded.probeRequests, baselineCp, probeSession);
                    if (!isCurrentSession()) return;
                    if (!probeResults.length) return;

                    try {
                        const refinedToken = planStateByPath.get(context.stateKey) ?? context.requestToken;
                        const refinedEndgameToken = endgameStateByPath.get(context.stateKey) ?? context.requestEndgameToken;
                        const refinedCacheKey = cacheKeyOf(context.fen, context.originPath, refinedToken, refinedEndgameToken);
                        const refinedPayload = buildBookmakerRequest({
                            fen: context.analysisFen,
                            lastMove: context.playedMove || null,
                            variations,
                            probeResults,
                            openingData,
                            afterFen,
                            afterVariations,
                            phase: phaseOf(context.node.ply),
                            ply: context.node.ply,
                            variant: ctrl?.data.game.variant.key ?? 'standard',
                            planStateToken: refinedToken,
                            endgameStateToken: refinedEndgameToken,
                        });
                        activeRefinedFetchController = new AbortController();
                        const refinedRes = await fetch(bookmakerEndpoint, {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify(refinedPayload),
                            signal: activeRefinedFetchController.signal,
                        });
                        activeRefinedFetchController = null;

                        if (!isCurrentSession() || !refinedRes.ok) return;

                        const refined = await refinedRes.json();
                        const decodedRefined = decodeBookmakerResponse(refined, {
                            html,
                            commentary,
                            probeRequests: decoded.probeRequests,
                            authorQuestions: decoded.authorQuestions,
                            authorEvidence: decoded.authorEvidence,
                        });
                        const emittedRefinedToken = decodedRefined.planStateToken;
                        const emittedRefinedEndgameToken = decodedRefined.endgameStateToken;
                        if (emittedRefinedToken) planStateByPath.set(context.stateKey, emittedRefinedToken);
                        else planStateByPath.delete(context.stateKey);
                        if (emittedRefinedEndgameToken) endgameStateByPath.set(context.stateKey, emittedRefinedEndgameToken);
                        else endgameStateByPath.delete(context.stateKey);
                        const refinedCommentary = decodedRefined.commentary;
                        if (
                            (decodedRefined.sourceMode?.startsWith('fallback_rule') ||
                                decodedRefined.sourceMode === 'rule_circuit_open') &&
                            suspiciousFallback(refinedCommentary)
                        ) {
                            activeRequestKey = null;
                            return;
                        }
                        const decoratedRefinedHtml = decorateDecodedBookmakerHtml(decodedRefined);
                        const refinedEntry: BookmakerCacheEntry = buildStoredBookmakerEntry(
                            decodedRefined,
                            decoratedRefinedHtml,
                            entryTokenContext(context),
                        );
                        cache.set(refinedCacheKey, refinedEntry);
                        persistBookmakerSnapshot(context, refinedCommentary, refinedEntry);
                        if (currentContext?.cacheKey === refinedCacheKey && isCurrentSession()) {
                            applyCachedEntry(refinedEntry);
                        }

                        const refinedLines = variationLinesFromResponse(refined, variationLinesFromResponse(data, variations));
                        syncStudy(context.commentPath, context.originPath, refinedCommentary, refinedLines);
                    } catch (err) {
                        if (!(err instanceof DOMException && err.name === 'AbortError') && isCurrentSession()) {
                            showRetry('The refined follow-up could not finish. The first draft remains available.');
                        }
                    }
                })();
            }
            activeRequestKey = null;
        } catch (err) {
            stopLoadingTicker();
            activeRequestKey = null;
            if (err instanceof DOMException && err.name === 'AbortError') return;
            showRetry();
        }
    };

    bookmakerRequestTrigger = runCurrentRequest;

    return (nodes: Tree.Node[]) => {
        const node = nodes[nodes.length - 1];
        if (!node?.fen) {
            currentContext = null;
            bookmakerRequestTrigger = runCurrentRequest;
            abortNetwork();
            activeProbeSession++;
            probes.stop();
            stopLoadingTicker();
            lastRequestedFen = null;
            setBookmakerRefs(null);
            resetMetaOnRoot();
            return show('');
        }

        const fen = node.fen;
        const prevNode = nodes.length >= 2 ? nodes[nodes.length - 2] : undefined;
        const playedMove = typeof node.uci === 'string' && prevNode?.fen ? node.uci : null;
        const analysisFen = playedMove ? prevNode!.fen : fen;
        const analysisCeval = playedMove ? prevNode?.ceval : node.ceval;
        const commentPath = ctrl?.path ?? '';
        const originPath = playedMove ? treePath.init(commentPath) : commentPath;
        const stateKey = stateKeyOf(originPath, analysisFen);
        const requestToken = planStateByPath.get(stateKey) ?? null;
        const requestEndgameToken = endgameStateByPath.get(stateKey) ?? null;
        const cacheKey = cacheKeyOf(fen, originPath, requestToken, requestEndgameToken);
        const nextContext: CurrentBookmakerContext = {
            nodes,
            node,
            fen,
            playedMove,
            analysisFen,
            analysisCeval,
            commentPath,
            originPath,
            stateKey,
            requestToken,
            requestEndgameToken,
            cacheKey,
        };
        const sameContext =
            currentContext?.cacheKey === nextContext.cacheKey && currentContext?.commentPath === nextContext.commentPath;
        currentContext = nextContext;
        bookmakerRequestTrigger = runCurrentRequest;

        if (!sameContext) {
            abortNetwork();
            activeProbeSession++;
            probes.stop();
            stopLoadingTicker();
            lastRequestedFen = null;
        }

        if (requestsBlocked) {
            resetMetaOnRoot();
            setBookmakerRefs(null);
            if (blockedHtml) show(blockedHtml);
            return;
        }

        const cached = cache.get(cacheKey);
        if (cached) {
            applyCachedEntry(cached);
            return;
        }

        if (restoreStudySnapshotForContext(nextContext)) return;
        if (restoreSessionSnapshotForContext(nextContext)) return;
        if (restoreStudyFallbackForContext(nextContext)) return;

        if (activeRequestKey === cacheKey) return;
        showIdle();
    };
}

export function bookmakerClear() {
    lastShownHtml = '';
    setBookmakerRefs(null);
    clearBookmakerPanel();
}

export function bookmakerRestore(ctrl?: AnalyseCtrl): void {
    setBookmakerRefs(null);
    restoreBookmakerPanel(lastShownHtml, ctrl?.getOrientation() ?? 'white', bookmakerEvalDisplay());
}
