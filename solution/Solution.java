package solution;

import java.util.concurrent.ThreadLocalRandom;

class Solution {
    public static void main(String[] args) throws InterruptedException {
        insertEntries_success();
        insertEntries_hasMax();
        insertMax_removesLeastFrequent();
        insertAndRetrieveMillionRandomEntries_success();
        timedoutEntries_removed();
    }


    // Tests

    public static void insertEntries_success() {
        LFUCache<Integer, Integer> cache = new LFUCache().withMaxEntries(10);

        for (int i = 0; i < 10; i++) {
            cache.put(i, i*i);
        }

        for (int i = 0; i < 10; i++) {
            assert(cache.get(i) == i*i);
        }
    }

    public static void insertEntries_hasMax() {
        LFUCache<Integer, Integer> cache = new LFUCache().withMaxEntries(10);

        for (int i = 0; i < 11; i++) {
            cache.put(i, i*i);
        }

        assert(cache.size() == 10);
    }

    public static void insertMax_removesLeastFrequent() {
        LFUCache<Integer, Integer> cache = new LFUCache().withMaxEntries(17);

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

    public static void insertAndRetrieveMillionRandomEntries_success() {
        // Default max size of 1024
        LFUCache<Integer, Integer> cache = new LFUCache();

        for (int i = 0; i < 1_000_000; i++) {
            cache.put(
                ThreadLocalRandom.current().nextInt(0, 2048),
                ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE)
            );
            cache.get(ThreadLocalRandom.current().nextInt(0, 2048));
        }

        assert(cache.size() == 1024);
        System.out.println(cache.getFrequencyCounts());
    }

    public static void timedoutEntries_removed() throws InterruptedException {
        LFUCache<Integer, Integer> cache = new LFUCache().withInvalidationTimeout(1);

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
}

