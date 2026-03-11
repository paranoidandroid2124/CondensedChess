import { playable, playedTurns, fenToEpd, readDests, readDrops, writeDests, validUci } from 'lib/game';
import * as keyboard from './keyboard';
import { treeReconstruct, plyColor } from './util';
import { plural } from './view/util';
import type { AnalyseOpts, AnalyseData, ServerEvalData, JustCaptured } from './interfaces';
import type { Api as ChessgroundApi } from '@lichess-org/chessground/api';
import { Autoplay, type AutoplayDelay } from './autoplay';
import { makeTree, treePath, treeOps, type TreeWrapper } from 'lib/tree';
import { compute as computeAutoShapes } from './autoShape';
import type { Config as ChessgroundConfig } from '@lichess-org/chessground/config';
import type { CevalHandler, EvalMeta, CevalOpts } from 'lib/ceval';
import { CevalCtrl, isEvalBetter, sanIrreversible } from 'lib/ceval';
import { TreeView } from './treeView/treeView';
import type { Prop, Toggle } from 'lib';
import { defined, prop, toggle, debounce, throttle, requestIdleCallback, propWithEffect, myUserId, myUsername } from 'lib';
import { pubsub } from 'lib/pubsub';
import type { DrawShape } from '@lichess-org/chessground/draw';
import { lichessRules, scalachessCharPair } from 'chessops/compat';
import EvalCache from './evalCache';
import { ForkCtrl } from './fork';
import type { Socket } from './socket';
import { nextGlyphSymbol, add3or5FoldGlyphs } from './nodeFinder';
import { opposite, parseUci, makeSquare, roleToChar } from 'chessops/util';
import { type Outcome, isNormal } from 'chessops/types';
import { makeFen, parseFen } from 'chessops/fen';
import type { Position, PositionError } from 'chessops/chess';
import type { Result } from '@badrap/result';
import { setupPosition } from 'chessops/variant';
import { makeSanAndPlay } from 'chessops/san';
import { makeUci } from 'chessops';
import { storedBooleanProp, storedProp, tempStorage } from 'lib/storage';
import { PromotionCtrl } from 'lib/game/promotion';
import { valid as crazyValid } from './crazy/crazyCtrl';
import bookmakerNarrative, { bookmakerClear, type BookmakerNarrative } from './bookmaker';
import ExplorerCtrl from './explorer/explorerCtrl';
import { uciToMove } from '@lichess-org/chessground/util';
import { IdbTree } from './idbTree';
import pgnImport from './pgnImport';
import { emptyPgnError, normalizeInlinePgn, submitPgnToImportPipeline } from './pgnPipeline';
import ForecastCtrl from './forecast/forecastCtrl';
import * as studyApi from './studyApi';

import type { PgnError } from 'chessops/pgn';

import { confirm } from 'lib/view';
import api from './api';
import { displayColumns } from 'lib/device';
import { dispatchChessgroundResize } from 'lib/chessgroundResize';
import * as Prefs from 'lib/prefs';
import {
  applyBoardLabelMode,
  boardLabelModeFromCoords,
  boardLabelModeToCoords,
  type BoardLabelMode,
} from './boardWorkspace';
import { make as makeNarrative, type GameNarrativeMoment, type NarrativeCtrl } from './narrative/narrativeCtrl';
import {
  initialReviewState,
  reduceReviewState,
  shouldFetchReviewPatterns,
  type NarrativeMomentFilter,
  type ReviewPrimaryTab,
  type ReviewReferenceTab,
  type ReviewUIAction,
  type ReviewUIState,
} from './review/state';

const reviewStateStorageKey = 'analyse.review-shell.v1';
const recentImportStorageKey = 'analyse.import-recents.v1';
const reviewPrimaryTabs = new Set<ReviewPrimaryTab>([
  'overview',
  'moments',
  'repair',
  'patterns',
  'moves',
  'reference',
]);
const reviewReferenceTabs = new Set<ReviewReferenceTab>(['explorer', 'board', 'import']);
const reviewMomentFilters = new Set<NarrativeMomentFilter>(['all', 'critical', 'collapses']);
const boardLabelModes = new Set<BoardLabelMode>(['off', 'inside', 'rim', 'full']);

export default class AnalyseCtrl implements CevalHandler {
  data: AnalyseData;
  element: HTMLElement;
  tree: TreeWrapper;
  socket: Socket;
  chessground: ChessgroundApi;
  ceval: CevalCtrl;
  evalCache: EvalCache;
  idbTree: IdbTree = new IdbTree(this);
  actionMenu: Toggle = toggle(false);
  isEmbed: boolean;

  // current tree state, cursor, and denormalized node lists
  path: Tree.Path;
  node: Tree.Node;
  nodeList: Tree.Node[];
  mainline: Tree.Node[];

  // sub controllers
  autoplay: Autoplay;
  explorer: ExplorerCtrl;
  forecast?: ForecastCtrl;
  fork: ForkCtrl;
  promotion: PromotionCtrl;

  bookmaker?: BookmakerNarrative;
  narrative?: NarrativeCtrl;

  // state flags
  justPlayed?: string; // pos
  justDropped?: string; // role
  justCaptured?: JustCaptured;
  redirecting = false;
  onMainline = true;
  synthetic: boolean; // false if coming from a real game
  ongoing: boolean; // true if real game is ongoing
  private cevalEnabledProp = storedBooleanProp('engine.enabled', false);

  // display flags
  flipped = false;
  showComments = true; // whether to display comments in the move tree
  variationArrowOpacity: Prop<number | false>;
  showGauge: Prop<boolean>;
  private showCevalProp: Prop<boolean> = storedBooleanProp('analyse.show-engine', !!this.cevalEnabledProp());
  private boardLabelModeProp!: Prop<BoardLabelMode>;
  private showCapturedProp!: Prop<boolean>;
  possiblyShowMoveAnnotationsOnBoard = storedBooleanProp('analyse.show-move-annotation', true);
  keyboardHelp: boolean = location.hash === '#keyboard';
  threatMode: Prop<boolean> = prop(false);
  disclosureMode = storedBooleanProp('analyse.disclosure.enabled', false);
  reviewState: Prop<ReviewUIState>;

  treeView: TreeView;
  cgVersion = {
    js: 1, // increment to recreate chessground
    dom: 1,
  };

  // underboard inputs
  fenInput?: string;
  pgnInput?: string;
  pgnError?: string;
  private recentImportDraftsCache?: string[];

  // study write queue (HTTP only, no sockets)
  private studyWriteQueue: Array<() => Promise<void>> = [];
  private studyWriting = false;
  studyWriteError?: string;

  // other paths
  initialPath: Tree.Path;
  contextMenuPath?: Tree.Path;
  gamePath?: Tree.Path;
  pendingCopyPath: Prop<Tree.Path | null>;
  pendingDeletionPath: Prop<Tree.Path | null>;

