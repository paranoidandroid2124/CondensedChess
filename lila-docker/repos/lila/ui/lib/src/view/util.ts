import { h, type VNode, type VNodeChildren } from 'snabbdom';
import { numberFormat } from '../format';

const ratio2percent = (r: number): string => Math.round(100 * r) + '%';

export function numberRow(name: string, value: number): VNode;
// should only be used for games percentage, due to title speaking about games
export function numberRow(name: string, value: [number, number], typ: 'percent'): VNode;
export function numberRow(name: string, value: VNodeChildren, typ: 'raw'): VNode;
export function numberRow(name: string, value: any, typ?: string): VNode {
  return h('tr', [
    h('th', name),
    h(
      'td',
      {
        attrs: typ === 'percent' ? { title: `${value[0]} games` } : {},
      },
      typ === 'raw'
        ? value
        : typ === 'percent'
          ? value[1] > 0
            ? ratio2percent(value[0] / value[1])
            : 0
          : numberFormat(value),
    ),
  ]);
}
