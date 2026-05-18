import { ratelimitSecondsFromResponse, resetAtFromResponse } from './responsePayload';

const moveNumberFromPly = (ply: number): number => Math.max(1, Math.ceil(ply / 2));

function renderMoveReviewStateCard(
  kind: 'auth' | 'quota' | 'idle' | 'error',
  title: string,
  message: string,
  actionHtml: string,
): string {
  return `
    <div class="move-review-state move-review-state--${kind}" role="status" aria-live="polite">
      <div class="move-review-state__body">
        <h3 class="move-review-state__title">${title}</h3>
        <p class="move-review-state__message">${message}</p>
      </div>
      <div class="move-review-state__actions">${actionHtml}</div>
    </div>
  `;
}

export function moveReviewIdleHtml(): string {
  return renderMoveReviewStateCard(
    'idle',
    'Explain This Move',
    'Ask for a move-level explanation only when you want it. This keeps requests focused, lowers cost, and avoids queue pileups.',
    '<button type="button" class="button button-metal" data-move-review-request="1">Explain this move</button>',
  );
}

export function moveReviewTooEarlyHtml(minPly: number): string {
  return renderMoveReviewStateCard(
    'idle',
    'Need a few more moves first',
    `Move explanations open from move ${moveNumberFromPly(minPly)}. Continue a little further so the commentary has enough position and branch context to say something useful.`,
    '',
  );
}

export function moveReviewRetryHtml(message = 'Commentary generation took too long or failed. Try again when the position is settled.'): string {
  return renderMoveReviewStateCard(
    'error',
    'Commentary unavailable',
    message,
    '<button type="button" class="button button-metal" data-move-review-request="1" data-move-review-force="1">Retry explanation</button>',
  );
}

export async function blockedHtmlFromErrorResponse(res: Response, loginHref: string): Promise<string | null> {
  if (res.status === 400) {
    try {
      const data = await res.json();
      if (data?.error === 'moveReview_too_early') {
        return renderMoveReviewStateCard(
          'idle',
          'Need a few more moves first',
          data?.msg || `Move explanations open from move ${moveNumberFromPly(5)}. Continue a few moves first before asking for commentary.`,
          '',
        );
      }
    } catch {}
    return null;
  }

  if (res.status === 403) {
    try {
      const data = await res.json();
      const resetAt = resetAtFromResponse(data);
      return renderMoveReviewStateCard(
        'quota',
        'Move explanation blocked',
        `This move request hit the current usage policy. Review the limits and retry after ${resetAt.slice(0, 10)}.`,
        '<a href="/support" class="button primary">Support Chesstory</a>',
      );
    } catch {
      return renderMoveReviewStateCard(
        'quota',
        'Move explanation blocked',
        'This move request hit the current usage policy. Please retry later.',
        '<a href="/support" class="button primary">Support Chesstory</a>',
      );
    }
  }

  if (res.status === 401) {
    return renderMoveReviewStateCard(
      'auth',
      'Sign In Required',
      'Sign in to continue using move explanations.',
      `<a class="button" href="${loginHref}">Sign in</a>`,
    );
  }

  if (res.status === 429) {
    try {
      const data = await res.json();
      const seconds = ratelimitSecondsFromResponse(data);
      const message =
        typeof seconds === 'number'
          ? `Move explanation quota reached. Try again in ${seconds}s.`
          : 'Move explanation quota reached. Please retry shortly.';
      return renderMoveReviewStateCard('quota', 'Rate Limit Reached', message, '');
    } catch {
      return renderMoveReviewStateCard('quota', 'Rate Limit Reached', 'Move explanation quota reached. Please retry shortly.', '');
    }
  }

  return null;
}
