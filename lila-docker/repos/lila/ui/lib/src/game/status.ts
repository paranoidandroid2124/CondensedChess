export type StatusName =
  | 'created'
  | 'started'
  | 'aborted'
  | 'mate'
  | 'resign'
  | 'stalemate'
  | 'timeout'
  | 'draw'
  | 'insufficientMaterialClaim'
  | 'outoftime'
  | 'noStart'
  | 'cheat'
  | 'unknownFinish';

export interface Status {
  id: number;
  name: StatusName;
}