  // misc
  requestInitialPly?: number; // start ply from the URL location hash
  cgConfig: any; // latest chessground config (useful for revert)
  pvUciQueue: Uci[] = [];
  private narrativeRouteOverlay: { fen: FEN; shapes: DrawShape[] } | null = null;

  constructor(
    readonly opts: AnalyseOpts,
    readonly redraw: Redraw,
  ) {
    this.data = opts.data;
    this.element = opts.element;
    this.isEmbed = !!opts.embed;
    this.treeView = new TreeView(this);
    this.promotion = new PromotionCtrl(
      this.withCg,
      () => this.withCg(g => g.set(this.cgConfig)),
      this.redraw,
    );

    if (this.data.forecast) this.forecast = new ForecastCtrl(this.data.forecast, this.data, redraw);
    if (this.opts.bookmaker) this.bookmaker = bookmakerNarrative(this);

    this.narrative = makeNarrative(this);

    this.instanciateEvalCache();

    if (opts.inlinePgn) this.data = this.changePgn(opts.inlinePgn, false) || this.data;

    this.initialize(this.data, false);
    this.initWorkspacePrefs();
    this.syncWorkspacePrefs();
    this.initCeval();
    this.pendingCopyPath = propWithEffect(null, this.redraw);
    this.pendingDeletionPath = propWithEffect(null, this.redraw);
    this.reviewState = propWithEffect(this.loadStoredReviewState(), this.redraw);
    this.initialPath = this.makeInitialPath();
    this.setPath(this.initialPath);

    this.showGround();

    this.variationArrowOpacity = this.makeVariationOpacityProp();
    this.resetAutoShapes();
    this.explorer.setNode();
    this.explorer.setNode();
    this.refreshReviewShellState();

    if (location.hash === '#menu') requestIdleCallback(this.actionMenu.toggle, 500);
    this.startCeval();
    keyboard.bind(this);

    const urlEngine = new URLSearchParams(location.search).get('engine');
    if (urlEngine) {
      try {
        this.ceval.engines.select(urlEngine);
        this.cevalEnabled(true);
        this.threatMode(false);
      } catch (e) {
        console.info(e);
      }
      site.redirect('/analysis');
    }

    pubsub.on('jump', (ply: string) => {
      this.jumpToMain(parseInt(ply));
      this.redraw();
    });

    pubsub.on('ply.trigger', () =>
      pubsub.emit('ply', this.node.ply, this.tree.lastMainlineNode(this.path).ply === this.node.ply),
    );
    pubsub.on('analysis.chart.click', index => {
      this.jumpToIndex(index);
      this.redraw();
    });
    pubsub.on('board.change', (is3d: boolean) => {
      if (this.chessground) {
        this.chessground.state.addPieceZIndex = is3d;
        this.chessground.redrawAll();
        redraw();
      }
    });
    this.mergeIdbThenShowTreeView();
    const analysisApi = api(this);
    const globals = window as any;
    if (globals.chesstory) globals.chesstory.analysis = analysisApi;
  }

  private studyRef(): studyApi.StudyRef | null {
    const s = this.opts.study as { id?: string; chapterId?: string } | undefined;
    if (!s?.id || !s?.chapterId) return null;
    return { id: s.id, chapterId: s.chapterId };
  }

  canWriteStudy(): boolean {
    const s = this.opts.study as { canWrite?: boolean } | undefined;
    return !!s?.canWrite;
  }

  private enqueueStudyWrite(task: (ref: studyApi.StudyRef) => Promise<void>): void {
    if (!this.canWriteStudy() || this.studyWriteError) return;
    const ref = this.studyRef();
    if (!ref) return;

    // Bind the study/chapter at enqueue-time so chapter switches don't corrupt queued writes.
    const bound = () => task(ref);
    this.studyWriteQueue.push(bound);
    if (!this.studyWriting) void this.flushStudyWrites();
    this.redraw();
  }

  private async flushStudyWrites(): Promise<void> {
    if (this.studyWriting) return;
    this.studyWriting = true;
    this.redraw();
    try {
      while (this.studyWriteQueue.length) {
        const task = this.studyWriteQueue.shift();
        if (task) await task();
      }
    } catch (e) {
      this.studyWriteQueue = [];
      this.studyWriteError = e instanceof Error ? e.message : String(e);
      console.warn('Study sync failed', e);
    } finally {
      this.studyWriting = false;
      this.redraw();
    }
  }

  isStudyWriting(): boolean {
    return this.studyWriting || this.studyWriteQueue.length > 0;
  }

  studyCommentText(path: Tree.Path): string {
    const uid = myUserId();
    if (!uid) return '';
    const node = this.tree.nodeAtPath(path);
    const comment = node?.comments?.find(c => typeof c.by === 'object' && c.by.id === uid);
    return comment?.text || '';
  }

  setStudyComment(path: Tree.Path, text: string): void {
    const uid = myUserId();
    const normalized = text.trim().length ? text : '';
    if (uid) {
      const name = myUsername() || uid;
      this.tree.updateAt(path, node => {
        const comments = (node.comments || []).slice();
        const idx = comments.findIndex(c => typeof c.by === 'object' && c.by.id === uid);
        if (!normalized) {
          if (idx >= 0) comments.splice(idx, 1);
        } else if (idx >= 0) {
          comments[idx].text = normalized;
        } else {
          comments.push({
            id: `local-${Date.now()}`,
            by: { id: uid, name },
            text: normalized,
          });
        }
        node.comments = comments.length ? comments : undefined;
      });
      this.redraw();
    }

    this.enqueueStudyWrite(async ref => {
      const res = await studyApi.setNodeComment(ref, path, text);
      this.tree.updateAt(res.path as Tree.Path, node => {
        node.comments = res.node.comments;
      });
      this.redraw();
    });
  }

  syncBookmaker(payload: studyApi.BookmakerSyncPayload): void {
    this.enqueueStudyWrite(ref => studyApi.bookmakerSync(ref, payload));
  }

  initialize(data: AnalyseData, merge: boolean): void {
    this.data = data;
    this.synthetic = data.game.id === 'synthetic';
    this.ongoing = !this.synthetic && playable(data);
    this.treeView.hidden = true;
    const prevTree = merge && this.tree.root;
    this.tree = makeTree(treeReconstruct(this.data.treeParts, this.data.sidelines));
    if (prevTree) this.tree.merge(prevTree);
    const mainline = treeOps.mainlineNodeList(this.tree.root);
    if (this.data.game.status.name === 'draw') {
      if (add3or5FoldGlyphs(mainline)) this.data.game.threefold = true;
    }

    this.autoplay = new Autoplay(this);
    if (this.socket) this.socket.clearCache();
    else this.socket = this.makeLocalSocket();
    if (this.explorer) this.explorer.destroy();
    this.explorer = new ExplorerCtrl(this, this.opts.explorer, this.explorer);
    this.gamePath = this.synthetic || this.ongoing ? undefined : treePath.fromNodeList(mainline);
    this.fork = new ForkCtrl(this);

    site.sound.preloadBoardSounds();
  }

