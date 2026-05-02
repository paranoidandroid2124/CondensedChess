import { hl, type VNode } from 'lib/view';
import type { MoveExplanationCtrl } from './moveExplanation';
import { visibleMoveExplanationBlocks } from './moveExplanationSurface';

export function view(ctrl: MoveExplanationCtrl): VNode | undefined {
  const state = ctrl.state();
  if (state.kind !== 'ready') return;

  const blocks = visibleMoveExplanationBlocks(state);
  if (!blocks.length) return;

  return hl('section.move-explanation', [
    hl(
      'div.move-explanation__blocks',
      blocks.map(entry =>
        hl('article.move-explanation__block', { key: entry.key }, [
          entry.text ? hl('p.move-explanation__text', entry.text) : null,
          entry.line ? hl('p.move-explanation__line', [hl('san', entry.line)]) : null,
        ]),
      ),
    ),
  ]);
}
