import type AnalyseCtrl from '../ctrl';
import type { BookmakerSyncPayload } from '../studyApi';

type PendingBookmakerStudySync = {
  payload: BookmakerSyncPayload;
  savedAt: number;
};

const pendingBookmakerStudySync = new Map<string, PendingBookmakerStudySync>();
const maxPendingBookmakerSync = 200;

export function rememberBookmakerStudySync(payload: BookmakerSyncPayload): void {
  const key = payload.commentPath;
  pendingBookmakerStudySync.set(key, { payload, savedAt: Date.now() });

  if (pendingBookmakerStudySync.size <= maxPendingBookmakerSync) return;

  const oldest = [...pendingBookmakerStudySync.entries()].sort((a, b) => a[1].savedAt - b[1].savedAt);
  for (const [k] of oldest.slice(0, pendingBookmakerStudySync.size - maxPendingBookmakerSync)) {
    pendingBookmakerStudySync.delete(k);
  }
}

export function flushBookmakerStudySyncQueue(ctrl: AnalyseCtrl): void {
  if (!ctrl?.canWriteStudy()) return;

  const entries = [...pendingBookmakerStudySync.values()].sort((a, b) => a.savedAt - b.savedAt);
  if (!entries.length) return;

  for (const entry of entries) ctrl.syncBookmaker(entry.payload);
  pendingBookmakerStudySync.clear();
}
