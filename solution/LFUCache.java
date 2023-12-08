package solution;

import java.time.LocalDateTime;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A least frequently used cache implementation. Insertion, retrieval, and
 * removal are constant time (with the small caveat that the first insertion
 * may be slow if there is a backlog of timed-out entries. This can be mitigated
 * by disabling {@code greedyPurge} (discussed below)).
 *
 * Cache entries are stored alongside their retieval frequency and insertion
 * time. When the time since insertion exceeds {@code invalidationTimeout}, the
 * entry is considered invalid. When {@code greedyPurge} is enabled (default),
 * the cache cannot be seen in an invalid state (i.e. containing invalid
 * elements). When {@code greedyPurge} is disabled, they will only be removed on
 * attempted retrieval - thus, unless scheduled {@code purgeInvalidEntries()}
 * are performed, they will build up in the cache.
 *
 * Fast insertion is performed using a Map between Ks (or equivalent) and
 * doubly-linked nodes containing the corresponding frequency and a Set of Ks
 * with that frequency. When a K increases in frequency, it moves one node
 * forward (possibly creating a new one). Nodes with an empty set of Ks are
 * removed from the list.
 */

class LFUCache<K, V> extends AbstractMap<K, V> implements Cache<K, V> {
    // Optional params with defaults
    private static final int DEFAULT_MAX_ENTRIES = 1024;
    private static final long DEFAULT_INVALIDATION_TIMEOUT = 30;
    private static final boolean DEFAULT_GREEDY_PURGE = true;

    private int maxEntries;
    private long invalidationTimeout; // In seconds
    private boolean greedyPurge;
    private Map<K, V> cache;
    private LinkedHashMap<K, LocalDateTime> insertionTimeOrderedTimestampMap;
    /* 
     * increasingOrderedFrequencyListMap will always have keys of type K or
     * equal to a K; they're Objects here to avoid running into casts in get().
     */
    private Map<Object, FrequencyEquivalenceNode> increasingOrderedFrequencyListMap;
    private FrequencyEquivalenceNode frequencyListHead;

    public LFUCache() {
        this.maxEntries = DEFAULT_MAX_ENTRIES;
        this.invalidationTimeout = DEFAULT_INVALIDATION_TIMEOUT;
        this.greedyPurge = DEFAULT_GREEDY_PURGE;
        this.cache = new HashMap<>();
        this.insertionTimeOrderedTimestampMap = new LinkedHashMap<>();
        this.increasingOrderedFrequencyListMap = new HashMap<>();
        this.frequencyListHead = new FrequencyEquivalenceNode(1);
    }

