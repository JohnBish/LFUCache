package solution;

import java.util.concurrent.ThreadLocalRandom;

class Solution {
    public static void main(String[] args) throws InterruptedException {
        // Tests
        insertEntries_success();
        insertEntries_hasMax();
        insertMax_removesLeastFrequent();
        insertAndRetrieveMillionRandomEntries_success();
        timedoutEntries_removed();
        noGreedyPurge_timedoutEntries_notRemoved();
    }

    private static void insertEntries_success() {
        // Sadly, the fluent inteface precludes type inference
        Cache<Integer, Integer> cache = new LFUCache<Integer, Integer>()
            .withMaxEntries(10);

        for (int i = 0; i < 10; i++) {
            cache.put(i, i*i);
        }

        for (int i = 0; i < 10; i++) {
            assert(cache.get(i) == i*i);
        }
    }

    private static void insertEntries_hasMax() {
        LFUCache<Integer, Integer> cache = new LFUCache<Integer, Integer>()
            .withMaxEntries(10);

        for (int i = 0; i < 11; i++) {
            cache.put(i, i*i);
        }

        assert(cache.size() == 10);
    }

    private static void insertMax_removesLeastFrequent() {
        LFUCache<Integer, Integer> cache = new LFUCache<Integer, Integer>()
            .withMaxEntries(17);

        for (int i = 0; i < 17; i++) {
            cache.put(i, i*i);
        }
        for(int i = 0; i < 12; i++) {
            cache.get(i);
        }
        for(int i = 13; i < 17; i++) {
            cache.get(i);
        }

        assert(cache.size() == 17);
        assert(
            cache
                .getFrequenciesRepr()
                .equals("{1: [12], 2: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15, 16]}")
        );

        cache.put(23, 100007);

        assert(cache.size() == 17);
        assert(!cache.containsKey(12));
        assert(
            cache
                .getFrequenciesRepr()
                .equals("{1: [23], 2: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15, 16]}")
        );
        for (int i = 0; i < 12; i++) {
            assert(cache.get(i) == i*i);
        }
        for(int i = 13; i < 17; i++) {
            assert(cache.get(i) == i*i);
        }
        assert(cache.get(23) == 100007);
    }

    private static void insertAndRetrieveMillionRandomEntries_success() {
        // Default max size of 1024
        LFUCache<Integer, Integer> cache = new LFUCache<>();

        for (int i = 0; i < 1_000_000; i++) {
            cache.put(
                ThreadLocalRandom.current().nextInt(0, 2048),
                ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE)
            );
            cache.get(ThreadLocalRandom.current().nextInt(0, 2048));
        }

        assert(cache.size() == 1024);
        // System.out.println(cache.getFrequencyCountsRepr());
        assert(cache.getTotalFrequencyCounts() == 1024);
    }

    private static void timedoutEntries_removed() throws InterruptedException {
        LFUCache<Integer, Integer> cache = new LFUCache<Integer, Integer>()
            .withInvalidationTimeout(1);

        for (int i = 0; i < 10; i++) {
            cache.put(i, i*i);
        }
        Thread.sleep(500);
        for (int i = 10; i < 20; i++) {
            cache.put(i, i*i);
        }
        Thread.sleep(700);

        for (int i = 0; i < 10; i++) {
            assert(cache.get(i) == null);
        }
        for (int i = 10; i < 20; i++) {
            assert(cache.get(i) == i*i);
        }
    }

    private static void noGreedyPurge_timedoutEntries_notRemoved() throws InterruptedException {
        LFUCache<Integer, Integer> cache = new LFUCache<Integer, Integer>()
            .withInvalidationTimeout(1)
            .withGreedyPurge(false);

        for (int i = 0; i < 10; i++) {
            cache.put(i, i*i);
        }
        Thread.sleep(1200);
        assert(cache.keySet().size() == 10);
        // Explicit gets still remove invalid elements
        for (int i = 0; i < 10; i++) {
            assert(cache.get(i) == null);
        }
    }
}

