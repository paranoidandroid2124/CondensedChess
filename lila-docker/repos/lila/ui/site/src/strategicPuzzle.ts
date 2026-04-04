import { Chessground as makeChessground } from '@lichess-org/chessground';
import type { Api as ChessgroundApi } from '@lichess-org/chessground/api';
import { myUsername } from 'lib';

type Credit = 'full' | 'partial';
type Outcome = 'full' | 'partial' | 'wrong' | 'giveup';
type SolveStage = 'plan' | 'move' | 'reveal';
type RevealFocus = 'start' | 'proof';
const site = window.site as any;

interface SourcePayload {
  seedId: string;
  opening?: string;
  eco?: string;
}

interface PositionPayload {
  fen: string;
  sideToMove: Color;
}

interface DominantFamilySummary {
  key: string;
  dominantIdeaKind: string;
  anchor: string;
}

interface ShellChoice {
  uci: string;
  san: string;
  credit: Credit;
  nextNodeId?: string;
  terminalId?: string;
  afterFen?: string;
  familyKey?: string;
  label?: string;
  feedback: string;
}

interface PlanStart {
  uci: string;
  san: string;
  credit: Credit;
  label?: string;
  feedback: string;
  afterFen?: string;
  terminalId?: string;
}

interface PuzzlePlan {
  id: string;
  familyKey?: string;
  dominantIdeaKind?: string;
  anchor?: string;
  task: string;
  feedback: string;
  allowedStarts: PlanStart[];
  featuredTerminalId: string;
  featuredStartUci?: string;
}

interface RuntimeProofLayer {
  rootChoices: ShellChoice[];
  nodes: PlayerNode[];
  forcedReplies: ForcedReply[];
}

interface ForcedReply {
  id: string;
  fromNodeId: string;
  uci: string;
  san: string;
  afterFen: string;
  nextNodeId?: string;
}

interface PlayerNode {
  id: string;
  step: number;
  fen: string;
  prompt: string;
  badMoveFeedback: string;
  choices: ShellChoice[];
}

interface TerminalReveal {
  id: string;
  outcome: Outcome;
  title: string;
  summary: string;
  commentary: string;
  familyKey?: string;
  dominantIdeaKind?: string;
  anchor?: string;
  lineSan: string[];
  siblingMoves: string[];
  opening?: string;
  eco?: string;
  dominantFamilyKey?: string;
  planId?: string;
  planTask?: string;
  whyPlan?: string;
  whyMove?: string;
  acceptedStarts: string[];
  featuredStart?: string;
}

interface RuntimeShell {
  schema: string;
  startFen: string;
  sideToMove: Color;
  prompt: string;
  plans: PuzzlePlan[];
  proof: RuntimeProofLayer;
  terminals: TerminalReveal[];
}

interface AttemptSummary {
  puzzleId: string;
  status: Outcome;
  completedAt: string;
}

interface ProgressPayload {
  authenticated: boolean;
  currentStreak: number;
  recentAttempts: AttemptSummary[];
}

interface PuzzleDoc {
  id: string;
  source: SourcePayload;
  position: PositionPayload;
  dominantFamily?: DominantFamilySummary;
  qualityScore: { total: number };
}

interface BootstrapPayload {
  puzzle: PuzzleDoc;
  runtimeShell: RuntimeShell;
  progress: ProgressPayload;
}

interface CompleteResponse {
  saved: boolean;
  currentStreak: number;
  nextPuzzleId?: string;
}

type FeedbackKind = 'neutral' | 'success' | 'warning';

interface FeedbackState {
  kind: FeedbackKind;
  text: string;
}

interface ProofResolution {
  planId: string;
  start: PlanStart;
  terminal: TerminalReveal;
  lineSan: string[];
  lineUcis: string[];
  startFen: string;
  finalFen: string;
}

interface StrategicPuzzleSnapshot {
  url: string;
  puzzleId: string;
  stage: SolveStage;
  revealFocus: RevealFocus;
  selectedPlanId: string | null;
  selectedStartUci: string | null;
  proof: ProofResolution | null;
  feedback: FeedbackState;
  completion: CompleteResponse | null;
  historyOpen: boolean;
}

interface StrategicPuzzleHistoryState {
  strategicPuzzle: StrategicPuzzleSnapshot;
}

class StrategicPuzzleApp {
  private cg: ChessgroundApi | undefined;
  private payload: BootstrapPayload;
  private readonly app: HTMLElement;
  private stage: SolveStage = 'plan';
  private revealFocus: RevealFocus = 'start';
  private selectedPlanId: string | null = null;
  private selectedStartUci: string | null = null;
  private proof: ProofResolution | null = null;
  private feedback: FeedbackState = { kind: 'neutral', text: '' };
  private busy = false;
  private completion: CompleteResponse | null = null;
  private boardFeedback: Exclude<FeedbackKind, 'neutral'> | null = null;
  private historyOpen = false;

  constructor(payload: BootstrapPayload, app: HTMLElement) {
    this.payload = payload;
    this.app = app;
  }

  mount() {
    window.addEventListener('popstate', this.onPopState);
    this.app.innerHTML = this.shellView();
    this.render();
  }

  private get planMap(): Map<string, PuzzlePlan> {
    return new Map(this.payload.runtimeShell.plans.map(plan => [plan.id, plan]));
  }

  private get proofLayer(): RuntimeProofLayer {
    return this.payload.runtimeShell.proof;
  }

  private get nodeMap(): Map<string, PlayerNode> {
    return new Map(this.proofLayer.nodes.map(node => [node.id, node]));
  }

