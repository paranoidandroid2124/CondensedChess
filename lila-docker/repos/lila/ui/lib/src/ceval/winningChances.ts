/* eslint no-restricted-syntax:"error" */ // no side effects allowed due to re-export by index.ts

import type { WinningChances } from './types';

const toPov = (color: Color, diff: number): number => (color === 'white' ? diff : -diff);

// Logistic eval-to-winning-chances mapping used by the analysis UI.
const rawWinningChances = (cp: number): WinningChances => {
  const MULTIPLIER = -0.00368208;
  return 2 / (1 + Math.exp(MULTIPLIER * cp)) - 1;
};

const cpWinningChances = (cp: number): WinningChances =>
  rawWinningChances(Math.min(Math.max(-1000, cp), 1000));

const mateWinningChances = (mate: number): WinningChances => {
  const cp = (21 - Math.min(10, Math.abs(mate))) * 100;
  const signed = cp * (mate > 0 ? 1 : -1);
  return rawWinningChances(signed);
};

const evalWinningChances = (ev: EvalScore): WinningChances =>
  typeof ev.mate !== 'undefined' ? mateWinningChances(ev.mate) : cpWinningChances(ev.cp!);

export const povChances = (color: Color, ev: EvalScore): WinningChances =>
  toPov(color, evalWinningChances(ev));

export const povDiff = (color: Color, e1: EvalScore, e2: EvalScore): number =>
  (povChances(color, e1) - povChances(color, e2)) / 2;

export const areSimilarEvals = (pov: Color, bestEval: EvalScore, secondBestEval: EvalScore): boolean =>
  povDiff(pov, bestEval, secondBestEval) < 0.14;

export const hasMultipleSolutions = (color: Color, bestEval: EvalScore, secondBestEval: EvalScore): boolean =>
  povChances(color, secondBestEval) >= 0.3524 || areSimilarEvals(color, bestEval, secondBestEval);
