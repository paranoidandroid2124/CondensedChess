import { renderInsufficientCredits } from '../CreditWidget';
import { ratelimitSecondsFromResponse, resetAtFromResponse } from './responsePayload';

export async function blockedHtmlFromErrorResponse(res: Response, loginHref: string): Promise<string | null> {
  if (res.status === 403) {
    try {
      const data = await res.json();
      return renderInsufficientCredits(resetAtFromResponse(data));
    } catch {
      return renderInsufficientCredits('Unknown');
    }
  }

  if (res.status === 401) {
    return `<p>Sign in to use Bookmaker.</p><p><a class="button" href="${loginHref}">Sign in</a></p>`;
  }

  if (res.status === 429) {
    try {
      const data = await res.json();
      const seconds = ratelimitSecondsFromResponse(data);
      return typeof seconds === 'number'
        ? `<p>LLM quota exceeded. Try again in ${seconds}s.</p>`
        : '<p>LLM quota exceeded.</p>';
    } catch {
      return '<p>LLM quota exceeded.</p>';
    }
  }

  return null;
}
