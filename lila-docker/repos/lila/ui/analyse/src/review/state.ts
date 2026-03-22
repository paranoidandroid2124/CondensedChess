export type ReviewPrimaryTab = 'overview' | 'moments' | 'repair' | 'patterns' | 'moves' | 'import';
export type ReviewUtilityPanel = 'explorer' | 'board';

export type NarrativeMomentFilter = 'all' | 'critical' | 'collapses';

export type ReviewUIState = {
  primaryTab: ReviewPrimaryTab;
  utilityPanel: ReviewUtilityPanel | null;
  momentFilter: NarrativeMomentFilter;
  selectedMomentPly: Ply | null;
  selectedCollapseId: string | null;
};

export type ReviewUIAction =
  | { type: 'primary-tab'; tab: ReviewPrimaryTab }
  | { type: 'utility-panel'; panel: ReviewUtilityPanel | null }
  | { type: 'moment-filter'; filter: NarrativeMomentFilter }
  | { type: 'select-moment'; ply: Ply | null }
  | { type: 'select-collapse'; collapseId: string | null };

export const initialReviewState = (): ReviewUIState => ({
  primaryTab: 'moves',
  utilityPanel: null,
  momentFilter: 'all',
  selectedMomentPly: null,
  selectedCollapseId: null,
});

export function shouldFetchReviewPatterns(
  state: Pick<ReviewUIState, 'primaryTab'>,
  opts: { narrativeAvailable: boolean; hasDnaData: boolean; dnaLoading: boolean },
): boolean {
  return state.primaryTab === 'patterns' && opts.narrativeAvailable && !opts.hasDnaData && !opts.dnaLoading;
}

export function reduceReviewState(state: ReviewUIState, action: ReviewUIAction): ReviewUIState {
  switch (action.type) {
    case 'primary-tab':
      return { ...state, primaryTab: action.tab };
    case 'utility-panel':
      return { ...state, utilityPanel: action.panel };
    case 'moment-filter':
      return { ...state, primaryTab: 'moments', momentFilter: action.filter };
    case 'select-moment':
      return { ...state, primaryTab: 'moments', selectedMomentPly: action.ply };
    case 'select-collapse':
      return { ...state, primaryTab: 'repair', selectedCollapseId: action.collapseId };
  }
}
