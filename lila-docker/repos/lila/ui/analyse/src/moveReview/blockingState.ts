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
    'Review This Move',
    'Open a focused review when you want to understand this moment without leaving the board.',
    '<button type="button" class="button button-metal" data-move-review-request="1">Review this move</button>',
  );
}

export function moveReviewTooEarlyHtml(minPly: number): string {
  return renderMoveReviewStateCard(
    'idle',
    'Need a few more moves first',
    `Move reviews open from move ${moveNumberFromPly(minPly)}. Continue a little further so the position has enough context to review well.`,
    '',
  );
}

export function moveReviewRetryHtml(message = 'The review took too long or failed. Try again when the position is settled.'): string {
  return renderMoveReviewStateCard(
    'error',
    'Review unavailable',
    message,
    '<button type="button" class="button button-metal" data-move-review-request="1" data-move-review-force="1">Retry review</button>',
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
          data?.msg || `Move reviews open from move ${moveNumberFromPly(5)}. Continue a few moves first before opening the review.`,
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
        'Move review blocked',
        `This review request hit the current usage policy. Review the limits and retry after ${resetAt.slice(0, 10)}.`,
        '<a href="/support" class="button primary">Support Chesstory</a>',
      );
    } catch {
      return renderMoveReviewStateCard(
        'quota',
        'Move review blocked',
        'This review request hit the current usage policy. Please retry later.',
        '<a href="/support" class="button primary">Support Chesstory</a>',
      );
    }
  }

  if (res.status === 401) {
    return renderMoveReviewStateCard(
      'auth',
      'Sign in required',
      'Sign in to continue using move reviews.',
      `<a class="button" href="${loginHref}">Sign in</a>`,
    );
  }

  if (res.status === 429) {
    try {
      const data = await res.json();
      const seconds = ratelimitSecondsFromResponse(data);
      const message =
        typeof seconds === 'number'
          ? `Move review quota reached. Try again in ${seconds}s.`
          : 'Move review quota reached. Please retry shortly.';
      return renderMoveReviewStateCard('quota', 'Review limit reached', message, '');
    } catch {
      return renderMoveReviewStateCard('quota', 'Review limit reached', 'Move review quota reached. Please retry shortly.', '');
    }
  }

  return null;
}
