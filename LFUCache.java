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
    private final int maxEntries;
    private final long invalidationTimeout;
    private final boolean greedyPurge;
    private Map<K, V> cache;
    private LinkedHashMap<K, LocalDateTime> insertionTimeOrderedTimestampMap;
    private LinkedHashMap<K, MutableInteger> increasingOrderedFrequencyMap;

    public static class Builder {
        // Optional params with defaults
        private int maxEntries = 1024;
        private long invalidationTimeout = 30;
        private boolean greedyPurge = true;

        public Builder maxEntries(int maxEntries) {
            this.maxEntries = maxEntries;
            return this;
        }

        public Builder invalidationTimeout(long invalidationTimeout) {
            this.invalidationTimeout = invalidationTimeout;
            return this;
        }

        public Builder greedyPurge(boolean greedyPurge) {
            this.greedyPurge = greedyPurge;
            return this;
        }

        public LFUCache build() {
            return new LFUCache(this);
        }
    }

    private LFUCache(Builder builder) {
        this.maxEntries = builder.maxEntries;
        this.invalidationTimeout = builder.invalidationTimeout;
        this.greedyPurge = builder.greedyPurge;
        this.cache = new HashMap<>();
        this.insertionTimeOrderedTimestampMap = new LinkedHashMap<>();
        this.increasingOrderedFrequencyMap = new LinkedHashMap<>();
    }

    @Override
    public V get(Object key) {
        if (greedyPurge) {
            purgeInvalidEntries();
        }

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
    public Set<Map.Entry<K, V>> entrySet() {
        if (greedyPurge) {
            purgeInvalidEntries();
        }

        return cache.entrySet();
    }
}
