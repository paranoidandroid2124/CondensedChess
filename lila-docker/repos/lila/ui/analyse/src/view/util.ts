import { plyToTurn } from 'lib/game/chess';
import {
  attributesModule,
  classModule,
  propsModule,
  eventListenersModule,
  init,
} from 'snabbdom';

export const patch = init([classModule, attributesModule, propsModule, eventListenersModule]);

export function nodeFullName(node: Tree.Node) {
  if (node.san) return plyToTurn(node.ply) + (node.ply % 2 === 1 ? '.' : '...') + ' ' + node.san;
  return 'Initial position';
}

export const plural = (noun: string, nb: number): string => nb + ' ' + (nb === 1 ? noun : noun + 's');
