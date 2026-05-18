import type {
  MoveReviewRefsV1,
  MoveReviewStrategicLedgerV1,
  DecodedMoveReviewResponse,
  MoveReviewExplanationV1,
  PolishMetaV1,
} from './responsePayload';
import type { EndgameStateToken, PlanStateToken } from './types';
import { preferenceLocalStorage, preferenceSessionStorage } from 'lib/cookieConsent';

export type StoredMoveReviewTokenContext = {
  stateKey: string;
  analysisFen: string;
  originPath: string;
};

export type StoredMoveReviewEntry = {
  html: string;
  refs: MoveReviewRefsV1 | null;
  polishMeta: PolishMetaV1 | null;
  sourceMode: string | null;
  model: string | null;
  cacheHit: boolean | null;
  moveReviewExplanation?: MoveReviewExplanationV1 | null;
  mainPlansCount: number;
  moveReviewLedger?: MoveReviewStrategicLedgerV1 | null;
  planStateToken?: PlanStateToken | null;
  endgameStateToken?: EndgameStateToken | null;
  tokenContext?: StoredMoveReviewTokenContext | null;
};

export type StudyMoveReviewSnapshot = {
  schema: 'chesstory.move_review.study.v1';
  studyId: string;
  chapterId: string;
  commentPath: string;
  originPath: string;
  savedAt: number;
  commentary?: string | null;
  entry: StoredMoveReviewEntry;
};

export type StudyMoveReviewRef = {
  studyId: string;
  chapterId: string;
};

const storageSchema = 'chesstory.move_review.study.v1';
const storagePrefix = storageSchema;
const storageIndexKey = `${storagePrefix}.index`;
const maxSnapshots = 300;
const sessionSchema = 'chesstory.move_review.session.v1';
const sessionPrefix = sessionSchema;

type StoredMoveReviewEntrySource = Pick<
  DecodedMoveReviewResponse,
  | 'refs'
  | 'polishMeta'
  | 'sourceMode'
  | 'model'
  | 'cacheHit'
  | 'moveReviewExplanation'
  | 'mainStrategicPlans'
  | 'moveReviewLedger'
  | 'planStateToken'
  | 'endgameStateToken'
>;

export function buildStoredMoveReviewEntry(
  decoded: StoredMoveReviewEntrySource,
  html: string,
  tokenContext: StoredMoveReviewTokenContext,
): StoredMoveReviewEntry {
  return {
    html,
    refs: decoded.refs,
    polishMeta: decoded.polishMeta,
    sourceMode: decoded.sourceMode,
    model: decoded.model,
    cacheHit: decoded.cacheHit,
    moveReviewExplanation: decoded.moveReviewExplanation,
    mainPlansCount: decoded.mainStrategicPlans.length,
    moveReviewLedger: decoded.moveReviewLedger,
    planStateToken: decoded.planStateToken,
    endgameStateToken: decoded.endgameStateToken,
    tokenContext,
  };
}

function hasStorage(): boolean {
  return !!preferenceLocalStorage();
}

function localStore(): Storage | null {
  return preferenceLocalStorage();
}

function sessionStore(): Storage | null {
  return preferenceSessionStorage();
}

function snapshotKey(ref: StudyMoveReviewRef, commentPath: string): string {
  return `${storagePrefix}:${ref.studyId}:${ref.chapterId}:${commentPath}`;
}

function readIndexAt(key: string): string[] {
  const store = localStore();
  if (!store) return [];
  try {
    const raw = store.getItem(key);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((v): v is string => typeof v === 'string') : [];
  } catch {
    return [];
  }
}

function readIndex(): string[] {
  return readIndexAt(storageIndexKey);
}

function writeIndex(keys: string[]): void {
  const store = localStore();
  if (!store) return;
  try {
    store.setItem(storageIndexKey, JSON.stringify(keys));
  } catch {
    // Ignore storage quota and serialization failures.
  }
}

function normalizeStudySnapshot(raw: unknown): StudyMoveReviewSnapshot | null {
  const parsed = raw as StudyMoveReviewSnapshot | null;
  if (
    !parsed ||
    parsed.schema !== storageSchema ||
    !parsed.entry ||
    typeof parsed.entry.html !== 'string'
  )
    return null;
  return parsed;
}

function pruneSnapshots(): void {
  if (!hasStorage()) return;
  const keys = readIndex();
  if (keys.length <= maxSnapshots) return;

  const snapshots = keys
    .map(key => {
      try {
        const raw = localStore()?.getItem(key);
        if (!raw) return null;
        const parsed = JSON.parse(raw) as StudyMoveReviewSnapshot;
        return { key, savedAt: typeof parsed?.savedAt === 'number' ? parsed.savedAt : 0 };
      } catch {
        return { key, savedAt: 0 };
      }
    })
    .filter(Boolean) as Array<{ key: string; savedAt: number }>;

  snapshots.sort((a, b) => a.savedAt - b.savedAt);
  const toDrop = snapshots.slice(0, snapshots.length - maxSnapshots);
  toDrop.forEach(({ key }) => {
    try {
      localStore()?.removeItem(key);
    } catch {
      // Ignore best-effort cleanup failures.
    }
  });
  writeIndex(snapshots.slice(toDrop.length).map(({ key }) => key));
}