  private makeInitialPath = (): string => {
    // if correspondence, always use latest actual move to set 'current' style
    if (this.ongoing) return treePath.fromNodeList(treeOps.mainlineNodeList(this.tree.root));
    const loc = window.location,
      hashPly = loc.hash === '#last' ? this.tree.lastPly() : parseInt(loc.hash.slice(1)),
      startPly = hashPly >= 0 ? hashPly : this.opts.inlinePgn ? this.tree.lastPly() : undefined;
    if (defined(startPly)) {
      // remove location hash - https://stackoverflow.com/questions/1397329/how-to-remove-the-hash-from-window-location-with-javascript-without-page-refresh/5298684#5298684
      window.history.replaceState(null, '', loc.pathname + loc.search);
      this.requestInitialPly = startPly;
      const mainline = treeOps.mainlineNodeList(this.tree.root);
      return treeOps.takePathWhile(mainline, n => n.ply <= startPly);
    } else return treePath.root;
  };

  enableBookmaker = (v: boolean) => {
    this.bookmaker = v ? bookmakerNarrative(this) : undefined;
    if (this.bookmaker) this.bookmaker(this.nodeList);
    else bookmakerClear();
  };

  private setPath = (path: Tree.Path): void => {
    this.path = path;
    this.nodeList = this.tree.getNodeList(path);
    this.node = treeOps.last(this.nodeList) as Tree.Node;
    this.mainline = treeOps.mainlineNodeList(this.tree.root);
    this.onMainline = this.tree.pathIsMainline(path);
    this.fenInput = undefined;
    this.pgnInput = undefined;
    if (this.bookmaker) this.bookmaker(this.nodeList);
    this.idbTree.saveMoves();
    this.idbTree.revealNode();
  };

  flip = () => {
    this.flipped = !this.flipped;
    this.chessground?.set({
      orientation: this.bottomColor(),
    });
    this.explorer.onFlip();
    this.onChange();
    this.redraw();
  };

  topColor(): Color {
    return opposite(this.bottomColor());
  }

  bottomColor(): Color {
    if (this.data.game.variant.key === 'racingKings') return this.flipped ? 'black' : 'white';
    return this.flipped ? opposite(this.data.orientation) : this.data.orientation;
  }

  bottomIsWhite = () => this.bottomColor() === 'white';

  getOrientation(): Color {
    return this.bottomColor();
  }

  getNode(): Tree.Node {
    return this.node;
  }

  turnColor(): Color {
    return plyColor(this.node.ply);
  }

  togglePlay(delay: AutoplayDelay): void {
    this.autoplay.toggle(delay);
    this.actionMenu(false);
  }

  private showGround(): void {
    this.onChange();
    if (!defined(this.node.dests)) this.getDests();
    this.withCg(cg => {
      cg.set(this.makeCgOpts());
      this.setAutoShapes();
      if (this.node.shapes) cg.setShapes(this.node.shapes.slice() as DrawShape[]);
    });
  }

  private getDests: () => void = throttle(800, () => {
    if (defined(this.node.dests)) return;
    const path = this.path;
    this.position(this.node).unwrap(
      pos => {
        const dests = new Map<Key, Key[]>();
        for (const [orig, destSet] of pos.allDests()) {
          dests.set(makeSquare(orig) as Key, Array.from(destSet, makeSquare) as Key[]);
        }
        this.addDests(writeDests(dests), path);

        const dropSet = pos.dropDests();
        if (dropSet.nonEmpty())
          this.tree.updateAt(path, n => {
            n.drops = Array.from(dropSet, makeSquare).join('');
          });
      },
      _ => this.addDests('', path),
    );
  });

  serverMainline = () => this.mainline.slice(0, playedTurns(this.data) + 1);

  makeCgOpts(): ChessgroundConfig {
    const node = this.node,
      color = this.turnColor(),
      dests = readDests(this.node.dests),
      drops = readDrops(this.node.drops),
      movableColor =
        (dests && dests.size > 0) || drops === null || drops.length
          ? color
          : undefined,
      config: ChessgroundConfig = {
        fen: node.fen,
        turnColor: color,
        movable: {
          color: movableColor,
          dests: (movableColor === color && dests) || new Map(),
        },
        check: !!node.check,
        lastMove: uciToMove(node.uci),
      };
    if (!dests && !node.check) {
      // premove while dests are loading from server
      // can't use when in check because it highlights the wrong king
      config.turnColor = opposite(color);
      config.movable!.color = color;
    }
    config.premovable = {
      enabled: config.movable!.color && config.turnColor !== config.movable!.color,
    };
    this.cgConfig = config;
    return config;
  }

  setChessground = (cg: CgApi) => {
    this.chessground = cg;

    this.setAutoShapes();
    if (this.node.shapes) this.chessground.setShapes(this.node.shapes.slice() as DrawShape[]);
    this.cgVersion.dom = this.cgVersion.js;
  };

  private onChange: () => void = throttle(300, () => {
    pubsub.emit('analysis.change', this.node.fen, this.path);
  });

  private updateHref: () => void = debounce(() => {
    if (!this.opts.study) window.history.replaceState(null, '', '#' + this.node.ply);
  }, 750);

  playedLastMoveMyself = () =>
    !!this.justPlayed && !!this.node.uci && this.node.uci.startsWith(this.justPlayed);

  jump(path: Tree.Path): void {
    const pathChanged = path !== this.path,
      isForwardStep = pathChanged && path.length === this.path.length + 2;
    if (this.path !== path)
      this.treeView.requestAutoScroll(treeOps.distance(this.path, path) > 8 ? 'instant' : 'smooth');
    this.setPath(path);
    if (pathChanged) {
      if (isForwardStep) site.sound.move(this.node);
      this.threatMode(false);
      this.ceval?.stop();
      this.startCeval();
      site.sound.saySan(this.node.san, true);
    }
    this.justPlayed = this.justDropped = this.justCaptured = undefined;
    this.explorer.setNode();
    this.updateHref();
    this.promotion.cancel();
    pubsub.emit('ply', this.node.ply, this.tree.lastMainlineNode(this.path).ply === this.node.ply);
    this.showGround();
    this.pluginUpdate(this.node.fen);
    this.syncReviewSelectionFromNode();
  }

  userJump = (path: Tree.Path): void => {
    this.autoplay.stop();
    this.withCg(cg => cg.selectSquare(null));
    this.jump(path);
  };

  canJumpTo = (_path: Tree.Path): boolean => true;