  private get replyMap(): Map<string, ForcedReply> {
    return new Map(this.proofLayer.forcedReplies.map(reply => [reply.fromNodeId, reply]));
  }

  private get terminalMap(): Map<string, TerminalReveal> {
    return new Map(this.payload.runtimeShell.terminals.map(terminal => [terminal.id, terminal]));
  }

  private get selectedPlan(): PuzzlePlan | null {
    return this.selectedPlanId ? this.planMap.get(this.selectedPlanId) || null : null;
  }

  private get selectedStart(): PlanStart | null {
    return this.selectedPlan?.allowedStarts.find(start => start.uci === this.selectedStartUci) || null;
  }

  private get currentStarts(): PlanStart[] {
    return this.selectedPlan?.allowedStarts || [];
  }

  private get revealStartFen(): string {
    return this.proof?.startFen || this.proof?.start.afterFen || this.payload.runtimeShell.startFen;
  }

  private get currentFen(): string {
    if (this.stage === 'reveal' && this.proof) return this.revealFocus === 'proof' ? this.proof.finalFen : this.revealStartFen;
    return this.payload.runtimeShell.startFen;
  }

  private get reveal(): TerminalReveal | null {
    return this.proof?.terminal || null;
  }

  private get lineSans(): string[] {
    return this.proof?.lineSan || [];
  }

  private get acceptedStarts(): string[] {
    if (this.reveal?.acceptedStarts.length) return this.reveal.acceptedStarts;
    if (this.selectedPlan?.allowedStarts.length) return this.selectedPlan.allowedStarts.map(start => start.san);
    return [];
  }

  private get featuredStartSan(): string | null {
    return this.reveal?.featuredStart || this.selectedStart?.san || this.acceptedStarts[0] || null;
  }

  private get orientation(): Color {
    return this.payload.puzzle.position.sideToMove;
  }

  private get nextAvailable(): boolean {
    return Boolean(this.reveal && this.completion?.nextPuzzleId);
  }

  private render() {
    this.ensureShell();
    const shell = this.app.querySelector<HTMLElement>('.sp-runtime-shell');
    if (shell) shell.classList.toggle('has-reveal', Boolean(this.reveal));
    const topbar = this.app.querySelector<HTMLElement>('[data-region="topbar"]');
    if (topbar) topbar.innerHTML = this.renderTopbar();
    const boardMeta = this.app.querySelector<HTMLElement>('[data-region="board-meta"]');
    if (boardMeta) boardMeta.innerHTML = this.renderBoardMeta();
    const boardStage = this.app.querySelector<HTMLElement>('.sp-runtime-board-stage');
    if (boardStage) boardStage.className = `sp-runtime-board-stage${this.boardFeedback ? ` is-${this.boardFeedback}` : ''}`;
    const boardCallouts = this.app.querySelector<HTMLElement>('[data-region="board-callouts"]');
    if (boardCallouts) boardCallouts.innerHTML = this.renderBoardCallouts();
    const statePane = this.app.querySelector<HTMLElement>('[data-region="state-pane"]');
    if (statePane) statePane.innerHTML = this.renderStatePane(this.accountPatternsUrl());
    this.bindBoard();
    this.bindButtons();
    this.replaceHistoryState();
  }

  private ensureShell() {
    if (this.app.querySelector('#sp-runtime-board')) return;
    this.cg?.destroy();
    this.cg = undefined;
    this.app.innerHTML = this.shellView();
  }

  private shellView(): string {
    return `
      <section class="sp-demo-shell sp-runtime-shell">
        <section data-region="topbar"></section>
        <article class="sp-demo-board-card sp-demo-board-card--runtime">
          <div class="sp-runtime-board-shell">
            <div class="sp-runtime-board-stage">
              <div class="sp-runtime-board-meta" data-region="board-meta"></div>
              <div id="sp-runtime-board" class="sp-runtime-board"></div>
            </div>
          </div>
          <div data-region="board-callouts"></div>
        </article>
        <div data-region="state-pane"></div>
      </section>
    `;
  }

  private replaceHistoryState(url = this.currentUrl()) {
    if (!history.replaceState) return;
    history.replaceState({ strategicPuzzle: this.historySnapshot(url) }, '', url);
  }

  private pushHistoryState(url: string) {
    if (!history.pushState) {
      this.replaceHistoryState(url);
      return;
    }
    history.pushState({ strategicPuzzle: this.historySnapshot(url) }, '', url);
  }

  private historySnapshot(url: string): StrategicPuzzleSnapshot {
    return {
      url,
      puzzleId: this.payload.puzzle.id,
      stage: this.stage,
      revealFocus: this.revealFocus,
      selectedPlanId: this.selectedPlanId,
      selectedStartUci: this.selectedStartUci,
      proof: this.proof,
      feedback: { ...this.feedback },
      completion: this.completion,
      historyOpen: this.historyOpen,
    };
  }

  private currentUrl(): string {
    return `${window.location.pathname}${window.location.search}${window.location.hash}`;
  }

  private async restoreSnapshot(snapshot: StrategicPuzzleSnapshot) {
    if (snapshot.puzzleId !== this.payload.puzzle.id) {
      const payload = await this.loadBootstrapById(snapshot.puzzleId);
      if (!payload) {
        window.location.reload();
        return;
      }
      this.payload = payload;
    }
    this.stage = snapshot.stage;
    this.revealFocus = snapshot.revealFocus || 'start';
    this.selectedPlanId = snapshot.selectedPlanId;
    this.selectedStartUci = snapshot.selectedStartUci;
    this.proof = snapshot.proof || null;
    this.feedback = { ...snapshot.feedback };
    this.completion = snapshot.completion;
    this.historyOpen = snapshot.historyOpen ?? false;
    this.busy = false;
    this.boardFeedback = null;
    this.render();
  }