    public LFUCache<K, V> withMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
        return this;
    }

    public LFUCache<K, V> withInvalidationTimeout(long invalidationTimeout) {
        this.invalidationTimeout = invalidationTimeout;
        return this;
    }

    public LFUCache<K, V> withGreedyPurge(boolean greedyPurge) {
        this.greedyPurge = greedyPurge;
        return this;
    }

    @Override
    public V get(Object key) {
        if (hasTimedOut(key)) {
            this.remove(key);
        }

        incrementFrequency(key);

        return cache.get(key);
    }

    @Override
    public V put(K key, V value) {
        if (greedyPurge) {
            // Free up as much space as possible before deleting valid entries
            purgeInvalidEntries();
        }

        if (increasingOrderedFrequencyListMap.containsKey(key)) {
            increasingOrderedFrequencyListMap
                .get(key)
                .removeKey(key);
            increasingOrderedFrequencyListMap.remove(key);
        } else {
            if (cache.size() >= maxEntries) {
                Object toRemove;
                if (frequencyListHead.isEmpty()) {
                    toRemove = frequencyListHead
                        .getNext()
                        .getFirstKey();
                } else {
                    toRemove = frequencyListHead.getFirstKey();
                }

                this.remove(toRemove);
            }
        }

        insertionTimeOrderedTimestampMap.put(key, LocalDateTime.now());
        increasingOrderedFrequencyListMap.put(key, frequencyListHead);
        frequencyListHead.addKey(key);
        return cache.put(key, value);
    }

    private void incrementFrequency(Object key) {
        if (increasingOrderedFrequencyListMap.containsKey(key)) {
            FrequencyEquivalenceNode current = increasingOrderedFrequencyListMap.get(key);
            int currentFreq = current.getFrequency();
            FrequencyEquivalenceNode next = current.getNext();

            /* Make sure nextFrequencyNode has a frequency of 1 higher
             * than the old. Otherwise, insert a new one.
             */
            if (next == null) {
                FrequencyEquivalenceNode newNode
                    = new FrequencyEquivalenceNode(currentFreq + 1);
                current.setNext(newNode);
                newNode.setPrev(current);
                newNode.addKey(key);
            } else if (next.getFrequency() == currentFreq + 1) {
                current.getNext().addKey(key);
            } else {
                FrequencyEquivalenceNode newNode
                    = new FrequencyEquivalenceNode(currentFreq + 1);
                current.setNext(newNode);
                newNode.setPrev(current);
                newNode.setNext(next);
                next.setPrev(current.getNext());
                newNode.addKey(key);
            }

            increasingOrderedFrequencyListMap.put(key, current.getNext());
            // Deletion and relinking handled here
            current.removeKey(key);
        }
    }

    /*
     * Note that this evaluates to false for a key that has already timed out
     * and been removed from the cache.
     */
    private boolean hasTimedOut(Object key) {
        if (!insertionTimeOrderedTimestampMap.containsKey(key)) {
            return false;
        }

        LocalDateTime insertionTime = insertionTimeOrderedTimestampMap.get(key);
        if (insertionTime.plusSeconds(invalidationTimeout).isBefore(LocalDateTime.now())) {
            // System.out.println("Invalid key detected; removing");
            return true;
        } else {
            return false;
        }
    }

    /*
     * Called frequently enough, this should be a cheap operation.
     * Occasionally, a put() may be called after a long period of inactivity.
     * If the cache is very large and completely invalid we may have to traverse
     * the entire thing. This can be mitigated by disabling greedyPurge and
     * scheduling periodic purges instead.
     */
    public void purgeInvalidEntries() {
        // Guaranteed to be in insertion order
        for (K timestampKey: insertionTimeOrderedTimestampMap.keySet()) {
            if (hasTimedOut(timestampKey)) {
                this.remove(timestampKey);
            } else {
                // All further entries are valid
                break;
            }
        }
    }

    @Override
    public V remove(Object key) {
        insertionTimeOrderedTimestampMap.remove(key);
        increasingOrderedFrequencyListMap.get(key).removeKey(key);
        increasingOrderedFrequencyListMap.remove(key);
        return cache.remove(key);
    }

    /* 
     * Client usage of entrySet() is assumed to not directly change the relative
     * frequencies; only direct (or indirect, i.e. through AbstractMap) usage of
     * get() should do this.
     */
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        if (greedyPurge) {
            purgeInvalidEntries();
        }

        return cache.entrySet();
    }

    public String getFrequenciesRepr() {
        return frequencyListHead.toString();
    }

    public String getFrequencyCounts() {
        return frequencyListHead.frequencyCounts();
    }

    /*
    * A node representing a set of values of equivalent frequency in a linked
    * list. Automatically links neighbours if there are no more values with
    * this frequency, but never removes head node with frequency 1.
    *
    * Keys are of type Object in order to avoid casts in LFUCache's get() and
    * remove(), but will always be of type K or equal to a K.
    */
    private class FrequencyEquivalenceNode {
        private int frequency;
        private Set<Object> keySet;
        private FrequencyEquivalenceNode prev;
        private FrequencyEquivalenceNode next;

        FrequencyEquivalenceNode(int frequency) {
            this.frequency = frequency;
            this.keySet = new LinkedHashSet<>();
        }

        public int getFrequency() {
            return frequency;
        }

        public void addKey(Object key) {
            keySet.add(key);
        }

        public void removeKey(Object key) {
            keySet.remove(key);
            if (isEmpty()) {
                /*
                 * Remove this node from the linked list and link neighbours if
                 * they exist, UNLESS this is head.
                 */
                if (prev != null) {
                    prev.setNext(next);
                } else {
                    return;
                }

                if (next != null) {
                    next.setPrev(prev);
                }
            }
        }

        public Object getFirstKey() {
            if (isEmpty()) {
                return null;
            }
            return keySet.iterator().next();
        }

        public boolean isEmpty() {
            return keySet.isEmpty();
        }

        public FrequencyEquivalenceNode getPrev() {
            return prev;
        }

        public void setPrev(FrequencyEquivalenceNode prev) {
            this.prev = prev;
        }

        public FrequencyEquivalenceNode getNext() {
            return next;
        }

        public void setNext(FrequencyEquivalenceNode next) {
            this.next = next;
        }

        @Override
        public String toString() {
            if (prev == null) {
                if (next == null) {
                    return "{"
                        + frequency
                        + ": "
                        + keySet.toString()
                        + "}";
                } else {
                    return "{"
                        + frequency
                        + ": "
                        + keySet.toString()
                        + ", "
                        + next.toString();
                }
            } else if (next == null) {
                return frequency
                    + ": "
                    + keySet.toString()
                    + "}";
            } else {
                return frequency
                    + ": "
                    + keySet.toString()
                    + ", "
                    + next.toString();
            }
        }

        public String frequencyCounts() {
            if (prev == null) {
                if (next == null) {
                    return "{"
                        + frequency
                        + ": "
                        + keySet.size()
                        + "}";
                } else {
                    return "{"
                        + frequency
                        + ": "
                        + keySet.size()
                        + ", "
                        + next.frequencyCounts();
                }
            } else if (next == null) {
                return frequency
                    + ": "
                    + keySet.size()
                    + "}";
            } else {
                return frequency
                    + ": "
                    + keySet.size()
                    + ", "
                    + next.frequencyCounts();
            }
        }
    }
}

