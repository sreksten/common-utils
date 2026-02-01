package com.threeamigos.common.util.implementations.injection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Thread-safe LRU cache with bounded size and hit rate tracking.
 *
 * <p>This cache provides atomic computeIfAbsent semantics using double-checked
 * locking. All operations are thread-safe, and the cache maintains accurate
 * statistics for hit rate monitoring.
 *
 * <p>Null values are supported through an internal sentinel value. The cache
 * will correctly cache and return null values from suppliers.
 *
 * <p>The cache uses the Least Recently Used (LRU) eviction policy. When the
 * cache exceeds maxCacheSize, the least recently accessed entry is evicted.
 * Note that the cache may temporarily contain maxCacheSize + 1 entries during
 * insertion before eviction occurs.
 *
 * <p>Performance characteristics:
 * <ul>
 *   <li>Cache hit: O(1), uncontended synchronization overhead
 *   <li>Cache miss: O(1) + O(computation), computation serialized per cache
 *   <li>Eviction: O(1), automatically handled by LinkedHashMap
 * </ul>
 *
 * <p>Thread-safety: All operations are thread-safe. Under concurrent access,
 * at most one thread will compute a value for any given key. Other threads
 * will block until the computation completes.
 *
 * <p>Example usage:
 * <pre>
 * Cache&lt;String, ExpensiveResult&gt; cache = new Cache&lt;&gt;();
 * ExpensiveResult result = cache.computeIfAbsent("key", () -&gt; {
 *     return computeExpensiveResult();
 * });
 *
 * double hitRate = cache.getCacheHitRate();
 * System.out.println("Cache hit rate: " + hitRate);
 * </pre>
 *
 * Checked and commented with Claude.
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of cached values
 *
 * @author Stefano Reksten
 */
public class Cache<K, V> {

    private static final int DEFAULT_MAX_CACHE_SIZE = 10_000;
    private static final int DEFAULT_INITIAL_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * Sentinel value used internally to represent cached null values.
     * This allows distinguishing between "not in cache" and "cached null value".
     */
    private static final Object NULL_PLACEHOLDER = new Object();

    private final int maxCacheSize;

    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    private final Map<K, Object> internalCache;

    /**
     * Lock used to ensure atomic computeIfAbsent operations.
     * All cache computations are serialized on this lock to prevent
     * duplicate computation of the same key.
     */
    private final Object computeLock = new Object();

    /**
     * Creates a cache with default settings: max size 10,000, initial capacity 16,
     * and load factor 0.75.
     */
    public Cache() {
        maxCacheSize = DEFAULT_MAX_CACHE_SIZE;
        internalCache = buildCache(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a cache with custom settings.
     *
     * @param maxCacheSize the maximum number of entries in the cache (must be positive)
     * @param initialCapacity the initial capacity of the underlying map (must be positive)
     * @param loadFactor the load factor for the underlying map (must be in (0, 1))
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public Cache(int maxCacheSize, int initialCapacity, float loadFactor) {
        if (maxCacheSize <= 0) {
            throw new IllegalArgumentException(
                    "maxCacheSize must be positive, got: " + maxCacheSize);
        }
        if (initialCapacity <= 0) {
            throw new IllegalArgumentException(
                    "initialCapacity must be positive, got: " + initialCapacity);
        }
        if (loadFactor <= 0 || loadFactor >= 1) {
            throw new IllegalArgumentException(
                    "loadFactor must be in (0, 1), got: " + loadFactor);
        }
        this.maxCacheSize = maxCacheSize;
        internalCache = buildCache(initialCapacity, loadFactor);
    }

    private Map<K, Object> buildCache(int initialCapacity, float loadFactor) {
        return Collections.synchronizedMap(
                new LinkedHashMap<K, Object>(initialCapacity, loadFactor, true) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<K, Object> eldest) {
                        // Note: This allows cache to temporarily grow to maxCacheSize + 1
                        // before eviction occurs (LinkedHashMap contract)
                        return size() > maxCacheSize;
                    }
                });
    }