  private async loadBootstrapById(puzzleId: string): Promise<BootstrapPayload | null> {
    try {
      const res = await fetch(`/api/strategic-puzzle/${encodeURIComponent(puzzleId)}`);
      if (!res.ok) return null;
      return (await res.json()) as BootstrapPayload;
    } catch (err) {
      console.warn('strategic puzzle bootstrap fetch failed', err);
      return null;
    }
  }

  private accountPatternsUrl(): string | null {
    const username = myUsername();
    if (!username || !this.payload.progress.authenticated) return null;
    return `/account-intel/lichess/${encodeURIComponent(username)}?kind=my_account_intelligence_lite`;
  }

  private onPopState = (event: PopStateEvent) => {
    const snapshot = readHistorySnapshot(event.state);
    if (!snapshot) {
      window.location.reload();
      return;
    }
    void this.restoreSnapshot(snapshot);
  };

  private stageIndex(): number {
    switch (this.stage) {
      case 'plan':
        return 1;
      case 'move':
        return 2;
      case 'reveal':
        return 3;
    }
  }

  private stageLabel(): string {
    switch (this.stage) {
      case 'plan':
        return 'find the task';
      case 'move':
        return 'choose the start';
      case 'reveal':
        return 'review why it works';
    }
  }

  private renderTopbar(): string {
    const introText = 'Find the task, choose the start, then review the task, the start, and the exact proof.';
    const streakLabel = this.payload.progress.authenticated ? `Current streak ${this.payload.progress.currentStreak}` : 'Anonymous session';
    return `
      <section class="sp-runtime-topbar">
        <div class="sp-runtime-topbar__lead">
          <p class="sp-demo-kicker">Strategic Puzzle</p>
          <h1>${this.orientation === 'white' ? 'White' : 'Black'} to move</h1>
          <p class="sp-runtime-intro">${escapeHtml(introText)}</p>
        </div>
        <div class="sp-runtime-topbar__stats">
          <div class="sp-metric-card"><strong>${this.stageIndex()} / 3</strong><span>${escapeHtml(this.stageLabel())}</span></div>
          <div class="sp-runtime-topbar__status">
            <span class="sp-chip sp-chip--streak">${escapeHtml(streakLabel)}</span>
          </div>
      </section>
    `;
  }

  private renderBoardMeta(): string {
    return `
      <span class="sp-chip sp-chip--turn">${escapeHtml(capitalize(this.orientation))} to move</span>
      <span class="sp-chip sp-chip--theme">${escapeHtml(this.payload.puzzle.dominantFamily?.dominantIdeaKind ? humanize(this.payload.puzzle.dominantFamily.dominantIdeaKind) : 'strategic puzzle')}</span>
      <span class="sp-chip sp-chip--echo">${escapeHtml(this.stageLabel())}</span>
    `;
  }

  private renderBoardCallouts(): string {
    const acceptedStarts = this.acceptedStarts;
    const featuredStart = this.featuredStartSan;
    const primary =
      this.stage === 'reveal'
        ? this.revealFocus === 'proof'
          ? {
              title: 'Exact proof board',
              text: 'The board is now showing the confirmed continuation. Switch back to the started position whenever you want to re-anchor the review in the first move.',
            }
          : {
              title: 'Started position',
              text: featuredStart
                ? `${featuredStart} has already been played. The board is paused here so you can see how the bounded task begins before opening the proof.`
                : 'The board is paused right after the stored start so you can review how the bounded task begins before opening the proof.',
            }
        : this.stage === 'move'
          ? {
              title: 'Board is live',
              text: 'Make the first move on the board or choose one below. Only stored starts for the selected task count.',
            }
          : {
              title: 'Board state',
              text: 'The board stays fixed while you decide which bounded task matters in this exact position.',
            };
    const secondary =
      this.stage === 'reveal'
        ? this.revealFocus === 'proof'
          ? {
              title: 'Selected start',
              text: featuredStart || 'This review stores one public start.',
            }
          : {
              title: 'Next review',
              text: 'Open the exact proof when you want the full confirmed continuation and proof endpoint on the board.',
            }
        : this.stage === 'move'
          ? {
              title: 'Accepted starts',
              text: acceptedStarts.length
                ? acceptedStarts.join(', ')
                : 'No public starts are stored for the selected task.',
            }
          : {
              title: 'Next unlock',
              text: 'Select a task to open the first-move stage on the board.',
            };
    return `
      <div class="sp-demo-board-callouts">
        <div class="sp-callout">
          <strong>${escapeHtml(primary.title)}</strong>
          <span>${escapeHtml(primary.text)}</span>
        </div>
        <div class="sp-callout">
          <strong>${escapeHtml(secondary.title)}</strong>
          <span>${escapeHtml(secondary.text)}</span>
        </div>
      </div>
    `;
  }

