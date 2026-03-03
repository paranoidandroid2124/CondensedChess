 
/** promisify [indexedDB](https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API) and add nothing
 * ### basic usage:
 * ```ts
 *   import { objectStorage } from 'lib/objectStorage';
 *
 *   const store = await objectStorage<number>({ store: 'store' });
 *   const value = await store.get('someKey') ?? 10;
 *   await store.put('someOtherKey', value + 1);
 * ```
 * ### cursors/indices:
 * ```ts
 *   import { objectStorage, range } from 'lib/objectStorage';
 *
 *   const store = await objectStorage<MyObj>({
 *     store: 'store',
 *     indices: [{ name: 'size', keyPath: 'size' }]
 *   });
 *
 *   await store.readCursor({ index: 'size', query: range({ above: 5 }) }, obj => {
 *     console.log(obj);
 *   });
 *
 *   await store.writeCursor(
 *     { index: 'size', query: range({ min: 4, max: 12 }) },
 *     async ({ value, update, delete }) => {
 *       if (value.size < 10) await update({ ...value, size: value.size + 1 });
 *       else await delete();
 *     }
 *   );
 * ```
 * ### upgrade/migration:
 * ```ts
 *   import { objectStorage } from 'lib/objectStorage';
 *
 *   const upgradedStore = await objectStorage<MyObj>({
 *     store: 'upgradedStore',
 *     version: 2,
 *     upgrade: (e, store) => {
 *       // raw idb needed here
 *       if (e.oldVersion < 2) store.createIndex('color', 'color'); // manual index creation
 *       const req = store.openCursor();
 *       req.onsuccess = cursorEvent => {
 *         const cursor = (cursorEvent.target as IDBRequest<IDBCursorWithValue>).result;
 *         if (!cursor) return;
 *         cursor.update(transformYourObject(e.oldVersion, cursor.value));
 *         cursor.continue();
 *       };
 *     }
 *   });
 * ```
 * other needs can be met by raw idb calls on the `txn` function result
 * @see https://developer.mozilla.org/en-US/docs/Web/API/IndexedDB_API
 */
export declare function objectStorage<V, K extends IDBValidKey = IDBValidKey>(dbInfo: DbInfo): Promise<ObjectStorage<V, K>>;
export declare function range<K extends IDBValidKey>(range: {
    min?: K;
    max?: K;
    above?: K;
    below?: K;
}): IDBKeyRange | undefined;
export declare function deleteObjectStorage(info: DbInfo): IDBOpenDBRequest | undefined;
export declare function nonEmptyStore(info: DbInfo): Promise<boolean>;
export interface DbInfo {
    /** name of the object store */
    store: string;
    /** defaults to store name because you should aim for one store per db to minimize version
     * upgrade callback complexity. raw idb is best for versioned multi-store dbs */
    db?: string;
    /** db version (default: 1), your upgrade callback receives e.oldVersion */
    version?: number;
    /** indices for the object store, changes must increment version */
    indices?: {
        name: string;
        keyPath: string | string[];
        options?: IDBIndexParameters;
    }[];
    /** upgrade function to handle schema changes @see objectStorage */
    upgrade?: (e: IDBVersionChangeEvent, store?: IDBObjectStore) => void;
}
export type WriteCursorCallback<V> = {
    (it: {
        /** just the value */
        value: V;
        /** await this to modify the store value */
        update: (v: V) => Promise<void>;
        /** await this to delete the entry from the store. iteration is not affected */
        delete: () => Promise<void>;
    }): any;
};
export interface CursorOpts {
    /** supply an index name to use for the cursor, otherwise iterate the store */
    index?: string;
    /** The key range to filter the cursor results */
    query?: IDBKeyRange | IDBValidKey | null;
    /** 'prev', 'prevunique', 'next', or 'nextunique' (default is 'next')*/
    dir?: IDBCursorDirection;
}
export interface ObjectStorage<V, K extends IDBValidKey = IDBValidKey> {
    /** list all keys in the object store */
    list(): Promise<K[]>;
    /** check if a key exists */
    has(key: K): Promise<boolean>;
    /** retrieve a value by key */
    get(key: K): Promise<V>;
    /** retrieve or fail gracefully */
    getOpt(key: K): Promise<V | undefined>;
    /** retrieve multiple values by key range, or all values if omitted */
    getMany(keys?: IDBKeyRange): Promise<V[]>;
    /** put a value into the store under a specific key and return that key */
    put(key: K, value: V): Promise<K>;
    /** count the number of entries matching a key or range. Count all values if omitted */
    count(key?: K | IDBKeyRange): Promise<number>;
    /** remove value(s) by key or key range */
    remove(key: K | IDBKeyRange): Promise<void>;
    /** clear all entries from the object store */
    clear(): Promise<void>;
    /** initiate a database transaction */
    txn(mode: IDBTransactionMode): IDBTransaction;
    /** create a raw cursor to iterate over an index or store's records */
    cursor(opts: CursorOpts, mode: IDBTransactionMode): AsyncGenerator<IDBCursorWithValue>;
    /** read records using an idb cursor via simple value callback. resolves when iteration completes */
    readCursor(o: CursorOpts, it: (v: V) => any): Promise<void>;
    /** read, write, or delete records via cursor callback. promise resolves when iteration is done */
    writeCursor(o: CursorOpts, it: WriteCursorCallback<V>): Promise<void>;
    /** delete this database from browser storage */
    deleteDb(): void;
}