  userJumpIfCan(path: Tree.Path, sideStep = false): void {
    if (path === this.path || !this.canJumpTo(path)) return;
    if (sideStep) {
      // when stepping lines, anchor the chessground animation at the parent
      this.node = this.tree.nodeAtPath(path.slice(0, -2));
      this.chessground?.set(this.makeCgOpts());
      this.chessground?.state.dom.redrawNow(true);
    }
    this.userJump(path);
  }

  mainlinePlyToPath(ply: Ply): Tree.Path {
    return treeOps.takePathWhile(this.mainline, n => n.ply <= ply);
  }

  jumpToMain = (ply: Ply): void => {
    this.userJump(this.mainlinePlyToPath(ply));
  };

  jumpToIndex = (index: number): void => {
    this.jumpToMain(index + 1 + this.tree.root.ply);
  };

  jumpToGlyphSymbol(color: Color, symbol: string): void {
    const node = nextGlyphSymbol(color, symbol, this.mainline, this.node.ply);
    if (node) this.jumpToMain(node.ply);
    this.redraw();
  }

  reloadData(data: AnalyseData, merge: boolean): void {
    this.initialize(data, merge);
    this.syncWorkspacePrefs();
    this.redirecting = false;
    this.setPath(treePath.root);
    this.refreshReviewShellState();
    this.initCeval();
    this.instanciateEvalCache();
    this.cgVersion.js++;
    this.mergeIdbThenShowTreeView();
  }

  changePgn(pgn: string, andReload: boolean): AnalyseData | undefined {
    this.pgnError = '';
    const normalized = normalizeInlinePgn(pgn);
    if (!normalized) {
      this.pgnError = emptyPgnError;
      requestAnimationFrame(this.redraw);
      return undefined;
    }
    try {
      const data: AnalyseData = {
        ...pgnImport(normalized),
        orientation: this.bottomColor(),
        pref: this.data.pref,
        externalEngines: this.data.externalEngines,
      } as AnalyseData;
      if (andReload) {
        this.reloadData(data, false);
        this.userJump(this.mainlinePlyToPath(this.tree.lastPly()));
        this.redraw();
      }
      return data;
    } catch (err) {
      this.pgnError = (err as PgnError).message;
      requestAnimationFrame(this.redraw);
    }
    return undefined;
  }

  importPgn(rawPgn: string): boolean {
    this.pgnError = '';
    if (!submitPgnToImportPipeline(rawPgn)) {
      this.pgnError = emptyPgnError;
      requestAnimationFrame(this.redraw);
      return false;
    }
    this.rememberRecentImportDraft(rawPgn);
    this.redirecting = true;
    this.redraw();
    return true;
  }

  changeFen(fen: FEN): void {
    this.redirecting = true;
    window.location.href =
      '/analysis/' +
      this.data.game.variant.key +
      '/' +
      encodeURIComponent(fen).replace(/%20/g, '_').replace(/%2F/g, '/');
  }

  crazyValid = (role: Role, key: Key): boolean => {
    const color = this.chessground.state.movable.color;
    return (
      (color === 'white' || color === 'black') &&
      crazyValid(this.chessground, this.node.drops, { color, role }, key)
    );
  };

  getCrazyhousePockets = () => this.node.crazy?.pockets;

  sendNewPiece = (role: Role, key: Key): void => {
    const color = this.chessground.state.movable.color;
    if (color === 'white' || color === 'black') this.userNewPiece({ color, role }, key);
  };

  userNewPiece = (piece: Piece, pos: Key): void => {
    if (crazyValid(this.chessground, this.node.drops, piece, pos)) {
      const before = { fen: this.node.fen, path: this.path };
      this.justPlayed = roleToChar(piece.role).toUpperCase() + '@' + pos;
      this.justDropped = piece.role;
      this.justCaptured = undefined;
      site.sound.move();
      this.applyUci(this.justPlayed as Uci);
      this.redraw();
      if (this.path !== before.path) {
        this.enqueueStudyWrite(ref =>
          studyApi.anaDrop(ref, {
            role: piece.role,
            pos,
            fen: before.fen,
            path: before.path,
            variant: this.data.game.variant.key,
            ch: ref.chapterId,
          }).then(() => { }),
        );
      }
    } else this.jump(this.path);
  };

  userMove = (orig: Key, dest: Key, capture?: JustCaptured): void => {
    this.justPlayed = orig;
    this.justDropped = undefined;
    if (
      !this.promotion.start(orig, dest, {
        submit: (orig, dest, prom) => this.sendMove(orig, dest, capture, prom),
      })
    )
      this.sendMove(orig, dest, capture);
  };

  sendMove = (orig: Key, dest: Key, capture?: JustCaptured, prom?: Role): void => {
    if (capture) this.justCaptured = capture;
    const before = { fen: this.node.fen, path: this.path };
    const uci = (orig + dest + (prom ? roleToChar(prom) : '')) as Uci;
    this.applyUci(uci);
    this.redraw();
    if (this.path !== before.path) {
      this.enqueueStudyWrite(ref =>
        studyApi.anaMove(ref, {
          orig,
          dest,
          fen: before.fen,
          path: before.path,
          variant: this.data.game.variant.key,
          promotion: prom,
          ch: ref.chapterId,
        }).then(() => { }),
      );
    }
  };

  onPremoveSet = () => { };

  addNode(node: Tree.Node, path: Tree.Path) {
    this.idbTree.onAddNode(node, path);
    const newPath = this.tree.addNode(node, path);
    if (!newPath) {
      console.log("Can't addNode", node, path);
      return this.redraw();
    }

    this.jump(newPath);

    this.redraw();
    const queuedUci = this.pvUciQueue.shift();
    if (queuedUci) this.playUci(queuedUci, this.pvUciQueue);
    else this.chessground.playPremove();
  }

  addDests(dests: string, path: Tree.Path): void {
    this.tree.addDests(dests, path);
    if (path === this.path) {
      this.showGround();
      this.pluginUpdate(this.node.fen);
      if (this.outcome()) this.ceval.stop();
    }
    this.withCg(cg => cg.playPremove());
  }

  async deleteNode(path: Tree.Path): Promise<void> {
    this.pendingDeletionPath(null);
    const node = this.tree.nodeAtPath(path);
    if (!node) return;
    const count = treeOps.countChildrenAndComments(node);
    if (
      (count.nodes >= 10 || count.comments > 0) &&
      !(await confirm(
        'Delete ' +
        plural('move', count.nodes) +
        (count.comments ? ' and ' + plural('comment', count.comments) : '') +
        '?',
      ))
    )
      return;
    if (path) this.enqueueStudyWrite(ref => studyApi.deleteNode(ref, path));
    this.tree.deleteNodeAt(path);
    if (treePath.contains(this.path, path)) this.userJump(treePath.init(path));
    else this.jump(this.path);
    this.redraw();
  }

