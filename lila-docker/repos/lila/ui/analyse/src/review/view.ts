import type { LooseVNode, LooseVNodes, VNode } from 'lib/view';
import { bind, hl } from 'lib/view';
import type AnalyseCtrl from '../ctrl';
import { moveReviewToggleBox } from '../moveReview';
import type { ReviewPrimaryTab } from './state';

export type ReviewViewNodes = {
  cevalNode?: LooseVNodes;
  pvsNode?: LooseVNodes;
  moveListNode: VNode;
  forkNode?: LooseVNode;
  explorerNode?: LooseVNode;
  boardSettingsNodes: VNode[];
  importNode?: VNode;
};

const tabs: [ReviewPrimaryTab, string][] = [
  ['explain', 'Coach'],
  ['engine', 'Reference'],
  ['explorer', 'Explorer'],
  ['moves', 'Move list'],
  ['import', 'PGN'],
  ['board', 'Board setup'],
];

export function reviewView(ctrl: AnalyseCtrl, nodes: ReviewViewNodes): VNode {
  const active = normalizeTab(ctrl.reviewPrimaryTab());
  return hl('section.analyse-review.analyse-review--move-review-only', [
    hl('div.analyse-review__surface-switch', tabs.map(([tab, label]) => tabButton(ctrl, active, tab, label))),
    hl('div.analyse-review__body', [renderTab(ctrl, nodes, active)]),
  ]);
}

function normalizeTab(tab: ReviewPrimaryTab): ReviewPrimaryTab {
  return tabs.some(([candidate]) => candidate === tab) ? tab : 'explain';
}

function tabButton(ctrl: AnalyseCtrl, active: ReviewPrimaryTab, tab: ReviewPrimaryTab, label: string): VNode {
  return hl(
    `button.analyse-review__surface-toggle${active === tab ? '.active' : ''}`,
    {
      attrs: {
        type: 'button',
        'aria-pressed': active === tab ? 'true' : 'false',
      },
      hook: bind('click', () => ctrl.setReviewPrimaryTab(tab)),
    },
    label,
  );
}

function renderTab(ctrl: AnalyseCtrl, nodes: ReviewViewNodes, tab: ReviewPrimaryTab): VNode {
  switch (tab) {
    case 'engine':
      return panel('Reference lines', [nodes.cevalNode, nodes.pvsNode].filter(Boolean));
    case 'explorer':
      return panel('Explorer', [nodes.explorerNode]);
    case 'moves':
      return panel('Move list', [nodes.moveListNode, nodes.forkNode]);
    case 'import':
      return panel('PGN import', [nodes.importNode]);
    case 'board':
      return panel('Board setup', nodes.boardSettingsNodes);
    default:
      return panel('Coach review', [
        hl('div.analyse-review__explain-card', [
          moveReviewToggleBox(ctrl),
          nodes.cevalNode,
          nodes.pvsNode,
        ]),
      ]);
  }
}

function panel(title: string, content: LooseVNodes[]): VNode {
  return hl('div.analyse-review__workspace', [
    hl('header.analyse-review__workspace-head', [hl('span.analyse-review__eyebrow', 'Review player'), hl('h2', title)]),
    hl('div.analyse-review__workspace-body', content.length ? content : [hl('div.analyse-review__empty', 'Nothing to show here.')]),
  ]);
}
