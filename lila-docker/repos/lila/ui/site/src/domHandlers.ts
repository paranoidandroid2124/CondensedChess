import * as licon from 'lib/licon';
import {
  closeCookieConsent,
  openCookieConsent,
  preferenceStorageAllowed,
  setCookieConsent,
  syncCookieConsentDialogState,
} from 'lib/cookieConsent';
import { writeTextClipboard, text as xhrText } from 'lib/xhr';
import topBar from './topBar';
import { userComplete } from 'lib/view/userComplete';
import { confirm } from 'lib/view';

let pendingThemeChoice: string | null = null;

function submitThemeChoice(choice: string, trigger?: HTMLElement): void {
  trigger?.setAttribute('aria-busy', 'true');
  xhrText(`/pref/bg?v=${encodeURIComponent(choice)}`, { method: 'post' })
    .then(() => {
      pendingThemeChoice = null;
      window.location.reload();
    })
    .catch(err => {
      console.error(err);
      trigger?.removeAttribute('aria-busy');
    });
}

function applyPendingThemeChoice(): void {
  if (!pendingThemeChoice || !preferenceStorageAllowed()) return;
  const choice = pendingThemeChoice;
  pendingThemeChoice = null;
  submitThemeChoice(choice);
}

export function addWindowHandlers() {
  let animFrame: number;

  window.addEventListener('resize', () => {
    cancelAnimationFrame(animFrame);
    animFrame = requestAnimationFrame(setViewportHeight);
  });

  // ios safari vh correction
  function setViewportHeight() {
    document.body.style.setProperty('---viewport-height', `${window.innerHeight}px`);
  }
}

export function addDomHandlers() {
  topBar();
  syncCookieConsentDialogState();
  initAccountIntelProduct();

  $('#main-wrap').on('click', '.copy-me__button', function (this: HTMLElement) {
    const showCheckmark = () => {
      $(this).attr('data-icon', licon.Checkmark).removeClass('button-metal');
      setTimeout(() => $(this).attr('data-icon', licon.Clipboard).addClass('button-metal'), 1000);
    };
    const fetchContent = $(this).parent().hasClass('fetch-content');
    $(this.parentElement!.firstElementChild!).each(function (this: any) {
      try {
        if (fetchContent) writeTextClipboard(this.href, showCheckmark);
        else navigator.clipboard.writeText(this.value || this.href).then(showCheckmark);
      } catch (e) {
        console.error(e);
      }
    });
    return false;
  });

  $('body').on('click', '.relation-button', function (this: HTMLAnchorElement) {
    const $a = $(this).addClass('processing').css('opacity', 0.3);
    const dropdownOverflowParent = this.closest<HTMLElement>('.dropdown-overflow');
    if (dropdownOverflowParent) {
      dropdownOverflowParent.dispatchEvent(new CustomEvent('reload', { detail: this.href }));
    } else {
      xhrText(this.href, { method: 'post' }).then(html => {
        if ($a.hasClass('aclose')) $a.hide();
        else if (html.includes('relation-actions')) $a.parent().replaceWith(html);
        else $a.replaceWith(html);
      });
    }
    return false;
  });

  $('body').on('click', '.js-theme-choice', function (this: HTMLElement, e: Event) {
    e.preventDefault();
    const choice = this.getAttribute('data-theme-choice');
    if (!choice || this.getAttribute('aria-pressed') === 'true') return false;
    if (!preferenceStorageAllowed()) {
      pendingThemeChoice = choice;
      openCookieConsent();
      return false;
    }
    submitThemeChoice(choice, this);
    return false;
  });

  $('body').on('click', '.js-cookie-consent-open', function (e: Event) {
    e.preventDefault();
    openCookieConsent();
    return false;
  });

  $('body').on('click', '.js-cookie-consent-close', function (e: Event) {
    e.preventDefault();
    pendingThemeChoice = null;
    closeCookieConsent();
    return false;
  });

  $('body').on('click', '.js-cookie-consent-essential', function (e: Event) {
    e.preventDefault();
    pendingThemeChoice = null;
    setCookieConsent('essential');
    return false;
  });

  $('body').on('click', '.js-cookie-consent-accept', function (e: Event) {
    e.preventDefault();
    setCookieConsent('preferences');
    applyPendingThemeChoice();
    return false;
  });

  $('body').on('click', '.js-cookie-consent-save', function (e: Event) {
    e.preventDefault();
    const root = document.getElementById('cookie-consent');
    const allowPreferences = !!root?.querySelector<HTMLInputElement>('.js-cookie-consent-prefs')?.checked;
    setCookieConsent(allowPreferences ? 'preferences' : 'essential');
    if (allowPreferences) applyPendingThemeChoice();
    else pendingThemeChoice = null;
    return false;
  });

  $('.user-autocomplete').each(function (this: HTMLInputElement) {
    const focus = !!this.autofocus;
    const start = () =>
      userComplete({
        input: this,
        friend: !!this.dataset.friend,
        tag: this.dataset.tag as any,
        focus,
      });

    if (focus) start();
    else $(this).one('focus', start);
  });

  $('#main-wrap').on(
    'click',
    '.yes-no-confirm, .ok-cancel-confirm',
    async function (this: HTMLElement, e: Event) {
      if (!e.isTrusted) return;
      e.preventDefault();
      const [confirmText, cancelText] = this.classList.contains('yes-no-confirm')
        ? ['Yes', 'No']
        : ['OK', 'Cancel'];
      if (await confirm(this.title || 'Confirm this action?', confirmText, cancelText))
        (e.target as HTMLElement)?.click();
    },
  );

  $('#main-wrap').on('click', 'a.bookmark', function (this: HTMLAnchorElement) {
    const t = $(this).toggleClass('bookmarked');
    xhrText(this.href, { method: 'post' });
    const count = (parseInt(t.text(), 10) || 0) + (t.hasClass('bookmarked') ? 1 : -1);
    t.find('span').html('' + (count > 0 ? count : ''));
    return false;
  });
}

