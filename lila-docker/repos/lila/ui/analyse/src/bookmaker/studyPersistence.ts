import type {
  BookmakerRefsV1,
  BookmakerStrategicLedgerV1,
  DecodedBookmakerResponse,
  PolishMetaV1,
} from './responsePayload';
import type { EndgameStateToken, PlanStateToken } from './types';

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
  mainPlansCount: number;
  latentPlansCount: number;
  holdReasonsCount: number;
  bookmakerLedger?: BookmakerStrategicLedgerV1 | null;
  planStateToken?: PlanStateToken | null;
  endgameStateToken?: EndgameStateToken | null;
  tokenContext?: StoredBookmakerTokenContext | null;
};

export type StudyBookmakerSnapshot = {
  schema: 'chesstory.bookmaker.study.v1';
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

const storagePrefix = 'chesstory.bookmaker.study.v1';
const storageIndexKey = `${storagePrefix}.index`;
const maxSnapshots = 300;
const sessionPrefix = 'chesstory.bookmaker.session.v1';

type StoredBookmakerEntrySource = Pick<
  DecodedBookmakerResponse,
  | 'refs'
  | 'polishMeta'
  | 'sourceMode'
  | 'model'
  | 'cacheHit'
  | 'mainStrategicPlans'
  | 'latentPlans'
  | 'holdReasons'
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
    mainPlansCount: decoded.mainStrategicPlans.length,
    latentPlansCount: decoded.latentPlans.length,
    holdReasonsCount: decoded.holdReasons.length,
    bookmakerLedger: decoded.bookmakerLedger,
    planStateToken: decoded.planStateToken,
    endgameStateToken: decoded.endgameStateToken,
    tokenContext,
  };
}

function hasStorage(): boolean {
  return typeof window !== 'undefined' && !!window.localStorage;
}

function snapshotKey(ref: StudyBookmakerRef, commentPath: string): string {
  return `${storagePrefix}:${ref.studyId}:${ref.chapterId}:${commentPath}`;
}

function readIndex(): string[] {
  if (!hasStorage()) return [];
  try {
    const raw = window.localStorage.getItem(storageIndexKey);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed.filter((v): v is string => typeof v === 'string') : [];
  } catch {
    return [];
  }
}

function writeIndex(keys: string[]): void {
  if (!hasStorage()) return;
  try {
    window.localStorage.setItem(storageIndexKey, JSON.stringify(keys));
  } catch {
    // Ignore storage quota and serialization failures.
  }
}

function pruneSnapshots(): void {
  if (!hasStorage()) return;
  const keys = readIndex();
  if (keys.length <= maxSnapshots) return;

  const snapshots = keys
    .map(key => {
      try {
        const raw = window.localStorage.getItem(key);
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
      window.localStorage.removeItem(key);
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
  if (!hasStorage()) return;
  const key = snapshotKey(ref, commentPath);
  const payload: StudyBookmakerSnapshot = {
    schema: 'chesstory.bookmaker.study.v1',
    studyId: ref.studyId,
    chapterId: ref.chapterId,
    commentPath,
    originPath,
    savedAt: Date.now(),
    commentary,
    entry,
  };

  try {
    window.localStorage.setItem(key, JSON.stringify(payload));
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
  if (!hasStorage()) return null;
  try {
    const raw = window.localStorage.getItem(snapshotKey(ref, commentPath));
    if (!raw) return null;
    const parsed = JSON.parse(raw) as StudyBookmakerSnapshot;
    if (
      !parsed ||
      parsed.schema !== 'chesstory.bookmaker.study.v1' ||
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

export function listStudyBookmakerSnapshots(ref: StudyBookmakerRef): StudyBookmakerSnapshot[] {
  if (!hasStorage()) return [];
  const keys = readIndex();
  const snapshots = keys
    .map(key => {
      try {
        const raw = window.localStorage.getItem(key);
        if (!raw) return null;
        const parsed = JSON.parse(raw) as StudyBookmakerSnapshot;
        if (
          parsed?.schema !== 'chesstory.bookmaker.study.v1' ||
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
    .filter(Boolean) as StudyBookmakerSnapshot[];

  snapshots.sort((a, b) => b.savedAt - a.savedAt);
  return snapshots;
}

function sessionKey(scope: string, commentPath: string): string {
  return `${sessionPrefix}:${scope}:${commentPath}`;
}

export function persistSessionBookmakerSnapshot(
  scope: string,
  commentPath: string,
  commentary: string | null,
  entry: StoredBookmakerEntry,
): void {
  if (typeof window === 'undefined' || !window.sessionStorage) return;
  try {
    window.sessionStorage.setItem(
      sessionKey(scope, commentPath),
      JSON.stringify({
        schema: 'chesstory.bookmaker.session.v1',
        scope,
        commentPath,
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
  if (typeof window === 'undefined' || !window.sessionStorage) return null;
  try {
    const raw = window.sessionStorage.getItem(sessionKey(scope, commentPath));
    if (!raw) return null;
    const parsed = JSON.parse(raw) as {
      schema?: string;
      scope?: string;
      commentPath?: string;
      savedAt?: number;
      commentary?: string | null;
      entry?: StoredBookmakerEntry;
    };
    if (
      parsed?.schema !== 'chesstory.bookmaker.session.v1' ||
      parsed.scope !== scope ||
      parsed.commentPath !== commentPath ||
      !parsed.entry ||
      typeof parsed.entry.html !== 'string'
    )
      return null;
    return {
      schema: 'chesstory.bookmaker.study.v1',
      studyId: '',
      chapterId: '',
      commentPath,
      originPath: '',
      savedAt: typeof parsed.savedAt === 'number' ? parsed.savedAt : 0,
      commentary: parsed.commentary,
      entry: parsed.entry,
    };
  } catch {
    return null;
  }
}
