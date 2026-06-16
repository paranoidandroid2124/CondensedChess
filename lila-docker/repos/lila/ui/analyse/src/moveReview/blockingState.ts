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
    'Review this position',
    'Turn this moment into a board-first coach review without leaving the game.',
    '<button type="button" class="button button-metal" data-move-review-request="1">Start review</button>',
  );
}

export function moveReviewTooEarlyHtml(minPly: number): string {
  return renderMoveReviewStateCard(
    'idle',
    'Play a little further first',
    `Reviews open from move ${moveNumberFromPly(minPly)} so there is enough position history for a useful lesson.`,
    '',
  );
}

export function moveReviewRetryHtml(message = 'The review lost its line. Try again from this position.'): string {
  return renderMoveReviewStateCard(
    'error',
    'Review not ready',
    message,
    '<button type="button" class="button button-metal" data-move-review-request="1" data-move-review-force="1">Try again</button>',
  );
}

export async function blockedHtmlFromErrorResponse(res: Response, loginHref: string): Promise<string | null> {
  if (res.status === 400) {
    try {
      const data = await res.json();
      if (data?.error === 'moveReview_too_early') {
        return renderMoveReviewStateCard(
          'idle',
          'Play a little further first',
          data?.msg || `Reviews open from move ${moveNumberFromPly(5)} so the position has enough history for a useful lesson.`,
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
        'Review paused for now',
        `This account has reached the current review limit. Try again after ${resetAt.slice(0, 10)}.`,
        '<a href="/support" class="button primary">Support Chesstory</a>',
      );
    } catch {
      return renderMoveReviewStateCard(
        'quota',
        'Review paused for now',
        'This account has reached the current review limit. Please try again later.',
        '<a href="/support" class="button primary">Support Chesstory</a>',
      );
    }
  }

  if (res.status === 401) {
    return renderMoveReviewStateCard(
      'auth',
      'Sign in to keep reviewing',
      'Sign in so Chesstory can keep the coach review attached to this game.',
      `<a class="button" href="${loginHref}">Sign in</a>`,
    );
  }

  if (res.status === 429) {
    try {
      const data = await res.json();
      const seconds = ratelimitSecondsFromResponse(data);
      const message =
        typeof seconds === 'number'
          ? `Reviews need a short pause. Try again in ${seconds}s.`
          : 'Reviews need a short pause. Please try again shortly.';
      return renderMoveReviewStateCard('quota', 'Review paused for now', message, '');
    } catch {
      return renderMoveReviewStateCard('quota', 'Review paused for now', 'Reviews need a short pause. Please try again shortly.', '');
    }
  }

  return null;
}
