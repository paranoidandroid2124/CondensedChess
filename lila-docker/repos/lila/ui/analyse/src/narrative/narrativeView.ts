import type { VNode } from 'snabbdom';
import type { NarrativeCtrl, DefeatDnaReport } from './narrativeCtrl';
import { hl, bind, dataIcon, onInsert } from 'lib/view';
import * as licon from 'lib/licon';
import { renderEval } from 'lib/ceval/util';
import { type BoardPreview, renderBoardPreview } from 'lib/view/boardPreview';
import { makeBoardFen, parseFen } from 'chessops/fen';
import { setupPosition } from 'chessops/variant';
import { lichessRules } from 'chessops/compat';
import { makeSanAndPlay } from 'chessops/san';
import { parseUci } from 'chessops/util';
import type { DrawShape } from '@lichess-org/chessground/draw';

type VariationLine = { moves: string[]; scoreCp: number; mate?: number | null; depth?: number; tags?: string[] };

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

export type EngineAlternative = {
    uci: string;
    san?: string;
    cpAfterAlt?: number;
    cpLossVsPlayed?: number;
    pv?: string[];
};

export type ActiveStrategicRouteRef = {
    routeId: string;
    piece: string;
    route: string[];
    purpose: string;
    confidence: number;
};

export type ActiveStrategicMoveRef = {
    label: string;
    source: string;
    uci: string;
    san?: string;
    fenAfter?: string;
};

type GameNarrativeMoment = {
    momentId?: string;
    ply: number;
    moveNumber?: number;
    side?: 'white' | 'black';
    moveClassification?: string;
    momentType: string;
    fen: string;
    narrative: string;
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
    strategicBranch?: boolean;
    activeStrategicNote?: string;
    activeStrategicSourceMode?: string;
    activeStrategicRoutes?: ActiveStrategicRouteRef[];
    activeStrategicMoves?: ActiveStrategicMoveRef[];
};
type GameNarrativeReview = {
    schemaVersion?: number;
    reviewPerspective?: string;
    totalPlies: number;
    evalCoveredPlies: number;
    evalCoveragePct: number;
    selectedMoments: number;
    selectedMomentPlies: number[];
    blundersCount?: number;
    missedWinsCount?: number;
    brilliantMovesCount?: number;
    momentTypeCounts?: Record<string, number>;
};
type GameNarrativeResponse = {
    schema: string;
    intro: string;
    moments: GameNarrativeMoment[];
    conclusion: string;
    themes: string[];
    review?: GameNarrativeReview;
    sourceMode?: string;
    model?: string | null;
    planTier?: string;
    llmLevel?: string;
};

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
                'Narrative Analysis'
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
                    ctrl.loading() ? hl('div.loader', ctrl.loadingDetail() || 'Deep full analysis in progress...') :
                        ctrl.error() ? hl('div.error', [
                            hl('div', ctrl.error()),
                            ctrl.needsLogin() ? hl('a.button', { attrs: { href: ctrl.loginHref() } }, 'Sign in') : null
                        ]) : ctrl.data() ? narrativeDocView(ctrl, ctrl.data()!) : hl('div.narrative-empty', 'No narrative generated yet.'),
                ]),
        !ctrl.loading() && !ctrl.data() && hl('div.actions', [
            hl('div.narrative-disclosure', 'Full Analysis runs a deeper on-device WASM scan and may take longer on large PGNs.'),
            hl('button.button.action', {
                hook: bind('click', ctrl.fetchNarrative, ctrl.root.redraw)
            }, 'Run Deep Full Analysis')
        ])
    ]);
}

// ── Collapse Tab ──────────────────────────────────────────────────────

function collapseTabView(ctrl: NarrativeCtrl): VNode {
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
        ...moments.map(m => narrativeCollapseCardView(ctrl, m)),
    ]);
}

