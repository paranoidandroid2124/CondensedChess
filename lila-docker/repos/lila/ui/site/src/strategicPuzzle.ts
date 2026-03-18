import { Chessground as makeChessground } from '@lichess-org/chessground';
import type { Api as ChessgroundApi } from '@lichess-org/chessground/api';

type Credit = 'full' | 'partial';
type Outcome = 'full' | 'partial' | 'wrong' | 'giveup';

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

type BetaFeedbackChoice = 'would_pay' | 'maybe' | 'not_now';

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
  betaFeedbackSubmitted: BetaFeedbackChoice | null;
  betaFeedbackMessage: string;
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
  private betaFeedbackLoading = false;
  private betaFeedbackSubmitted: BetaFeedbackChoice | null = null;
  private betaFeedbackMessage = '';

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
      betaFeedbackSubmitted: this.betaFeedbackSubmitted,
      betaFeedbackMessage: this.betaFeedbackMessage,
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
    this.betaFeedbackSubmitted = snapshot.betaFeedbackSubmitted;
    this.betaFeedbackMessage = snapshot.betaFeedbackMessage;
    this.betaFeedbackLoading = false;
    this.busy = false;
    this.render();
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
    const dominant = this.payload.puzzle.dominantFamily;
    return `
      <section class="sp-runtime-topbar">
        <div class="sp-runtime-topbar__lead">
          <p class="sp-demo-kicker">Live strategic puzzle</p>
          <h1>${this.orientation === 'white' ? 'White' : 'Black'} to move</h1>
          <p class="sp-runtime-intro">Build the route through three planned decisions. The board never calls the engine at runtime, but the shell still behaves like a short rehearsal with fixed replies and terminal reveals.</p>
        </div>
        <div class="sp-runtime-topbar__stats">
          <div class="sp-metric-card"><strong>${this.payload.progress.currentStreak}</strong><span>current streak</span></div>
          <div class="sp-metric-card"><strong>${this.currentStep} / 3</strong><span>decision step</span></div>
          <div class="sp-metric-card"><strong>${this.payload.puzzle.qualityScore.total}</strong><span>quality score</span></div>
          <div class="sp-metric-card"><strong>${this.currentChoices.length}</strong><span>active choices</span></div>
        </div>
      </section>
      <section class="sp-demo-shell sp-runtime-shell">
        <article class="sp-demo-board-card sp-demo-board-card--runtime">
          <div class="sp-runtime-board-shell">
            <div class="sp-runtime-board-stage">
              <div class="sp-runtime-board-meta">
                <span class="sp-chip sp-chip--turn">${capitalize(this.orientation)} to move</span>
                <span class="sp-chip sp-chip--theme">${escapeHtml(dominant?.dominantIdeaKind ? humanize(dominant.dominantIdeaKind) : 'strategic puzzle')}</span>
                <span class="sp-chip sp-chip--echo">3-step shell</span>
              </div>
              <div id="sp-runtime-board" class="sp-runtime-board"></div>
            </div>
            <div class="sp-runtime-inline">
              <div class="sp-callout">
                <strong>Line so far</strong>
                <span>${this.lineSans.length ? escapeHtml(this.lineSans.join(' ')) : 'No moves played yet.'}</span>
              </div>
              <div class="sp-callout">
                <strong>Current lens</strong>
                <span>${escapeHtml(this.currentPrompt)}</span>
              </div>
              <div class="sp-callout">
                <strong>Interaction</strong>
                <span>Drag any move you want. If it does not match the precomputed shell, the board snaps back and asks you to retry.</span>
              </div>
              <div class="sp-callout">
                <strong>Reveal policy</strong>
                <span>Opening, ECO, dominant family, and commentary stay hidden until the line resolves.</span>
              </div>
            </div>
          </div>
        </article>
        <div class="sp-demo-side">
          <section class="sp-demo-panel sp-demo-panel--solve">
            <p class="sp-demo-panel__label">Now solve</p>
            <div class="sp-stepper">
              <span class="${this.currentStep >= 1 ? 'is-live' : ''}">1. Start the route</span>
              <span class="${this.currentStep >= 2 ? 'is-live' : ''}">2. Keep the plan</span>
              <span class="${this.currentStep >= 3 ? 'is-live' : ''}">3. Finish the shell</span>
            </div>
            <h3>Choose the move that keeps the plan alive.</h3>
            <p class="sp-demo-panel__copy">${escapeHtml(this.currentPrompt)}</p>
            <div class="sp-feedback-strip ${this.feedback.kind === 'success' ? 'is-success' : this.feedback.kind === 'warning' ? 'is-warning' : ''}">
              <div>
                <strong>${this.feedback.kind === 'success' ? 'Accepted' : this.feedback.kind === 'warning' ? 'Retry' : 'Guidance'}</strong>
                <span>${escapeHtml(this.feedback.text || 'Use the board to test the plan. Correct choices drive the fixed replies automatically.')}</span>
              </div>
            </div>
            <div class="sp-choice-grid">
              ${this.currentChoices.map(choice => `
                <button type="button" class="sp-choice ${choice.credit === 'full' ? 'is-primary' : ''}" data-choice="${choice.uci}">
                  <span class="sp-choice__move">${escapeHtml(choice.san)}</span>
                  <small>${escapeHtml(choice.label || (choice.credit === 'full' ? 'Main line' : 'Alternate'))}</small>
                </button>
              `).join('')}
            </div>
            <div class="sp-runtime-actions">
              <button type="button" class="sp-demo-link" data-action="reveal">${reveal ? 'Reveal shown' : 'Reveal best line'}</button>
              <button type="button" class="sp-demo-link" data-action="reset">Reset line</button>
              ${reveal && this.nextAvailable ? `<button type="button" class="sp-demo-link is-strong" data-action="next">Next puzzle</button>` : ''}
            </div>
          </section>
          <section class="sp-demo-panel sp-demo-panel--reveal ${reveal ? 'is-open' : 'is-hidden'}">
            <p class="sp-demo-panel__label">Reveal state</p>
            ${reveal ? `
              <h3>${escapeHtml(reveal.title)}</h3>
              <p class="sp-demo-panel__copy">${escapeHtml(reveal.summary)}</p>
              <div class="sp-summary-card">
                <p class="sp-summary-card__eyebrow">${reveal.outcome === 'full' ? 'Terminal explanation' : 'Alternate terminal'}</p>
                <h4>${escapeHtml(reveal.familyKey ? humanize(reveal.familyKey) : 'Strategic explanation')}</h4>
                <p>${escapeHtml(shorten(reveal.commentary || reveal.summary, 900))}</p>
              </div>
              <div class="sp-mini-facts">
                <div><strong>Family</strong><span>${escapeHtml(reveal.familyKey ? humanize(reveal.familyKey) : dominant?.key ? humanize(dominant.key) : 'n/a')}</span></div>
                <div><strong>Opening</strong><span>${escapeHtml(reveal.opening || 'Hidden in solve mode')}</span></div>
                <div><strong>ECO</strong><span>${escapeHtml(reveal.eco || 'n/a')}</span></div>
              </div>
            ` : `
              <h3>Terminal explanation stays hidden</h3>
              <p class="sp-demo-panel__copy">Reach a terminal or use “Reveal best line” to open the explanation panel.</p>
            `}
          </section>
        </div>
      </section>
      <section class="sp-demo-lines sp-runtime-lines ${reveal ? 'is-open' : ''}">
        <div class="sp-demo-section-head">
          <p class="sp-demo-kicker">Line rail</p>
          <h2>${reveal ? 'Reached terminal' : 'Shell branches stay compressed until reveal'}</h2>
          <p>${reveal ? 'The line rail now shows the exact sequence you reached and the sibling continuations stored in the same terminal.' : 'The page keeps the analytical shape without dumping engine trees before the solve is over.'}</p>
        </div>
        <div class="sp-line-grid">
          ${reveal ? `
            <article class="sp-line-card">
              <p class="sp-line-card__label">Reached line</p>
              <h3>${escapeHtml(reveal.lineSan.join(' '))}</h3>
              <p>${escapeHtml(reveal.summary)}</p>
            </article>
            <article class="sp-line-card">
              <p class="sp-line-card__label">Sibling continuations</p>
              <h3>${reveal.siblingMoves.length ? escapeHtml(reveal.siblingMoves.join(', ')) : 'No sibling moves stored'}</h3>
              <p>Sibling moves share the same terminal family and are treated as the same strategic finish.</p>
            </article>
            <article class="sp-line-card">
              <p class="sp-line-card__label">Persistence</p>
              <h3>${this.completion?.saved ? 'Saved for your account' : this.payload.progress.authenticated ? 'Ready to save on terminal' : 'Anonymous session'}</h3>
              <p>${this.payload.progress.authenticated ? `Current streak is ${this.completion?.currentStreak ?? this.payload.progress.currentStreak}.` : 'Sign in if you want streaks and recent history to persist.'}</p>
            </article>
            <article class="sp-line-card">
              <p class="sp-line-card__label">What happens next</p>
              <h3>${this.nextAvailable ? 'Another uncleared puzzle is ready' : this.payload.progress.authenticated ? 'You finished the current public pool' : 'Anonymous mode can keep sampling'}</h3>
              <p>${this.nextAvailable ? 'The next button now pulls another puzzle that has not yet been cleared by this account.' : this.payload.progress.authenticated ? 'This account has already cleared every published strategic puzzle in the current pool.' : 'Because anonymous play is not persisted, the site can still serve random puzzles.'}</p>
            </article>
          ` : `
            <article class="sp-line-card">
              <p class="sp-line-card__label">Accepted starts</p>
              <h3>${escapeHtml(this.payload.runtimeShell.rootChoices.filter(c => c.credit === 'full').map(c => c.san).join(', '))}</h3>
              <p>Multiple starts can still count as correct as long as they preserve the same strategic thesis.</p>
            </article>
            <article class="sp-line-card">
              <p class="sp-line-card__label">Fixed replies</p>
              <h3>${this.payload.runtimeShell.forcedReplies.length}</h3>
              <p>Opponent responses are precomputed and auto-played, so the player’s attention stays on the planning decisions.</p>
            </article>
            <article class="sp-line-card">
              <p class="sp-line-card__label">Recent history</p>
              <h3>${this.payload.progress.recentAttempts.length}</h3>
              <p>${this.payload.progress.authenticated ? 'Recent results are loaded from server-side attempt history.' : 'Anonymous sessions do not persist attempt history.'}</p>
            </article>
          `}
        </div>
      </section>
      ${reveal ? this.renderBetaFeedbackPrompt() : ''}
    `;
  }

  private renderBetaFeedbackPrompt(): string {
    const currentUrl = encodeURIComponent(this.currentUrl());
    const waitlistHref = `/beta-feedback?surface=strategic_puzzle&feature=strategic_puzzle&entrypoint=strategic_puzzle_completion&notify=true&returnTo=${currentUrl}`;
    const choices: Array<[BetaFeedbackChoice, string]> = [
      ['would_pay', 'Would pay'],
      ['maybe', 'Maybe'],
      ['not_now', 'Not for now'],
    ];
    return `
      <section class="sp-beta-feedback">
        <div class="sp-beta-feedback__copy">
          <p class="sp-demo-kicker">Open beta signal</p>
          <h2>Would you pay for a deeper strategic puzzle library if this kept helping your study?</h2>
          <p>${escapeHtml(
            this.betaFeedbackSubmitted
              ? 'Your answer is saved. If you want a launch email when paid plans open later, join the waitlist.'
              : 'We only ask after a completed or revealed puzzle so the answer comes after real use, not on the landing page.'
          )}</p>
        </div>
        <div class="sp-beta-feedback__actions">
          ${this.betaFeedbackSubmitted
            ? `<span class="sp-beta-feedback__saved">${escapeHtml(humanize(this.betaFeedbackSubmitted))}</span>`
            : choices
                .map(
                  ([value, label]) => `
                    <button
                      type="button"
                      class="sp-demo-link sp-beta-feedback__choice"
                      data-beta-willingness="${value}"
                      ${this.betaFeedbackLoading ? 'disabled' : ''}
                    >${label}</button>
                  `,
                )
                .join('')}
          <a href="${waitlistHref}" class="sp-demo-link sp-beta-feedback__waitlist">Join paid-plan waitlist</a>
        </div>
        ${this.betaFeedbackMessage ? `<p class="sp-beta-feedback__message">${escapeHtml(this.betaFeedbackMessage)}</p>` : ''}
      </section>
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
  }

  private bindButtons() {
    this.app.querySelectorAll<HTMLElement>('[data-choice]').forEach(button => {
      button.addEventListener('click', () => {
        const uci = button.dataset.choice!;
        const choice = this.currentChoices.find(it => it.uci === uci);
        if (choice) this.playChoice(choice);
      });
    });
    this.app.querySelector<HTMLElement>('[data-action="reset"]')?.addEventListener('click', () => this.reset());
    this.app.querySelector<HTMLElement>('[data-action="reveal"]')?.addEventListener('click', () => this.revealBestLine());
    this.app.querySelector<HTMLElement>('[data-action="next"]')?.addEventListener('click', () => this.loadNext());
    this.app.querySelectorAll<HTMLElement>('[data-beta-willingness]').forEach(button => {
      button.addEventListener('click', () => {
        const willingness = button.dataset.betaWillingness as BetaFeedbackChoice | undefined;
        if (willingness) void this.submitBetaFeedback(willingness);
      });
    });
  }

  private handleBoardMove(orig: Key, dest: Key) {
    const prefix = `${orig}${dest}`;
    const choice = this.currentChoices.find(it => it.uci.startsWith(prefix));
    if (!choice) {
      this.feedback = {
        kind: 'warning',
        text: this.currentNodeId ? this.nodeMap.get(this.currentNodeId)?.badMoveFeedback || 'That move is outside the current shell.' : 'That start is outside the current shell. Try to preserve the plan rather than making a generic improving move.',
      };
      this.currentFen = this.currentNodeId ? this.nodeMap.get(this.currentNodeId)?.fen || this.payload.runtimeShell.startFen : this.payload.runtimeShell.startFen;
      this.render();
      return;
    }
    this.playChoice(choice);
  }

  private playChoice(choice: ShellChoice) {
    if (this.busy) return;
    this.feedback = {
      kind: choice.credit === 'full' ? 'success' : 'neutral',
      text: choice.feedback,
    };
    this.lineUcis.push(choice.uci);
    this.lineSans.push(choice.san);
    const nextFen = choice.afterFen || this.currentFen;
    if (choice.terminalId) {
      this.currentFen = nextFen;
      this.currentNodeId = null;
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
      this.feedback = { kind: 'success', text: `${choice.feedback} ${forced.san} was auto-played.` };
      this.render();
    }, 320);
  }

  private revealTerminal(terminalId: string, outcome: Outcome) {
    const terminal = this.terminalMap.get(terminalId);
    if (!terminal) return;
    this.reveal = terminal;
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
    this.feedback = { kind: 'neutral', text: 'The featured line was revealed.' };
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
      this.feedback = { kind: 'neutral', text: '' };
      this.completion = null;
      this.betaFeedbackLoading = false;
      this.betaFeedbackSubmitted = null;
      this.betaFeedbackMessage = '';
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
    this.feedback = { kind: 'neutral', text: 'The shell was reset to the start position.' };
    this.completion = null;
    this.render();
  }

  private async submitBetaFeedback(willingness: BetaFeedbackChoice) {
    if (this.betaFeedbackLoading) return;
    this.betaFeedbackLoading = true;
    this.betaFeedbackMessage = '';
    this.render();
    try {
      const res = await fetch('/api/beta-feedback', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          surface: 'strategic_puzzle',
          feature: 'strategic_puzzle',
          entrypoint: 'strategic_puzzle_completion',
          willingness,
          notify: false,
        }),
      });
      const data = (await res.json().catch(() => null)) as { ok?: boolean; message?: string } | null;
      if (!res.ok || !data?.ok) {
        this.betaFeedbackMessage = data?.message || 'We could not save that beta response.';
        return;
      }
      this.betaFeedbackSubmitted = willingness;
      this.betaFeedbackMessage = data.message || 'Thanks. We saved your beta feedback.';
    } catch (err) {
      console.warn('strategic puzzle beta feedback failed', err);
      this.betaFeedbackMessage = 'Network error while saving beta feedback.';
    } finally {
      this.betaFeedbackLoading = false;
      this.render();
    }
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

export default initModule;
