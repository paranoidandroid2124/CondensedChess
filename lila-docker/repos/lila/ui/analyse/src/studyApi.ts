import { defaultInit, ensureOk, jsonHeader, xhrHeader } from 'lib/xhr';

export type StudyRef = {
  id: string;
  chapterId: string;
};

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