    /**
     * Returns the value associated with the key, computing it if necessary.
     *
     * <p>If the key is already in the cache, returns the cached value and
     * increments the hit count. Otherwise, computes the value using the
     * supplier function, stores it in the cache, increments the miss count,
     * and returns the computed value.
     *
     * <p>This operation is atomic: if multiple threads call this method
     * concurrently with the same key, the supplier function will be called
     * at most once. Other threads will block until the computation completes.
     *
     * <p>Null values from the supplier are supported and will be cached.
     *
     * @param key the key whose associated value is to be returned
     * @param supplierFunction the function to compute the value if not cached
     * @return the cached or computed value, which may be null
     * @throws NullPointerException if key or supplierFunction is null
     * @throws RuntimeException if the supplier function throws an exception
     */
    public V computeIfAbsent(K key, Supplier<V> supplierFunction) {
        if (key == null) {
            throw new NullPointerException("key cannot be null");
        }
        if (supplierFunction == null) {
            throw new NullPointerException("supplierFunction cannot be null");
        }

        // Fast path: check cache without holding compute lock
        Object cached = internalCache.get(key);
        if (cached != null) {
            cacheHits.incrementAndGet();
            return unwrap(cached);
        }

        // Slow path: need to compute
        synchronized (computeLock) {
            // Double-check after acquiring lock
            cached = internalCache.get(key);
            if (cached != null) {
                cacheHits.incrementAndGet();
                return unwrap(cached);
            }

            // Cache miss - compute value
            cacheMisses.incrementAndGet();
            V value = supplierFunction.get();
            internalCache.put(key, wrap(value));
            return value;
        }
    }

    /**
     * Unwraps a cached value, converting NULL_PLACEHOLDER to actual null.
     */
    @SuppressWarnings("unchecked")
    private V unwrap(Object value) {
        return value == NULL_PLACEHOLDER ? null : (V) value;
    }

    /**
     * Wraps a value for storage, converting null to NULL_PLACEHOLDER.
     */
    private Object wrap(V value) {
        return value != null ? value : NULL_PLACEHOLDER;
    }

    /**
     * Returns the cache hit rate as a value between 0.0 and 1.0.
     *
     * <p>The hit rate is calculated as: hits / (hits + misses).
     * If no operations have been performed, returns 0.0.
     *
     * <p>Note: Under concurrent access, this calculation provides an
     * approximately consistent snapshot. The individual counter-reads
     * are atomic, but the two reads are not synchronized together.
     *
     * @return the cache hit rate, between 0.0 and 1.0
     */
    public double getCacheHitRate() {
        long hits = cacheHits.get();
        long total = hits + cacheMisses.get();
        return total == 0 ? 0.0 : (double) hits / total;
    }

    /**
     * Returns the number of cache hits.
     *
     * @return the number of cache hits
     */
    public long getHitCount() {
        return cacheHits.get();
    }

    /**
     * Returns the number of cache misses.
     *
     * @return the number of cache misses
     */
    public long getMissCount() {
        return cacheMisses.get();
    }

    /**
     * Returns the current number of entries in the cache.
     *
     * @return the current cache size
     */
    public int size() {
        return internalCache.size();
    }

    /**
     * Removes all entries from the cache.
     * Does not reset hit/miss statistics.
     */
    public void clear() {
        internalCache.clear();
    }

    /**
     * Removes a specific key from the cache.
     *
     * @param key the key to remove
     */
    public void invalidate(K key) {
        internalCache.remove(key);
    }

    /**
     * Invalidates all entries in the cache that satisfy the given predicate.
     *
     * @param predicate the predicate to test each key against
     */
    public void invalidateAll(Predicate<K> predicate) {
        internalCache.entrySet().removeIf(entry -> predicate.test(entry.getKey()));
    }
}
