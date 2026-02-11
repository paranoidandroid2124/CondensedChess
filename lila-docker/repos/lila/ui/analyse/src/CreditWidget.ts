export interface CreditStatus {
    remaining: number;
    maxCredits: number;
    tier: string;
    resetAt: string;
}

export function renderCreditWidget(status: CreditStatus): string {
    const percent = Math.round((status.remaining / status.maxCredits) * 100);
    const tierClass = status.tier.toLowerCase();

    return `
    <div class="llm-credit-widget ${tierClass}">
      <div class="credit-info">
        <span class="label">Credits</span>
        <span class="value">${status.remaining} / ${status.maxCredits}</span>
      </div>
      <div class="progress-bar">
        <div class="fill" style="width: ${percent}%"></div>
      </div>
      <a href="/plan" class="upgrade-link">
        ${status.tier === 'pro' ? 'Manage Plan' : 'Upgrade to Pro'}
      </a>
    </div>
  `;
}

export function renderInsufficientCredits(resetAt: string): string {
    return `
    <div class="bookmaker-error credit-exhausted">
      <div class="icon">⚠</div>
      <h3>Credits Exhausted</h3>
      <p>You've used all your analysis credits for this period.</p>
      <p class="reset-hint">Next reset: <strong>${resetAt.slice(0, 10)}</strong></p>
      <div class="actions">
        <a href="/plan" class="button primary">Upgrade to Pro</a>
        <p class="small">Pro members get 2,000 credits/mo and AI polish.</p>
      </div>
    </div>
  `;
}
