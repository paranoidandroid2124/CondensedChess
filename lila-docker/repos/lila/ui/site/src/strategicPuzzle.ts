import { Chessground as makeChessground } from '@lichess-org/chessground';
import type { Api as ChessgroundApi } from '@lichess-org/chessground/api';
import type { DrawShape } from '@lichess-org/chessground/draw';
import { parseUci, makeSquare } from 'chessops/util';
import { myUsername } from 'lib';

type Credit = 'full' | 'partial';
type Outcome = 'full' | 'partial' | 'wrong' | 'giveup';
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
}

interface RuntimeShell {
  schema: string;
  startFen: string;
  sideToMove: Color;
  prompt: string;
  rootChoices: ShellChoice[];
  nodes: PlayerNode[];
  forcedReplies: ForcedReply[];
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
  nextPuzzleUrl?: string;
}

type FeedbackKind = 'neutral' | 'success' | 'warning';

interface FeedbackState {
  kind: FeedbackKind;
  text: string;
}

interface StrategicPuzzleSnapshot {
  url: string;
  payload: BootstrapPayload;
  currentFen: string;
  currentNodeId: string | null;
  lineUcis: string[];
  lineSans: string[];
  reveal: TerminalReveal | null;
  feedback: FeedbackState;
  completion: CompleteResponse | null;
  hintStep: 0 | 1 | 2;
  choiceAssistVisible: boolean;
  selectedChoiceId: string | null;
  historyOpen: boolean;
}

interface StrategicPuzzleHistoryState {
  strategicPuzzle: StrategicPuzzleSnapshot;
}

class StrategicPuzzleApp {
  private cg: ChessgroundApi | undefined;
  private payload: BootstrapPayload;
  private readonly app: HTMLElement;
  private currentFen: string;
  private currentNodeId: string | null = null;
  private lineUcis: string[] = [];
  private lineSans: string[] = [];
  private reveal: TerminalReveal | null = null;
  private feedback: FeedbackState = { kind: 'neutral', text: '' };
  private busy = false;
  private completion: CompleteResponse | null = null;
  private hintStep: 0 | 1 | 2 = 0;
  private choiceAssistVisible = false;
  private boardFeedback: Exclude<FeedbackKind, 'neutral'> | null = null;
  private selectedChoiceId: string | null = null;
  private historyOpen = false;

  constructor(payload: BootstrapPayload, app: HTMLElement) {
    this.payload = payload;
    this.app = app;
    this.currentFen = payload.runtimeShell.startFen;
  }

  mount() {
    window.addEventListener('popstate', this.onPopState);
    this.render();
  }

  private get nodeMap(): Map<string, PlayerNode> {
    return new Map(this.payload.runtimeShell.nodes.map(node => [node.id, node]));
  }

  private get replyMap(): Map<string, ForcedReply> {
    return new Map(this.payload.runtimeShell.forcedReplies.map(reply => [reply.fromNodeId, reply]));
  }

  private get terminalMap(): Map<string, TerminalReveal> {
    return new Map(this.payload.runtimeShell.terminals.map(terminal => [terminal.id, terminal]));
  }

  private get currentStep(): number {
    return this.currentNodeId ? this.nodeMap.get(this.currentNodeId)?.step || 1 : 1;
  }

  private get currentPrompt(): string {
    return this.currentNodeId ? this.nodeMap.get(this.currentNodeId)?.prompt || this.payload.runtimeShell.prompt : this.payload.runtimeShell.prompt;
  }

  private get currentChoices(): ShellChoice[] {
    return this.currentNodeId ? this.nodeMap.get(this.currentNodeId)?.choices || [] : this.payload.runtimeShell.rootChoices;
  }

  private get orientation(): Color {
    return this.payload.puzzle.position.sideToMove;
  }

  private get nextAvailable(): boolean {
    if (!this.reveal) return false;
    if (!this.payload.progress.authenticated) return true;
    return Boolean(this.completion?.nextPuzzleId);
  }