  allowedEval(node: Tree.Node = this.node): Tree.ClientEval | Tree.ServerEval | false | undefined {
    return this.cevalEnabled() ? node.ceval : false;
  }

  outcome(node?: Tree.Node): Outcome | undefined {
    return this.position(node || this.node).unwrap(
      pos => pos.outcome(),
      _ => undefined,
    );
  }

  position(node: Tree.Node): Result<Position, PositionError> {
    const setup = parseFen(node.fen).unwrap();
    return setupPosition(lichessRules(this.data.game.variant.key), setup);
  }

  private makeLocalSocket(): Socket {
    return {
      send: this.opts.socketSend,
      receive: () => false,
      sendAnaMove: () => { },
      sendAnaDrop: () => { },
      sendAnaDests: () => { },
      clearCache: () => { },
    };
  }

  private applyUci(uci: Uci): void {
    const path = this.path;
    this.position(this.node).unwrap(
      pos => {
        const move = parseUci(uci);
        if (!move || !pos.isLegal(move)) return this.jump(path);

        const ply = this.node.ply + 1;
        const san = makeSanAndPlay(pos, move);
        const setup = pos.toSetup();
        const node: Tree.Node = {
          id: scalachessCharPair(move),
          ply,
          san,
          fen: makeFen(setup),
          uci: makeUci(move),
          children: [],
          check: pos.isCheck() ? makeSquare(setup.board.kingOf(pos.turn)!) : undefined,
        };
        this.addNode(node, path);
      },
      _ => this.jump(path),
    );
  }

  promote(path: Tree.Path, toMainline: boolean): void {
    if (path) this.enqueueStudyWrite(ref => studyApi.promoteNode(ref, path, toMainline));
    this.tree.promoteAt(path, toMainline);
    this.jump(path);
  }

  forceVariation(path: Tree.Path, force: boolean): void {
    if (path) this.enqueueStudyWrite(ref => studyApi.forceVariationNode(ref, path, force));
    this.tree.forceVariationAt(path, force);
    this.jump(path);
  }

  visibleChildren(node = this.node): Tree.Node[] {
    return node.children.filter(kid => !kid.comp);
  }

  reset(): void {
    this.showGround();
    this.redraw();
  }

  encodeNodeFen(): FEN {
    return this.node.fen.replace(/\s/g, '_');
  }

  nextNodeBest() {
    return treeOps.withMainlineChild(this.node, (n: Tree.Node) => validUci(n.eval?.best));
  }

  setAutoShapes = (): void => {
    if (!site.blindMode) this.chessground?.setAutoShapes(computeAutoShapes(this));
  };

  setNarrativeRouteOverlay = (overlay: { fen: FEN; shapes: DrawShape[] } | null): void => {
    this.narrativeRouteOverlay = overlay;
    this.setAutoShapes();
    this.redraw();
  };

  narrativeRouteOverlayShapes = (): DrawShape[] =>
    this.narrativeRouteOverlay?.fen === this.node.fen ? this.narrativeRouteOverlay.shapes : [];

  private onNewCeval = (ev: Tree.ClientEval, path: Tree.Path, isThreat?: boolean): void => {
    this.tree.updateAt(path, (node: Tree.Node) => {
      if (node.fen !== ev.fen && !isThreat) return;

      if (isThreat) {
        const threat = ev as Tree.LocalEval;
        if (!node.threat || isEvalBetter(threat, node.threat)) node.threat = threat;
      } else if ((!node.ceval || isEvalBetter(ev, node.ceval)) && !(ev.cloud && this.ceval.engines.external))
        node.ceval = ev;
      else if (!ev.cloud) {
        if (node.ceval?.cloud && this.ceval.isDeeper()) node.ceval = ev;
      }

      if (path === this.path) {
        this.setAutoShapes();
        if (!isThreat) {
          this.evalCache.onLocalCeval();
        }
        this.redraw();
      }
    });
  };

  private initCeval(): void {
    const opts: CevalOpts = {
      variant: this.data.game.variant,
      initialFen: this.data.game.initialFen,
      emit: (ev: Tree.ClientEval, work: EvalMeta) => this.onNewCeval(ev, work.path, work.threatMode),
      onUciHover: this.setAutoShapes,
      redraw: this.redraw,
      externalEngines:
        this.data.externalEngines?.map(engine => ({
          ...engine,
          endpoint: this.opts.externalEngineEndpoint,
        })) || [],
      onSelectEngine: () => {
        this.initCeval();
        this.redraw();
      },
    };
    if (this.ceval) this.ceval.init(opts);
    else this.ceval = new CevalCtrl(opts);
  }

  isCevalAllowed = () =>
    !this.ongoing &&
    (this.synthetic || !playable(this.data)) &&
    !location.search.includes('evals=0');

  cevalEnabled = (enable?: boolean): boolean | 'force' => {
    const state = this.cevalEnabledProp() && this.isCevalAllowed() && !this.ceval.isPaused;
    if (enable === undefined) return state;
    this.showCevalProp(enable);
    this.cevalEnabledProp(enable);
    if (enable && this.ceval.isPaused) this.ceval.resume();
    if (enable !== state) {
      if (enable) this.startCeval();
      else {
        this.threatMode(false);
        this.ceval.stop();
      }
      this.setAutoShapes();
      this.ceval.showEnginePrefs(false);
      this.redraw();
    }
    return enable;
  };

  startCeval = () => {
    if (!this.ceval.download) this.ceval.stop();
    if (this.node.threefold || !this.cevalEnabled() || this.outcome()) return;
    this.ceval.start(this.path, this.nodeList, undefined, this.threatMode());
    this.evalCache.fetch(this.path, this.ceval.search.multiPv);
  };

  clearCeval(): void {
    this.tree.removeCeval();
    this.evalCache.clear();
    this.startCeval();
  }

  showVariationArrows() {
    if (!this.allowLines()) return false;
    const kids = this.variationArrowOpacity() ? this.node.children : [];
    return Boolean(kids.filter(x => !x.comp).length);
  }

  showAnalysis() {
    return this.cevalEnabled() && this.isCevalAllowed();
  }

  showMoveGlyphs = (): boolean => true;

  showMoveAnnotationsOnBoard = (): boolean =>
    this.possiblyShowMoveAnnotationsOnBoard() && this.showMoveGlyphs();

  showEvalGauge(): boolean {
    return (
      this.showGauge() &&
      displayColumns() > 1 &&
      this.showAnalysis() &&
      this.isCevalAllowed() &&
      !this.outcome()
    );
  }

  boardLabelMode = (): BoardLabelMode => this.boardLabelModeProp();

  boardCoords = (): Prefs.Coords => boardLabelModeToCoords(this.boardLabelModeProp());