  private revealThemeLabel(reveal: TerminalReveal, plan: PuzzlePlan | null): string {
    const theme =
      plan?.dominantIdeaKind ||
      reveal.dominantIdeaKind ||
      this.payload.puzzle.dominantFamily?.dominantIdeaKind ||
      plan?.familyKey ||
      reveal.familyKey ||
      this.payload.puzzle.dominantFamily?.key;
    const anchor = plan?.anchor || reveal.anchor || this.payload.puzzle.dominantFamily?.anchor;
    if (theme && anchor && humanize(theme) !== humanize(anchor)) return `${humanize(theme)} · ${humanize(anchor)}`;
    if (theme) return humanize(theme);
    if (anchor) return humanize(anchor);
    return 'Bounded task';
  }

  private renderStatePane(accountPatternsUrl: string | null): string {
    const reveal = this.reveal;
    const plan = this.selectedPlan;
    const panelLabel = this.stage === 'reveal' ? 'Reveal' : this.stage === 'move' ? 'Start' : 'Plan';
    const panelTitle =
      this.stage === 'reveal'
        ? escapeHtml(reveal?.title || 'Review the plan')
        : this.stage === 'move'
          ? 'Choose the first move that starts the task.'
          : 'Find the bounded task first.';
    const panelCopy =
      this.stage === 'reveal'
        ? escapeHtml(reveal?.planTask || plan?.task || reveal?.summary || 'Review the proof and the plan behind it.')
        : this.stage === 'move'
          ? escapeHtml(plan?.task || 'Choose the first move that starts the selected task.')
          : escapeHtml(this.payload.runtimeShell.prompt || 'Choose the bounded strategic task that fits the exact position.');
    const contextTitle = this.stage === 'reveal' ? 'Review focus' : this.stage === 'move' ? 'Selected task' : 'Current position';
    const contextCopy =
      this.stage === 'reveal'
        ? this.revealFocus === 'proof'
          ? 'The board is following the exact proof now. Keep the task and why-start cards as the primary reading, and use this board only as supporting truth.'
          : 'The board is paused right after the chosen start so you can read the task and why-start first. Open exact proof only when you want the supporting continuation.'
        : this.stage === 'move'
          ? plan?.feedback || 'The plan is set. Choose the first move that actually starts it.'
          : 'Pick the task that matters most in this exact position before worrying about move order.';
    const lineLabel =
      this.stage === 'reveal'
        ? this.revealFocus === 'proof'
          ? 'Exact proof'
          : 'Selected start'
        : this.stage === 'move'
          ? 'Accepted starts'
          : 'Task pool';
    const lineValue =
      this.stage === 'reveal'
        ? this.revealFocus === 'proof'
          ? escapeHtml(this.lineSans.length ? this.lineSans.join(' ') : 'Proof line unavailable.')
          : escapeHtml(this.featuredStartSan || 'No public start stored.')
        : this.stage === 'move'
          ? escapeHtml(this.currentStarts.length ? this.currentStarts.map(start => start.san).join(', ') : 'No starts stored for this task.')
          : escapeHtml(this.payload.runtimeShell.plans.length ? `${this.payload.runtimeShell.plans.length} plan candidates` : 'No plan candidates are stored.');
    return `
      <section class="sp-demo-panel sp-runtime-pane${reveal ? ' is-reveal' : ' is-solve'}">
        <div class="sp-runtime-pane__scroll">
          <div class="sp-runtime-pane__section sp-runtime-pane__section--head">
            <p class="sp-demo-panel__label">${panelLabel}</p>
            <div class="sp-stepper">
              <span class="${this.stageIndex() >= 1 ? 'is-live' : ''}">1. Find the task</span>
              <span class="${this.stageIndex() >= 2 ? 'is-live' : ''}">2. Choose the start</span>
              <span class="${this.stageIndex() >= 3 ? 'is-live' : ''}">3. Review why it works</span>
            </div>
            <h3>${panelTitle}</h3>
            <p class="sp-demo-panel__copy">${panelCopy}</p>
          </div>
          <div class="sp-runtime-pane__context">
            <div class="sp-callout">
              <strong>${lineLabel}</strong>
              <span>${lineValue}</span>
            </div>
            <div class="sp-callout">
              <strong>${contextTitle}</strong>
              <span>${escapeHtml(contextCopy)}</span>
            </div>
          </div>
          <div class="sp-feedback-strip ${this.feedback.kind === 'success' ? 'is-success' : this.feedback.kind === 'warning' ? 'is-warning' : ''}">
            <div>
              <strong>${this.feedback.kind === 'success' ? 'Accepted' : this.feedback.kind === 'warning' ? 'Retry' : 'Guidance'}</strong>
              <span>${escapeHtml(this.defaultFeedbackText())}</span>
            </div>
          </div>
          ${this.stage === 'plan' ? this.renderPlanGrid() : this.stage === 'move' ? this.renderStartGrid() : reveal ? this.renderRevealStack(reveal) : ''}
        </div>
        ${this.renderPaneFooter(accountPatternsUrl)}
      </section>
    `;
  }

  private defaultFeedbackText(): string {
    if (this.feedback.text) return this.feedback.text;
    switch (this.stage) {
      case 'plan':
        return 'Start by identifying the task that best fits the exact position.';
      case 'move':
        return 'The task is fixed. Choose the move that starts it cleanly.';
      case 'reveal':
        return this.revealFocus === 'proof'
          ? 'The exact proof is open on the board. Use it as support for the task and why-start review.'
          : 'The review is open at the started position. Begin with the task and chosen start before opening the proof.';
    }
  }

