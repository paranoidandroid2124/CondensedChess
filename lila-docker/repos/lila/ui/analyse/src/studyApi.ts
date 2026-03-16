import { defaultInit, ensureOk, jsonHeader, xhrHeader } from 'lib/xhr';
import { validateNotebookDossier, type NotebookDossierV1 } from './notebookDossier';

export type StudyRef = {
  id: string;
  chapterId: string;
};

export type StudyChapterSummary = {
  id: string;
  name: string;
  url?: string;
};

export type CreateStudyResponse = {
  id: string;
  chapterId: string;
  name: string;
  chapterName: string;
  canWrite: boolean;
  chapters: StudyChapterSummary[];
  url: string;
  visibility?: string;
};

export class StudyApiError extends Error {
  constructor(
    readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = 'StudyApiError';
  }
}

type NodeResponse = {
  path: string;
  node: Tree.Node;
};

type MoveResponse = {
  ch: string;
  path: string;
  node: Tree.Node;
};

async function postJson<T>(url: string, data: any): Promise<T> {
  const res = await fetch(url, {
    ...defaultInit,
    method: 'post',
    headers: {
      ...jsonHeader,
      ...xhrHeader,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  });
  return ensureOk(res).json() as Promise<T>;
}

async function postNoContent(url: string, data: any): Promise<void> {
  const res = await fetch(url, {
    ...defaultInit,
    method: 'post',
    headers: {
      ...jsonHeader,
      ...xhrHeader,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(data),
  });
  ensureOk(res);
}

async function postFormJson<T>(url: string, form: URLSearchParams): Promise<T> {
  const res = await fetch(url, {
    ...defaultInit,
    method: 'post',
    headers: {
      ...xhrHeader,
      Accept: 'application/json',
      'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
    },
    body: form.toString(),
  });

  if (!res.ok) {
    const message = (await res.text().catch(() => '')).trim() || `Study request failed (${res.status})`;
    throw new StudyApiError(res.status, message);
  }

  const contentType = res.headers.get('content-type') || '';
  if (!contentType.includes('application/json'))
    throw new StudyApiError(res.status, 'Unexpected response while creating study.');

  return res.json() as Promise<T>;
}

export function anaMove(ref: StudyRef, d: any): Promise<MoveResponse> {
  return postJson(`/api/study/${ref.id}/${ref.chapterId}/ana-move`, { d });
}

export function anaDrop(ref: StudyRef, d: any): Promise<MoveResponse> {
  return postJson(`/api/study/${ref.id}/${ref.chapterId}/ana-drop`, { d });
}

export function deleteNode(ref: StudyRef, path: string): Promise<void> {
  return postNoContent(`/api/study/${ref.id}/${ref.chapterId}/delete-node`, { path });
}

export function promoteNode(ref: StudyRef, path: string, toMainline: boolean): Promise<void> {
  return postNoContent(`/api/study/${ref.id}/${ref.chapterId}/promote`, { path, toMainline });
}

export function forceVariationNode(ref: StudyRef, path: string, force: boolean): Promise<void> {
  return postNoContent(`/api/study/${ref.id}/${ref.chapterId}/force-variation`, { path, force });
}

export function setNodeComment(ref: StudyRef, path: string, text: string): Promise<NodeResponse> {
  return postJson(`/api/study/${ref.id}/${ref.chapterId}/comment`, { path, text });
}

export type BookmakerSyncPayload = {
  commentPath: string;
  originPath: string;
  commentary: string;
  variations: any[];
  maxLines?: number;
  maxPlies?: number;
};

export function bookmakerSync(ref: StudyRef, payload: BookmakerSyncPayload): Promise<void> {
  return postNoContent(`/api/study/${ref.id}/${ref.chapterId}/bookmaker-sync`, payload);
}

export function setNotebookDossier(studyId: string, dossier: NotebookDossierV1): Promise<void> {
  const errors = validateNotebookDossier(dossier);
  if (errors.length) throw new Error(`Invalid notebook dossier payload: ${errors.join(' | ')}`);
  return postNoContent(`/api/study/${studyId}/notebook-dossier`, dossier);
}

export function createStudyFromAnalysis(payload: { pgn: string; orientation?: string }): Promise<CreateStudyResponse> {
  const form = new URLSearchParams();
  form.set('pgn', payload.pgn);
  form.set('as', 'study');
  if (payload.orientation) form.set('orientation', payload.orientation);
  return postFormJson('/notebook', form);
}
