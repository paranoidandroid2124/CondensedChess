export const randomToken = (): string => {
  try {
    const data = globalThis.crypto.getRandomValues(new Uint8Array(9));
    return btoa(String.fromCharCode(...data)).replace(/[/+]/g, '_');
  } catch (_) {
    return Math.random().toString(36).slice(2, 12);
  }
};

export function clamp(value: number, bounds: { min?: number; max?: number }): number {
  return Math.max(bounds.min ?? -Infinity, Math.min(value, bounds.max ?? Infinity));
}

/**
 * Comparison of enumerable primitives.
 * Complex properties get reference equality only.
 * If two vars have the same type and this type is in `excludedComparisonTypes`, then `true` is returned.
 */
export function isEquivalent(a: any, b: any, excludedComparisonTypes: string[] = []): boolean {
  if (a === b) return true;
  if (typeof a !== typeof b) return false;
  if (excludedComparisonTypes.some(t => typeof a === t)) return true;
  if (Array.isArray(a))
    return (
      Array.isArray(b) &&
      a.length === b.length &&
      a.every((x, i) => isEquivalent(x, b[i], excludedComparisonTypes))
    );
  if (typeof a !== 'object') return false;
  const [aKeys, bKeys] = [Object.keys(a), Object.keys(b)];
  if (aKeys.length !== bKeys.length) return false;
  return aKeys.every(key => bKeys.includes(key) && isEquivalent(a[key], b[key], excludedComparisonTypes));
}