  private renderPlanGrid(): string {
    const plans = this.payload.runtimeShell.plans;
    if (!plans.length) {
      return `
        <div class="sp-choice-grid">
          <div class="sp-choice-grid__notice">
            <strong>No public plan layer is stored</strong>
            <span>This puzzle still has proof data, but no selectable plan shell is available.</span>
          </div>
        </div>
      `;
    }
    return `
      <div class="sp-choice-grid">
        ${plans
          .map(plan => {
            const featuredOutcome = this.terminalMap.get(plan.featuredTerminalId)?.outcome || 'full';
            const badge = featuredOutcome === 'partial' ? 'Playable alternative' : 'Primary task';
            const meta = [plan.familyKey ? humanize(plan.familyKey) : null, `${plan.allowedStarts.length} start${plan.allowedStarts.length === 1 ? '' : 's'}`]
              .filter(Boolean)
              .join(' · ');
            const emphasisClass = featuredOutcome === 'partial' ? '' : ' is-primary';
            return `
              <button type="button" class="sp-choice${emphasisClass}${this.selectedPlanId === plan.id ? ' is-selected' : ''}" data-plan-id="${escapeHtml(plan.id)}">
                <span class="sp-choice__move">${escapeHtml(plan.task)}</span>
                <span class="sp-choice__copy">${escapeHtml(plan.feedback)}</span>
                <span class="sp-choice__copy">${escapeHtml([badge, meta].filter(Boolean).join(' · '))}</span>
              </button>
            `;
          })
          .join('')}
      </div>
    `;
  }

  private renderStartGrid(): string {
    const plan = this.selectedPlan;
    if (!plan) {
      return `
        <div class="sp-choice-grid">
          <div class="sp-choice-grid__notice">
            <strong>Choose a task first</strong>
            <span>The move stage opens after you select the bounded task.</span>
          </div>
        </div>
      `;
    }
    if (!plan.allowedStarts.length) {
      return `
        <div class="sp-choice-grid">
          <div class="sp-choice-grid__notice">
            <strong>No starts stored</strong>
            <span>This task has no public start moves yet, so the proof cannot be launched from the move stage.</span>
          </div>
        </div>
      `;
    }
    return `
      <div class="sp-choice-grid">
        ${plan.allowedStarts
          .map(start => {
            const creditLabel = start.credit === 'full' ? 'Full-credit start' : 'Accepted, softer start';
            const metaLabel = [start.label, creditLabel].filter(Boolean).join(' · ');
            const emphasisClass = start.credit === 'full' ? ' is-primary' : '';
            return `
              <button type="button" class="sp-choice${emphasisClass}${this.selectedStartUci === start.uci ? ' is-selected' : ''}" data-start-uci="${escapeHtml(start.uci)}">
                <span class="sp-choice__move">${escapeHtml(start.san || start.label || start.uci)}</span>
                <span class="sp-choice__copy">${escapeHtml(metaLabel || 'Start move')}</span>
                <span class="sp-choice__copy">${escapeHtml(start.feedback)}</span>
              </button>
            `;
          })
          .join('')}
      </div>
    `;
  }

  private renderRevealStack(reveal: TerminalReveal): string {
    const plan = this.selectedPlan;
    const selectedStart = this.selectedStart;
    const planTask = reveal.planTask || plan?.task || reveal.summary || 'Shared plan explanation unavailable.';
    const whyPlan = reveal.whyPlan || reveal.summary || plan?.feedback || planTask;
    const taskLead = reveal.summary || plan?.feedback || whyPlan;
    const whyMove = reveal.whyMove || selectedStart?.feedback || reveal.commentary || whyPlan;
    const acceptedStarts = this.acceptedStarts.length ? this.acceptedStarts : reveal.siblingMoves;
    const featuredStart = this.featuredStartSan || 'n/a';
    const alternateStarts = acceptedStarts.filter(start => start !== featuredStart);
    const dominant = this.payload.puzzle.dominantFamily;
    return `
      <div class="sp-runtime-reveal-stack">
        <div class="sp-summary-card">
          <p class="sp-summary-card__eyebrow">Task</p>
          <h4>${escapeHtml(planTask)}</h4>
          <p>${escapeHtml(shorten(taskLead, 420))}</p>
        </div>
        <div class="sp-summary-card">
          <p class="sp-summary-card__eyebrow">Why This Plan</p>
          <h4>${escapeHtml(this.revealThemeLabel(reveal, plan))}</h4>
          <p>${escapeHtml(shorten(whyPlan, 900))}</p>
        </div>
        <div class="sp-summary-card">
          <p class="sp-summary-card__eyebrow">Why This Start</p>
          <h4>${escapeHtml(featuredStart)}</h4>
          <p>${escapeHtml(shorten(whyMove, 900))}</p>
        </div>
        ${this.renderProofReview(reveal, featuredStart)}
        <details class="sp-runtime-alt-starts">
          <summary>Other good starts</summary>
          <div class="sp-runtime-alt-starts__body">
            <strong>${escapeHtml(alternateStarts.length ? alternateStarts.join(', ') : 'No other starts stored')}</strong>
            <p>${alternateStarts.length ? 'These starts still converge to the same bounded task.' : 'This reveal stores only one public start for the task.'}</p>
          </div>
        </details>
        <div class="sp-mini-facts">
          <div><strong>Plan</strong><span>${escapeHtml(plan?.familyKey ? humanize(plan.familyKey) : reveal.familyKey ? humanize(reveal.familyKey) : dominant?.key ? humanize(dominant.key) : 'n/a')}</span></div>
          <div><strong>Featured start</strong><span>${escapeHtml(featuredStart)}</span></div>
          <div><strong>Opening</strong><span>${escapeHtml(reveal.opening || 'n/a')}</span></div>
          <div><strong>ECO</strong><span>${escapeHtml(reveal.eco || 'n/a')}</span></div>
        </div>
        ${this.renderRecentAttempts()}
      </div>
    `;
  }