  setBoardLabelMode = (mode: BoardLabelMode): void => {
    if (!boardLabelModes.has(mode)) return;
    this.boardLabelModeProp(mode);
    this.syncWorkspacePrefs();
    applyBoardLabelMode(this.chessground, mode);
    requestAnimationFrame(dispatchChessgroundResize);
    this.redraw();
  };

  showCapturedMaterial = (): boolean => this.showCapturedProp();

  setShowCapturedMaterial = (show: boolean): void => {
    this.showCapturedProp(show);
    this.syncWorkspacePrefs();
    requestAnimationFrame(dispatchChessgroundResize);
    this.redraw();
  };

  setShowEvalGauge = (show: boolean): void => {
    this.showGauge(show);
    requestAnimationFrame(dispatchChessgroundResize);
    this.redraw();
  };

  showEnginePanel = (): boolean => (this.isReviewShell() ? this.showCevalProp() : this.showCeval());

  setShowEnginePanel = (show: boolean): void => {
    this.ceval.showEnginePrefs(false);
    this.showCevalProp(show);
    if (show) this.cevalEnabled(true);
    this.redraw();
  };

  resetOrientation = (): void => {
    if (!this.flipped) return;
    this.flipped = false;
    this.chessground?.set({
      orientation: this.bottomColor(),
    });
    this.explorer.onFlip();
    this.onChange();
    this.redraw();
  };

  showCeval = (show?: boolean) => {
    const barMode = this.activeControlMode();
    if (show === undefined) return displayColumns() > 1 || barMode === 'ceval';
    this.ceval.showEnginePrefs(false);
    this.showCevalProp(show);
    if (show) this.cevalEnabled(true);
    return show;
  };

  activeControlMode = () =>
    this.showCevalProp() ? 'ceval' : false;

  isReviewShell = (): boolean => !!this.data.userAnalysis && !this.opts.study;

  reviewPrimaryTab = (): ReviewPrimaryTab => this.reviewState().primaryTab;

  reviewReferenceTab = (): ReviewReferenceTab => this.reviewState().referenceTab;

  reviewMomentFilter = (): NarrativeMomentFilter => this.reviewState().momentFilter;

  selectedReviewMomentPly = (): Ply | null => this.reviewState().selectedMomentPly;

  selectedReviewCollapseId = (): string | null => this.reviewState().selectedCollapseId;

  private initWorkspacePrefs() {
    const defaultBoardLabelMode = boardLabelModeFromCoords(this.data.pref.coords);
    this.boardLabelModeProp = storedProp<BoardLabelMode>(
      'analyse.board-view.labels',
      defaultBoardLabelMode,
      str => (boardLabelModes.has(str as BoardLabelMode) ? (str as BoardLabelMode) : defaultBoardLabelMode),
      v => v,
    );
    this.showCapturedProp = storedBooleanProp('analyse.board-view.material', !!this.data.pref.showCaptured);
    this.showGauge = storedBooleanProp('analyse.board-view.gauge', true);
  }

  private syncWorkspacePrefs() {
    this.data.pref.coords = this.boardCoords();
    this.data.pref.showCaptured = this.showCapturedMaterial();
  }

  private rememberRecentImportDraft(rawPgn: string) {
    const normalized = normalizeInlinePgn(rawPgn);
    if (!normalized) return;
    const next = [normalized, ...this.recentImportDrafts().filter(pgn => pgn !== normalized)].slice(0, 4);
    this.recentImportDraftsCache = next;
    tempStorage.set(recentImportStorageKey, JSON.stringify(next));
  }

  private loadStoredReviewState(): ReviewUIState {
    const fallback = initialReviewState();
    const raw = tempStorage.get(reviewStateStorageKey);
    if (!raw) return fallback;
    try {
      const parsed = JSON.parse(raw) as Partial<ReviewUIState>;
      return {
        primaryTab: reviewPrimaryTabs.has(parsed.primaryTab as ReviewPrimaryTab) ? parsed.primaryTab! : fallback.primaryTab,
        referenceTab: reviewReferenceTabs.has(parsed.referenceTab as ReviewReferenceTab)
          ? parsed.referenceTab!
          : fallback.referenceTab,
        momentFilter: reviewMomentFilters.has(parsed.momentFilter as NarrativeMomentFilter)
          ? parsed.momentFilter!
          : fallback.momentFilter,
        selectedMomentPly:
          typeof parsed.selectedMomentPly === 'number' && parsed.selectedMomentPly > 0
            ? Math.trunc(parsed.selectedMomentPly)
            : fallback.selectedMomentPly,
        selectedCollapseId:
          typeof parsed.selectedCollapseId === 'string' && parsed.selectedCollapseId.length
            ? parsed.selectedCollapseId
            : fallback.selectedCollapseId,
      };
    } catch (_) {
      tempStorage.remove(reviewStateStorageKey);
      return fallback;
    }
  }

  private persistReviewState(state: ReviewUIState): void {
    if (!this.isReviewShell()) return;
    tempStorage.set(reviewStateStorageKey, JSON.stringify(state));
  }

  setReviewPrimaryTab = (tab: ReviewPrimaryTab): void => {
    if (!this.isReviewShell()) return;
    this.dispatchReviewState({ type: 'primary-tab', tab });
  };

  setReviewReferenceTab = (tab: ReviewReferenceTab): void => {
    if (!this.isReviewShell()) return;
    this.dispatchReviewState({ type: 'reference-tab', tab });
  };

  setReviewMomentFilter = (filter: NarrativeMomentFilter): void => {
    if (!this.isReviewShell()) return;
    this.dispatchReviewState({ type: 'moment-filter', filter });
  };

  selectReviewMoment = (ply: Ply | null): void => {
    if (!this.isReviewShell()) return;
    const moment = ply ? this.findReviewMomentByPly(ply) : undefined;
    this.setReviewState({
      ...this.reviewState(),
      primaryTab: 'moments',
      selectedMomentPly: ply,
      selectedCollapseId: moment?.collapse?.interval ?? this.selectedReviewCollapseId(),
    });
  };

  selectReviewCollapse = (collapseId: string | null): void => {
    if (!this.isReviewShell()) return;
    const moment = collapseId ? this.findReviewMomentByCollapseId(collapseId) : undefined;
    this.setReviewState({
      ...this.reviewState(),
      primaryTab: 'repair',
      selectedCollapseId: collapseId,
      selectedMomentPly: moment?.ply ?? this.selectedReviewMomentPly(),
    });
  };

  private dispatchReviewState(action: ReviewUIAction): void {
    this.setReviewState(reduceReviewState(this.reviewState(), action));
  }

  private setReviewState(next: ReviewUIState): void {
    const ensured = this.ensureReviewSelections(next);
    this.reviewState(ensured);
    this.persistReviewState(ensured);
    this.syncReviewShellState(ensured);
  }

  refreshReviewShellState = (): void => {
    if (!this.isReviewShell()) return;
    this.setReviewState(this.reviewState());
  };

