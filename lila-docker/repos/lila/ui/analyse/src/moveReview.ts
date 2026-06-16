import { storedBooleanPropWithEffect } from 'lib/storage';
import type AnalyseCtrl from './ctrl';
import { treePath } from 'lib/tree';
import { fetchOpeningReferenceViaProxy } from './moveReview/openingProxy';
import { initMoveReviewHandlers, setMoveReviewRefs } from './moveReview/interactionHandlers';

import { clearMoveReviewPanel, renderMoveReviewPanel, restoreMoveReviewPanel, syncMoveReviewEvalDisplay } from './moveReview/rendering';
import { buildMoveReviewRequest, deriveAfterVariations } from './moveReview/requestPayload';
import { blockedHtmlFromErrorResponse, moveReviewIdleHtml, moveReviewRetryHtml, moveReviewTooEarlyHtml } from './moveReview/blockingState';
import {
    buildStoredMoveReviewEntry,
    listStudyMoveReviewSnapshots,
    persistSessionMoveReviewSnapshot,
    persistStudyMoveReviewSnapshot,
    readSessionMoveReviewSnapshot,
    readStudyMoveReviewSnapshot,
    type StoredMoveReviewEntry,
    type StudyMoveReviewRef,
} from './moveReview/studyPersistence';
import { flushMoveReviewStudySyncQueue, rememberMoveReviewStudySync } from './moveReview/studySyncQueue';
import {
    decodeMoveReviewResponse,
    moveReviewNeedsRetry,
    type DecodedMoveReviewResponse,
    type MoveReviewStrategicLedgerV1,
    type MoveReviewRefsV1,
    type PolishMetaV1,
    variationLinesFromResponse,
} from './moveReview/responsePayload';
import { moveReviewLedgerRootAttrs } from './moveReview/ledgerSurface';
import { escapeHtml } from './moveReview/surfaceShared';
import { restoreStoredMoveReviewTokens } from './moveReview/stateContinuity';
import { decorateMoveReviewHtml } from './moveReview/coachSurface';
import type {
    EndgameStateToken,
    PlanStateToken,
    EvalVariation,
} from './moveReview/types';

export type MoveReviewNarrative = (nodes: Tree.Node[]) => void;

type TriggerMoveReviewRequest = (opts?: { force?: boolean }) => void;

type MoveReviewCacheEntry = StoredMoveReviewEntry;

let requestsBlocked = false;
let blockedHtml: string | null = null;
let lastRequestedFen: string | null = null;
let lastShownHtml = '';

let moveReviewRequestTrigger: TriggerMoveReviewRequest | null = null;
const MIN_MOVE_REVIEW_PLY = 5;

type LoadingStage = 'position' | 'lines' | 'compose' | 'polish';

const loadingStageOrder: Record<LoadingStage, number> = {
    position: 1,
    lines: 2,
    compose: 3,
    polish: 4,
};

const loadingStageTitle: Record<LoadingStage, string> = {
    position: 'Read the position',
    lines: 'Check the lines',
    compose: 'Write the lesson',
    polish: 'Finish the review',
};

const loadingStageMessages: Record<LoadingStage, string[]> = {
    position: [
        'Reading the position...',
        'Checking king safety and piece activity...',
        'Finding the move that changed the game...',
    ],
    lines: [
        'Checking the main continuations...',
        'Comparing tactical and positional routes...',
        'Following forcing lines one by one...',
    ],
    compose: [
        'Building the coach explanation...',
        'Aligning plans with the current structure...',
        'Turning the line into a lesson you can replay...',
    ],
    polish: [
        'Preparing the final review...',
        'Ensuring move order and notation stay exact...',
        'Getting the board and notes ready together...',
    ],
};