  private renderProofReview(reveal: TerminalReveal, featuredStart: string): string {
    const proofLine = reveal.lineSan.length ? reveal.lineSan.join(' ') : this.lineSans.length ? this.lineSans.join(' ') : 'Proof line unavailable';
    if (this.revealFocus === 'proof') {
      return `
        <article class="sp-line-card sp-line-card--inline">
          <p class="sp-line-card__label">Exact proof</p>
          <h3>${escapeHtml(proofLine)}</h3>
          <p>The board above is now following the confirmed continuation. Switch back to the started position whenever you want to review why ${escapeHtml(featuredStart)} is the right beginning.</p>
          <div class="sp-runtime-actions">
            <button type="button" class="sp-demo-link" data-action="show-start-board">Back to started position</button>
          </div>
        </article>
      `;
    }
    return `
      <article class="sp-line-card sp-line-card--inline">
        <p class="sp-line-card__label">How The Task Begins</p>
        <h3>${escapeHtml(featuredStart)}</h3>
        <p>The board above is paused immediately after ${escapeHtml(featuredStart)}. Open the exact proof only when you want the confirmed continuation and deeper proof position.</p>
        <div class="sp-runtime-actions">
          <button type="button" class="sp-demo-link is-strong" data-action="show-proof-board">Open exact proof on the board</button>
        </div>
      </article>
    `;
  }

  private renderPaneFooter(accountPatternsUrl: string | null): string {
    const footerCopy =
      this.stage === 'reveal'
        ? this.nextAvailable
          ? 'Another plan-first puzzle is ready when you want the next position.'
          : this.payload.progress.authenticated
            ? 'This account has cleared the current public puzzle pool. Replay the review or jump to your games.'
            : 'Replay the plan shell from the start, or keep sampling anonymously.'
        : this.stage === 'move'
          ? 'Changing the task resets the move stage. Giving up opens the featured review from a stored start.'
          : 'Choose the task first. If you give up, the featured task and its review will open.';
    return `
      <div class="sp-runtime-pane__footer${this.stage === 'reveal' ? ' is-reveal' : ''}">
        <div class="sp-runtime-pane__footer-copy">
          <strong>Next action</strong>
          <span>${escapeHtml(footerCopy)}</span>
        </div>
        <div class="sp-runtime-pane__footer-actions sp-runtime-actions">
          ${this.stage === 'reveal' ? this.renderRevealFooterActions(accountPatternsUrl) : this.renderSolveFooterActions()}
        </div>
      </div>
    `;
  }

  private renderSolveFooterActions(): string {
    return `
      ${this.stage === 'move' ? `<button type="button" class="sp-demo-link" data-action="back-plans">Back to tasks</button>` : ''}
      <button type="button" class="sp-demo-link" data-action="reset">Reset puzzle</button>
      <button type="button" class="sp-demo-link is-warning" data-action="reveal">Give up and open review</button>
    `;
  }

  private renderRevealFooterActions(accountPatternsUrl: string | null): string {
    return `
      ${this.nextAvailable ? `<button type="button" class="sp-demo-link is-strong" data-action="next">Next puzzle</button>` : ''}
      <button type="button" class="sp-demo-link" data-action="reset">Replay puzzle</button>
      ${accountPatternsUrl ? `<a href="${accountPatternsUrl}" class="sp-demo-link">See this in my games</a>` : ''}
    `;
  }

  private renderRecentAttempts(): string {
    if (!this.reveal || !this.payload.progress.authenticated || !this.payload.progress.recentAttempts.length) return '';
    return `
      <details class="sp-history-drawer"${this.historyOpen ? ' open' : ''}>
        <summary>Recent attempts</summary>
        <div class="sp-history-drawer__body">
          ${this.payload.progress.recentAttempts
            .slice(0, 6)
            .map(
              attempt => `
                <div class="sp-history-entry">
                  <strong>${escapeHtml(humanDate(attempt.completedAt))}</strong>
                  <span>${escapeHtml(humanize(attempt.status))}</span>
                </div>
              `,
            )
            .join('')}
        </div>
      </details>
    `;
  }

  private bindBoard() {
    const boardEl = this.app.querySelector('#sp-runtime-board') as HTMLElement | null;
    if (!boardEl) return;
    const config = {
      fen: this.currentFen,
      lastMove: this.currentBoardLastMove(),
      orientation: this.orientation,
      coordinates: true,
      autoCastle: false,
      movable: {
        free: this.stage === 'move',
        color: (this.stage === 'move' ? this.orientation : undefined) as any,
      },
      premovable: { enabled: false },
      selectable: { enabled: true },
      highlight: { lastMove: true },
      animation: { duration: 220 },
      events: {
        move: (orig: Key, dest: Key) => this.handleBoardMove(orig, dest),
      },
    };
    if (!this.cg) {
      this.cg = makeChessground(boardEl, config);
      return;
    }
    this.cg.set(config as any);
  }

