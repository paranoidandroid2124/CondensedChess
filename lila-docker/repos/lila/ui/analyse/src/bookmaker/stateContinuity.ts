import type { StoredBookmakerEntry } from './studyPersistence';
import type { EndgameStateToken, PlanStateToken } from './types';

type StoredTokenRestoreContext = {
  stateKey: string;
  analysisFen: string;
  originPath: string;
};

export function restoreStoredBookmakerTokens(
  entry: StoredBookmakerEntry,
  context: StoredTokenRestoreContext,
  planStateByPath: Map<string, PlanStateToken | null>,
  endgameStateByPath: Map<string, EndgameStateToken | null>,
): {
  planStateToken: PlanStateToken | null;
  endgameStateToken: EndgameStateToken | null;
} {
  const tokenContext = entry.tokenContext;
  const shouldRestoreTokens =
    !!tokenContext &&
    tokenContext.stateKey === context.stateKey &&
    tokenContext.analysisFen === context.analysisFen &&
    tokenContext.originPath === context.originPath;
  const planStateToken = shouldRestoreTokens ? entry.planStateToken ?? null : null;
  const endgameStateToken = shouldRestoreTokens ? entry.endgameStateToken ?? null : null;

  if (planStateToken) planStateByPath.set(context.stateKey, planStateToken);
  else planStateByPath.delete(context.stateKey);

  if (endgameStateToken) endgameStateByPath.set(context.stateKey, endgameStateToken);
  else endgameStateByPath.delete(context.stateKey);

  return { planStateToken, endgameStateToken };
}
