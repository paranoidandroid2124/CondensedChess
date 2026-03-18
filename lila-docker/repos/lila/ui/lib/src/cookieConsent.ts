export type CookieConsentChoice = 'essential' | 'preferences';

const consentCookieName = 'chesstory_cookie_consent';
const consentVersion = 'v1';
const consentMaxAgeSeconds = 60 * 60 * 24 * 180;

const preferenceKeyPrefixes = ['analyse.', 'bookmaker.', 'chesstory.bookmaker.', 'engine.'];
const preferenceExactKeys = ['analysis.panel', 'chesstory.evalChart', 'markdown.rtfm', 'log.window'];
const preferenceCookies = ['bg', 'zoom'];
const preferenceIndexedDbNames = ['analyse-collapse', 'big-file', 'ceval-wasm-cache--db', 'lichess', 'log--db'];
let lastDialogFocus: HTMLElement | null = null;

function body(): HTMLElement | null {
  return typeof document === 'undefined' ? null : document.body;
}

function windowLocalStorage(): Storage | undefined {
  if (typeof window === 'undefined') return undefined;
  try {
    return window.localStorage;
  } catch {
    return undefined;
  }
}

function windowSessionStorage(): Storage | undefined {
  if (typeof window === 'undefined') return undefined;
  try {
    return window.sessionStorage;
  } catch {
    return undefined;
  }
}

function setConsentDataset(choice: CookieConsentChoice): void {
  const root = body();
  if (!root) return;
  root.dataset.cookieConsentDecided = '1';
  root.dataset.cookieConsentPrefs = choice === 'preferences' ? '1' : '0';
}

function consentRoot(): HTMLElement | null {
  return typeof document === 'undefined' ? null : document.getElementById('cookie-consent');
}

function consentPanel(): HTMLElement | null {
  return consentRoot()?.querySelector<HTMLElement>('.cookie-consent__panel') ?? null;
}

function consentFocusable(): HTMLElement[] {
  const panel = consentPanel();
  if (!panel) return [];
  return Array.from(
    panel.querySelectorAll<HTMLElement>('button:not([disabled]), input:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])'),
  ).filter(element => element.getAttribute('aria-hidden') !== 'true');
}

function focusConsentPanel(): void {
  const panel = consentPanel();
  (consentFocusable()[0] ?? panel)?.focus();
}

function onConsentKeydown(event: KeyboardEvent): void {
  const root = consentRoot();
  if (!root?.classList.contains('cookie-consent--editing')) return;
  if (event.key === 'Escape' && cookieConsentDecided()) {
    event.preventDefault();
    closeCookieConsent();
    return;
  }
  if (event.key !== 'Tab') return;
  const focusables = consentFocusable();
  if (!focusables.length) {
    event.preventDefault();
    consentPanel()?.focus();
    return;
  }
  const currentIndex = focusables.indexOf(document.activeElement as HTMLElement);
  const nextIndex = event.shiftKey
    ? currentIndex <= 0
      ? focusables.length - 1
      : currentIndex - 1
    : currentIndex === -1 || currentIndex >= focusables.length - 1
      ? 0
      : currentIndex + 1;
  event.preventDefault();
  focusables[nextIndex]?.focus();
}

function syncDialogState(focusPanel = false): void {
  const root = consentRoot();
  const panel = consentPanel();
  if (!root || !panel) return;
  const editing = root.classList.contains('cookie-consent--editing');
  panel.setAttribute('aria-hidden', editing ? 'false' : 'true');
  document.removeEventListener('keydown', onConsentKeydown);
  if (!editing) {
    if (cookieConsentDecided() && lastDialogFocus?.isConnected) lastDialogFocus.focus();
    return;
  }
  document.addEventListener('keydown', onConsentKeydown);
  if (focusPanel) {
    lastDialogFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    requestAnimationFrame(focusConsentPanel);
  }
}

function dispatchConsentChanged(choice: CookieConsentChoice): void {
  if (typeof document === 'undefined') return;
  document.dispatchEvent(
    new CustomEvent('chesstory:cookie-consent', {
      detail: { choice, preferences: choice === 'preferences' },
    }),
  );
}