  private bindButtons() {
    this.app.querySelector<HTMLElement>('[data-action="back-plans"]')?.addEventListener('click', () => this.backToPlans());
    this.app.querySelector<HTMLElement>('[data-action="reset"]')?.addEventListener('click', () => this.reset());
    this.app.querySelector<HTMLElement>('[data-action="reveal"]')?.addEventListener('click', () => this.revealFeaturedProof());
    this.app.querySelector<HTMLElement>('[data-action="show-proof-board"]')?.addEventListener('click', () => this.showProofBoard());
    this.app.querySelector<HTMLElement>('[data-action="show-start-board"]')?.addEventListener('click', () => this.showStartedBoard());
    this.app.querySelector<HTMLElement>('[data-action="next"]')?.addEventListener('click', () => this.loadNext());
    this.app.querySelectorAll<HTMLElement>('[data-plan-id]').forEach(button => {
      button.addEventListener('click', () => {
        const planId = button.dataset.planId;
        if (planId) this.selectPlan(planId);
      });
    });
    this.app.querySelectorAll<HTMLElement>('[data-start-uci]').forEach(button => {
      button.addEventListener('click', () => {
        const uci = button.dataset.startUci;
        const start = this.currentStarts.find(it => it.uci === uci);
        if (start) this.playStart(start);
      });
    });
    const historyDrawer = this.app.querySelector<HTMLDetailsElement>('.sp-history-drawer');
    if (historyDrawer) historyDrawer.ontoggle = () => (this.historyOpen = historyDrawer.open);
  }

  private handleBoardMove(orig: Key, dest: Key) {
    if (this.stage !== 'move') {
      this.feedback = { kind: 'neutral', text: 'Choose the task first. The board becomes interactive when the move stage opens.' };
      this.render();
      return;
    }
    const prefix = `${orig}${dest}`;
    const start = this.currentStarts.find(it => it.uci.startsWith(prefix));
    if (!start) {
      void site.sound?.play?.('genericNotify', 0.7);
      this.flashBoard('warning');
      this.selectedStartUci = null;
      this.feedback = {
        kind: 'warning',
        text: this.selectedPlan
          ? 'That move does not start the selected task. Keep the bounded plan fixed, then try again.'
          : 'Choose the task first, then select its start move.',
      };
      this.render();
      return;
    }
    this.playStart(start);
  }

  private selectPlan(planId: string) {
    const plan = this.planMap.get(planId);
    if (!plan) return;
    this.stage = 'move';
    this.revealFocus = 'start';
    this.selectedPlanId = plan.id;
    this.selectedStartUci = null;
    this.proof = null;
    this.completion = null;
    this.historyOpen = false;
    this.feedback = { kind: 'neutral', text: plan.feedback };
    this.render();
  }

  private backToPlans() {
    this.stage = 'plan';
    this.revealFocus = 'start';
    this.selectedStartUci = null;
    this.proof = null;
    this.feedback = { kind: 'neutral', text: 'Choose the task before choosing the start move.' };
    this.completion = null;
    this.render();
  }

  private playStart(start: PlanStart) {
    if (this.busy || !this.selectedPlan) return;
    const proof = this.followProofFromStart(this.selectedPlan.id, start);
    if (!proof) {
      this.feedback = { kind: 'warning', text: 'The stored proof for that start is incomplete, so the reveal could not be opened.' };
      this.render();
      return;
    }
    this.selectedStartUci = start.uci;
    this.proof = proof;
    this.stage = 'reveal';
    this.revealFocus = 'start';
    this.feedback = { kind: 'success', text: start.feedback };
    void site.sound?.move?.({ san: start.san, filter: 'game', volume: 0.8 });
    this.flashBoard('success');
    void this.complete(false);
    this.render();
  }

  private revealFeaturedProof() {
    const proof = this.followFeaturedProof(this.selectedPlanId || undefined);
    if (!proof) return;
    this.selectedPlanId = proof.planId;
    this.selectedStartUci = proof.start.uci;
    this.proof = proof;
    this.stage = 'reveal';
    this.revealFocus = 'start';
    this.feedback = { kind: 'neutral', text: 'The featured review is open from the stored start. This counts as a give-up for the current puzzle.' };
    void this.complete(true);
    this.render();
  }

  private showProofBoard() {
    if (!this.proof) return;
    this.revealFocus = 'proof';
    this.render();
  }

  private showStartedBoard() {
    if (!this.proof) return;
    this.revealFocus = 'start';
    this.render();
  }

  private followFeaturedProof(planId?: string): ProofResolution | null {
    const plan = planId ? this.planMap.get(planId) : this.payload.runtimeShell.plans[0];
    if (!plan) return null;
    const start =
      (plan.featuredStartUci && plan.allowedStarts.find(candidate => candidate.uci === plan.featuredStartUci)) ||
      [...plan.allowedStarts].sort((left, right) => creditRank(right.credit) - creditRank(left.credit))[0];
    return start ? this.followProofFromStart(plan.id, start) : null;
  }

  private followProofFromStart(planId: string, start: PlanStart): ProofResolution | null {
    const rootChoice = this.proofLayer.rootChoices.find(choice => choice.uci === start.uci);
    if (!rootChoice) return null;
    return this.followCanonicalChoice(planId, start, rootChoice, null, [], [], this.payload.runtimeShell.startFen, 0);
  }

