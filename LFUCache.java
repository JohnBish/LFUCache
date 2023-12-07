import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A least frequently used cache implementation. Insertion, retrieval, and
 * removal are all constant time.
 */

class LFUCache<K, V> extends AbstractMap<K, V> implements Cache<K, V> {
    static final int DEFAULT_MAX_ENTRIES = 1024;
    static final long DEFAULT_INVALIDATION_TIMEOUT = 30; // seconds

    int maxEntries;
    long invalidationTimeout;
    Map<K, V> cache;
    LinkedHashMap<K, LocalDateTime> insertionTimeOrderedTimestampMap;
    LinkedHashMap<K, MutableInteger> increasingOrderedFrequencyMap;

    LFUCache() {
        this.maxEntries = DEFAULT_MAX_ENTRIES;
        this.invalidationTimeout = DEFAULT_INVALIDATION_TIMEOUT;
        this.cache = new HashMap<>();
        this.insertionTimeOrderedTimestampMap = new LinkedHashMap<>();
        this.increasingOrderedFrequencyMap = new LinkedHashMap<>();
    }

    LFUCache(int maxEntries) {
        this.maxEntries = maxEntries;
        this.invalidationTimeout = DEFAULT_INVALIDATION_TIMEOUT;
        this.cache = new HashMap<>();
        this.insertionTimeOrderedTimestampMap = new LinkedHashMap<>();
        this.increasingOrderedFrequencyMap = new LinkedHashMap<>();
    }

    LFUCache(int maxEntries, long invalidationTimeout) {
        this.maxEntries = maxEntries;
        this.invalidationTimeout = invalidationTimeout;
        this.cache = new HashMap<>();
        this.insertionTimeOrderedTimestampMap = new LinkedHashMap<>();
        this.increasingOrderedFrequencyMap = new LinkedHashMap<>();
    }

    @Override
    public V get(Object key) {
        purgeInvalidEntries();

        if (increasingOrderedFrequencyMap.containsKey(key)) {
            /*
             * The reason increasingOrderedFrequencyMap's values must be 
             * mutable is below: since key is not necessarily of type K (but
             * possibly equal to a K), there is no way to call put(),
             * computeIfPresent(), etc.
             */
            increasingOrderedFrequencyMap.get(key).incrementValue();
        }

        return cache.get(key);
    }

    public void purgeInvalidEntries() {

    }

    /*
     * Note that this evaluates to false for a key that has already timed out
     * and been removed from the cache
     */
    private boolean hasTimedOut(Object key) {
        if (!insertionTimeOrderedTimestampMap.containsKey(key)) {
            return false;
        }

        LocalDateTime insertionTime = insertionTimeOrderedTimestampMap.get(key);
        if (insertionTime.plusSeconds(invalidationTimeout).isAfter(LocalDateTime.now())) {
            return true;
        } else {
            return false;
        }
    }

    /* 
     * Usage of entrySet() is assumed to not directly change the relative
     * frequencies; only direct (or indirect) usage of get() should do this.
     */
    @Override
    public Set entrySet() {
        purgeInvalidEntries();
        /*
         * 'Unchecked' cast. We, the developer, know that cache is also a
         * Map<K, V>.
         */
        return cache.entrySet();
    }
}