export function persistStudyMoveReviewSnapshot(
  ref: StudyMoveReviewRef,
  commentPath: string,
  originPath: string,
  commentary: string | null,
  entry: StoredMoveReviewEntry,
): void {
  const store = localStore();
  if (!store) return;
  const key = snapshotKey(ref, commentPath);
  const payload: StudyMoveReviewSnapshot = {
    schema: storageSchema,
    studyId: ref.studyId,
    chapterId: ref.chapterId,
    commentPath,
    originPath,
    savedAt: Date.now(),
    commentary,
    entry,
  };

  try {
    store.setItem(key, JSON.stringify(payload));
    const keys = readIndex().filter(existing => existing !== key);
    keys.push(key);
    writeIndex(keys);
    pruneSnapshots();
  } catch {
    // Ignore quota errors and keep runtime cache as best effort.
  }
}

export function readStudyMoveReviewSnapshot(
  ref: StudyMoveReviewRef,
  commentPath: string,
): StudyMoveReviewSnapshot | null {
  const store = localStore();
  if (!store) return null;
  try {
    const key = snapshotKey(ref, commentPath);
    const raw = store.getItem(key);
    if (!raw) return null;
    const parsed = normalizeStudySnapshot(JSON.parse(raw));
    if (
      !parsed ||
      parsed.studyId !== ref.studyId ||
      parsed.chapterId !== ref.chapterId ||
      parsed.commentPath !== commentPath ||
      !parsed.entry ||
      typeof parsed.entry.html !== 'string'
    )
      return null;
    return parsed;
  } catch {
    return null;
  }
}

export function listStudyMoveReviewSnapshots(ref: StudyMoveReviewRef): StudyMoveReviewSnapshot[] {
  if (!hasStorage()) return [];
  const keys = readIndex();
  const snapshots = keys
    .map(key => {
      try {
        const raw = localStore()?.getItem(key);
        if (!raw) return null;
        const parsed = JSON.parse(raw) as StudyMoveReviewSnapshot;
        if (
          parsed?.schema !== storageSchema ||
          parsed.studyId !== ref.studyId ||
          parsed.chapterId !== ref.chapterId ||
          !parsed.entry ||
          typeof parsed.entry.html !== 'string'
        )
          return null;
        return parsed;
      } catch {
        return null;
      }
    })
    .filter(Boolean) as StudyMoveReviewSnapshot[];

  snapshots.sort((a, b) => b.savedAt - a.savedAt);
  return snapshots;
}

function sessionKey(scope: string, commentPath: string): string {
  return `${sessionPrefix}:${scope}:${commentPath}`;
}

export function persistSessionMoveReviewSnapshot(
  scope: string,
  commentPath: string,
  originPath: string,
  commentary: string | null,
  entry: StoredMoveReviewEntry,
): void {
  const store = sessionStore();
  if (!store) return;
  try {
    store.setItem(
      sessionKey(scope, commentPath),
      JSON.stringify({
        schema: sessionSchema,
        scope,
        commentPath,
        originPath,
        savedAt: Date.now(),
        commentary,
        entry,
      }),
    );
  } catch {
    // Ignore session storage failures.
  }
}

export function readSessionMoveReviewSnapshot(scope: string, commentPath: string): StudyMoveReviewSnapshot | null {
  const store = sessionStore();
  if (!store) return null;
  try {
    const key = sessionKey(scope, commentPath);
    const raw = store.getItem(key);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as {
      schema?: string;
      scope?: string;
      commentPath?: string;
      originPath?: string;
      savedAt?: number;
      commentary?: string | null;
      entry?: StoredMoveReviewEntry;
    };
    if (
      parsed?.schema !== sessionSchema ||
      parsed.scope !== scope ||
      parsed.commentPath !== commentPath ||
      !parsed.entry ||
      typeof parsed.entry.html !== 'string'
    )
      return null;
    return {
      schema: storageSchema,
      studyId: '',
      chapterId: '',
      commentPath,
      originPath: parsed.originPath || parsed.entry.tokenContext?.originPath || '',
      savedAt: typeof parsed.savedAt === 'number' ? parsed.savedAt : 0,
      commentary: parsed.commentary,
      entry: parsed.entry,
    };
  } catch {
    return null;
  }
}

export function listSessionMoveReviewSnapshots(scope: string): StudyMoveReviewSnapshot[] {
  const store = sessionStore();
  if (!store) return [];
  const prefix = `${sessionPrefix}:${scope}:`;
  const snapshots: StudyMoveReviewSnapshot[] = [];

  for (let i = 0; i < store.length; i += 1) {
    const key = store.key(i);
    if (!key || !key.startsWith(prefix)) continue;
    const commentPath = key.slice(prefix.length);
    if (!commentPath) continue;
    const snapshot = readSessionMoveReviewSnapshot(scope, commentPath);
    if (snapshot) snapshots.push(snapshot);
  }

  snapshots.sort((a, b) => b.savedAt - a.savedAt);
  return snapshots;
}
