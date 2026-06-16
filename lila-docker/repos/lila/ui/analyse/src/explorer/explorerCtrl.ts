import { type Prop, prop } from 'lib';
import { storedBooleanProp } from 'lib/storage';
import { pieceCount } from 'lib/game/chess';
import { debounce } from 'lib/async';
import * as xhr from './explorerXhr';
import { replayable } from 'lib/game';
import type AnalyseCtrl from '../ctrl';
import type { Hovering, ExplorerData, OpeningData, ExplorerOpts } from './interfaces';
import { clearLastShow } from './explorerView';

export const MAX_DEPTH = 50;

export default class ExplorerCtrl {
  allowed: Prop<boolean>;
  enabled: Prop<boolean>;
  private canUseTablebase: boolean;
  private effectiveVariant: VariantKey;

  loading = prop(true);
  failing = prop<Error | null>(null);
  hovering = prop<Hovering | null>(null);
  movesAway = prop(0);
  private abortController: AbortController | undefined;
  private cache: Partial<Record<string, ExplorerData>> = {};

  constructor(
    readonly root: AnalyseCtrl,
    readonly opts: ExplorerOpts,
    previous?: ExplorerCtrl,
  ) {
    this.allowed = prop(previous ? previous.allowed() : true);
    this.enabled = storedBooleanProp('analyse.explorer.enabled', false);
    this.canUseTablebase = root.synthetic || replayable(root.data) || !!root.data.opponent.ai;
    this.effectiveVariant =
      root.data.game.variant.key === 'fromPosition' ? 'standard' : root.data.game.variant.key;
    window.addEventListener('hashchange', this.checkHash, false);
    this.checkHash();
  }

  private checkHash = (e?: HashChangeEvent) => {
    const parts = location.hash.split('/');
    if (parts[0] === '#explorer' || parts[0] === '#opening') {
      this.enabled(true);
      if (e) this.reload();
    }
  };

  reload = () => {
    this.cache = {};
    this.setNode();
    this.root.redraw();
  };

  destroy = clearLastShow;

  private fetch = debounce(
    () => {
      const fen = this.root.node.fen;
      const processData = (res: ExplorerData) => {
        this.cache[fen] = res;
        this.movesAway(res.moves.length ? 0 : this.movesAway() + 1);
        this.loading(false);
        this.failing(null);
        this.root.redraw();
      };
      const onError = (err: Error) => {
        if (err.name !== 'AbortError') {
          this.loading(false);
          this.failing(err);
          this.root.redraw();
        }
      };
      this.abortController?.abort();
      this.abortController = new AbortController();
      if (this.canUseTablebase && this.tablebaseRelevant(fen))
        xhr
          .tablebase(this.opts.tablebaseEndpoint, fen, this.abortController.signal)
          .then(processData, onError);
      else {
        xhr
          .opening(
            {
              endpoint: this.opts.endpoint,
              variant: this.effectiveVariant,
              rootFen: this.root.nodeList[0].fen,
              play: this.root.nodeList.slice(1).map(s => s.uci!),
              fen,
            },
            processData,
            this.abortController.signal,
          )
          .catch(onError)
          .then(() => this.root.redraw());
      }
    },
    250,
    true,
  );

  private empty: OpeningData = {
    white: 0,
    black: 0,
    draws: 0,
    isOpening: true,
    moves: [],
    fen: '',
    opening: this.root.data.game.opening,
  };

  private tablebaseRelevant = (fen: FEN) => pieceCount(fen) <= 9 && this.root.isCevalAllowed();

  setNode = () => {
    if (!this.enabled()) return;
    const node = this.root.node;
    if (node.ply >= MAX_DEPTH && !this.tablebaseRelevant(node.fen)) this.cache[node.fen] = this.empty;
    const cached = this.cache[node.fen];
    if (cached) {
      this.movesAway(cached.moves.length ? 0 : this.movesAway() + 1);
      this.loading(false);
      this.failing(null);
    } else {
      this.loading(true);
      this.fetch();
    }
  };

  current = () => this.cache[this.root.node.fen];
  toggle = () => {
    this.movesAway(0);
    this.enabled(!this.enabled());
    this.setNode();
  };
  disable = () => {
    if (this.enabled()) {
      this.enabled(false);
    }
  };
  setHovering = (fen: FEN, uci: Uci | null) => {
    this.root.fork.hover(uci);
    this.hovering(uci ? { fen, uci } : null);
    this.root.setAutoShapes();
  };
}