  private syncReviewShellState(state: ReviewUIState): void {
    if (!this.isReviewShell()) return;
    const explorerOpen = state.primaryTab === 'reference' && state.referenceTab === 'explorer' && this.explorer.allowed();
    if (explorerOpen) {
      if (!this.explorer.enabled()) this.explorer.toggle();
      this.explorer.setNode();
    } else this.explorer.disable();
    this.actionMenu(false);
    if (
      shouldFetchReviewPatterns(state, {
        narrativeAvailable: !!this.narrative,
        hasDnaData: !!this.narrative?.dnaData(),
        dnaLoading: !!this.narrative?.dnaLoading(),
      })
    )
      void this.narrative!.fetchDefeatDna();
  }

  private ensureReviewSelections(state: ReviewUIState): ReviewUIState {
    let next = state;
    const moment = next.selectedMomentPly ? this.findReviewMomentByPly(next.selectedMomentPly) : undefined;
    if (next.primaryTab === 'moments') {
      const filtered = this.reviewMoments(next.momentFilter);
      if (!filtered.length) next = { ...next, selectedMomentPly: null };
      else if (!moment || !filtered.some(candidate => candidate.ply === moment.ply))
        next = { ...next, selectedMomentPly: filtered[0].ply };
    }
    if (next.primaryTab === 'repair') {
      const collapseMoment = next.selectedCollapseId
        ? this.findReviewMomentByCollapseId(next.selectedCollapseId)
        : undefined;
      const firstCollapse = this.reviewCollapseMoments()[0];
      if (!firstCollapse) next = { ...next, selectedCollapseId: null };
      else if (!collapseMoment)
        next = {
          ...next,
          selectedCollapseId: firstCollapse.collapse!.interval,
          selectedMomentPly: next.selectedMomentPly ?? firstCollapse.ply,
        };
    }
    return next;
  }

  private reviewMoments(filter: NarrativeMomentFilter = this.reviewMomentFilter()): GameNarrativeMoment[] {
    const moments = this.narrative?.data()?.moments || [];
    if (filter === 'all') return moments.slice();
    if (filter === 'collapses') return moments.filter(moment => !!moment.collapse);
    return moments.filter(this.isCriticalReviewMoment);
  }

  private reviewCollapseMoments(): GameNarrativeMoment[] {
    return (this.narrative?.data()?.moments || []).filter(moment => !!moment.collapse);
  }

  private isCriticalReviewMoment(moment: GameNarrativeMoment): boolean {
    if (moment.collapse) return true;
    const haystack = [moment.moveClassification, moment.momentType, moment.strategicSalience]
      .filter(Boolean)
      .join(' ')
      .toLowerCase();
    return ['critical', 'turning', 'blunder', 'mistake', 'missed', 'swing'].some(token => haystack.includes(token));
  }

  private findReviewMomentByPly(ply: Ply): GameNarrativeMoment | undefined {
    return (this.narrative?.data()?.moments || []).find(moment => moment.ply === ply);
  }

  private findReviewMomentByCollapseId(collapseId: string): GameNarrativeMoment | undefined {
    return (this.narrative?.data()?.moments || []).find(moment => moment.collapse?.interval === collapseId);
  }

  private syncReviewSelectionFromNode(): void {
    if (!this.isReviewShell()) return;
    const state = this.reviewState();
    let next = state;

    if (state.primaryTab === 'moments') {
      const exactMoment = this.findReviewMomentByPly(this.node.ply);
      if (exactMoment && exactMoment.ply !== state.selectedMomentPly) {
        next = {
          ...next,
          selectedMomentPly: exactMoment.ply,
          selectedCollapseId: exactMoment.collapse?.interval ?? next.selectedCollapseId,
        };
      }
    }

    if (state.primaryTab === 'repair') {
      const collapseMoment =
        this.reviewCollapseMoments().find(moment => this.reviewCollapseContainsPly(moment, this.node.ply)) ||
        (state.selectedCollapseId ? this.findReviewMomentByCollapseId(state.selectedCollapseId) : undefined);
      if (
        collapseMoment?.collapse &&
        (collapseMoment.collapse.interval !== state.selectedCollapseId || collapseMoment.ply !== state.selectedMomentPly)
      ) {
        next = {
          ...next,
          selectedCollapseId: collapseMoment.collapse.interval,
          selectedMomentPly: collapseMoment.ply,
        };
      }
    }

    if (next !== state) {
      this.reviewState(next);
      this.persistReviewState(next);
    }
  }

  private reviewCollapseContainsPly(moment: GameNarrativeMoment, ply: Ply): boolean {
    const collapse = moment.collapse;
    if (!collapse) return false;
    const [startRaw, endRaw] = collapse.interval.split('-').map(Number);
    const start = Number.isFinite(startRaw) ? startRaw : moment.ply;
    const end = Number.isFinite(endRaw) ? endRaw : start;
    return (ply >= start && ply <= end) || ply === collapse.earliestPreventablePly || ply === moment.ply;
  }

  activeControlBarTool() {
    if (this.isReviewShell()) return false;
    return this.actionMenu()
      ? 'action-menu'
      : this.narrative?.enabled()
        ? 'narrative'
        : this.explorer.enabled()
          ? 'opening-explorer'
          : false;
  }

  allowLines() {
    return true;
  }

  toggleDiscloseOf(path = this.path.slice(0, -2)) {
    const disclose = this.idbTree.discloseOf(this.tree.nodeAtPath(path), this.tree.pathIsMainline(path));
    if (disclose) this.idbTree.setCollapsed(path, disclose === 'expanded');
    return Boolean(disclose);
  }

  toggleThreatMode = (v = !this.threatMode()) => {
    if (v === this.threatMode()) return;
    if (this.node.check || !this.showAnalysis()) return;
    if (!this.cevalEnabled()) return;
    this.threatMode(v);
    this.setAutoShapes();
    this.startCeval();
    this.redraw();
  };

  togglePossiblyShowMoveAnnotationsOnBoard = (v: boolean): void => {
    this.possiblyShowMoveAnnotationsOnBoard(v);
    this.resetAutoShapes();
    this.redraw();
  };

  recentImportDrafts = (): string[] => {
    if (this.recentImportDraftsCache) return this.recentImportDraftsCache;
    const raw = tempStorage.get(recentImportStorageKey);
    if (!raw) return (this.recentImportDraftsCache = []);
    try {
      const parsed = JSON.parse(raw);
      this.recentImportDraftsCache = Array.isArray(parsed) ? parsed.filter((v): v is string => typeof v === 'string') : [];
    } catch (_) {
      this.recentImportDraftsCache = [];
    }
    return this.recentImportDraftsCache;
  };

  useImportDraft = (draft: string): void => {
    this.pgnInput = draft;
    this.pgnError = '';
    this.redirecting = false;
    this.setReviewReferenceTab('import');
    this.redraw();
  };

