/* eslint no-restricted-syntax:"error" */ // no side effects allowed due to re-export by index.ts

import { domDialog } from './dialog';
import { escapeHtml } from '../index';

// non-blocking window.alert-alike
export async function alert(msg: string): Promise<void> {
  await domDialog({
    htmlText: $html`<div>${escapeHtmlAddBreaks(msg)}</div>
    <span><button class="button">OK</button></span>`,
    class: 'alert',
    modal: true,
    noCloseButton: true,
    noClickAway: true,
    show: true,
    actions: { selector: 'button', result: 'ok' },
  });
}

// non-blocking window.confirm-alike
export async function confirm(
  msg: string,
  ok: string = 'OK',
  cancel: string = 'Cancel',
): Promise<boolean> {
  return (
    (
      await domDialog({
        htmlText: $html`<div>${escapeHtmlAddBreaks(msg)}</div>
          <span><button class="button button-empty cancel">${cancel}</button>
          <button class="button ok">${ok}</button></span>`,
        class: 'alert',
        noCloseButton: true,
        noClickAway: true,
        modal: true,
        show: true,
        focus: '.ok',
        actions: [
          { selector: '.cancel', result: 'cancel' },
          { selector: '.ok', result: 'ok' },
        ],
      })
    ).returnValue === 'ok'
  );
}

// non-blocking window.prompt-alike
export async function prompt(
  msg: string,
  def: string = '',
  valid: (text: string) => boolean = () => true,
): Promise<string | null> {
  const res = await domDialog({
    htmlText: $html`<div>${escapeHtmlAddBreaks(msg)}</div>
      <input type="text"${valid(def) ? '' : ' class="invalid"'} value="${escapeHtml(def)}">
      <span>
        <button class="button button-empty cancel">Cancel</button>
        <button class="button ok${valid(def) ? '"' : ' disabled" disabled'}>OK</button>
      </span>`,
    class: 'alert',
    noCloseButton: true,
    noClickAway: true,
    modal: true,
    show: true,
    focus: 'input',
    actions: [
      { selector: '.ok', result: 'ok' },
      { selector: '.cancel', result: 'cancel' },
      {
        selector: 'input',
        event: 'keydown',
        listener: (e: KeyboardEvent, dlg) => {
          if (e.key !== 'Enter' && e.key !== 'Escape') return;
          e.preventDefault();
          if (e.key === 'Enter' && valid(dlg.view.querySelector<HTMLInputElement>('input')!.value))
            dlg.close('ok');
          else if (e.key === 'Escape') dlg.close('cancel');
        },
      },
      {
        selector: 'input',
        event: 'input',
        listener: (e, dlg) => {
          if (!(e.target instanceof HTMLInputElement)) return;
          const ok = dlg.view.querySelector<HTMLButtonElement>('.ok')!;
          const invalid = !valid(e.target.value);
          e.target.classList.toggle('invalid', invalid);
          ok.classList.toggle('disabled', invalid);
          ok.disabled = invalid;
        },
      },
    ],
  });
  return res.returnValue === 'ok' ? res.view.querySelector('input')!.value : null;
}

function escapeHtmlAddBreaks(s: string) {
  return escapeHtml(s).replace(/\n/g, '<br>');
}
