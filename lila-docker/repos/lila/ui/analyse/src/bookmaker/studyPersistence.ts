import type {
  BookmakerRefsV1,
  BookmakerStrategicLedgerV1,
  DecodedBookmakerResponse,
  MoveReviewExplanationV1,
  PolishMetaV1,
} from './responsePayload';
import type { EndgameStateToken, PlanStateToken } from './types';
import { preferenceLocalStorage, preferenceSessionStorage } from 'lib/cookieConsent';

export type StoredBookmakerTokenContext = {
  stateKey: string;
  analysisFen: string;
  originPath: string;
};

export type StoredBookmakerEntry = {
  html: string;
  refs: BookmakerRefsV1 | null;
  polishMeta: PolishMetaV1 | null;
  sourceMode: string | null;
  model: string | null;
  cacheHit: boolean | null;
  moveReviewExplanation?: MoveReviewExplanationV1 | null;
  mainPlansCount: number;
  bookmakerLedger?: BookmakerStrategicLedgerV1 | null;
  planStateToken?: PlanStateToken | null;
  endgameStateToken?: EndgameStateToken | null;
  tokenContext?: StoredBookmakerTokenContext | null;
};

export type StudyBookmakerSnapshot = {
  schema: 'chesstory.move_review.study.v1' | 'chesstory.bookmaker.study.v1';
  studyId: string;
  chapterId: string;
  commentPath: string;
  originPath: string;
  savedAt: number;
  commentary?: string | null;
  entry: StoredBookmakerEntry;
};

export type StudyBookmakerRef = {
  studyId: string;
  chapterId: string;
};

const storageSchema = 'chesstory.move_review.study.v1';
const legacyStorageSchema = 'chesstory.bookmaker.study.v1';
const storagePrefix = storageSchema;
const legacyStoragePrefix = legacyStorageSchema;
const storageIndexKey = `${storagePrefix}.index`;
const legacyStorageIndexKey = `${legacyStoragePrefix}.index`;
const maxSnapshots = 300;
const sessionSchema = 'chesstory.move_review.session.v1';
const legacySessionSchema = 'chesstory.bookmaker.session.v1';
const sessionPrefix = sessionSchema;
const legacySessionPrefix = legacySessionSchema;

type StoredBookmakerEntrySource = Pick<
  DecodedBookmakerResponse,
  | 'refs'
  | 'polishMeta'
  | 'sourceMode'
  | 'model'
  | 'cacheHit'
  | 'moveReviewExplanation'
  | 'mainStrategicPlans'
  | 'bookmakerLedger'
  | 'planStateToken'
  | 'endgameStateToken'
>;

