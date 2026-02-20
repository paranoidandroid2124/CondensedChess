import { ratelimitSecondsFromResponse, resetAtFromResponse } from './responsePayload';

function renderBookmakerStateCard(
  kind: 'credits' | 'auth' | 'quota',
  title: string,
  message: string,
  actionHtml: string,
): string {
  return `
    <div class="bookmaker-state bookmaker-state--${kind}" role="status" aria-live="polite">
      <div class="bookmaker-state__body">
        <h3 class="bookmaker-state__title">${title}</h3>
        <p class="bookmaker-state__message">${message}</p>
      </div>
      <div class="bookmaker-state__actions">${actionHtml}</div>
    </div>
  `;
}

export async function blockedHtmlFromErrorResponse(res: Response, loginHref: string): Promise<string | null> {
  if (res.status === 403) {
    try {
      const data = await res.json();
      const resetAt = resetAtFromResponse(data);
      return renderBookmakerStateCard(
        'credits',
        'Credits Exhausted',
        `You've used all analysis credits for this period. Next reset: ${resetAt.slice(0, 10)}.`,
        '<a href="/plan" class="button primary">Upgrade to Pro</a>',
      );
    } catch {
      return renderBookmakerStateCard(
        'credits',
        'Credits Exhausted',
        "You've used all analysis credits for this period. Upgrade to continue.",
        '<a href="/plan" class="button primary">Upgrade to Pro</a>',
      );
    }
  }

  if (res.status === 401) {
    return renderBookmakerStateCard(
      'auth',
      'Sign In Required',
      'Sign in to continue using Bookmaker analysis.',
      `<a class="button" href="${loginHref}">Sign in</a>`,
    );
  }

  if (res.status === 429) {
    try {
      const data = await res.json();
      const seconds = ratelimitSecondsFromResponse(data);
      const message =
        typeof seconds === 'number'
          ? `LLM quota exceeded. Try again in ${seconds}s.`
          : 'LLM quota exceeded. Please retry shortly.';
      return renderBookmakerStateCard('quota', 'Rate Limit Reached', message, '');
    } catch {
      return renderBookmakerStateCard('quota', 'Rate Limit Reached', 'LLM quota exceeded. Please retry shortly.', '');
    }
  }

  return null;
}
