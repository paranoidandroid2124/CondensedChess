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
import { make as makePractice, type PracticeCtrl } from './practice/practiceCtrl';
import { make as makeRetro, type RetroCtrl } from './retrospect/retroCtrl';
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
import { storedBooleanProp, storedBooleanPropWithEffect } from 'lib/storage';
import { PromotionCtrl } from 'lib/game/promotion';
import { valid as crazyValid } from './crazy/crazyCtrl';
import bookmakerNarrative, { bookmakerClear, type BookmakerNarrative } from './bookmaker';
import ExplorerCtrl from './explorer/explorerCtrl';
import { uciToMove } from '@lichess-org/chessground/util';
import { IdbTree } from './idbTree';
import pgnImport from './pgnImport';
import ForecastCtrl from './forecast/forecastCtrl';
import * as studyApi from './studyApi';

import type { PgnError } from 'chessops/pgn';

import { confirm } from 'lib/view';
import api from './api';
import { displayColumns } from 'lib/device';
import { make as makeNarrative, type NarrativeCtrl } from './narrative/narrativeCtrl';

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
  retro?: RetroCtrl;
  fork: ForkCtrl;
  practice?: PracticeCtrl;
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
  showBestMoveArrowsProp: Prop<boolean>;
  variationArrowOpacity: Prop<number | false>;
  showGauge = storedBooleanProp('analyse.show-gauge', true);
  private showCevalProp: Prop<boolean> = storedBooleanProp('analyse.show-engine', !!this.cevalEnabledProp());
  showFishnetAnalysis = storedBooleanProp('analyse.show-computer', true);
  possiblyShowMoveAnnotationsOnBoard = storedBooleanProp('analyse.show-move-annotation', true);
  keyboardHelp: boolean = location.hash === '#keyboard';
  threatMode: Prop<boolean> = prop(false);
  disclosureMode = storedBooleanProp('analyse.disclosure.enabled', false);

  treeView: TreeView;
  cgVersion = {
    js: 1, // increment to recreate chessground
    dom: 1,
  };

  // underboard inputs
  fenInput?: string;
  pgnInput?: string;
  pgnError?: string;

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
    this.initCeval();
    this.pendingCopyPath = propWithEffect(null, this.redraw);
    this.pendingDeletionPath = propWithEffect(null, this.redraw);
    this.initialPath = this.makeInitialPath();
    this.setPath(this.initialPath);

    this.showGround();

    this.variationArrowOpacity = this.makeVariationOpacityProp();
    this.showBestMoveArrowsProp = storedBooleanPropWithEffect(
      'analyse.auto-shapes',
      true,
      this.setAutoShapes,
    );
    this.resetAutoShapes();
    this.explorer.setNode();
    this.explorer.setNode();

    if (location.hash === '#practice')
      this.togglePractice();
    else if (location.hash === '#menu') requestIdleCallback(this.actionMenu.toggle, 500);
    this.setCevalPracticeOpts();
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
    if (this.retro && this.data.game.variant.key !== 'racingKings')
      this.retro = makeRetro(this, this.bottomColor());
    if (this.practice) this.startCeval();
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
      movableColor = this.practice
        ? this.bottomColor()
        : (dests && dests.size > 0) || drops === null || drops.length
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
      if (this.retro) this.retro.onJump();
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
    if (pathChanged) {
      if (this.practice) this.practice.onJump();
    }
    pubsub.emit('ply', this.node.ply, this.tree.lastMainlineNode(this.path).ply === this.node.ply);
    this.showGround();
    this.pluginUpdate(this.node.fen);
  }

  userJump = (path: Tree.Path): void => {
    this.autoplay.stop();
    this.withCg(cg => cg.selectSquare(null));
    if (this.practice) {
      const prev = this.path;
      this.practice.preUserJump(prev, path);
      this.jump(path);
      this.withCg(cg => cg.cancelPremove());
      this.practice.postUserJump(prev, this.path);
    } else this.jump(path);
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
    this.redirecting = false;
    this.setPath(treePath.root);
    this.initCeval();
    this.instanciateEvalCache();
    this.cgVersion.js++;
    this.mergeIdbThenShowTreeView();
  }

  changePgn(pgn: string, andReload: boolean): AnalyseData | undefined {
    this.pgnError = '';
    try {
      const data: AnalyseData = {
        ...pgnImport(pgn),
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
    if (this.practice) this.practice.onUserMove();
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
    return (this.cevalEnabled() && node.ceval) || (this.showFishnetAnalysis() && node.eval);
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
    return node.children.filter(
      kid =>
        !kid.comp ||
        (this.showFishnetAnalysis() && !this.retro?.hideComputerLine(kid)) ||
        (treeOps.contains(kid, this.node) && !this.retro?.forceCeval()),
    );
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
          this.retro?.onCeval();
          this.practice?.onCeval();
          this.evalCache.onLocalCeval();
        }
        if (!(site.blindMode && this.retro)) this.redraw();
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
    const force = Boolean(this.practice || this.retro?.forceCeval());
    const unforcedState = this.cevalEnabledProp() && this.isCevalAllowed() && !this.ceval.isPaused;

    if (enable === undefined) return force ? 'force' : unforcedState;
    if (!force) {
      this.showCevalProp(enable);
      this.cevalEnabledProp(enable);
    }
    if (enable && this.ceval.isPaused) this.ceval.resume();
    if (enable !== unforcedState) {
      if (enable) this.startCeval();
      else {
        this.threatMode(false);
        this.ceval.stop();
      }
      this.setAutoShapes();
      this.ceval.showEnginePrefs(false);
      this.redraw();
    }
    return force ? 'force' : enable;
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
    return Boolean(kids.filter(x => !x.comp || this.showFishnetAnalysis()).length);
  }

  showAnalysis() {
    return this.showFishnetAnalysis() || (this.cevalEnabled() && this.isCevalAllowed());
  }

  showMoveGlyphs = (): boolean => this.showFishnetAnalysis();

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

  showCeval = (show?: boolean) => {
    const barMode = this.activeControlMode();
    if (show === undefined) return displayColumns() > 1 || barMode === 'ceval' || barMode === 'practice';
    this.ceval.showEnginePrefs(false);
    this.showCevalProp(show);
    if (show) this.cevalEnabled(true);
    return show;
  };

  activeControlMode = () =>
    this.practice
      ? 'practice'
      : !!this.retro
        ? 'retro'
        : this.showCevalProp()
          ? 'ceval'
          : false;

  activeControlBarTool() {
    return this.actionMenu() ? 'action-menu' : this.explorer.enabled() ? 'opening-explorer' : false;
  }

  allowLines() {
    return (
      !this.retro?.isSolving()
    );
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
    if (this.threatMode() && this.practice) this.togglePractice();
    this.setAutoShapes();
    this.startCeval();
    this.redraw();
  };

  togglePossiblyShowMoveAnnotationsOnBoard = (v: boolean): void => {
    this.possiblyShowMoveAnnotationsOnBoard(v);
    this.resetAutoShapes();
  };

  toggleFishnetAnalysis = () => {
    this.showFishnetAnalysis(!this.showFishnetAnalysis());
    this.resetAutoShapes();
    pubsub.emit('analysis.comp.toggle', this.showFishnetAnalysis());
  };

  toggleActionMenu = () => {
    if (!this.actionMenu() && this.explorer.enabled()) this.explorer.toggle();
    this.actionMenu.toggle();
  };

  toggleRetro = (): void => {
    if (this.retro) this.retro = undefined;
    else {
      this.closeTools();
      this.retro = makeRetro(this, this.bottomColor());
    }
    this.setAutoShapes();
  };

  toggleExplorer = (): void => {
    if (!this.explorer.allowed()) return;
    if (!this.explorer.enabled()) {
      this.retro = undefined;
      this.actionMenu(false);
    }
    this.explorer.toggle();
  };

  togglePractice = (enable = !this.practice) => {
    if (enable === !!this.practice) return;
    this.practice = undefined;
    if (!enable || !this.isCevalAllowed()) {
      this.setCevalPracticeOpts();
      this.showGround();
    } else {
      this.closeTools();
      this.threatMode(false);
      this.practice = makePractice(this, undefined);
      this.setCevalPracticeOpts();
      this.setAutoShapes();
      this.startCeval();
    }
  };

  private setCevalPracticeOpts() {
    this.ceval.setOpts({ custom: this.practice?.customCeval });
  }


  private closeTools = () => {
    this.retro = undefined;
    this.togglePractice(false);
    if (this.explorer.enabled()) this.explorer.toggle();
    this.actionMenu(false);
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
    if (this.retro) this.retro.onMergeAnalysisData();
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

  showBestMoveArrows = () => this.showBestMoveArrowsProp() && !this.retro?.hideComputerLine(this.node);

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
