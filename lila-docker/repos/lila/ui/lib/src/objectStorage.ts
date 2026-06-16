/* eslint no-restricted-syntax:"error" */ // no side effects allowed due to re-export by index.ts

export async function objectStorage<V, K extends IDBValidKey = IDBValidKey>(
  dbInfo: DbInfo,
): Promise<ObjectStorage<V, K>> {
  const db = await dbConnect(dbInfo);

  return {
    list: () => promise(() => objectStore('readonly').getAllKeys()),
    get: (key: K) => promise(() => objectStore('readonly').get(key)),
    getOpt: (key: K) => promise<V | undefined>(() => objectStore('readonly').get(key)).catch(() => undefined),
    getMany: (keys?: IDBKeyRange) => promise(() => objectStore('readonly').getAll(keys)),
    put: (key: K, value: V) => promise(() => objectStore('readwrite').put(value, key)),
    remove: (key: K | IDBKeyRange) => promise(() => objectStore('readwrite').delete(key)),
    clear: () => promise(() => objectStore('readwrite').clear()),
  };

  function objectStore(mode: IDBTransactionMode) {
    return db.transaction(dbInfo.store, mode).objectStore(dbInfo.store);
  }

  function promise<V>(f: () => IDBRequest) {
    return new Promise<V>((resolve, reject) => {
      const res = f();
      res.onsuccess = (e: Event) => resolve((e.target as IDBRequest).result);
      res.onerror = (e: Event) => reject((e.target as IDBRequest).result);
    });
  }
}

export function deleteObjectStorage(info: DbInfo): IDBOpenDBRequest | undefined {
  return 'indexedDB' in window && 'deleteDatabase' in window.indexedDB
    ? window.indexedDB.deleteDatabase(info.db ?? info.store)
    : undefined;
}

export interface DbInfo {
  /** name of the object store */
  store: string;
  /** defaults to store name because you should aim for one store per db to minimize version
   * upgrade callback complexity. raw idb is best for versioned multi-store dbs */
  db?: string;
  /** db version (default: 1), your upgrade callback receives e.oldVersion */
  version?: number;
  /** indices for the object store, changes must increment version */
  indices?: { name: string; keyPath: string | string[]; options?: IDBIndexParameters }[];
  /** upgrade function to handle schema changes @see objectStorage */
  upgrade?: (e: IDBVersionChangeEvent, store?: IDBObjectStore) => void;
}

export interface ObjectStorage<V, K extends IDBValidKey = IDBValidKey> {
  /** list all keys in the object store */
  list(): Promise<K[]>;
  /** retrieve a value by key */
  get(key: K): Promise<V>;
  /** retrieve or fail gracefully */
  getOpt(key: K): Promise<V | undefined>;
  /** retrieve multiple values by key range, or all values if omitted */
  getMany(keys?: IDBKeyRange): Promise<V[]>;
  /** put a value into the store under a specific key and return that key */
  put(key: K, value: V): Promise<K>;
  /** remove value(s) by key or key range */
  remove(key: K | IDBKeyRange): Promise<void>;
  /** clear all entries from the object store */
  clear(): Promise<void>;
}

async function dbConnect(info: DbInfo): Promise<IDBDatabase> {
  const dbName = info.db ?? info.store;

  return new Promise<IDBDatabase>((resolve, reject) => {
    if (!('indexedDB' in window) || !('open' in window.indexedDB)) reject('no indexedDB');
    const result = window.indexedDB.open(dbName, info.version ?? 1);

    result.onsuccess = (e: Event) => resolve((e.target as IDBOpenDBRequest).result);
    result.onerror = (e: Event) => reject((e.target as IDBOpenDBRequest).error ?? 'IndexedDB Unavailable');
    result.onupgradeneeded = (e: IDBVersionChangeEvent) => {
      const db = (e.target as IDBOpenDBRequest).result;
      const txn = (e.target as IDBOpenDBRequest).transaction;
      const store = db.objectStoreNames.contains(info.store)
        ? txn!.objectStore(info.store)
        : db.createObjectStore(info.store);

      const existing = new Set(store.indexNames);

      info.indices?.forEach(({ name, keyPath, options }) => {
        if (!existing.has(name)) store.createIndex(name, keyPath, options);
        else {
          const idx = store.index(name);
          if (
            idx.keyPath !== keyPath ||
            idx.unique !== !!options?.unique ||
            idx.multiEntry !== !!options?.multiEntry
          ) {
            store.deleteIndex(name);
            store.createIndex(name, keyPath, options);
          }
        }
        existing.delete(name);
      });
      existing.forEach(indexName => store.deleteIndex(indexName));
      info.upgrade?.(e, store);
    };
  });
}
