import type AnalyseCtrl from '../ctrl';
import type { MoveReviewSyncPayload } from '../studyApi';

type PendingMoveReviewStudySync = {
  payload: MoveReviewSyncPayload;
  savedAt: number;
};

const pendingMoveReviewStudySync = new Map<string, PendingMoveReviewStudySync>();
const maxPendingMoveReviewSync = 200;

export function rememberMoveReviewStudySync(payload: MoveReviewSyncPayload): void {
  const key = payload.commentPath;
  pendingMoveReviewStudySync.set(key, { payload, savedAt: Date.now() });

  if (pendingMoveReviewStudySync.size <= maxPendingMoveReviewSync) return;

  const oldest = [...pendingMoveReviewStudySync.entries()].sort((a, b) => a[1].savedAt - b[1].savedAt);
  for (const [k] of oldest.slice(0, pendingMoveReviewStudySync.size - maxPendingMoveReviewSync)) {
    pendingMoveReviewStudySync.delete(k);
  }
}

export function flushMoveReviewStudySyncQueue(ctrl: AnalyseCtrl): void {
  if (!ctrl?.canWriteStudy()) return;

  const entries = [...pendingMoveReviewStudySync.values()].sort((a, b) => a.savedAt - b.savedAt);
  if (!entries.length) return;

  for (const entry of entries) ctrl.syncMoveReview(entry.payload);
  pendingMoveReviewStudySync.clear();
}
