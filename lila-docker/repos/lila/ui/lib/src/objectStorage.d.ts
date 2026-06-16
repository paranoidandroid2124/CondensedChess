export declare function objectStorage<V, K extends IDBValidKey = IDBValidKey>(dbInfo: DbInfo): Promise<ObjectStorage<V, K>>;
export declare function deleteObjectStorage(info: DbInfo): IDBOpenDBRequest | undefined;
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