  private render() {
    this.cg?.destroy();
    this.app.innerHTML = this.view();
    this.bindBoard();
    this.bindButtons();
    this.replaceHistoryState();
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
      payload: this.payload,
      currentFen: this.currentFen,
      currentNodeId: this.currentNodeId,
      lineUcis: this.lineUcis.slice(),
      lineSans: this.lineSans.slice(),
      reveal: this.reveal,
      feedback: { ...this.feedback },
      completion: this.completion,
      hintStep: this.hintStep,
      choiceAssistVisible: this.choiceAssistVisible,
      selectedChoiceId: this.selectedChoiceId,
      historyOpen: this.historyOpen,
    };
  }

  private currentUrl(): string {
    return `${window.location.pathname}${window.location.search}${window.location.hash}`;
  }

  private restoreSnapshot(snapshot: StrategicPuzzleSnapshot) {
    this.payload = snapshot.payload;
    this.currentFen = snapshot.currentFen;
    this.currentNodeId = snapshot.currentNodeId;
    this.lineUcis = snapshot.lineUcis.slice();
    this.lineSans = snapshot.lineSans.slice();
    this.reveal = snapshot.reveal;
    this.feedback = { ...snapshot.feedback };
    this.completion = snapshot.completion;
    this.hintStep = snapshot.hintStep;
    this.choiceAssistVisible = snapshot.choiceAssistVisible ?? false;
    this.selectedChoiceId = snapshot.selectedChoiceId ?? null;
    this.historyOpen = snapshot.historyOpen ?? false;
    this.busy = false;
    this.boardFeedback = null;
    this.render();
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
    this.restoreSnapshot(snapshot);
  };

  private view(): string {
    const reveal = this.reveal;
    const introText = 'Work through three planning decisions on the board. The explanation opens after you finish the line or show the solution.';
    const accountPatternsUrl = this.accountPatternsUrl();
    const streakLabel = this.payload.progress.authenticated ? `Current streak ${this.payload.progress.currentStreak}` : 'Anonymous session';
    return `
      <section class="sp-demo-shell sp-runtime-shell${reveal ? ' has-reveal' : ''}">
        <section class="sp-runtime-topbar">
          <div class="sp-runtime-topbar__lead">
            <p class="sp-demo-kicker">Strategic Puzzle</p>
            <h1>${this.orientation === 'white' ? 'White' : 'Black'} to move</h1>
            <p class="sp-runtime-intro">${escapeHtml(introText)}</p>
          </div>
          <div class="sp-runtime-topbar__stats">
            <div class="sp-metric-card"><strong>${this.currentStep} / 3</strong><span>${reveal ? 'review ready' : 'current step'}</span></div>
            <div class="sp-runtime-topbar__status">
              <span class="sp-chip sp-chip--streak">${escapeHtml(streakLabel)}</span>
            </div>
          </div>
        </section>
        <article class="sp-demo-board-card sp-demo-board-card--runtime">
          <div class="sp-runtime-board-shell">
            <div class="sp-runtime-board-stage${this.boardFeedback ? ` is-${this.boardFeedback}` : ''}">
              <div class="sp-runtime-board-meta">
                <span class="sp-chip sp-chip--turn">${escapeHtml(capitalize(this.orientation))} to move</span>
                <span class="sp-chip sp-chip--theme">${escapeHtml(this.payload.puzzle.dominantFamily?.dominantIdeaKind ? humanize(this.payload.puzzle.dominantFamily.dominantIdeaKind) : 'strategic puzzle')}</span>
                <span class="sp-chip sp-chip--echo">3-step plan</span>
              </div>
              <div id="sp-runtime-board" class="sp-runtime-board"></div>
            </div>
          </div>
        </article>
        ${this.renderStatePane(accountPatternsUrl)}
      </section>
    `;
  }

  private renderStatePane(accountPatternsUrl: string | null): string {
    const reveal = this.reveal;
    const panelLabel = reveal ? 'Explanation' : 'Solve';
    const panelTitle = reveal ? escapeHtml(reveal.title) : 'Choose the move that keeps the plan alive.';
    const panelCopy = reveal ? escapeHtml(reveal.summary) : escapeHtml(this.currentPrompt);
    const contextTitle = reveal ? 'Review focus' : 'Your task';
    const contextCopy = reveal
      ? 'Keep the finished route on the board while you review why it works and decide what to do next.'
      : `${escapeHtml(this.currentPrompt)} ${
          this.choiceAssistVisible ? 'Candidate help is open if you want it.' : 'Use the board first. Hint or a wrong move will open candidate help.'
        }`;
    return `
      <section class="sp-demo-panel sp-runtime-pane${reveal ? ' is-reveal' : ' is-solve'}">
        <div class="sp-runtime-pane__scroll">
          <div class="sp-runtime-pane__section sp-runtime-pane__section--head">
            <p class="sp-demo-panel__label">${panelLabel}</p>
            ${!reveal ? `
              <div class="sp-stepper">
                <span class="${this.currentStep >= 1 ? 'is-live' : ''}">1. Start the plan</span>
                <span class="${this.currentStep >= 2 ? 'is-live' : ''}">2. Keep the plan</span>
                <span class="${this.currentStep >= 3 ? 'is-live' : ''}">3. Finish the line</span>
              </div>
            ` : ''}
            <h3>${panelTitle}</h3>
            <p class="sp-demo-panel__copy">${panelCopy}</p>
          </div>
          <div class="sp-runtime-pane__context">
            <div class="sp-callout">
              <strong>Line so far</strong>
              <span>${this.lineSans.length ? escapeHtml(this.lineSans.join(' ')) : 'No moves played yet.'}</span>
            </div>
            <div class="sp-callout">
              <strong>${contextTitle}</strong>
              <span>${contextCopy}</span>
            </div>
          </div>
          <div class="sp-feedback-strip ${this.feedback.kind === 'success' ? 'is-success' : this.feedback.kind === 'warning' ? 'is-warning' : ''}">
            <div>
              <strong>${this.feedback.kind === 'success' ? 'Accepted' : this.feedback.kind === 'warning' ? 'Retry' : 'Guidance'}</strong>
              <span>${escapeHtml(this.feedback.text || 'Use the board first. Hint or a wrong move will open candidate help. Show the solution only when you want the explanation right away.')}</span>
            </div>
          </div>
          ${!reveal ? this.renderChoiceGrid() : this.renderRevealStack(reveal)}
        </div>
        ${this.renderPaneFooter(accountPatternsUrl)}
      </section>
    `;
  }

  private renderRevealStack(reveal: TerminalReveal): string {
    const dominant = this.payload.puzzle.dominantFamily;
    return `
      <div class="sp-runtime-reveal-stack">
        <div class="sp-summary-card">
          <p class="sp-summary-card__eyebrow">Why this works</p>
          <h4>${escapeHtml(reveal.familyKey ? humanize(reveal.familyKey) : 'Plan review')}</h4>
          <p>${escapeHtml(shorten(reveal.commentary || reveal.summary, 900))}</p>
        </div>
        <article class="sp-line-card sp-line-card--inline">
          <p class="sp-line-card__label">Main line</p>
          <h3>${escapeHtml(reveal.lineSan.length ? reveal.lineSan.join(' ') : 'Main line unavailable')}</h3>
          <p>This is the demonstrated route from the current position.</p>
        </article>
        <details class="sp-runtime-alt-starts">
          <summary>Other good first moves</summary>
          <div class="sp-runtime-alt-starts__body">
            <strong>${escapeHtml(reveal.siblingMoves.length ? reveal.siblingMoves.join(', ') : 'No other starts stored')}</strong>
            <p>${reveal.siblingMoves.length ? 'Other starts still reach the same strategic finish.' : 'No other starts are stored for this route.'}</p>
          </div>
        </details>
        <div class="sp-mini-facts">
          <div><strong>Pattern</strong><span>${escapeHtml(reveal.familyKey ? humanize(reveal.familyKey) : dominant?.key ? humanize(dominant.key) : 'n/a')}</span></div>
          <div><strong>Opening</strong><span>${escapeHtml(reveal.opening || 'Hidden in solve mode')}</span></div>
          <div><strong>ECO</strong><span>${escapeHtml(reveal.eco || 'n/a')}</span></div>
        </div>
        ${this.renderRecentAttempts()}
      </div>
    `;
  }

  private renderPaneFooter(accountPatternsUrl: string | null): string {
    const reveal = this.reveal;
    const footerCopy = reveal
      ? this.nextAvailable
        ? 'Another explained position is ready when you want the next puzzle.'
        : this.payload.progress.authenticated
          ? 'This account has cleared the current puzzle pool. Replay this position or jump to your games.'
          : 'Replay this position from the start, or keep sampling anonymously.'
      : 'Show the solution ends the attempt and keeps the explanation in this pane.';
    return `
      <div class="sp-runtime-pane__footer${reveal ? ' is-reveal' : ''}">
        <div class="sp-runtime-pane__footer-copy">
          <strong>Next action</strong>
          <span>${footerCopy}</span>
        </div>
        <div class="sp-runtime-pane__footer-actions sp-runtime-actions">
          ${reveal ? this.renderRevealFooterActions(accountPatternsUrl) : this.renderSolveFooterActions()}
        </div>
      </div>
    `;
  }

  private renderSolveFooterActions(): string {
    return `
      <button type="button" class="sp-demo-link" data-action="hint" ${this.hintStep >= 2 ? 'disabled aria-disabled="true"' : ''}>${this.hintStep === 0 ? 'Hint' : this.hintStep === 1 ? 'More hint' : 'Hint shown'}</button>
      ${this.feedback.kind === 'warning' ? `<button type="button" class="sp-demo-link" data-action="retry">Try again</button>` : ''}
      <button type="button" class="sp-demo-link" data-action="reset">Reset line</button>
      <button type="button" class="sp-demo-link is-warning" data-action="reveal">Show the solution</button>
    `;
  }

  private renderRevealFooterActions(accountPatternsUrl: string | null): string {
    return `
      ${this.nextAvailable ? `<button type="button" class="sp-demo-link is-strong" data-action="next">Next puzzle</button>` : ''}
      <button type="button" class="sp-demo-link" data-action="reset">Replay puzzle</button>
      ${accountPatternsUrl ? `<a href="${accountPatternsUrl}" class="sp-demo-link">See this in my games</a>` : ''}
    `;
  }

  private renderChoiceGrid(): string {
    const choices = this.currentChoices;
    if (!this.choiceAssistVisible) {
      return `
        <div class="sp-choice-grid sp-choice-grid--hidden">
          <div class="sp-choice-grid__notice">
            <strong>Candidate help is closed</strong>
            <span>Use the board first. Hint or a wrong move will open the candidate moves for this attempt.</span>
          </div>
        </div>
      `;
    }
    if (!choices.length) {
      return `
        <div class="sp-choice-grid">
          <div class="sp-choice-grid__notice">
            <strong>Move list is loading</strong>
            <span>Drag on the board or wait for the next step to show its candidate moves.</span>
          </div>
        </div>
      `;
    }
    return `
      <div class="sp-choice-grid">
        ${choices
          .map(
            choice => `
              <button type="button" class="sp-choice${this.selectedChoiceId === choice.uci ? ' is-selected' : ''}" data-choice-uci="${escapeHtml(choice.uci)}">
                <span class="sp-choice__move">${escapeHtml(choice.san || choice.label || choice.uci)}</span>
                <span class="sp-choice__copy">${escapeHtml(choice.label || 'Candidate move')}</span>
              </button>
            `,
          )
          .join('')}
      </div>
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
    this.cg = makeChessground(boardEl, {
      fen: this.currentFen,
      orientation: this.orientation,
      coordinates: true,
      autoCastle: false,
      movable: {
        free: true,
        color: this.orientation,
      },
      premovable: { enabled: false },
      selectable: { enabled: true },
      highlight: { lastMove: false },
      animation: { duration: 220 },
      events: {
        move: (orig, dest) => this.handleBoardMove(orig, dest),
      },
    });
    this.syncHintShapes();
  }

  private bindButtons() {
    this.app.querySelector<HTMLElement>('[data-action="hint"]')?.addEventListener('click', () => this.showHint());
    this.app.querySelector<HTMLElement>('[data-action="retry"]')?.addEventListener('click', () => this.retry());
    this.app.querySelector<HTMLElement>('[data-action="reset"]')?.addEventListener('click', () => this.reset());
    this.app.querySelector<HTMLElement>('[data-action="reveal"]')?.addEventListener('click', () => this.revealBestLine());
    this.app.querySelector<HTMLElement>('[data-action="next"]')?.addEventListener('click', () => this.loadNext());
    this.app.querySelectorAll<HTMLElement>('[data-choice-uci]').forEach(button => {
      button.addEventListener('click', () => {
        const uci = button.dataset.choiceUci;
        const choice = this.currentChoices.find(it => it.uci === uci);
        if (choice) this.playChoice(choice);
      });
    });
    const historyDrawer = this.app.querySelector<HTMLDetailsElement>('.sp-history-drawer');
    if (historyDrawer) historyDrawer.ontoggle = () => (this.historyOpen = historyDrawer.open);
  }

  private handleBoardMove(orig: Key, dest: Key) {
    const prefix = `${orig}${dest}`;
    const choice = this.currentChoices.find(it => it.uci.startsWith(prefix));
    if (!choice) {
      void site.sound?.play?.('genericNotify', 0.7);
      this.flashBoard('warning');
      this.selectedChoiceId = null;
      this.choiceAssistVisible = true;
      this.feedback = {
        kind: 'warning',
        text: this.currentNodeId
          ? this.nodeMap.get(this.currentNodeId)?.badMoveFeedback || 'That move breaks the current plan.'
          : 'That start does not keep the plan alive. Review the idea, then try again or reset the line.',
      };
      this.render();
      return;
    }
    this.playChoice(choice);
  }

  private playChoice(choice: ShellChoice) {
    if (this.busy) return;
    this.hintStep = 0;
    this.selectedChoiceId = choice.uci;
    this.feedback = {
      kind: choice.credit === 'full' ? 'success' : 'neutral',
      text: choice.feedback,
    };
    this.lineUcis.push(choice.uci);
    this.lineSans.push(choice.san);
    void site.sound?.move?.({ san: choice.san, filter: 'game', volume: 0.8 });
    this.flashBoard('success');
    const nextFen = choice.afterFen || this.currentFen;
    if (choice.terminalId) {
      this.currentFen = nextFen;
      this.currentNodeId = null;
      this.selectedChoiceId = null;
      this.clearHintShapes();
      this.revealTerminal(choice.terminalId, choice.credit === 'full' ? 'full' : 'partial');
      return;
    }

    const replyLookup = this.currentNodeId ? `${this.currentNodeId}:${choice.uci}` : `root:${choice.uci}`;
    const forced = this.replyMap.get(replyLookup);
    if (!forced) {
      this.revealBestLine();
      return;
    }

    this.busy = true;
    this.currentFen = nextFen;
    this.render();
    window.setTimeout(() => {
      this.lineUcis.push(forced.uci);
      this.lineSans.push(forced.san);
      this.currentFen = forced.afterFen;
      this.currentNodeId = forced.nextNodeId || null;
      this.busy = false;
      this.selectedChoiceId = null;
      void site.sound?.move?.({ san: forced.san, filter: 'game', volume: 0.65 });
      this.feedback = { kind: 'success', text: `${choice.feedback} ${forced.san} was auto-played.` };
      this.render();
    }, 320);
  }

  private revealTerminal(terminalId: string, outcome: Outcome) {
    const terminal = this.terminalMap.get(terminalId);
    if (!terminal) return;
    this.reveal = terminal;
    this.hintStep = 0;
    this.choiceAssistVisible = false;
    this.selectedChoiceId = null;
    this.clearHintShapes();
    void site.sound?.play?.('genericNotify', 0.9);
    this.complete(outcome, terminal.id, false);
    this.render();
  }

  private revealBestLine() {
    const featured = this.followFeaturedTerminal();
    if (!featured) return;
    this.reveal = featured.terminal;
    this.lineSans = featured.lineSan;
    this.lineUcis = featured.lineUcis;
    this.currentFen = featured.finalFen;
    this.currentNodeId = null;
    this.hintStep = 0;
    this.choiceAssistVisible = false;
    this.selectedChoiceId = null;
    this.clearHintShapes();
    this.feedback = { kind: 'neutral', text: 'The explanation is open and this attempt counts as a give-up.' };
    this.complete('giveup', featured.terminal.id, true);
    this.render();
  }

  private followFeaturedTerminal():
    | {
        terminal: TerminalReveal;
        lineSan: string[];
        lineUcis: string[];
        finalFen: string;
      }
    | undefined {
    let choices = this.payload.runtimeShell.rootChoices;
    let currentNodeId: string | null = null;
    let currentFen = this.payload.runtimeShell.startFen;
    const lineSan: string[] = [];
    const lineUcis: string[] = [];

    for (let hop = 0; hop < 5; hop++) {
      const fullChoice = choices.find(choice => choice.credit === 'full') || choices[0];
      if (!fullChoice) return;
      lineSan.push(fullChoice.san);
      lineUcis.push(fullChoice.uci);
      currentFen = fullChoice.afterFen || currentFen;
      if (fullChoice.terminalId) {
        const terminal = this.terminalMap.get(fullChoice.terminalId);
        if (!terminal) return;
        return { terminal, lineSan, lineUcis, finalFen: currentFen };
      }
      const replyLookup = currentNodeId ? `${currentNodeId}:${fullChoice.uci}` : `root:${fullChoice.uci}`;
      const forced = this.replyMap.get(replyLookup);
      if (!forced) return;
      lineSan.push(forced.san);
      lineUcis.push(forced.uci);
      currentFen = forced.afterFen;
      currentNodeId = forced.nextNodeId || null;
      if (!currentNodeId) return;
      choices = this.nodeMap.get(currentNodeId)?.choices || [];
    }
    return;
  }

  private async complete(status: Outcome, terminalId?: string, giveUp = false) {
    const activePuzzleId = this.payload.puzzle.id;
    try {
      const res = await fetch(`/api/strategic-puzzle/${this.payload.puzzle.id}/complete`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          lineUcis: this.lineUcis,
          status,
          terminalId,
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
    this.busy = true;
    try {
      const res = await fetch(`/api/strategic-puzzle/next?after=${encodeURIComponent(this.payload.puzzle.id)}`);
      if (!res.ok) {
        this.feedback = { kind: 'neutral', text: 'No uncleared strategic puzzle is left for this account right now.' };
        this.render();
        return;
      }
      this.payload = (await res.json()) as BootstrapPayload;
      this.currentFen = this.payload.runtimeShell.startFen;
      this.currentNodeId = null;
      this.lineUcis = [];
      this.lineSans = [];
      this.reveal = null;
      this.hintStep = 0;
      this.choiceAssistVisible = false;
      this.selectedChoiceId = null;
      this.historyOpen = false;
      this.feedback = { kind: 'neutral', text: '' };
      this.completion = null;
      this.pushHistoryState(`/strategic-puzzle/${this.payload.puzzle.id}`);
      this.render();
    } catch (err) {
      console.warn('strategic puzzle next fetch failed', err);
    } finally {
      this.busy = false;
    }
  }

  private reset() {
    this.currentFen = this.payload.runtimeShell.startFen;
    this.currentNodeId = null;
    this.lineUcis = [];
    this.lineSans = [];
    this.reveal = null;
    this.hintStep = 0;
    this.choiceAssistVisible = false;
    this.selectedChoiceId = null;
    this.historyOpen = false;
    this.clearHintShapes();
    this.feedback = { kind: 'neutral', text: 'The line was reset to the start position.' };
    this.completion = null;
    this.render();
  }

  private retry() {
    if (this.reveal) return;
    this.selectedChoiceId = null;
    this.feedback = { kind: 'neutral', text: 'Try the position again from here.' };
    this.render();
  }

  private showHint() {
    if (this.reveal || this.hintStep >= 2) return;
    this.hintStep = this.hintStep === 0 ? 1 : 2;
    this.choiceAssistVisible = true;
    this.feedback = {
      kind: 'neutral',
      text:
        this.hintStep === 1
          ? 'Hint: focus on the key pieces first.'
          : 'More hint: the intended route is now drawn on the board.',
    };
    this.render();
  }

  private syncHintShapes() {
    this.cg?.setAutoShapes(this.computeHintShapes());
  }

  private clearHintShapes() {
    this.cg?.setAutoShapes([]);
  }

  private computeHintShapes(): DrawShape[] {
    if (this.reveal || this.hintStep === 0) return [];
    const rankedChoices = [...this.currentChoices]
      .sort((a, b) => creditRank(b.credit) - creditRank(a.credit))
      .slice(0, 3);
    if (!rankedChoices.length) return [];

    if (this.hintStep === 1) {
      const seen = new Set<string>();
      return rankedChoices
        .map(choice => {
          const move = parseUci(choice.uci);
          if (!move || !('from' in move) || !('to' in move)) return null;
          const orig = makeSquare(move.from);
          if (seen.has(orig)) return null;
          seen.add(orig);
          return {
            orig,
            brush: choice.credit === 'full' ? 'green' : 'paleBlue',
          } as DrawShape;
        })
        .filter((shape): shape is DrawShape => Boolean(shape));
    }

    return rankedChoices
      .map(choice => {
        const move = parseUci(choice.uci);
        if (!move || !('from' in move) || !('to' in move)) return null;
        return {
          orig: makeSquare(move.from),
          dest: makeSquare(move.to),
          brush: choice.credit === 'full' ? 'green' : 'paleBlue',
        } as DrawShape;
      })
      .filter((shape): shape is DrawShape => Boolean(shape));
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

}

export function initModule(payload: BootstrapPayload) {
  const app = document.getElementById('strategic-puzzle-app');
  if (!app) return;
  new StrategicPuzzleApp(payload, app).mount();
}

function readHistorySnapshot(state: unknown): StrategicPuzzleSnapshot | null {
  const snapshot = (state as StrategicPuzzleHistoryState | null | undefined)?.strategicPuzzle;
  return snapshot && typeof snapshot.url === 'string' ? snapshot : null;
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

export default initModule;