function collapseTimelineView(ctrl: NarrativeCtrl, moments: GameNarrativeMoment[]): VNode {
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
                attrs: { title: `${c.rootCause} (ply ${c.interval})` },
                hook: bind('click', () => {
                    ctrl.root.jumpToMain(start);
                    ctrl.root.redraw();
                }),
            }),
        );

        // Earliest preventable ply marker (diamond)
        const preventPct = (c.earliestPreventablePly / totalPlies) * 100;
        markers.push(
            hl('div.timeline-marker', {
                style: { left: `${preventPct}%` },
                attrs: { title: `Preventable at ply ${c.earliestPreventablePly}` },
                hook: bind('click', () => {
                    ctrl.root.jumpToMain(c.earliestPreventablePly);
                    ctrl.root.redraw();
                }),
            }),
        );
    }

    return hl('div.collapse-timeline', [
        hl('div.timeline-track', [...segments, ...markers]),
        hl('div.timeline-labels', [
            hl('span', '1'),
            hl('span', String(totalPlies)),
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

function defeatDnaContentView(ctrl: NarrativeCtrl): VNode {
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
            hl('div.dna-stat-label', 'Avg Recovery (plies)'),
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
                    hl('td.dna-cell-interval', `Ply ${c.interval}`),
                    hl('td.dna-cell-cause', {
                        style: { color: CAUSE_COLORS[c.rootCause] || DEFAULT_CAUSE_COLOR }
                    }, c.rootCause),
                    hl('td.dna-cell-recov', `${c.recoverabilityPlies}p`),
                    hl('td.dna-cell-prevent', `Ply ${c.earliestPreventablePly}`),
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

function narrativeDocView(ctrl: NarrativeCtrl, doc: GameNarrativeResponse): VNode {
    return hl('div.narrative-doc', {
        hook: onInsert((el: HTMLElement) => bindPreviewHover(ctrl, el)),
    }, [
        hl('div.narrative-preview', [
            ctrl.pvBoard()
                ? renderBoardPreview(ctrl.pvBoard() as BoardPreview, ctrl.root.getOrientation())
                : hl('div.narrative-preview-empty', 'Hover a move to preview'),
        ]),
        narrativeReviewView(doc),
        doc.sourceMode || doc.model || doc.planTier || doc.llmLevel
            ? hl('div.narrative-review-metrics', [
                doc.sourceMode ? hl('span.narrative-review-metric', `Source: ${doc.sourceMode}`) : null,
                doc.model ? hl('span.narrative-review-metric', `Model: ${doc.model}`) : null,
                doc.planTier ? hl('span.narrative-review-metric', `Plan: ${doc.planTier}`) : null,
                doc.llmLevel ? hl('span.narrative-review-metric', `Level: ${doc.llmLevel}`) : null,
            ])
            : null,
        hl('div.narrative-intro', [
            hl('div.narrative-themes', doc.themes?.length ? doc.themes.map(t => hl('span.narrative-theme', t)) : null),
            hl('pre.narrative-prose', doc.intro),
        ]),
        ...(doc.moments || []).map(m => narrativeMomentView(ctrl, m)),
        hl('div.narrative-conclusion', [hl('pre.narrative-prose', doc.conclusion)]),
    ]);
}

function narrativeReviewView(doc: GameNarrativeResponse): VNode | null {
    const review = doc.review;
    if (!review) return null;

    const totalPlies = Math.max(0, review.totalPlies || 0);
    const evalCoveredPlies = Math.max(0, review.evalCoveredPlies || 0);
    const evalCoveragePct = Math.max(0, Math.min(100, review.evalCoveragePct || 0));
    const selectedMoments = Math.max(0, review.selectedMoments || 0);
    const selectedMomentPlies = (review.selectedMomentPlies || [])
        .map(p => Math.trunc(p))
        .filter(p => p > 0 && (totalPlies <= 0 || p <= totalPlies));

    const summary =
        totalPlies > 0
            ? `PGN span ${totalPlies} plies. Engine eval coverage ${evalCoveredPlies}/${totalPlies} (${evalCoveragePct}%).`
            : `Engine eval coverage ${evalCoveredPlies} plies.`;

    return hl('section.narrative-review', [
        hl('div.narrative-review-summary', summary),
        hl('div.narrative-review-metrics', [
            review.blundersCount !== undefined ? hl('span.narrative-review-metric.blunder', `Blunders: ${review.blundersCount}`) : null,
            review.missedWinsCount !== undefined ? hl('span.narrative-review-metric.missed', `Missed Wins: ${review.missedWinsCount}`) : null,
            hl('span.narrative-review-metric', `Selected moments: ${selectedMoments}`),
        ]),
        totalPlies > 0
            ? hl('div.narrative-review-timeline', [
                hl('div.narrative-review-track'),
                ...selectedMomentPlies.map((ply, idx) => {
                    const ratio = totalPlies <= 1 ? 0 : (ply - 1) / (totalPlies - 1);
                    const left = Math.max(0, Math.min(100, Math.round(ratio * 1000) / 10));
                    return hl('span.narrative-review-marker', {
                        key: `moment-${ply}-${idx}`,
                        attrs: { style: `left:${left}%;`, title: `Ply ${ply}` },
                    });
                }),
            ])
            : null,
    ]);
}

function narrativeMomentView(ctrl: NarrativeCtrl, moment: GameNarrativeMoment): VNode {
    const title = `Ply ${moment.ply}`;
    const variations = (moment.variations || []).filter(v => Array.isArray(v.moves) && v.moves.length);
    const hasStrategicBlock =
        !!moment.activeStrategicNote ||
        !!moment.activeStrategicMoves?.length ||
        !!moment.activeStrategicRoutes?.length;

    return hl('section.narrative-moment', {
        attrs: { 'data-ply': moment.ply }
    }, [
        hl('header.narrative-moment-header', [
            hl('div.narrative-moment-title-box', [
                hl('button.button.button-empty.narrative-jump', {
                    hook: bind(
                        'click',
                        () => {
                            ctrl.root.jumpToMain(moment.ply);
                            ctrl.root.redraw();
                        },
                        undefined,
                    ),
                }, title),
                moment.side ? hl(`span.narrative-side.${moment.side}`, moment.side) : null,
                moment.moveClassification ? narrativeBadgeView(moment.moveClassification, 'classification') : null,
                moment.momentType ? narrativeBadgeView(moment.momentType, 'type') : null,
                moment.strategicSalience ? narrativeBadgeView(moment.strategicSalience, 'salience') : null,
                moment.strategicBranch ? narrativeBadgeView('Strategic Branch', 'branch') : null,
            ]),
            moment.concepts?.length ? hl('div.narrative-concepts', moment.concepts.map(c => hl('span.narrative-concept', c))) : null,
        ]),
        narrativeProseView(ctrl, moment, moment.narrative, 'moment'),
        hasStrategicBlock ? narrativeStrategicNoteView(ctrl, moment) : null,
        moment.activePlan ? narrativeActivePlanView(moment.activePlan) : null,
        moment.collapse ? narrativeCollapseCardView(ctrl, moment) : null,
        moment.topEngineMove ? narrativeTopEngineMoveView(ctrl, moment) : null,
        variations.length
            ? hl('div.narrative-variations', [
                hl('h3', 'Variations'),
                hl('div.narrative-variation-list', variations.map((v, i) => narrativeVariationView(ctrl, moment.fen, v, i))),
            ])
            : null,
    ]);
}

function narrativeStrategicNoteView(ctrl: NarrativeCtrl, moment: GameNarrativeMoment): VNode {
    const note = moment.activeStrategicNote;
    const strategicMoves = (moment.activeStrategicMoves || []).filter(m => typeof m.uci === 'string' && m.uci.length >= 4);
    const strategicRoutes = (moment.activeStrategicRoutes || []).filter(r => Array.isArray(r.route) && r.route.length >= 2);

    return hl('div.narrative-strategic-note-box', [
        hl('h3.narrative-strategic-note-title', 'Strategic Note'),
        note ? narrativeProseView(ctrl, moment, note, 'note') : null,
        strategicMoves.length ? hl('div.narrative-strategic-moves', [
            hl('span.narrative-strategic-label', 'Mini-board moves:'),
            hl('div.narrative-strategic-move-list', strategicMoves.map((move, idx) => {
                const uci = move.uci.toLowerCase();
                const afterFen = move.fenAfter || '';
                const preview = afterFen && uci.length >= 4 ? `${afterFen}|${uci}` : null;
                return hl('span.narrative-strategic-chip', {
                    key: `${move.source}:${uci}:${idx}`,
                    attrs: preview ? { 'data-board': preview, title: move.label } : { title: move.label },
                }, move.san || uci);
            })),
        ]) : null,
        strategicRoutes.length ? hl('div.narrative-strategic-routes', [
            hl('span.narrative-strategic-label', 'Reroute paths:'),
            hl('div.narrative-strategic-route-list', strategicRoutes.map(route => {
                const routeText = route.route.join('-');
                const title = `${route.piece} ${route.purpose}`;
                return hl('span.narrative-strategic-chip.route', {
                    key: route.routeId,
                    attrs: {
                        'data-route': routeText,
                        'data-route-fen': moment.fen,
                        title,
                    },
                }, `${route.piece}${routeText} (${Math.round(route.confidence * 100)}%)`);
            })),
        ]) : null,
    ]);
}

function narrativeProseView(
    ctrl: NarrativeCtrl,
    moment: GameNarrativeMoment,
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

function buildInlineMoveRefMap(moment: GameNarrativeMoment, variantKey: string): Map<string, string> {
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
    moment: GameNarrativeMoment,
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

function narrativeCollapseCardView(ctrl: NarrativeCtrl, moment: GameNarrativeMoment): VNode | null {
    const collapse = moment.collapse;
    if (!collapse) return null;

    return hl('div.narrative-collapse-card', [
        hl('h3.narrative-collapse-title', [
            hl('span.icon', { attrs: { ...dataIcon(licon.Target) } }),
            ' Causal Collapse Analyzer'
        ]),
        hl('div.narrative-collapse-body', [
            hl('div.narrative-collapse-row', [
                hl('span.narrative-collapse-label', 'Collapse Interval:'),
                hl('span.narrative-collapse-value', `Ply ${collapse.interval}`)
            ]),
            hl('div.narrative-collapse-row', [
                hl('span.narrative-collapse-label', 'Root Cause:'),
                hl('span.narrative-collapse-value.cause', collapse.rootCause)
            ]),
            hl('div.narrative-collapse-row', [
                hl('span.narrative-collapse-label', 'Earliest Preventable:'),
                hl('button.button.button-empty.narrative-jump', {
                    hook: bind('click', () => {
                        ctrl.root.jumpToMain(collapse.earliestPreventablePly);
                        ctrl.root.redraw();
                    })
                }, `Ply ${collapse.earliestPreventablePly}`)
            ]),
            hl('div.narrative-collapse-row', [
                hl('span.narrative-collapse-label', 'Recoverability Window:'),
                hl('span.narrative-collapse-value', `${collapse.recoverabilityPlies} plies`)
            ]),
            patchReplayPanel(ctrl, moment),
        ])
    ]);
}

function patchReplayPanel(ctrl: NarrativeCtrl, moment: GameNarrativeMoment): VNode {
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
            }, `▶ Replay ${patchMoves.length} moves`),
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
                ctrl.root.jumpToMain(ply);
                ctrl.root.redraw();
            })
        }, `↗ Jump to Ply ${ply}`),
    ]);
}

