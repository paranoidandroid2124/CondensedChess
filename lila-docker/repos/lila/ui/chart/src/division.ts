import type { ChartDataset, Point } from 'chart.js';
import { chartYMax, chartYMin } from './index';
import type { Division } from './interface';

const siteI18n = ((window as any).i18n?.site ?? {}) as Record<string, string | undefined>;
const t = (key: string, fallback: string): string => siteI18n[key] || fallback;

export default function divisionDatasets(div?: Division): ChartDataset<'line'>[] {
  const lines: { div: string; loc: number }[] = [];
  if (div?.middle) {
    if (div.middle > 1) lines.push({ div: t('opening', 'Opening'), loc: 1 });
    lines.push({ div: t('middlegame', 'Middlegame'), loc: div.middle });
  }
  if (div?.end) {
    if (div.end > 1 && !div.middle) lines.push({ div: t('middlegame', 'Middlegame'), loc: 0 });
    lines.push({ div: t('endgame', 'Endgame'), loc: div.end });
  }
  const annotationColor = '#707070';

  return lines.map(line => ({
    type: 'line',
    xAxisID: 'x',
    yAxisID: 'y',
    label: line.div,
    data: [
      { x: line.loc, y: chartYMin },
      { x: line.loc, y: chartYMax },
    ],
    pointHoverRadius: 0,
    borderWidth: 1,
    borderColor: annotationColor,
    pointRadius: 0,
    order: 1,
    datalabels: {
      offset: -5,
      align: 45,
      rotation: 90,
      formatter: (val: Point) => (val.y && val.y > 0 ? line.div : ''),
    },
  }));
}