type AccountIntelState = {
  provider: string;
  username: string;
  kind: string;
  resultUrl: string;
  selectedJobId?: string | null;
  surfaceJobId?: string | null;
  latestSuccessfulJob?: {
    notebookUrl?: string;
  } | null;
  activeJob?: {
    jobId: string;
    status: string;
    progressStage: string;
  } | null;
  surface?: any;
  history: AccountIntelHistoryEntry[];
};

type AccountIntelHistoryEntry = {
  jobId: string;
  status: string;
  kind: string;
  requestedAt: string;
  finishedAt?: string | null;
  progressStage: string;
  warnings?: string[];
  url: string;
  notebookUrl?: string | null;
  sampledGameCount?: number | null;
  confidence?: string | null;
  headline?: string | null;
  surfacePreview?: {
    headline?: string | null;
    summary?: string | null;
    generatedAt?: string | null;
    confidence?: string | null;
    sampledGameCount?: number | null;
    warnings?: string[];
    patterns?: Array<{
      title?: string | null;
      side?: string | null;
      summary?: string | null;
    }>;
  } | null;
};

function initAccountIntelProduct() {
  const root = document.querySelector<HTMLElement>('.js-account-intel-product');
  if (!root) return;

  const stateScript = document.getElementById('account-intel-state');
  const initialState =
    stateScript?.textContent && stateScript.textContent.trim().length
      ? (JSON.parse(stateScript.textContent) as AccountIntelState)
      : null;
  const initialStateAttr = root.querySelector<HTMLElement>('.js-ai-state-source')?.dataset.initialState;
  let state = initialState || (initialStateAttr ? (JSON.parse(initialStateAttr) as AccountIntelState) : null);
  if (!state) return;

  let currentSide = normalizeSide(root.dataset.side || 'all', state.surface?.patterns || []);
  let currentSelectedJobId = state.selectedJobId || null;
  let compareJobId: string | null = null;
  let pollHandle: number | undefined;

  const pageBaseUrl = root.dataset.pageBaseUrl || `/account-intel/${state.provider}/${state.username}`;
  const setInner = (selector: string, html: string) => {
    const el = root.querySelector<HTMLElement>(selector);
    if (el) el.innerHTML = html;
  };

  const escapeHtml = (value: unknown) =>
    String(value ?? '')
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');

  const kindLabel = (kind: string) =>
    kind === 'my_account_intelligence_lite' ? 'My Account' : kind === 'opponent_prep' ? 'Opponent Prep' : kind;
  const humanDate = (raw?: string | null) => {
    if (!raw) return '';
    const date = new Date(raw);
    return Number.isNaN(date.getTime())
      ? raw
      : new Intl.DateTimeFormat(undefined, {
          year: 'numeric',
          month: 'short',
          day: 'numeric',
        }).format(date);
  };
  const sideUrl = (side: string) => {
    const url = new URL(state!.resultUrl, window.location.origin);
    if (side === 'all') url.searchParams.delete('side');
    else url.searchParams.set('side', side);
    return url.pathname + url.search;
  };
  const stateUrlForKind = (kind: string, jobId: string | null = currentSelectedJobId) => {
    const url = new URL(
      `/api/account-intel/${encodeURIComponent(state!.provider)}/${encodeURIComponent(state!.username)}`,
      window.location.origin,
    );
    url.searchParams.set('kind', kind);
    if (jobId) url.searchParams.set('jobId', jobId);
    return url.pathname + url.search;
  };
  const resultUrlForKind = (kind: string, jobId: string | null = currentSelectedJobId) => {
    const url = new URL(pageBaseUrl, window.location.origin);
    url.searchParams.set('kind', kind);
    if (jobId) url.searchParams.set('jobId', jobId);
    return url.pathname + url.search;
  };
  const readLocationIntent = () => {
    const url = new URL(window.location.href);
    return {
      kind: url.searchParams.get('kind') || state!.kind,
      side: url.searchParams.get('side') || 'all',
      jobId: url.searchParams.get('jobId'),
    };
  };
  const syncLocation = (kind: string, side: string, replace = false, jobId: string | null = currentSelectedJobId) => {
    const nextUrl = new URL(resultUrlForKind(kind, jobId), window.location.origin);
    if (side !== 'all') nextUrl.searchParams.set('side', side);
    const next = nextUrl.pathname + nextUrl.search;
    const current = window.location.pathname + window.location.search;
    if (next === current) return;
    if (replace) window.history.replaceState({}, '', next);
    else window.history.pushState({}, '', next);
  };
  const currentNotebookUrl = () =>
    (currentSelectedJobId ? state?.history.find(job => job.jobId === currentSelectedJobId)?.notebookUrl : null) ||
    state?.latestSuccessfulJob?.notebookUrl ||
    '';
  const currentPatterns = () => (state?.surface?.patterns || []) as any[];

  function normalizeSide(requested: string, patterns: any[]): string {
    if (requested === 'white' || requested === 'black' || requested === 'all') {
      if (requested === 'all' || patterns.some(p => p?.side === requested)) return requested;
    }
    return patterns[0]?.side === 'white' || patterns[0]?.side === 'black' ? patterns[0].side : 'all';
  }

  const renderSummaryStrip = () => {
    const surface = state?.surface;
    const sampled = surface?.source?.sampledGameCount ?? 0;
    const confidence = surface?.confidence?.label ?? 'weak';
    const generatedAt = humanDate(surface?.generatedAt);
    return `
      <div class="importer-summary-chip"><strong>${sampled}</strong><span>Sampled games</span></div>
      <div class="importer-summary-chip"><strong>${escapeHtml(capitalize(confidence))}</strong><span>Confidence</span></div>
      <div class="importer-summary-chip"><strong>${escapeHtml(kindLabel(state!.kind))}</strong><span>Generated ${escapeHtml(generatedAt)}</span></div>
    `;
  };

  const renderOverview = () => {
    const summary = escapeHtml(state?.surface?.summary || '');
    const cards = (state?.surface?.overview?.cards || []) as any[];
    return `
      <div class="importer-panel importer-panel--guide">
        <div class="importer-panel__head">
          <strong class="importer-panel__title">Overview</strong>
          <p class="importer-panel__copy">${summary}</p>
        </div>
        <div class="account-product-overview-grid">
          ${cards
            .map(
              card => `
                <div class="account-product-overview-card">
                  <span class="account-product-overview-kicker">${escapeHtml(card.title || 'Overview')}</span>
                  <strong>${escapeHtml(card.headline || '')}</strong>
                  <p>${escapeHtml(card.summary || '')}</p>
                  <span class="account-product-evidence-line">${renderEvidence(card.evidence)}</span>
                </div>`,
            )
            .join('')}
        </div>
      </div>
    `;
  };

  const renderOpenings = () => {
    const cards = (state?.surface?.openingCards || []) as any[];
    return `
      <div class="importer-panel importer-panel--guide">
        <div class="importer-panel__head">
          <strong class="importer-panel__title">Opening identity</strong>
          <p class="importer-panel__copy">Keep opening labels honest and split by side. This page is about what the account repeatedly enters, not one vanity line.</p>
        </div>
        <div class="account-product-opening-grid">
          ${cards
            .map(
              card => `
                <div class="account-product-opening-card">
                  <strong>${escapeHtml(card.title || 'Opening map')}</strong>
                  <span class="account-product-opening-family">${escapeHtml(card.openingFamily || 'Recent practical structure')}</span>
                  <p>${escapeHtml(card.story || '')}</p>
                  <span class="account-product-evidence-line">${renderEvidence(card.evidence)}</span>
                </div>`,
            )
            .join('')}
        </div>
      </div>
    `;
  };

  const renderSideToggle = () => {
    const patterns = currentPatterns();
    const whiteCount = patterns.filter(p => p?.side === 'white').length;
    const blackCount = patterns.filter(p => p?.side === 'black').length;
    const button = (side: string, label: string, active: boolean) =>
      `<a href="${escapeHtml(sideUrl(side))}" class="account-product-side-link js-ai-side-link${active ? ' is-active' : ''}" data-side="${side}">${escapeHtml(label)}</a>`;
    return `
      <div class="account-product-toggle-row">
        ${button('all', `All (${patterns.length})`, currentSide === 'all')}
        ${button('white', `White (${whiteCount})`, currentSide === 'white')}
        ${button('black', `Black (${blackCount})`, currentSide === 'black')}
      </div>
    `;
  };

  const renderPatterns = () => {
    const patterns = currentPatterns().filter(pattern => currentSide === 'all' || pattern?.side === currentSide);
    if (!patterns.length) {
      return `
        <div class="importer-panel importer-panel--results">
          <div class="importer-panel__head">
            <strong class="importer-panel__title">Top patterns</strong>
            <p class="importer-panel__copy">These are the account-level patterns that justify the product, not just a notebook chapter list.</p>
          </div>
          <div class="status-callout">
            <strong>No side-specific pattern yet</strong>
            <span>This side of the sample is still too thin, so the page is leaning on the opposite color or broader overview cards.</span>
          </div>
        </div>
      `;
    }
    return `
      <div class="importer-panel importer-panel--results">
        <div class="importer-panel__head">
          <strong class="importer-panel__title">Top patterns</strong>
          <p class="importer-panel__copy">These are the account-level patterns that justify the product, not just a notebook chapter list.</p>
        </div>
        <div class="account-product-patterns">
          ${patterns
            .map(pattern => {
              const actions = (pattern.actions || []) as any[];
              const evidenceGames = (pattern.evidenceGames || []) as any[];
              const anchor = pattern.anchor;
              return `
                <details class="account-product-pattern">
                  <summary>
                    <div class="account-product-pattern-head">
                      <div>
                        <span class="account-product-pattern-side">${escapeHtml(pattern.side || 'mixed')}</span>
                        <strong>${escapeHtml(pattern.title || 'Pattern')}</strong>
                        <span class="account-product-pattern-structure">${escapeHtml(pattern.structureFamily || '')}</span>
                      </div>
                      <div class="account-product-pattern-meta">
                        <span class="account-product-evidence-line">${renderEvidence(pattern.evidence)}</span>
                        <span>confidence ${escapeHtml((pattern.snapshotConfidenceMean ?? 0).toFixed ? pattern.snapshotConfidenceMean.toFixed(2) : pattern.snapshotConfidenceMean ?? 0)}</span>
                      </div>
                    </div>
                  </summary>
                  <div class="account-product-pattern-body">
                    <p class="account-product-pattern-summary">${escapeHtml(pattern.summary || '')}</p>
                    ${anchor ? renderAnchor(anchor) : ''}
                    ${
                      actions.length
                        ? `<div class="account-product-action-list">${actions.map(renderActionCard).join('')}</div>`
                        : ''
                    }
                    ${
                      evidenceGames.length
                        ? `<div class="account-product-evidence-block"><strong>Evidence games</strong><div class="account-product-evidence-grid">${evidenceGames
                            .map(renderEvidenceGame)
                            .join('')}</div></div>`
                        : ''
                    }
                  </div>
                </details>
              `;
            })
            .join('')}
        </div>
      </div>
    `;
  };

  const renderActions = () => {
    const actions = (state?.surface?.actions || []) as any[];
    const checklist = state?.surface?.checklist;
    const title = state?.kind === 'opponent_prep' ? 'How to steer' : 'What to do next';
    const copy =
      state?.kind === 'opponent_prep'
        ? 'Prep should end in steering decisions you can actually carry into the next game.'
        : 'Use the product surface as a repair sheet. The notebook is still there, but the next action should already be obvious here.';
    return `
      <div class="importer-panel importer-panel--guide">
        <div class="importer-panel__head">
          <strong class="importer-panel__title">${escapeHtml(title)}</strong>
          <p class="importer-panel__copy">${escapeHtml(copy)}</p>
        </div>
        <div class="account-product-action-stack">
          ${actions.map(renderActionCard).join('')}
          ${checklist ? renderChecklist(checklist) : ''}
        </div>
      </div>
    `;
  };

  const renderExemplars = () => {
    const games = (state?.surface?.exemplarGames || []) as any[];
    return `
      <div class="importer-panel importer-panel--guide">
        <div class="importer-panel__head">
          <strong class="importer-panel__title">Representative games</strong>
          <p class="importer-panel__copy">One or two games should explain why the selected patterns deserve attention.</p>
        </div>
        <div class="account-product-exemplar-list">
          ${games
            .map(game => {
              const meta = game.game || {};
              const link = meta.sourceUrl
                ? `<a href="${escapeHtml(meta.sourceUrl)}" target="_blank" rel="noopener">Open source game</a>`
                : '';
              return `
                <div class="account-product-exemplar-card">
                  <strong>${escapeHtml(game.title || 'Exemplar')}</strong>
                  <span>${escapeHtml(`${meta.white || '?'} vs ${meta.black || '?'}`)}</span>
                  <p>${escapeHtml(game.whyItMatters || '')}</p>
                  <p>${escapeHtml(game.takeaway || '')}</p>
                  ${link}
                </div>
              `;
            })
            .join('')}
        </div>
      </div>
    `;
  };

  const renderUtility = () => {
    const notebookUrl = currentNotebookUrl();
    return `
      <div class="importer-panel importer-panel--guide account-product-utility">
        <div class="importer-panel__head">
          <strong class="importer-panel__title">Deep dive</strong>
          <p class="importer-panel__copy">Stay on this page for the answer. Open the notebook only when you want the move tree, chapter flow, and a shareable study artifact.</p>
        </div>
        <div class="account-product-utility-links">
          ${notebookUrl ? `<a href="${escapeHtml(notebookUrl)}" class="account-product-secondary-link">Open notebook</a>` : ''}
          <div class="copy-me account-product-copy">
            <input type="text" readonly class="account-product-copy__value" value="${escapeHtml(window.location.pathname + window.location.search)}" />
            <button class="copy-me__button button-metal">Copy result link</button>
          </div>
        </div>
      </div>
    `;
  };

  const renderHistory = () => {
    const history = state?.history || [];
    return `
      <div class="importer-panel importer-panel--guide">
        <div class="importer-panel__head">
          <strong class="importer-panel__title">Run history</strong>
          <p class="importer-panel__copy">Keep the latest result in front, but make reruns and older runs easy to compare.</p>
        </div>
        <div class="account-product-history">
          ${history
            .slice(0, 10)
            .map(
              (job, idx) => `
                <div class="account-product-history-item${compareJobId === job.jobId ? ' is-compared' : ''}${currentSelectedJobId === job.jobId ? ' is-selected' : ''}">
                  <div class="account-product-history-copy">
                    <strong>${escapeHtml(idx === 0 ? `${job.status} • latest` : job.status)}</strong>
                    <span>${escapeHtml(humanDate(job.requestedAt))}</span>
                    <span>${escapeHtml(job.confidence || 'pending')}</span>
                    <span>${escapeHtml(job.sampledGameCount ? `${job.sampledGameCount} games` : 'no sample')}</span>
                  </div>
                  <div class="account-product-history-actions">
                    <a href="${escapeHtml(job.url)}">${currentSelectedJobId === job.jobId ? 'Viewing run' : 'Open run'}</a>
                    ${job.surfacePreview ? `<button type="button" class="account-product-history-compare js-ai-history-compare" data-job-id="${escapeHtml(job.jobId)}">${compareJobId === job.jobId ? 'Comparing' : 'Compare'}</button>` : ''}
                  </div>
                </div>`,
            )
            .join('')}
        </div>
      </div>
    `;
  };

  const renderCompare = () => {
    const historyEntry = compareJobId ? state?.history.find(job => job.jobId === compareJobId) : null;
    if (!historyEntry?.surfacePreview) {
      return `
        <div class="importer-panel importer-panel--guide account-product-compare">
          <div class="importer-panel__head">
            <strong class="importer-panel__title">Compare runs</strong>
            <p class="importer-panel__copy">Pick an older run from history to see how the headline, confidence, and top patterns moved.</p>
          </div>
          <div class="status-callout">
            <strong>No comparison selected</strong>
            <span>Use Compare on a past run to open a compact then-vs-now panel without leaving the page.</span>
          </div>
        </div>
      `;
    }
    const latest = state?.surface || {};
    const earlier = historyEntry.surfacePreview;
    const patternList = (patterns?: any[]) =>
      (patterns || [])
        .map(
          pattern => `<li><strong>${escapeHtml(pattern.title || 'Pattern')}</strong><span>${escapeHtml(pattern.side || 'mixed')}</span></li>`,
        )
        .join('');
    return `
      <div class="importer-panel importer-panel--guide account-product-compare">
        <div class="importer-panel__head">
          <strong class="importer-panel__title">Compare runs</strong>
          <p class="importer-panel__copy">A quick then-vs-now view keeps reruns useful instead of archival.</p>
        </div>
        <div class="account-product-compare-grid">
          <div class="account-product-compare-card">
            <span class="account-product-compare-kicker">Earlier</span>
            <strong>${escapeHtml(earlier.headline || 'Earlier run')}</strong>
            <p>${escapeHtml(earlier.summary || '')}</p>
            <span class="account-product-evidence-line">${escapeHtml(earlier.confidence || 'pending')} • ${escapeHtml(humanDate(earlier.generatedAt || historyEntry.requestedAt))}</span>
            <ul class="account-product-compare-list">${patternList(earlier.patterns)}</ul>
          </div>
          <div class="account-product-compare-card account-product-compare-card--now">
            <span class="account-product-compare-kicker">Now</span>
            <strong>${escapeHtml(latest.headline || 'Latest run')}</strong>
            <p>${escapeHtml(latest.summary || '')}</p>
            <span class="account-product-evidence-line">${escapeHtml(latest.confidence?.label || 'pending')} • ${escapeHtml(humanDate(latest.generatedAt))}</span>
            <ul class="account-product-compare-list">${patternList(latest.patterns)}</ul>
          </div>
        </div>
        <button type="button" class="account-product-secondary-link js-ai-clear-compare">Clear comparison</button>
      </div>
    `;
  };

  const renderActiveJob = () => {
    const job = state?.activeJob;
    if (!job || (job.status !== 'queued' && job.status !== 'running')) return '';
    return `
      <div class="status-callout status-callout--primary account-product-callout">
        <strong>${escapeHtml(job.status === 'running' ? 'Building account surface' : 'Queued')} • ${escapeHtml(kindLabel(state!.kind))}</strong>
        <span>${escapeHtml(stageLabel(job.progressStage || 'queued'))}</span>
        <div class="auth-links status-links">
          <a href="/account-intel/jobs/${escapeHtml(job.jobId)}" class="status-links__primary">Open build status</a>
        </div>
      </div>
    `;
  };

  const renderEvidence = (evidence?: any) => {
    const support = evidence?.supportingGames ?? 0;
    const total = evidence?.totalSampledGames ?? 0;
    const strength = evidence?.strength ?? 'weak';
    return `${support}/${total} games • ${escapeHtml(strength)}`;
  };

  const renderAnchor = (anchor: any) => `
    <div class="account-product-anchor">
      <strong>${escapeHtml(anchor.title || 'Anchor position')}</strong>
      <p>${escapeHtml(anchor.explanation || anchor.claim || '')}</p>
      <div class="account-product-anchor-meta">
        <span>${escapeHtml(anchor.claim || '')}</span>
        <span>ply ${escapeHtml(anchor.moveContext?.ply ?? 0)}</span>
      </div>
      <div class="account-product-anchor-plan">
        <strong>What to do next</strong>
        <p>${escapeHtml(anchor.recommendedPlan?.summary || '')}</p>
      </div>
    </div>
  `;

  const renderActionCard = (card: any) => `
    <div class="account-product-action-card">
      <strong>${escapeHtml(card.title || 'Action')}</strong>
      <p>${escapeHtml(card.instruction || '')}</p>
      <span>${escapeHtml(card.successMarker || '')}</span>
    </div>
  `;

  const renderChecklist = (card: any) => `
    <div class="account-product-checklist">
      <strong>${escapeHtml(card.title || 'Checklist')}</strong>
      <div class="account-product-checklist-items">
        ${(card.items || [])
          .map(
            (item: any) => `
              <div class="account-product-checklist-item">
                <span class="account-product-checklist-priority">${escapeHtml(item.priority || 'medium')}</span>
                <div>
                  <strong>${escapeHtml(item.label || '')}</strong>
                  ${item.reason ? `<span>${escapeHtml(item.reason)}</span>` : ''}
                </div>
              </div>`,
          )
          .join('')}
      </div>
    </div>
  `;

  const renderEvidenceGame = (game: any) => {
    const inner = `
      <div class="account-product-evidence-card">
        <strong>${escapeHtml(`${game.white || '?'} vs ${game.black || '?'}`)}</strong>
        <span>${escapeHtml(`${game.result || '-'} • ${game.opening || 'Imported game'}`)}</span>
        <span>${escapeHtml(game.role || 'support')}</span>
      </div>
    `;
    return game.sourceUrl
      ? `<a href="${escapeHtml(game.sourceUrl)}" target="_blank" rel="noopener">${inner}</a>`
      : inner;
  };

  const capitalize = (value: string) => (value ? value.charAt(0).toUpperCase() + value.slice(1) : value);
  const stageLabel = (stage: string) => {
    switch (stage) {
      case 'queued':
        return 'Waiting for the worker.';
      case 'fetching_games':
        return 'Fetching recent public games.';
      case 'extracting_primitives':
        return 'Extracting recurring structure signals.';
      case 'creating_notebook':
        return 'Attaching the notebook and account surface.';
      case 'completed':
        return 'Account surface created successfully.';
      case 'failed':
        return 'The job ended with an error.';
      default:
        return stage.replaceAll('_', ' ');
    }
  };

  const renderAll = () => {
    const surface = state?.surface;
    if (!surface) return;
    currentSide = normalizeSide(currentSide, currentPatterns());
    const headline = surface.headline || `@${state!.username}`;
    const headlineEl = root.querySelector<HTMLElement>('.js-ai-headline');
    if (headlineEl) headlineEl.textContent = headline;
    setInner('.js-ai-summary-strip', renderSummaryStrip());
    setInner('.js-ai-overview', renderOverview());
    setInner('.js-ai-openings', renderOpenings());
    setInner('.js-ai-side-toggle', renderSideToggle());
    setInner('.js-ai-patterns', renderPatterns());
    setInner('.js-ai-actions', renderActions());
    setInner('.js-ai-exemplars', renderExemplars());
    setInner('.js-ai-history', renderHistory());
    setInner('.js-ai-compare', renderCompare());
    setInner('.js-ai-active-job', renderActiveJob());
    setInner('.js-ai-utility', renderUtility());

    root.querySelectorAll<HTMLElement>('.js-ai-mode-link').forEach(link => {
      link.classList.toggle('is-active', link.dataset.kind === state!.kind);
    });
    const rerunKind = root.querySelector<HTMLInputElement>('.js-ai-rerun-kind');
    if (rerunKind) rerunKind.value = state!.kind;
  };

  const fetchState = async (stateUrl: string, historyMode: 'push' | 'replace' | 'none' = 'push') => {
    root.classList.add('is-loading');
    try {
      const response = await fetch(stateUrl, {
        headers: { Accept: 'application/json' },
        credentials: 'same-origin',
      });
      if (!response.ok) throw new Error(`Failed account-intel state fetch: ${response.status}`);
      state = (await response.json()) as AccountIntelState;
      if (!state) return;
      currentSelectedJobId = state.selectedJobId || null;
      currentSide = normalizeSide(currentSide, currentPatterns());
      renderAll();
      if (historyMode !== 'none') syncLocation(state.kind, currentSide, historyMode === 'replace', currentSelectedJobId);
      schedulePoll();
    } catch (err) {
      console.error(err);
    } finally {
      root.classList.remove('is-loading');
    }
  };

  const schedulePoll = () => {
    if (pollHandle) window.clearTimeout(pollHandle);
    if (!state?.activeJob || (state.activeJob.status !== 'queued' && state.activeJob.status !== 'running')) return;
    pollHandle = window.setTimeout(() => {
      fetchState(stateUrlForKind(state!.kind, currentSelectedJobId), 'none');
    }, 4000);
  };

  root.addEventListener('click', event => {
    const target = event.target as HTMLElement | null;
    const modeLink = target?.closest<HTMLAnchorElement>('.js-ai-mode-link');
    if (modeLink) {
      event.preventDefault();
      compareJobId = null;
      currentSide = 'all';
      currentSelectedJobId = null;
      fetchState(modeLink.dataset.stateUrl || stateUrlForKind(modeLink.dataset.kind || state!.kind, null), 'push');
      return;
    }
    const sideLink = target?.closest<HTMLAnchorElement>('.js-ai-side-link');
    if (sideLink) {
      event.preventDefault();
      currentSide = normalizeSide(sideLink.dataset.side || 'all', currentPatterns());
      renderAll();
      syncLocation(state!.kind, currentSide, false, currentSelectedJobId);
      return;
    }
    const compareButton = target?.closest<HTMLButtonElement>('.js-ai-history-compare');
    if (compareButton) {
      event.preventDefault();
      compareJobId = compareButton.dataset.jobId || null;
      setInner('.js-ai-compare', renderCompare());
      setInner('.js-ai-history', renderHistory());
      return;
    }
    const clearCompare = target?.closest<HTMLButtonElement>('.js-ai-clear-compare');
    if (clearCompare) {
      event.preventDefault();
      compareJobId = null;
      setInner('.js-ai-compare', renderCompare());
      setInner('.js-ai-history', renderHistory());
    }
  });

  window.addEventListener('popstate', () => {
    compareJobId = null;
    const intent = readLocationIntent();
    currentSide = intent.side;
    currentSelectedJobId = intent.jobId;
    void fetchState(stateUrlForKind(intent.kind, currentSelectedJobId), 'none');
  });

  renderAll();
  syncLocation(state.kind, currentSide, true, currentSelectedJobId);
  schedulePoll();
}