function narrativeBadgeView(text: string, kind: 'classification' | 'type' | 'salience' | 'branch'): VNode {
    const cls = text.toLowerCase().replace(/\s+/g, '-');
    return hl(`span.narrative-badge.${kind}.${cls}`, text);
}

function narrativeTopEngineMoveView(ctrl: NarrativeCtrl, moment: GameNarrativeMoment): VNode | null {
    const alt = moment.topEngineMove;
    if (!alt) return null;

    return hl('div.narrative-top-engine-move', [
        hl('h3', 'Why Not?'),
        hl('div.narrative-alt-move', [
            hl('span.narrative-alt-label', 'Better was:'),
            hl('span.narrative-alt-san', alt.san || alt.uci),
            alt.cpLossVsPlayed !== undefined ? hl('span.narrative-alt-loss', `(+${(alt.cpLossVsPlayed / 100).toFixed(1)} pawns)`) : null,
        ]),
        alt.pv?.length ? hl('div.narrative-variation-moves', renderMoves(ctrl, moment.fen, alt.pv)) : null,
    ]);
}

function narrativeVariationView(ctrl: NarrativeCtrl, fen: string, line: VariationLine, index: number): VNode {
    const label = String.fromCharCode('A'.charCodeAt(0) + (index % 26));
    const score = typeof line.mate === 'number' ? `#${line.mate}` : renderEval(line.scoreCp);
    const tags = Array.isArray(line.tags) && line.tags.length ? line.tags : null;

    return hl('div.narrative-variation', [
        hl('div.narrative-variation-meta', [
            hl('span.narrative-variation-label', label),
            hl('span.narrative-variation-score', score),
            tags ? hl('span.narrative-variation-tags', tags.join(', ')) : null,
        ]),
        hl('div.narrative-variation-moves', renderMoves(ctrl, fen, line.moves)),
    ]);
}

