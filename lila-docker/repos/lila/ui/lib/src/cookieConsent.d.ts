export type CookieConsentChoice = 'essential' | 'preferences';

export declare function cookieConsentDecided(): boolean;
export declare function preferenceStorageAllowed(): boolean;
export declare function preferenceLocalStorage(): Storage | null;
export declare function preferenceSessionStorage(): Storage | null;
export declare function clearPreferenceStorage(): void;
export declare function setCookieConsent(choice: CookieConsentChoice): void;
export declare function openCookieConsent(): void;
export declare function closeCookieConsent(): void;
export declare function syncCookieConsentDialogState(): void;
