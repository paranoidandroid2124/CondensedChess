export function boardRectSig(rect: Pick<DOMRectReadOnly, 'left' | 'top' | 'width' | 'height'>): string {
  const precision = 2;
  return [rect.left, rect.top, rect.width, rect.height].map(value => value.toFixed(precision)).join(':');
}