function renderMoves(ctrl: NarrativeCtrl, fen: string, moves: string[]): Array<VNode | string> {
    const setup = parseFen(fen);
    if (!setup.isOk) return ['(invalid FEN)'];

    const pos = setupPosition(lichessRules(ctrl.root.data.game.variant.key), setup.value);
    if (!pos.isOk) return ['(invalid position)'];

    const vnodes: Array<VNode | string> = [];
    let key = makeBoardFen(pos.value.board);

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

        vnodes.push(
            hl('span.narrative-move', { key, attrs: { 'data-board': `${afterFen}|${uci}` } }, san),
        );
    }

    return vnodes;
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

function bindPreviewHover(ctrl: NarrativeCtrl, root: HTMLElement): void {
    const anyRoot: any = root;
    if (anyRoot._chesstoryNarrativeBound) return;
    anyRoot._chesstoryNarrativeBound = true;

    root.addEventListener('mouseover', (e: MouseEvent) => {
        const routeEl = (e.target as HTMLElement | null)?.closest?.('[data-route][data-route-fen]') as HTMLElement | null;
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
                return;
            }
        }

        const el = (e.target as HTMLElement | null)?.closest?.('[data-board]') as HTMLElement | null;
        const board = el?.dataset?.board;
        if (!board || !board.includes('|')) return;
        const [fen, uci] = board.split('|');
        if (!fen || !uci) return;
        ctrl.root.setNarrativeRouteOverlay(null);
        ctrl.pvBoard({ fen, uci });
        ctrl.root.redraw();
    });

    root.addEventListener('mouseleave', () => {
        ctrl.root.setNarrativeRouteOverlay(null);
        ctrl.pvBoard(null);
        ctrl.root.redraw();
    });
}
