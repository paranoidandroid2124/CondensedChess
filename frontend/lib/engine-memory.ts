/**
 * Engine memory validation utilities.
 * Prevents crashes from excessive hash allocation based on browser capabilities.
 */

type BrowserType = "chrome" | "firefox" | "safari" | "mobile" | "unknown";

// Maximum recommended hash sizes by browser (MB)
const MAX_HASH_BY_BROWSER: Record<BrowserType, number> = {
    chrome: 1024,   // Chromium-based browsers support larger allocations
    firefox: 512,   // Firefox has stricter limits
    safari: 256,    // Safari is more conservative
    mobile: 128,    // Mobile browsers have very limited memory
    unknown: 256,   // Conservative default
};

// Warning thresholds (80% of max)
const WARNING_THRESHOLD = 0.8;

/**
 * Check if SharedArrayBuffer is available (required for multi-threading)
 */
export function isSharedArrayBufferAvailable(): boolean {
    if (typeof window === "undefined") return false;

    return typeof SharedArrayBuffer !== "undefined";
}

/**
 * Check if the page is cross-origin isolated (required for SAB in modern browsers)
 */
export function isCrossOriginIsolated(): boolean {
    if (typeof window === "undefined") return false;

    // crossOriginIsolated is true when COOP/COEP headers are set
    return !!(window as any).crossOriginIsolated;
}

/**
 * Check if multi-threading is supported
 */
export function isMultiThreadingSupported(): boolean {
    return isSharedArrayBufferAvailable() && isCrossOriginIsolated();
}

/**
 * Get the maximum recommended threads based on browser support
 */
export function getMaxThreads(): number {
    if (!isMultiThreadingSupported()) {
        return 1; // Single-thread fallback
    }

    // Use navigator.hardwareConcurrency or default to 4
    if (typeof navigator !== "undefined" && navigator.hardwareConcurrency) {
        return Math.min(navigator.hardwareConcurrency, 16);
    }

    return 4;
}

/**
 * Detect current browser type
 */
export function detectBrowser(): BrowserType {
    if (typeof navigator === "undefined") return "unknown";

    const ua = navigator.userAgent.toLowerCase();

    // Check mobile first
    if (/mobile|android|iphone|ipad|ipod/.test(ua)) {
        return "mobile";
    }

    if (/firefox/.test(ua)) return "firefox";
    if (/safari/.test(ua) && !/chrome/.test(ua)) return "safari";
    if (/chrome|chromium|edg/.test(ua)) return "chrome";

    return "unknown";
}

/**
 * Get recommended maximum hash size for current browser
 */
export function getMaxHash(): number {
    const browser = detectBrowser();
    return MAX_HASH_BY_BROWSER[browser];
}

/**
 * Validate and clamp hash size to safe limits
 * @param requestedHash - User requested hash size in MB
 * @returns Object with validated hash and any warnings
 */
export function validateHash(requestedHash: number): {
    hash: number;
    warning: string | null;
    clamped: boolean;
} {
    const browser = detectBrowser();
    const maxHash = MAX_HASH_BY_BROWSER[browser];
    const warningThreshold = Math.floor(maxHash * WARNING_THRESHOLD);

    if (requestedHash > maxHash) {
        return {
            hash: maxHash,
            warning: `Hash reduced from ${requestedHash}MB to ${maxHash}MB (${browser} browser limit)`,
            clamped: true,
        };
    }

    if (requestedHash >= warningThreshold) {
        return {
            hash: requestedHash,
            warning: `High memory usage (${requestedHash}MB). May cause instability on some devices.`,
            clamped: false,
        };
    }

    return {
        hash: requestedHash,
        warning: null,
        clamped: false,
    };
}

/**
 * Get recommended hash options for UI dropdown based on browser
 */
export function getHashOptions(): number[] {
    const maxHash = getMaxHash();
    const allOptions = [16, 32, 64, 128, 256, 512, 1024];
    return allOptions.filter(h => h <= maxHash);
}
