import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A least frequently used cache implementation. Insertion, retrieval, and removal are all constant time.
 */

class LFUCache<K, V> extends AbstractMap<K, V> implements Map<K, V> {
    static final int DEFAULT_MAX_ENTRIES = 1024;
    static final long DEFAULT_INVALIDATION_TIMEOUT = 30; // seconds

    int maxEntries;
    long invalidationTimeout;
    Map<K, V> cache;
    LinkedHashMap<K, LocalDateTime> insertionTimeOrderedTimestampMap;
    LinkedHashMap<K, Integer> increasingOrderedFrequencyMap;

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

    public V get(Object key) {
        if (hasTimedOut(key)) {
            cache.remove(key);
            return null;
        }
        return cache.get(key);
    }

    public boolean containsKey(Object key) {
        if (hasTimedOut(key)) {
            cache.remove(key);
            return false;
        }
        return cache.containsKey(key);
    }

    public void purgeInvalidEntries() {

    }

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

    // Usage of entrySet (e.g. by AbstractMap) is assumed to not change the relative frequencies
    @Override
    public Set entrySet() {
        purgeInvalidEntries();
        return cache.entrySet();
    }
}