function renderLoadingHud(stage: LoadingStage, message: string, streamPreview?: string): string {
    const step = loadingStageOrder[stage];
    const title = loadingStageTitle[stage];
    const isReveal = typeof streamPreview === 'string';
    const safeMessage = escapeHtml(message);
    const safeStream = isReveal ? escapeHtml(streamPreview || '') : '';

    return `
      <div class="move-review-thinking-hud glass${isReveal ? ' move-review-thinking-hud--reveal' : ''}">
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

function currentStudyMoveReviewRef(ctrl?: AnalyseCtrl): StudyMoveReviewRef | null {
    const study = ctrl?.opts?.study as { id?: string; chapterId?: string } | undefined;
    if (!study?.id || !study?.chapterId) return null;
    return { studyId: study.id, chapterId: study.chapterId };
}

function currentMoveReviewSessionScope(): string {
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

function buildSavedStudyRefs(ctrl: AnalyseCtrl | undefined, originPath: string, commentPath: string): MoveReviewRefsV1 | null {
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
            const moves: MoveReviewRefsV1['variations'][number]['moves'] = [];
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
        .filter(Boolean) as MoveReviewRefsV1['variations'];

    if (!variations.length) return null;
    return {
        schema: 'chesstory.refs.v1',
        startFen: originNode.fen,
        startPly: originNode.ply + 1,
        variations,
    };
}

function renderSavedStudyFallbackHtml(commentary: string, refs: MoveReviewRefsV1 | null): string {
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
        <h3>Saved line to replay</h3>
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
      <div class="move-review-content move-review-content--saved">
        <div class="move-review-toolbar">
          <span class="move-review-saved-pill">Saved review</span>
        </div>
        <div class="move-review-pv-preview"></div>
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
    if (!normalized) return 'Saved review';
    if (normalized.length <= 140) return normalized;
    return `${normalized.slice(0, 137).trimEnd()}...`;
}

function renderStudyReadingSurface(ctrl: AnalyseCtrl | undefined, ref: StudyMoveReviewRef, currentPath: string): string | null {
    const snapshots = listStudyMoveReviewSnapshots(ref)
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
                class="move-review-study-reading__item"
                data-move-review-study-path="${escapeHtml(snapshot.commentPath)}"
              >
                <span class="move-review-study-reading__label">${escapeHtml(label)}</span>
                <span class="move-review-study-reading__excerpt">${escapeHtml(excerpt)}</span>
              </button>
            `;
        })
        .join('');

    return `
      <div class="move-review-study-reading">
        <div class="move-review-study-reading__title">Saved study review</div>
        <div class="move-review-study-reading__list">${items}</div>
      </div>
    `;
}

function decorateDecodedMoveReviewHtml(decoded: DecodedMoveReviewResponse): string {
    return decorateMoveReviewHtml(decoded.html, decoded.refs, decoded.moveReviewPlayerSurface);
}

export function flushMoveReviewStudySync(ctrl: AnalyseCtrl): void {
    flushMoveReviewStudySyncQueue(ctrl);
}

const moveReviewEvalDisplay = storedBooleanPropWithEffect('analyse.move_review.showEval', true, value => {
    syncMoveReviewEvalDisplay(value);
});

export function requestMoveReviewCurrent(opts?: { force?: boolean }): void {
    moveReviewRequestTrigger?.(opts);
}

export function moveReviewToggleBox(ctrl?: AnalyseCtrl) {
    initMoveReviewHandlers(ctrl, () => moveReviewEvalDisplay(!moveReviewEvalDisplay()));

    $('#move-review-field').each(function (this: HTMLElement) {
        const box = this;
        if (box.dataset.toggleBoxInit) return;
        box.dataset.toggleBoxInit = '1';

        const state = storedBooleanPropWithEffect('analyse.move_review.display', true, value =>
            box.classList.toggle('toggle-box--toggle-off', !value),
        );

        const toggle = () => state(!state());

        if (!state()) box.classList.add('toggle-box--toggle-off');

        $(box)
            .children('legend')
            .on('click', toggle)
            .on('keypress', e => e.key === 'Enter' && toggle());
    });

    syncMoveReviewEvalDisplay(moveReviewEvalDisplay());
    moveReviewRestore(ctrl);

    const body = document.body;
    if (!body.dataset.moveReviewRequestInit) {
        body.dataset.moveReviewRequestInit = '1';
        $(body).on('click.move-review-request', '[data-move-review-request]', function (this: HTMLElement, e) {
            e.preventDefault();
            requestMoveReviewCurrent({ force: this.dataset.moveReviewForce === '1' });
        });
        $(body).on('click.move-review-study-nav', '[data-move-review-study-path]', function (this: HTMLElement, e) {
            e.preventDefault();
            const path = this.dataset.moveReviewStudyPath;
            if (path && ctrl?.userJumpIfCan) ctrl.userJumpIfCan(path);
        });
    }
}

function evalToVariations(ceval: any, maxPvs: number): EvalVariation[] | null {
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
}