export function buildStoredBookmakerEntry(
  decoded: StoredBookmakerEntrySource,
  html: string,
  tokenContext: StoredBookmakerTokenContext,
): StoredBookmakerEntry {
  return {
    html,
    refs: decoded.refs,
    polishMeta: decoded.polishMeta,
    sourceMode: decoded.sourceMode,
    model: decoded.model,
    cacheHit: decoded.cacheHit,
    moveReviewExplanation: decoded.moveReviewExplanation,
    mainPlansCount: decoded.mainStrategicPlans.length,
    bookmakerLedger: decoded.bookmakerLedger,
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

function snapshotKey(ref: StudyBookmakerRef, commentPath: string): string {
  return `${storagePrefix}:${ref.studyId}:${ref.chapterId}:${commentPath}`;
}

function legacySnapshotKey(ref: StudyBookmakerRef, commentPath: string): string {
  return `${legacyStoragePrefix}:${ref.studyId}:${ref.chapterId}:${commentPath}`;
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
  migrateLegacyStudySnapshots();
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

function normalizeStudySnapshot(raw: unknown): StudyBookmakerSnapshot | null {
  const parsed = raw as StudyBookmakerSnapshot | null;
  if (
    !parsed ||
    (parsed.schema !== storageSchema && parsed.schema !== legacyStorageSchema) ||
    !parsed.entry ||
    typeof parsed.entry.html !== 'string'
  )
    return null;
  return {
    ...parsed,
    schema: storageSchema,
  };
}

function migrateLegacyStudySnapshots(): void {
  const store = localStore();
  if (!store) return;
  const legacyKeys = readIndexAt(legacyStorageIndexKey);
  if (!legacyKeys.length) return;
  const nextKeys = new Set(readIndexAt(storageIndexKey));

  for (const legacyKey of legacyKeys) {
    try {
      const raw = store.getItem(legacyKey);
      if (!raw) continue;
      const parsed = normalizeStudySnapshot(JSON.parse(raw));
      if (!parsed) continue;
      const newKey = legacyKey.replace(legacyStoragePrefix, storagePrefix);
      store.setItem(newKey, JSON.stringify(parsed));
      store.removeItem(legacyKey);
      nextKeys.add(newKey);
    } catch {
      // Ignore malformed legacy snapshots.
    }
  }
  writeIndex([...nextKeys]);
  try {
    store.removeItem(legacyStorageIndexKey);
  } catch {
    // Ignore cleanup failures.
  }
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
        const parsed = JSON.parse(raw) as StudyBookmakerSnapshot;
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

export function persistStudyBookmakerSnapshot(
  ref: StudyBookmakerRef,
  commentPath: string,
  originPath: string,
  commentary: string | null,
  entry: StoredBookmakerEntry,
): void {
  const store = localStore();
  if (!store) return;
  const key = snapshotKey(ref, commentPath);
  const payload: StudyBookmakerSnapshot = {
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

export function readStudyBookmakerSnapshot(
  ref: StudyBookmakerRef,
  commentPath: string,
): StudyBookmakerSnapshot | null {
  const store = localStore();
  if (!store) return null;
  try {
    const key = snapshotKey(ref, commentPath);
    const legacyKey = legacySnapshotKey(ref, commentPath);
    const raw = store.getItem(key) || store.getItem(legacyKey);
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
    if (store.getItem(key) !== raw) {
      store.setItem(key, JSON.stringify(parsed));
      store.removeItem(legacyKey);
      const keys = readIndexAt(storageIndexKey).filter(existing => existing !== key);
      keys.push(key);
      writeIndex(keys);
    }
    return parsed;
  } catch {
    return null;
  }
}

export function listStudyBookmakerSnapshots(ref: StudyBookmakerRef): StudyBookmakerSnapshot[] {
  if (!hasStorage()) return [];
  const keys = readIndex();
  const snapshots = keys
    .map(key => {
      try {
        const raw = localStore()?.getItem(key);
        if (!raw) return null;
        const parsed = JSON.parse(raw) as StudyBookmakerSnapshot;
        if (
          (parsed?.schema !== storageSchema && parsed?.schema !== legacyStorageSchema) ||
          parsed.studyId !== ref.studyId ||
          parsed.chapterId !== ref.chapterId ||
          !parsed.entry ||
          typeof parsed.entry.html !== 'string'
        )
          return null;
        return { ...parsed, schema: storageSchema };
      } catch {
        return null;
      }
    })
    .filter(Boolean) as StudyBookmakerSnapshot[];

  snapshots.sort((a, b) => b.savedAt - a.savedAt);
  return snapshots;
}

function sessionKey(scope: string, commentPath: string): string {
  return `${sessionPrefix}:${scope}:${commentPath}`;
}

function legacySessionKey(scope: string, commentPath: string): string {
  return `${legacySessionPrefix}:${scope}:${commentPath}`;
}

export function persistSessionBookmakerSnapshot(
  scope: string,
  commentPath: string,
  originPath: string,
  commentary: string | null,
  entry: StoredBookmakerEntry,
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

export function readSessionBookmakerSnapshot(scope: string, commentPath: string): StudyBookmakerSnapshot | null {
  const store = sessionStore();
  if (!store) return null;
  try {
    const key = sessionKey(scope, commentPath);
    const legacyKey = legacySessionKey(scope, commentPath);
    const raw = store.getItem(key) || store.getItem(legacyKey);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as {
      schema?: string;
      scope?: string;
      commentPath?: string;
      originPath?: string;
      savedAt?: number;
      commentary?: string | null;
      entry?: StoredBookmakerEntry;
    };
    if (
      (parsed?.schema !== sessionSchema && parsed?.schema !== legacySessionSchema) ||
      parsed.scope !== scope ||
      parsed.commentPath !== commentPath ||
      !parsed.entry ||
      typeof parsed.entry.html !== 'string'
    )
      return null;
    if (store.getItem(key) !== raw) {
      store.setItem(
        key,
        JSON.stringify({
          ...parsed,
          schema: sessionSchema,
        }),
      );
      store.removeItem(legacyKey);
    }
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

export function listSessionBookmakerSnapshots(scope: string): StudyBookmakerSnapshot[] {
  const store = sessionStore();
  if (!store) return [];
  const prefix = `${sessionPrefix}:${scope}:`;
  const snapshots: StudyBookmakerSnapshot[] = [];

  for (let i = 0; i < store.length; i += 1) {
    const key = store.key(i);
    if (!key || !key.startsWith(prefix)) continue;
    const commentPath = key.slice(prefix.length);
    if (!commentPath) continue;
    const snapshot = readSessionBookmakerSnapshot(scope, commentPath);
    if (snapshot) snapshots.push(snapshot);
  }

  snapshots.sort((a, b) => b.savedAt - a.savedAt);
  return snapshots;
}