  resetImportDraft = (): void => {
    this.fenInput = undefined;
    this.pgnInput = undefined;
    this.pgnError = '';
    this.redirecting = false;
    this.redraw();
  };

  toggleActionMenu = () => {
    if (this.isReviewShell()) {
      if (this.reviewPrimaryTab() === 'reference' && this.reviewReferenceTab() === 'board')
        this.setReviewPrimaryTab('moves');
      else this.setReviewReferenceTab('board');
      return;
    }
    if (!this.actionMenu()) {
      if (this.explorer.enabled()) this.explorer.toggle();
      this.narrative?.enabled(false);
    }
    this.actionMenu.toggle();
  };

  toggleExplorer = (): void => {
    if (this.isReviewShell()) {
      if (!this.explorer.allowed()) return;
      if (this.reviewPrimaryTab() === 'reference' && this.reviewReferenceTab() === 'explorer')
        this.setReviewPrimaryTab('moves');
      else this.setReviewReferenceTab('explorer');
      return;
    }
    if (!this.explorer.allowed()) return;
    if (!this.explorer.enabled()) {
      this.actionMenu(false);
      this.narrative?.enabled(false);
    }
    this.explorer.toggle();
  };

  toggleNarrative = (): void => {
    if (!this.narrative) return;
    if (this.isReviewShell()) {
      this.setReviewPrimaryTab('overview');
      if (!this.narrative.data() && !this.narrative.loading()) void this.openNarrative();
      return;
    }
    if (!this.narrative.enabled()) {
      this.actionMenu(false);
      this.explorer.disable();
    }
    this.narrative.toggle();
  };

  openNarrative = async (): Promise<void> => {
    if (!this.narrative) return;
    if (this.isReviewShell()) this.setReviewPrimaryTab('overview');
    else {
      this.actionMenu(false);
      this.explorer.disable();
    }
    await this.narrative.openAndFetch();
  };

  withCg = <A>(f: (cg: ChessgroundApi) => A): A | undefined =>
    this.chessground && this.cgVersion.js === this.cgVersion.dom ? f(this.chessground) : undefined;

  hasFullComputerAnalysis = (): boolean => {
    return Object.keys(this.mainline[0].eval || {}).length > 0;
  };

  mergeAnalysisData(data: ServerEvalData) {
    this.tree.merge(data.tree);
    this.data.analysis = data.analysis;
    if (data.analysis)
      data.analysis.partial = !!treeOps.findInMainline(data.tree, this.partialAnalysisCallback);
    if (data.division) this.data.game.division = data.division;
    pubsub.emit('analysis.server.progress', this.data);
    this.redraw();
  }

  partialAnalysisCallback(n: Tree.Node) {
    return !n.eval && !!n.children.length && n.ply <= 300 && n.ply > 0;
  }

  private canEvalGet = (): boolean => {
    if (this.node.ply >= 15 && !this.opts.study) return false;

    // cloud eval does not support threefold repetition
    const fens = new Set();
    for (let i = this.nodeList.length - 1; i >= 0; i--) {
      const node = this.nodeList[i];
      const epd = fenToEpd(node.fen);
      if (fens.has(epd)) return false;
      if (node.san && sanIrreversible(this.data.game.variant.key, node.san)) return true;
      fens.add(epd);
    }
    return true;
  };

  private instanciateEvalCache = () => {
    this.evalCache = new EvalCache({
      variant: this.data.game.variant.key,
      canGet: this.canEvalGet,
      canPut: () =>
        !!(
          this.ceval?.isCacheable &&
          this.canEvalGet() &&
          // if not in study, only put decent opening moves
          (this.opts.study || (!this.node.ceval!.mate && Math.abs(this.node.ceval!.cp!) < 99))
        ),
      getNode: () => this.node,
      send: this.opts.socketSend,
      receive: this.onNewCeval,
      upgradable: this.evalCache?.upgradable(),
    });
  };

  playUci = (uci: Uci, uciQueue?: Uci[]) => {
    this.pvUciQueue = uciQueue ?? [];
    const move = parseUci(uci)!;
    const to = makeSquare(move.to);
    if (isNormal(move)) {
      const piece = this.chessground.state.pieces.get(makeSquare(move.from));
      const capture = this.chessground.state.pieces.get(to);
      this.sendMove(
        makeSquare(move.from),
        to,
        capture && piece && capture.color !== piece.color ? capture : undefined,
        move.promotion,
      );
    } else
      this.chessground.newPiece(
        {
          color: this.chessground.state.movable.color as Color,
          role: move.role,
        },
        to,
      );
  };

  playUciList(uciList: Uci[]): void {
    this.pvUciQueue = uciList;
    const firstUci = this.pvUciQueue.shift();
    if (firstUci) this.playUci(firstUci, this.pvUciQueue);
  }

  explorerMove(uci: Uci): void {
    this.playUci(uci);
    this.explorer.loading(true);
  }

  playBestMove(): void {
    const uci = this.node.ceval?.pvs[0].moves[0] || this.nextNodeBest();
    if (uci) this.playUci(uci);
  }

  pluginMove = (orig: Key, dest: Key, prom: Role | undefined): void => {
    const capture = this.chessground.state.pieces.get(dest);
    this.sendMove(orig, dest, capture, prom);
  };

  toggleVariationArrows = () => {
    const trueValue = this.variationArrowOpacity(false);
    this.variationArrowOpacity(trueValue === 0 ? 0.6 : -trueValue);
  };

  private makeVariationOpacityProp(): Prop<number | false> {
    let value = parseFloat(localStorage.getItem('analyse.variation-arrow-opacity') || '0');
    if (isNaN(value) || value < -1 || value > 1) value = 0;
    return (v?: number | false) => {
      if (v === false) return value;
      if (v === undefined || isNaN(v)) return value > 0 ? value : false;
      value = Math.min(1, Math.max(-1, v));
      localStorage.setItem('analyse.variation-arrow-opacity', value.toString());
      this.setAutoShapes();
      this.chessground.redrawAll();
      this.redraw();
      return value;
    };
  }

  private pluginUpdate = (fen: string) => {
    // If controller and chessground board states differ, ignore this update. Once the chessground
    // state is updated to match, pluginUpdate will be called again.
    if (!fen.startsWith(this.chessground?.getFen())) return;
  };

  showBestMoveArrows = () => false;

  private resetAutoShapes = () => {
    if (
      this.showBestMoveArrows() ||
      this.possiblyShowMoveAnnotationsOnBoard() ||
      this.variationArrowOpacity()
    )
      this.setAutoShapes();
    else this.chessground?.setAutoShapes([]);
  };

  private async mergeIdbThenShowTreeView() {
    await this.idbTree.merge();
    this.treeView.hidden = false;
    this.idbTree.revealNode();
    this.redraw();
  }
}
