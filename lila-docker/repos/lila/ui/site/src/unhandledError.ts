import { log } from 'lib/permalog';
import { domDialog } from 'lib/view';
import { escapeHtml } from 'lib';

function terseHref(): string {
  const { pathname, search, hash } = window.location;
  return `${pathname}${search}${hash}` || '/';
}

function debugDialog(message: string): void {
  if (site.debug)
    void domDialog({
      htmlText: escapeHtml(message),
      class: 'debug',
      show: true,
    });
}

export function addExceptionListeners() {
  window.addEventListener('error', e => {
    const loc = e.filename ? ` - (${e.filename}:${e.lineno}:${e.colno})` : '';
    const message = `${e.message}${loc}\n${e.error?.stack ?? ''}`;
    log(`${terseHref()} - ${message}`.trim());
    debugDialog(message);
  });

  window.addEventListener('unhandledrejection', e => {
    let reason = e.reason;
    if (typeof reason !== 'string')
      try {
        reason = JSON.stringify(e.reason);
      } catch (_) {
        reason = 'unhandled rejection, reason not a string';
      }
    log(`${terseHref()} - ${reason}`);
    debugDialog(reason);
  });
}
