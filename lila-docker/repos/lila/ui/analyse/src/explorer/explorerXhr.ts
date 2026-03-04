import type { ExplorerDb, OpeningData, TablebaseData } from './interfaces';
import * as xhr from 'lib/xhr';
import { readNdJson } from 'lib/xhr';

interface OpeningXhrOpts {
  endpoint: string;
  db: ExplorerDb;
  rootFen: FEN;
  play: string[];
  fen: FEN;
  variant?: VariantKey;
  withGames?: boolean;
}

export async function opening(
  opts: OpeningXhrOpts,
  processData: (data: OpeningData) => void,
  signal?: AbortSignal,
): Promise<void> {
  if (!opts.endpoint) throw new Error('Missing explorer endpoint');
  const endpoint = opts.endpoint.endsWith('/') ? opts.endpoint : `${opts.endpoint}/`;
  const url = new URL(opts.db, endpoint);
  const params = url.searchParams;
  params.set('variant', opts.variant || 'standard');
  params.set('fen', opts.rootFen);
  params.set('play', opts.play.join(','));
  if (!opts.withGames) {
    params.set('topGames', '0');
    params.set('recentGames', '0');
  }
  params.set('source', 'analysis');

  const res = await fetch(url.href, {
    cache: 'default',
    headers: {}, // avoid default headers for cors
    credentials: 'omit',
    signal,
  });

  await readNdJson<Partial<OpeningData>>(res, data => {
    data.isOpening = true;
    data.fen = opts.fen;
    processData(data as OpeningData);
  });
}

export async function tablebase(
  endpoint: string,
  variant: VariantKey,
  fen: FEN,
  signal?: AbortSignal,
): Promise<TablebaseData> {
  const effectiveVariant = variant === 'fromPosition' || variant === 'chess960' ? 'standard' : variant;
  const data = await xhr.json(xhr.url(`${endpoint}/${effectiveVariant}`, { fen }), {
    cache: 'default',
    headers: {}, // avoid default headers for cors
    credentials: 'omit',
    signal,
  });
  data.tablebase = true;
  data.fen = fen;
  return data as TablebaseData;
}
