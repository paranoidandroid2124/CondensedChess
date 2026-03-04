import { storedProp, type StoredProp } from 'lib/storage';
import type { ExplorerDb } from './interfaces';
import AnalyseCtrl from '../ctrl';

export interface ExplorerConfigData {
  db: StoredProp<ExplorerDb>;
}

export class ExplorerConfigCtrl {
  data: ExplorerConfigData;
  allDbs: ExplorerDb[];

  constructor(
    readonly root: AnalyseCtrl,
    readonly variant: VariantKey,
    previous?: ExplorerConfigCtrl,
  ) {
    this.allDbs = variant === 'standard' ? ['masters', 'lichess'] : ['lichess'];
    const allDbs = this.allDbs;
    const previousDb = previous?.data.db();
    const fallback = previousDb && allDbs.includes(previousDb) ? previousDb : allDbs[0];
    this.data = {
      db: storedProp<ExplorerDb>(
        'explorer.db2.' + variant,
        fallback,
        str => (allDbs.includes(str as ExplorerDb) ? (str as ExplorerDb) : allDbs[0]),
        v => v,
      ),
    };
  }
}
