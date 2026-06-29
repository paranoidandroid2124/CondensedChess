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
}