export default function moveReviewNarrative(ctrl?: AnalyseCtrl): MoveReviewNarrative {
    const cache = new Map<string, MoveReviewCacheEntry>();
    const planStateByPath = new Map<string, PlanStateToken | null>();
    const endgameStateByPath = new Map<string, EndgameStateToken | null>();
    const moveReviewEndpoint = '/api/commentary/move-review-position';
    let loadingTicker: number | null = null;
    let activeOpeningFetchController: AbortController | null = null;
    let activeInitialFetchController: AbortController | null = null;
    let activeRequestKey: string | null = null;

    type CurrentMoveReviewContext = {
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

    let currentContext: CurrentMoveReviewContext | null = null;

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
        renderMoveReviewPanel(html, ctrl?.getOrientation() ?? 'white', moveReviewEvalDisplay());
    };

    const applyMetaToRoot = (
        sourceMode: string | null,
        model: string | null,
        cacheHit: boolean | null,
        polishMeta: PolishMetaV1 | null,
        moveReviewLedger: MoveReviewStrategicLedgerV1 | null,
    ) => {
        const root = document.querySelector('.analyse__move-review-text');
        if (!root) return;
        if (sourceMode) root.setAttribute('data-commentary-source-mode', sourceMode);
        else root.removeAttribute('data-commentary-source-mode');
        if (model) root.setAttribute('data-commentary-model', model);
        else root.removeAttribute('data-commentary-model');
        if (cacheHit !== null) root.setAttribute('data-commentary-cache-hit', String(cacheHit));
        else root.removeAttribute('data-commentary-cache-hit');
        if (polishMeta) {
            root.setAttribute('data-commentary-polish-provider', polishMeta.provider);
            root.setAttribute('data-commentary-polish-phase', polishMeta.validationPhase);
            if (polishMeta.model) root.setAttribute('data-commentary-polish-model', polishMeta.model);
            else root.removeAttribute('data-commentary-polish-model');
            root.setAttribute('data-commentary-polish-source', polishMeta.sourceMode);
            root.setAttribute('data-commentary-polish-cache-hit', String(polishMeta.cacheHit));
            root.removeAttribute('data-commentary-polish-reasons');
        } else {
            root.removeAttribute('data-commentary-polish-provider');
            root.removeAttribute('data-commentary-polish-phase');
            root.removeAttribute('data-commentary-polish-model');
            root.removeAttribute('data-commentary-polish-source');
            root.removeAttribute('data-commentary-polish-cache-hit');
            root.removeAttribute('data-commentary-polish-reasons');
        }
        root.removeAttribute('data-commentary-strategy-mode');
        root.removeAttribute('data-commentary-strategy-score');
        root.removeAttribute('data-commentary-strategy-covered');
        root.removeAttribute('data-commentary-strategy-required');
        root.removeAttribute('data-commentary-strategy-pass');
        root.removeAttribute('data-commentary-strategy-plan');
        root.removeAttribute('data-commentary-strategy-route');
        root.removeAttribute('data-commentary-strategy-focus');
        const ledgerAttrs = moveReviewLedgerRootAttrs(moveReviewLedger);
        ['data-commentary-motif', 'data-commentary-stage', 'data-commentary-carry-over'].forEach(attr => {
            const value = ledgerAttrs[attr];
            if (typeof value === 'string') root.setAttribute(attr, value);
            else root.removeAttribute(attr);
        });
    };

    const applyStrategicMetaToRoot = (mainPlansCount: number) => {
        const root = document.querySelector('.analyse__move-review-text');
        if (!root) return;
        root.setAttribute('data-commentary-main-plans-count', String(mainPlansCount));
        root.removeAttribute('data-commentary-latent-plans-count');
        root.removeAttribute('data-commentary-hold-reasons-count');
    };

    const resetMetaOnRoot = () => {
        applyMetaToRoot(null, null, null, null, null);
        applyStrategicMetaToRoot(0);
    };

    const applyCachedEntry = (entry: MoveReviewCacheEntry) => {
        setMoveReviewRefs(entry.refs);
        show(entry.html);
        applyMetaToRoot(entry.sourceMode, entry.model, entry.cacheHit, entry.polishMeta, entry.moveReviewLedger || null);
        applyStrategicMetaToRoot(entry.mainPlansCount);
    };

    const entryTokenContext = (context: CurrentMoveReviewContext) => ({
        stateKey: context.stateKey,
        analysisFen: context.analysisFen,
        originPath: context.originPath,
    });

    const restoreStoredEntryForContext = (context: CurrentMoveReviewContext, entry: MoveReviewCacheEntry) => {
        const restored = restoreStoredMoveReviewTokens(entry, entryTokenContext(context), planStateByPath, endgameStateByPath);
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

    const restoreStudySnapshotForContext = (context: CurrentMoveReviewContext): boolean => {
        const ref = currentStudyMoveReviewRef(ctrl);
        if (!ref) return false;
        const snapshot = readStudyMoveReviewSnapshot(ref, context.commentPath);
        if (!snapshot?.entry?.html) return false;
        restoreStoredEntryForContext(context, snapshot.entry);
        return true;
    };

    const restoreSessionSnapshotForContext = (context: CurrentMoveReviewContext): boolean => {
        const snapshot = readSessionMoveReviewSnapshot(currentMoveReviewSessionScope(), context.commentPath);
        if (!snapshot?.entry?.html) return false;
        restoreStoredEntryForContext(context, snapshot.entry);
        return true;
    };

    const restoreStudyFallbackForContext = (context: CurrentMoveReviewContext): boolean => {
        const ref = currentStudyMoveReviewRef(ctrl);
        if (!ref) return false;
        const commentary = findSavedStudyAiComment(context.node);
        if (!commentary) return false;
        const refs = buildSavedStudyRefs(ctrl, context.originPath, context.commentPath);
        const entry: MoveReviewCacheEntry = {
            html: renderSavedStudyFallbackHtml(commentary, refs),
            refs,
            polishMeta: null,
            sourceMode: 'study_saved',
            model: null,
            cacheHit: true,
            mainPlansCount: 0,
            moveReviewLedger: null,
            planStateToken: null,
            endgameStateToken: null,
            tokenContext: entryTokenContext(context),
        };
        cache.set(context.cacheKey, entry);
        persistStudyMoveReviewSnapshot(ref, context.commentPath, context.originPath, commentary, entry);
        applyCachedEntry(entry);
        return true;
    };

    const persistMoveReviewSnapshot = (
        context: CurrentMoveReviewContext,
        commentary: string | null,
        entry: MoveReviewCacheEntry,
    ) => {
        persistSessionMoveReviewSnapshot(
            currentMoveReviewSessionScope(),
            context.commentPath,
            context.originPath,
            commentary,
            entry,
        );
        const studyRef = currentStudyMoveReviewRef(ctrl);
        if (studyRef)
            persistStudyMoveReviewSnapshot(studyRef, context.commentPath, context.originPath, commentary, entry);
    };

    const abortNetwork = () => {
        activeOpeningFetchController?.abort();
        activeInitialFetchController?.abort();
        activeOpeningFetchController = null;
        activeInitialFetchController = null;
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
            show(renderLoadingHud('polish', 'Opening the review...', preview), false);
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

    const showIdle = () => {
        setMoveReviewRefs(null);
        resetMetaOnRoot();
        const ref = currentStudyMoveReviewRef(ctrl);
        const readingSurface =
            ref && currentContext ? renderStudyReadingSurface(ctrl, ref, currentContext.commentPath) : null;
        const baseState =
            currentContext && currentContext.node.ply < MIN_MOVE_REVIEW_PLY
                ? moveReviewTooEarlyHtml(MIN_MOVE_REVIEW_PLY)
                : moveReviewIdleHtml();
        show(`${baseState}${readingSurface || ''}`);
    };

    const showRetry = (message?: string) => {
        setMoveReviewRefs(null);
        resetMetaOnRoot();
        show(moveReviewRetryHtml(message));
    };

    const syncStudy = (commentPath: string, originPath: string, commentary: string, lines: any[]) => {
        if (!commentary) return;
        const payload = { commentPath, originPath, commentary, variations: lines };
        rememberMoveReviewStudySync(payload);
        if (ctrl?.canWriteStudy()) ctrl.syncMoveReview(payload);
    };

    const runCurrentRequest = async (opts?: { force?: boolean }) => {
        const context = currentContext;
        if (!context) return;
        if (context.node.ply < MIN_MOVE_REVIEW_PLY) {
            setMoveReviewRefs(null);
            resetMetaOnRoot();
            show(moveReviewTooEarlyHtml(MIN_MOVE_REVIEW_PLY));
            return;
        }
        const force = !!opts?.force;
        if (requestsBlocked) {
            if (blockedHtml) show(blockedHtml);
            setMoveReviewRefs(null);
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
        const requestKey = context.cacheKey;
        activeRequestKey = requestKey;
        lastRequestedFen = context.fen;
        const isCurrentSession = () =>
            lastRequestedFen === context.fen && activeRequestKey === requestKey;

        try {
            setLoadingStage('position', isCurrentSession);

            const MIN_STRATEGIC_PV_PLIES = 5;
            const targetMultiPv = 5;

            let analysisEval: any = context.analysisCeval;
            let variations = evalToVariations(analysisEval, targetMultiPv) || [];

            if (!variations.length || variations[0].moves.length < MIN_STRATEGIC_PV_PLIES) {
                const startTime = Date.now();
                while (Date.now() - startTime < 1000) {
                    await new Promise(resolve => window.setTimeout(resolve, 200));
                    if (!isCurrentSession()) {
                        stopLoadingTicker();
                        return;
                    }
                    const updatedEval = context.playedMove ? context.nodes[context.nodes.length - 2]?.ceval : context.node.ceval;
                    const nextVariations = evalToVariations(updatedEval, targetMultiPv) || [];
                    if (nextVariations.length && nextVariations[0].moves.length >= MIN_STRATEGIC_PV_PLIES) {
                        variations = nextVariations;
                        break;
                    }
                }
            }

            if (
                context.playedMove &&
                !variations.some(v => Array.isArray(v.moves) && v.moves[0] === context.playedMove)
            ) {
                const existingPv = context.analysisCeval?.pvs?.find(
                    (pv: any) => Array.isArray(pv?.moves) && pv.moves[0] === context.playedMove,
                );
                const playedVar = {
                    moves: existingPv ? existingPv.moves.slice(0, 40) : [context.playedMove],
                    scoreCp: existingPv && typeof existingPv.cp === 'number' ? existingPv.cp : 0,
                    mate: existingPv && typeof existingPv.mate === 'number' ? existingPv.mate : null,
                    depth: existingPv && typeof existingPv.depth === 'number' ? existingPv.depth : 0,
                };
                variations = [...variations, playedVar].slice(0, targetMultiPv + 1);
            }

            const afterFen = context.playedMove ? context.fen : null;
            let afterVariations = afterFen ? evalToVariations(context.playedMove ? context.node.ceval : null, 1) : null;
            afterVariations = deriveAfterVariations(afterFen, afterVariations, context.playedMove, variations);

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
            const initialPayload = buildMoveReviewRequest({
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
            const res = await fetch(moveReviewEndpoint, {
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
                setMoveReviewRefs(null);
                show(blockedHtml);
                activeRequestKey = null;
                return;
            }

            const data = await res.json();
            const decoded = decodeMoveReviewResponse(data);
            const emittedToken = decoded.planStateToken;
            const emittedEndgameToken = decoded.endgameStateToken;
            if (emittedToken) planStateByPath.set(context.stateKey, emittedToken);
            else planStateByPath.delete(context.stateKey);
            if (emittedEndgameToken) endgameStateByPath.set(context.stateKey, emittedEndgameToken);
            else endgameStateByPath.delete(context.stateKey);

            const commentary = decoded.commentary;
            if (moveReviewNeedsRetry(decoded)) {
                activeRequestKey = null;
                return showRetry('The review took too long to finish. Retry for a clean explanation.');
            }
            const decoratedHtml = decorateDecodedMoveReviewHtml(decoded);
            const shouldStream = decoded.sourceMode === 'ai_polished' && commentary.length > 0;

            if (shouldStream) {
                await streamReveal(commentary, isCurrentSession);
                if (!isCurrentSession()) {
                    stopLoadingTicker();
                    return;
                }
            }

            const initialEntry: MoveReviewCacheEntry = buildStoredMoveReviewEntry(
                decoded,
                decoratedHtml,
                entryTokenContext(context),
            );
            cache.set(context.cacheKey, initialEntry);
            persistMoveReviewSnapshot(context, commentary, initialEntry);
            applyCachedEntry(initialEntry);

            const vLines = variationLinesFromResponse(data, variations);
            syncStudy(context.commentPath, context.originPath, commentary, vLines);
            activeRequestKey = null;
        } catch (err) {
            if (!isCurrentSession()) return;
            stopLoadingTicker();
            activeRequestKey = null;
            if (err instanceof DOMException && err.name === 'AbortError') return;
            showRetry();
        }
    };

    moveReviewRequestTrigger = runCurrentRequest;

    return (nodes: Tree.Node[]) => {
        const node = nodes[nodes.length - 1];
        if (!node?.fen) {
            currentContext = null;
            moveReviewRequestTrigger = runCurrentRequest;
            abortNetwork();
            stopLoadingTicker();
            lastRequestedFen = null;
            setMoveReviewRefs(null);
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
        const nextContext: CurrentMoveReviewContext = {
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
        moveReviewRequestTrigger = runCurrentRequest;

        if (!sameContext) {
            abortNetwork();
            stopLoadingTicker();
            lastRequestedFen = null;
        }

        if (requestsBlocked) {
            resetMetaOnRoot();
            setMoveReviewRefs(null);
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

export function moveReviewClear() {
    lastShownHtml = '';
    setMoveReviewRefs(null);
    clearMoveReviewPanel();
}

export function moveReviewRestore(ctrl?: AnalyseCtrl): void {
    setMoveReviewRefs(null);
    restoreMoveReviewPanel(lastShownHtml, ctrl?.getOrientation() ?? 'white', moveReviewEvalDisplay());
}
