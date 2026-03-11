import * as Prefs from 'lib/prefs';

export type BoardLabelMode = 'off' | 'inside' | 'rim' | 'full';
export type BoardLabelViewConfig = {
  coordinates: boolean;
  coordinatesOnSquares: boolean;
};

type BoardLabelTarget = {
  set(config: BoardLabelViewConfig): void;
  redrawAll(): void;
};

export function boardLabelModeFromCoords(coords: number): BoardLabelMode {
  if (coords === Prefs.Coords.Hidden) return 'off';
  if (coords === Prefs.Coords.Inside) return 'inside';
  if (coords === Prefs.Coords.All) return 'full';
  return 'rim';
}

export function boardLabelModeToCoords(mode: BoardLabelMode): Prefs.Coords {
  if (mode === 'off') return Prefs.Coords.Hidden;
  if (mode === 'inside') return Prefs.Coords.Inside;
  if (mode === 'full') return Prefs.Coords.All;
  return Prefs.Coords.Outside;
}

export function boardCoordsToViewConfig(coords: Prefs.Coords): BoardLabelViewConfig {
  return {
    coordinates: coords !== Prefs.Coords.Hidden,
    coordinatesOnSquares: coords === Prefs.Coords.All,
  };
}

export function boardLabelModeToViewConfig(mode: BoardLabelMode): BoardLabelViewConfig {
  return boardCoordsToViewConfig(boardLabelModeToCoords(mode));
}

export function applyBoardLabelMode(target: BoardLabelTarget | undefined, mode: BoardLabelMode): void {
  if (!target) return;
  target.set(boardLabelModeToViewConfig(mode));
  target.redrawAll();
}
