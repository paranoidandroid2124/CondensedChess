export type BookmakerInteractiveRefLike = {
  refId: string;
  san: string;
  uci: string;
};

type RenderInteractiveSanChipOpts = {
  interactiveClasses?: string;
  fallbackTag?: 'span' | 'code';
  fallbackClasses?: string;
  includeTabIndex?: boolean;
};

export function escapeHtml(raw: string): string {
  return raw
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

export function normalizeSanToken(raw: string | undefined | null): string {
  return (raw || '')
    .trim()
    .replace(/^[\(\[\{'"“”‘’]+/, '')
    .replace(/[\)\]\}'"“”‘’]+$/, '')
    .replace(/[!?]+$/g, '')
    .trim();
}

export function renderInteractiveSanChip(
  rawSan: string,
  ref: BookmakerInteractiveRefLike | null,
  opts: RenderInteractiveSanChipOpts = {},
): string {
  if (!ref) {
    const tag = opts.fallbackTag || 'code';
    const classAttr = opts.fallbackClasses ? ` class="${opts.fallbackClasses}"` : '';
    return `<${tag}${classAttr}>${escapeHtml(rawSan)}</${tag}>`;
  }

  const classes = opts.interactiveClasses || 'move-chip move-chip--interactive';
  const tabindex = opts.includeTabIndex === false ? '' : ' tabindex="0"';
  return `<span class="${classes}" data-ref-id="${escapeHtml(ref.refId)}" data-uci="${escapeHtml(ref.uci)}" data-san="${escapeHtml(ref.san)}"${tabindex}>${escapeHtml(rawSan)}</span>`;
}
