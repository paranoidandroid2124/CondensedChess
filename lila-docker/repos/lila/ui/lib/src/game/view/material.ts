import { h, type VNode } from 'snabbdom';
import { opposite } from '@lichess-org/chessground/util';
import { charToRole, ROLES, type Chess } from 'chessops';

interface CheckState {
  ply: Ply;
  check?: boolean | Key;
}

type CheckCount = Record<Color, number>;

type MaterialDiffSide = {
  [role in Role]: number;
};

interface MaterialDiff {
  white: MaterialDiffSide;
  black: MaterialDiffSide;
}

function getMaterialDiff(chess: FEN | Chess): MaterialDiff {
  const diff: MaterialDiff = {
    white: { king: 0, queen: 0, rook: 0, bishop: 0, knight: 0, pawn: 0 },
    black: { king: 0, queen: 0, rook: 0, bishop: 0, knight: 0, pawn: 0 },
  };
  if (isFen(chess)) {
    const fenLike = chess.split(' ')[0];
    for (let i = 0, part = 0; i < fenLike.length && part < 8; i++) {
      const ch = fenLike[i];
      const lower = ch.toLowerCase();
      const role = charToRole(ch);
      if (role) {
        const color = ch === lower ? 'black' : 'white';
        const them = diff[opposite(color)];
        if (them[role] > 0) them[role]--;
        else diff[color][role]++;
      } else if (ch === '[' || ch === ' ') break;
      else if (ch === '/') part++;
    }
  } else {
    const b = chess.board;
    for (const role of ROLES) {
      const c = [b.pieces('white', role).size(), b.pieces('black', role).size()];
      diff.white[role] = c[0] > c[1] ? c[0] - c[1] : 0;
      diff.black[role] = c[1] > c[0] ? c[1] - c[0] : 0;
    }
  }
  return diff;
}

function getScore(diff: MaterialDiff): number {
  return (
    (diff.white.queen - diff.black.queen) * 9 +
    (diff.white.rook - diff.black.rook) * 5 +
    (diff.white.bishop - diff.black.bishop) * 3 +
    (diff.white.knight - diff.black.knight) * 3 +
    (diff.white.pawn - diff.black.pawn)
  );
}

const NO_CHECKS: CheckCount = {
  white: 0,
  black: 0,
};

function countChecks(steps: CheckState[], ply: Ply): CheckCount {
  const checks: CheckCount = { ...NO_CHECKS };
  for (const step of steps) {
    if (ply < step.ply) break;
    if (step.check) {
      if (step.ply % 2 === 1) checks.white++;
      else checks.black++;
    }
  }
  return checks;
}

function isFen(chess: FEN | Chess): chess is FEN {
  return typeof chess === 'string';
}

function renderMaterialDiff(
  material: MaterialDiffSide,
  score: number,
  position: 'top' | 'bottom',
  checks?: number,
): VNode {
  const children: VNode[] = [];
  let role: Role;
  for (role in material) {
    if (material[role] > 0) {
      const content: VNode[] = [];
      for (let i = 0; i < material[role]; i++) content.push(h('mpiece.' + role));
      children.push(h('div', content));
    }
  }
  if (checks) for (let i = 0; i < checks; i++) children.push(h('div', h('mpiece.king')));
  if (score > 0) children.push(h('score', '+' + score));
  return h('div.material.material-' + position, children);
}

export function renderMaterialDiffs(
  showCaptured: boolean,
  bottomColor: Color,
  chess: FEN | Chess,
  showChecks: boolean,
  checkStates: CheckState[],
  ply: Ply,
): [VNode, VNode] {
  const material = getMaterialDiff(showCaptured ? chess : '');
  const score = getScore(material) * (bottomColor === 'white' ? 1 : -1);
  const checks: CheckCount = showChecks ? countChecks(checkStates, ply) : NO_CHECKS;
  const topColor = opposite(bottomColor);
  return [
    renderMaterialDiff(material[topColor], -score, 'top', checks[topColor]),
    renderMaterialDiff(material[bottomColor], score, 'bottom', checks[bottomColor]),
  ];
}
