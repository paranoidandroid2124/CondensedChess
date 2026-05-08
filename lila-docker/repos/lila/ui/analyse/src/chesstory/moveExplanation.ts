type HostNode = {
  fen: string;
  ply: number;
  uci?: string;
};

export type MoveExplanationHost = {
  path: string;
  node: HostNode;
  nodeList: HostNode[];
  redraw: Redraw;
};

export type MoveExplanationState = { kind: 'empty' };

export type MoveExplanationCtrl = {
  state: () => MoveExplanationState;
  refresh: () => Promise<void>;
};

export type MoveExplanationOptions = Record<string, never>;

const emptyState: MoveExplanationState = { kind: 'empty' };

export function makeMoveExplanation(_host: MoveExplanationHost, _opts: MoveExplanationOptions = {}): MoveExplanationCtrl {
  return {
    state: () => emptyState,
    refresh: () => Promise.resolve(),
  };
}
