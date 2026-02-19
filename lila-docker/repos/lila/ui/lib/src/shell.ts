export type ShellSelectors = {
  headerId: string;
  navId: string;
  navToggleId: string;
};

const defaultSelectors: ShellSelectors = {
  headerId: 'cs-top',
  navId: 'cs-nav',
  navToggleId: 'cs-nav-toggle',
};

export function isBrandV3ShellEnabled(): boolean {
  return document.body?.dataset?.brandV3Shell !== '0';
}

export function readShellSelectors(): ShellSelectors {
  const data = document.body?.dataset;
  return {
    headerId: data?.shellHeaderId || defaultSelectors.headerId,
    navId: data?.shellNavId || defaultSelectors.navId,
    navToggleId: data?.shellNavToggleId || defaultSelectors.navToggleId,
  };
}
