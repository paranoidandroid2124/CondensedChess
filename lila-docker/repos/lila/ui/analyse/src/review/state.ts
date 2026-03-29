export type ReviewPrimaryTab =
  | 'overview'
  | 'moments'
  | 'repair'
  | 'patterns'
  | 'moves'
  | 'import'
  | 'explain'
  | 'engine'
  | 'explorer'
  | 'board';
export type ReviewUtilityPanel = 'explorer' | 'board';
export type ReviewSurfaceMode = 'review' | 'raw';

export type NarrativeMomentFilter = 'all' | 'critical' | 'collapses';

export type ReviewUIState = {
  surfaceMode: ReviewSurfaceMode;
  primaryTab: ReviewPrimaryTab;
  utilityPanel: ReviewUtilityPanel | null;
  momentFilter: NarrativeMomentFilter;
  selectedMomentPly: Ply | null;
  selectedCollapseId: string | null;
  analysisDetailsOpen: boolean;
};

export type ReviewUIAction =
  | { type: 'surface-mode'; mode: ReviewSurfaceMode }
  | { type: 'primary-tab'; tab: ReviewPrimaryTab }
  | { type: 'utility-panel'; panel: ReviewUtilityPanel | null }
  | { type: 'moment-filter'; filter: NarrativeMomentFilter }
  | { type: 'select-moment'; ply: Ply | null }
  | { type: 'select-collapse'; collapseId: string | null }
  | { type: 'analysis-details'; open: boolean };

export const initialReviewState = (): ReviewUIState => ({
  surfaceMode: 'review',
  primaryTab: 'overview',
  utilityPanel: null,
  momentFilter: 'all',
  selectedMomentPly: null,
  selectedCollapseId: null,
  analysisDetailsOpen: false,
});

export function shouldFetchReviewPatterns(
  state: Pick<ReviewUIState, 'primaryTab'>,
  opts: { narrativeAvailable: boolean; hasDnaData: boolean; dnaLoading: boolean },
): boolean {
  return state.primaryTab === 'patterns' && opts.narrativeAvailable && !opts.hasDnaData && !opts.dnaLoading;
}

export function reduceReviewState(state: ReviewUIState, action: ReviewUIAction): ReviewUIState {
  switch (action.type) {
    case 'surface-mode':
      return { ...state, surfaceMode: action.mode, utilityPanel: action.mode === 'review' ? null : state.utilityPanel };
    case 'primary-tab':
      return {
        ...state,
        primaryTab: action.tab,
        utilityPanel: action.tab === 'explorer' || action.tab === 'board' ? action.tab : null,
      };
    case 'utility-panel':
      return {
        ...state,
        utilityPanel: action.panel,
        primaryTab:
          action.panel ??
          (state.primaryTab === 'explorer' || state.primaryTab === 'board' ? 'moves' : state.primaryTab),
      };
    case 'moment-filter':
      return { ...state, primaryTab: 'moments', momentFilter: action.filter };
    case 'select-moment':
      return { ...state, primaryTab: 'moments', selectedMomentPly: action.ply };
    case 'select-collapse':
      return { ...state, primaryTab: 'repair', selectedCollapseId: action.collapseId };
    case 'analysis-details':
      return { ...state, analysisDetailsOpen: action.open };
  }
}