  private followCanonicalChoice(
    planId: string,
    start: PlanStart,
    choice: ShellChoice,
    nodeId: string | null,
    lineUcis: string[],
    lineSan: string[],
    currentFen: string,
    hop: number,
    startFen?: string,
  ): ProofResolution | null {
    if (hop >= 5) return null;
    const withPlayerUcis = [...lineUcis, choice.uci];
    const withPlayerSan = [...lineSan, choice.san];
    const afterPlayerFen = choice.afterFen || currentFen;
    const resolvedStartFen = startFen || afterPlayerFen;
    if (choice.terminalId) {
      const terminal = this.terminalMap.get(choice.terminalId);
      if (!terminal) return null;
      return {
        planId,
        start,
        terminal,
        lineUcis: withPlayerUcis,
        lineSan: withPlayerSan,
        startFen: resolvedStartFen,
        finalFen: afterPlayerFen,
      };
    }
    const replyLookup = nodeId ? `${nodeId}:${choice.uci}` : `root:${choice.uci}`;
    const forced = this.replyMap.get(replyLookup);
    if (!forced || !forced.nextNodeId) return null;
    const withReplyUcis = [...withPlayerUcis, forced.uci];
    const withReplySan = [...withPlayerSan, forced.san];
    const nextNode = this.nodeMap.get(forced.nextNodeId);
    const nextChoice = nextNode?.choices.find(candidate => candidate.credit === 'full') || nextNode?.choices[0];
    if (!nextChoice) return null;
    return this.followCanonicalChoice(planId, start, nextChoice, forced.nextNodeId, withReplyUcis, withReplySan, forced.afterFen, hop + 1, resolvedStartFen);
  }

  private async complete(giveUp = false) {
    const activePuzzleId = this.payload.puzzle.id;
    try {
      const res = await fetch(`/api/strategic-puzzle/${this.payload.puzzle.id}/complete`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          planId: this.selectedPlanId,
          startUci: this.selectedStartUci,
          giveUp,
        }),
      });
      if (!res.ok) return;
      this.completion = (await res.json()) as CompleteResponse;
      if (this.completion.saved) this.payload.progress.currentStreak = this.completion.currentStreak;
      if (this.payload.progress.authenticated && !this.completion.nextPuzzleId) {
        this.feedback = { kind: 'neutral', text: 'You have cleared every published strategic puzzle in the current pool.' };
      }
      if (this.payload.puzzle.id === activePuzzleId) this.render();
    } catch (err) {
      console.warn('strategic puzzle completion failed', err);
    }
  }

  private async loadNext() {
    const nextPuzzleId = this.completion?.nextPuzzleId;
    if (!nextPuzzleId) {
      this.feedback = { kind: 'neutral', text: 'No uncleared strategic puzzle is left for this account right now.' };
      this.render();
      return;
    }
    this.busy = true;
    try {
      const nextPayload = await this.loadBootstrapById(nextPuzzleId);
      if (!nextPayload) {
        this.feedback = { kind: 'neutral', text: 'No uncleared strategic puzzle is left for this account right now.' };
        this.render();
        return;
      }
      this.payload = nextPayload;
      this.stage = 'plan';
      this.revealFocus = 'start';
      this.selectedPlanId = null;
      this.selectedStartUci = null;
      this.proof = null;
      this.historyOpen = false;
      this.feedback = { kind: 'neutral', text: '' };
      this.completion = null;
      this.pushHistoryState(`/strategic-puzzle/${nextPuzzleId}`);
      this.render();
    } catch (err) {
      console.warn('strategic puzzle next fetch failed', err);
    } finally {
      this.busy = false;
    }
  }

  private reset() {
    this.stage = 'plan';
    this.revealFocus = 'start';
    this.selectedPlanId = null;
    this.selectedStartUci = null;
    this.proof = null;
    this.historyOpen = false;
    this.feedback = { kind: 'neutral', text: 'The puzzle was reset to the exact start position.' };
    this.completion = null;
    this.render();
  }

  private flashBoard(kind: Exclude<FeedbackKind, 'neutral'>) {
    this.boardFeedback = kind;
    this.render();
    window.setTimeout(() => {
      if (this.boardFeedback !== kind) return;
      this.boardFeedback = null;
      this.render();
    }, 280);
  }

  private currentBoardLastMove(): Key[] | undefined {
    if (this.stage !== 'reveal' || !this.proof) return undefined;
    return this.revealFocus === 'proof'
      ? moveSquaresFromUci(this.proof.lineUcis[this.proof.lineUcis.length - 1])
      : moveSquaresFromUci(this.proof.start.uci);
  }
}

export function initModule(payload: BootstrapPayload) {
  const app = document.getElementById('strategic-puzzle-app');
  if (!app) return;
  new StrategicPuzzleApp(payload, app).mount();
}

function readHistorySnapshot(state: unknown): StrategicPuzzleSnapshot | null {
  const snapshot = (state as StrategicPuzzleHistoryState | null | undefined)?.strategicPuzzle;
  return snapshot && typeof snapshot.url === 'string' && typeof snapshot.puzzleId === 'string' ? snapshot : null;
}

function humanize(value: string) {
  return value.replace(/\|/g, ' / ').replace(/_/g, ' ');
}

function capitalize(value: string) {
  return value.charAt(0).toUpperCase() + value.slice(1);
}

function humanDate(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
  }).format(date);
}

function shorten(value: string, max: number) {
  return value.length <= max ? value : `${value.slice(0, max - 1).trimEnd()}…`;
}

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
}

function creditRank(credit: Credit): number {
  switch (credit) {
    case 'full':
      return 2;
    case 'partial':
      return 1;
    default:
      return 0;
  }
}

function moveSquaresFromUci(uci: string | null | undefined): Key[] | undefined {
  if (!uci || uci.length < 4) return undefined;
  const orig = uci.slice(0, 2) as Key;
  const dest = uci.slice(2, 4) as Key;
  return [orig, dest];
}

export default initModule;