function deleteCookie(name: string): void {
  if (typeof document === 'undefined') return;
  const secure = location.protocol === 'https:' ? '; Secure' : '';
  document.cookie = `${name}=; Path=/; Max-Age=0; SameSite=Lax${secure}`;
}

function deleteIndexedDb(name: string): void {
  if (typeof window === 'undefined' || !('indexedDB' in window)) return;
  try {
    window.indexedDB.deleteDatabase(name);
  } catch {
    // Ignore best-effort cleanup failures.
  }
}

async function clearPreferenceFileStorage(): Promise<void> {
  if (typeof navigator === 'undefined') return;
  try {
    const root = await navigator.storage?.getDirectory?.();
    const entries = (root as any)?.entries?.();
    if (!root || !entries) return;
    for await (const [name, handle] of entries as AsyncIterable<[string, { kind?: string }]>) {
      await root.removeEntry(name, { recursive: handle?.kind === 'directory' }).catch(() => {});
    }
  } catch {
    // Ignore best-effort cleanup failures.
  }
}

function clearMatching(storage: Storage | undefined, matcher: (key: string) => boolean): void {
  if (!storage) return;
  for (let i = storage.length - 1; i >= 0; i -= 1) {
    const key = storage.key(i);
    if (!key || !matcher(key)) continue;
    storage.removeItem(key);
  }
}

export function cookieConsentDecided(): boolean {
  return body()?.dataset.cookieConsentDecided === '1';
}

export function preferenceStorageAllowed(): boolean {
  return body()?.dataset.cookieConsentPrefs === '1';
}

export function preferenceLocalStorage(): Storage | null {
  if (!preferenceStorageAllowed() || typeof window === 'undefined') return null;
  try {
    return window.localStorage;
  } catch {
    return null;
  }
}

export function preferenceSessionStorage(): Storage | null {
  if (!preferenceStorageAllowed() || typeof window === 'undefined') return null;
  try {
    return window.sessionStorage;
  } catch {
    return null;
  }
}

export function clearPreferenceStorage(): void {
  clearMatching(
    windowLocalStorage(),
    key => preferenceExactKeys.includes(key) || preferenceKeyPrefixes.some(prefix => key.startsWith(prefix)),
  );
  clearMatching(
    windowSessionStorage(),
    key => preferenceExactKeys.includes(key) || preferenceKeyPrefixes.some(prefix => key.startsWith(prefix)),
  );
  preferenceCookies.forEach(deleteCookie);
  preferenceIndexedDbNames.forEach(deleteIndexedDb);
  void clearPreferenceFileStorage();
}

export function setCookieConsent(choice: CookieConsentChoice): void {
  if (typeof document === 'undefined') return;
  const secure = location.protocol === 'https:' ? '; Secure' : '';
  document.cookie = `${consentCookieName}=${consentVersion}:${choice === 'preferences' ? 'p' : 'e'}; Path=/; Max-Age=${consentMaxAgeSeconds}; SameSite=Lax${secure}`;
  setConsentDataset(choice);
  if (choice === 'essential') clearPreferenceStorage();
  const root = consentRoot();
  if (root) {
    root.classList.add('cookie-consent--decided');
    root.classList.remove('cookie-consent--editing');
    const checkbox = root.querySelector<HTMLInputElement>('.js-cookie-consent-prefs');
    if (checkbox) checkbox.checked = choice === 'preferences';
  }
  syncDialogState();
  dispatchConsentChanged(choice);
}

export function openCookieConsent(): void {
  const root = consentRoot();
  if (!root) return;
  root.classList.add('cookie-consent--editing');
  syncDialogState(true);
}

export function closeCookieConsent(): void {
  const root = consentRoot();
  if (!root || !cookieConsentDecided()) return;
  root.classList.remove('cookie-consent--editing');
  syncDialogState();
}

export function syncCookieConsentDialogState(): void {
  syncDialogState(consentRoot()?.classList.contains('cookie-consent--editing'));
}
