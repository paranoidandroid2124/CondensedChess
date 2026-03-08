import * as Prefs from 'lib/prefs';

export type BoardLabelMode = 'off' | 'rim' | 'full';

export function boardLabelModeFromCoords(coords: number): BoardLabelMode {
  if (coords === Prefs.Coords.Hidden) return 'off';
  if (coords === Prefs.Coords.All) return 'full';
  return 'rim';
}

export function boardLabelModeToCoords(mode: BoardLabelMode): Prefs.Coords {
  if (mode === 'off') return Prefs.Coords.Hidden;
  if (mode === 'full') return Prefs.Coords.All;
  return Prefs.Coords.Outside;
}
