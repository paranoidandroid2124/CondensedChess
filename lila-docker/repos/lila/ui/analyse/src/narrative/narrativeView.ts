import type { VNode } from 'snabbdom';
import type { NarrativeCtrl } from './narrativeCtrl';
import { hl, bind, dataIcon, onInsert } from 'lib/view';
import * as licon from 'lib/licon';
import { renderEval } from 'lib/ceval/util';
import { type BoardPreview, renderBoardPreview } from 'lib/view/boardPreview';
import { makeBoardFen, parseFen } from 'chessops/fen';
import { setupPosition } from 'chessops/variant';
import { lichessRules } from 'chessops/compat';
import { makeSanAndPlay } from 'chessops/san';
import { parseUci } from 'chessops/util';

type VariationLine = { moves: string[]; scoreCp: number; mate?: number | null; depth?: number; tags?: string[] };
type GameNarrativeMoment = {
    ply: number;
    momentType: string;
    fen: string;
    narrative: string;
    concepts: string[];
    variations: VariationLine[];
};
type GameNarrativeReview = {
    totalPlies: number;
    evalCoveredPlies: number;
    evalCoveragePct: number;
    selectedMoments: number;
    selectedMomentPlies: number[];
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
};

export function narrativeView(ctrl: NarrativeCtrl): VNode | null {
    if (!ctrl.enabled()) return null;

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
        hl('div.narrative-content', [
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
        doc.sourceMode || doc.model
            ? hl('div.narrative-review-metrics', [
                doc.sourceMode ? hl('span.narrative-review-metric', `Source: ${doc.sourceMode}`) : null,
                doc.model ? hl('span.narrative-review-metric', `Model: ${doc.model}`) : null,
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
            hl('span.narrative-review-metric', `Selected moments: ${selectedMoments}`),
            hl('span.narrative-review-metric', `Moment plies: ${selectedMomentPlies.join(', ') || 'none'}`),
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
    const title = `${moment.momentType} · ply ${moment.ply}`;
    const variations = (moment.variations || []).filter(v => Array.isArray(v.moves) && v.moves.length);

    return hl('section.narrative-moment', [
        hl('header.narrative-moment-header', [
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
            moment.concepts?.length ? hl('div.narrative-concepts', moment.concepts.map(c => hl('span.narrative-concept', c))) : null,
        ]),
        hl('pre.narrative-prose', moment.narrative),
        variations.length
            ? hl('div.narrative-variations', [
                hl('h3', 'Variations'),
                hl('div.narrative-variation-list', variations.map((v, i) => narrativeVariationView(ctrl, moment.fen, v, i))),
            ])
            : null,
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

function bindPreviewHover(ctrl: NarrativeCtrl, root: HTMLElement): void {
    const anyRoot: any = root;
    if (anyRoot._chesstoryNarrativeBound) return;
    anyRoot._chesstoryNarrativeBound = true;

    root.addEventListener('mouseover', (e: MouseEvent) => {
        const el = (e.target as HTMLElement | null)?.closest?.('[data-board]') as HTMLElement | null;
        const board = el?.dataset?.board;
        if (!board || !board.includes('|')) return;
        const [fen, uci] = board.split('|');
        if (!fen || !uci) return;
        ctrl.pvBoard({ fen, uci });
        ctrl.root.redraw();
    });

    root.addEventListener('mouseleave', () => {
        ctrl.pvBoard(null);
        ctrl.root.redraw();
    });
}
