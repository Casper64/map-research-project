package com.casperswebsites.trees;

import com.casperswebsites.trees.bplustree.BPlusTree;
import com.casperswebsites.trees.redblack.RedBlackTree;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Runs performance tests for a TreeMap, HashMap, RedBlackTree, and B+ Tree implementations.
 */
public class PerformanceTest {

    private static final int[] DATA_SIZES = { 100, 1_000, 10_000, 100_000, 1_000_000 };
    private static final int[] BPLUS_ORDERS = { 8, 16, 32, 128, 256 };
    private static final int WARMUP_ITERATIONS = 5;
    private static final int TEST_ITERATIONS = 10;
    private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    public static void main(String[] args) {
        System.out.println("=== Map Performance Comparison ===\n");

        PerformanceTest test = new PerformanceTest();

        for (int size : DATA_SIZES) {
            System.out.println("Data Size: " + size);
            System.out.println("-".repeat(50));

            test.runAllTests(size);
            System.out.println();
        }

        System.out.println("=== Performance Test Complete ===");
    }

    private void runAllTests(int size) {
        // Test sequential operations
        System.out.println("Sequential Operations:");
        testSequentialInserts(size);
        testSequentialDeletes(size);

        System.out.println();

        // Test random operations
        System.out.println("Random Operations:");
        testRandomInserts(size);
        testRandomDeletes(size);

        System.out.println();

        // Test iteration operations
        System.out.println("Iteration Operations:");
        testFullIteration(size);
        // testPartialIteration(size);

        System.out.println();
    }

    private void testSequentialInserts(int size) {
        System.out.println("Sequential Inserts:");

        // HashMap
        long hashMapTime = benchmarkSequentialInserts(new HashMap<>(), size);
        System.out.printf("  HashMap:           %8d µs\n", hashMapTime);

        // TreeMap
        long treeMapTime = benchmarkSequentialInserts(new TreeMap<>(), size);
        System.out.printf(
            "  TreeMap:           %8d µs (%.2fx)\n",
            treeMapTime,
            (double) treeMapTime / hashMapTime
        );

        // RedBlackTree
        long rbTime = benchmarkSequentialInserts(new RedBlackTree<>(), size);
        System.out.printf(
            "  RedBlackTree:      %8d µs (%.2fx)\n",
            rbTime,
            (double) rbTime / hashMapTime
        );

        // B+ Trees with different orders
        for (int order : BPLUS_ORDERS) {
            long bpTime = benchmarkSequentialInserts(new BPlusTree<>(order), size);
            System.out.printf(
                "  B+Tree (order %3d): %8d µs (%.2fx)\n",
                order,
                bpTime,
                (double) bpTime / hashMapTime
            );
        }
    }

    private void testSequentialDeletes(int size) {
        System.out.println("Sequential Deletes:");

        // HashMap
        long hashMapTime = benchmarkSequentialDeletes(new HashMap<>(), size);
        System.out.printf("  HashMap:           %8d µs\n", hashMapTime);

        // TreeMap
        long treeMapTime = benchmarkSequentialDeletes(new TreeMap<>(), size);
        System.out.printf(
            "  TreeMap:           %8d µs (%.2fx)\n",
            treeMapTime,
            (double) treeMapTime / hashMapTime
        );

        // RedBlackTree
        long rbTime = benchmarkSequentialDeletes(new RedBlackTree<>(), size);
        System.out.printf(
            "  RedBlackTree:      %8d µs (%.2fx)\n",
            rbTime,
            (double) rbTime / hashMapTime
        );

        // B+ Trees with different orders
        for (int order : BPLUS_ORDERS) {
            long bpTime = benchmarkSequentialDeletes(new BPlusTree<>(order), size);
            System.out.printf(
                "  B+Tree (order %3d): %8d µs (%.2fx)\n",
                order,
                bpTime,
                (double) bpTime / hashMapTime
            );
        }
    }

    private void testRandomInserts(int size) {
        System.out.println("Random Inserts:");

        // HashMap
        long hashMapTime = benchmarkRandomInserts(new HashMap<>(), size);
        System.out.printf("  HashMap:           %8d µs\n", hashMapTime);

        // TreeMap
        long treeMapTime = benchmarkRandomInserts(new TreeMap<>(), size);
        System.out.printf(
            "  TreeMap:           %8d µs (%.2fx)\n",
            treeMapTime,
            (double) treeMapTime / hashMapTime
        );

        // RedBlackTree
        long rbTime = benchmarkRandomInserts(new RedBlackTree<>(), size);
        System.out.printf(
            "  RedBlackTree:      %8d µs (%.2fx)\n",
            rbTime,
            (double) rbTime / hashMapTime
        );

        // B+ Trees with different orders
        for (int order : BPLUS_ORDERS) {
            long bpTime = benchmarkRandomInserts(new BPlusTree<>(order), size);
            System.out.printf(
                "  B+Tree (order %3d): %8d µs (%.2fx)\n",
                order,
                bpTime,
                (double) bpTime / hashMapTime
            );
        }
    }

    private void testRandomDeletes(int size) {
        System.out.println("Random Deletes:");

        // HashMap
        long hashMapTime = benchmarkRandomDeletes(new HashMap<>(), size);
        System.out.printf("  HashMap:           %8d µs\n", hashMapTime);

        // TreeMap
        long treeMapTime = benchmarkRandomDeletes(new TreeMap<>(), size);
        System.out.printf(
            "  TreeMap:           %8d µs (%.2fx)\n",
            treeMapTime,
            (double) treeMapTime / hashMapTime
        );

        // RedBlackTree
        long rbTime = benchmarkRandomDeletes(new RedBlackTree<>(), size);
        System.out.printf(
            "  RedBlackTree:      %8d µs (%.2fx)\n",
            rbTime,
            (double) rbTime / hashMapTime
        );

        // B+ Trees with different orders
        for (int order : BPLUS_ORDERS) {
            long bpTime = benchmarkRandomDeletes(new BPlusTree<>(order), size);
            System.out.printf(
                "  B+Tree (order %3d): %8d µs (%.2fx)\n",
                order,
                bpTime,
                (double) bpTime / hashMapTime
            );
        }
    }

    private void testFullIteration(int size) {
        System.out.println("Full Iteration:");

        // HashMap
        long hashMapTime = benchmarkFullIteration(new HashMap<>(), size);
        System.out.printf("  HashMap:           %8d µs\n", hashMapTime);

        // TreeMap
        long treeMapTime = benchmarkFullIteration(new TreeMap<>(), size);
        System.out.printf(
            "  TreeMap:           %8d µs (%.2fx)\n",
            treeMapTime,
            (double) treeMapTime / hashMapTime
        );

        // RedBlackTree
        long rbTime = benchmarkFullIteration(new RedBlackTree<>(), size);
        System.out.printf(
            "  RedBlackTree:      %8d µs (%.2fx)\n",
            rbTime,
            (double) rbTime / hashMapTime
        );

        // B+ Trees with different orders
        for (int order : BPLUS_ORDERS) {
            long bpTime = benchmarkFullIteration(new BPlusTree<>(order), size);
            System.out.printf(
                "  B+Tree (order %3d): %8d µs (%.2fx)\n",
                order,
                bpTime,
                (double) bpTime / hashMapTime
            );
        }
    }

    // private void testPartialIteration(int size) {
    //     System.out.println("Partial Iteration (25% range):");

    //     // HashMap
    //     long hashMapTime = benchmarkPartialIteration(new HashMap<>(), size);
    //     System.out.printf("  HashMap:           %8d µs\n", hashMapTime);

    //     // TreeMap
    //     long treeMapTime = benchmarkPartialIteration(new TreeMap<>(), size);
    //     System.out.printf(
    //         "  TreeMap:           %8d µs (%.2fx)\n",
    //         treeMapTime,
    //         (double) treeMapTime / hashMapTime
    //     );

    //     // RedBlackTree
    //     long rbTime = benchmarkPartialIteration(new RedBlackTree<>(), size);
    //     System.out.printf(
    //         "  RedBlackTree:      %8d µs (%.2fx)\n",
    //         rbTime,
    //         (double) rbTime / hashMapTime
    //     );

    //     // B+ Trees with different orders
    //     for (int order : BPLUS_ORDERS) {
    //         long bpTime = benchmarkPartialIteration(new BPlusTree<>(order), size);
    //         System.out.printf(
    //             "  B+Tree (order %3d): %8d µs (%.2fx)\n",
    //             order,
    //             bpTime,
    //             (double) bpTime / hashMapTime
    //         );
    //     }
    // }

    private long benchmarkSequentialInserts(Map<Integer, String> map, int size) {
        // First measure memory usage
        MemoryStats memoryStats = measureMemoryUsage(() -> {
            map.clear();
            for (int i = 0; i < size; i++) {
                map.put(i, "value" + i);
            }
        });

        // Then measure execution time
        long timeResult = benchmark(() -> {
            map.clear();
            for (int i = 0; i < size; i++) {
                map.put(i, "value" + i);
            }
        });

        System.out.printf("    Memory usage: %s\n", memoryStats);
        return timeResult;
    }

    private long benchmarkRandomInserts(Map<Integer, String> map, int size) {
        Integer[] keys = generateRandomKeys(size);

        // First measure memory usage
        MemoryStats memoryStats = measureMemoryUsage(() -> {
            map.clear();
            for (int key : keys) {
                map.put(key, "value" + key);
            }
        });

        // Then measure execution time
        long timeResult = benchmark(() -> {
            map.clear();
            for (int key : keys) {
                map.put(key, "value" + key);
            }
        });

        System.out.printf("    Memory usage: %s\n", memoryStats);
        return timeResult;
    }

    private long benchmarkSequentialDeletes(Map<Integer, String> map, int size) {
        // Memory usage isn't meaningful for deletion operations as it decreases
        MemoryStats memoryStats = measureMemoryUsage(() -> {
            // Setup
            map.clear();
            for (int i = 0; i < size; i++) {
                map.put(i, "value" + i);
            }

            // Delete everything to measure memory footprint difference
            for (int i = 0; i < size; i++) {
                map.remove(i);
            }
        });

        long timeResult = benchmark(() -> {
            // Setup
            map.clear();
            for (int i = 0; i < size; i++) {
                map.put(i, "value" + i);
            }

            // Actual deletion (timed part)
            long start = System.nanoTime();
            for (int i = 0; i < size; i++) {
                map.remove(i);
            }
            return System.nanoTime() - start;
        });

        System.out.printf("    Memory reduction: %s\n", memoryStats);
        return timeResult;
    }

    private long benchmarkRandomDeletes(Map<Integer, String> map, int size) {
        Integer[] keys = generateRandomKeys(size);

        // Memory usage is measured for completeness
        MemoryStats memoryStats = measureMemoryUsage(() -> {
            // Setup
            map.clear();
            for (int key : keys) {
                map.put(key, "value" + key);
            }

            // Delete everything to measure memory footprint difference
            for (int key : keys) {
                map.remove(key);
            }
        });

        long timeResult = benchmark(() -> {
            // Setup
            map.clear();
            for (int key : keys) {
                map.put(key, "value" + key);
            }

            // Actual deletion (timed part)
            long start = System.nanoTime();
            for (int key : keys) {
                map.remove(key);
            }
            return System.nanoTime() - start;
        });

        System.out.printf("    Memory reduction: %s\n", memoryStats);
        return timeResult;
    }

    private long benchmarkFullIteration(Map<Integer, String> map, int size) {
        // Setup
        map.clear();
        Integer[] keys = generateRandomKeys(size);
        for (int key : keys) {
            map.put(key, "value" + key);
        }

        // Memory usage for iteration is minimal but measured for completeness
        MemoryStats memoryStats = measureMemoryUsage(() -> {
            @SuppressWarnings("unused")
            int counter = 0;
            if (map instanceof RedBlackTree || map instanceof BPlusTree) {
                @SuppressWarnings("unchecked")
                Iterable<String> iterable = (Iterable<String>) ((Map<?, String>) map);
                for (@SuppressWarnings("unused")
                String value : iterable) {
                    counter++;
                }
            } else {
                for (@SuppressWarnings("unused")
                String value : map.values()) {
                    counter++;
                }
            }
        });

        long timeResult = benchmark(() -> {
            int count = 0;
            if (map instanceof RedBlackTree || map instanceof BPlusTree) {
                // Use the Iterable interface for custom implementations
                @SuppressWarnings("unchecked")
                Iterable<String> iterable = (Iterable<String>) ((Map<?, String>) map);
                for (@SuppressWarnings("unused")
                String value : iterable) {
                    count++;
                }
            } else {
                // Use values() for TreeMap
                for (@SuppressWarnings("unused")
                String value : map.values()) {
                    count++;
                }
            }
            // Prevent optimization
            if (count != size) throw new RuntimeException("Iteration count mismatch");
        });

        System.out.printf("    Iteration memory overhead: %s\n", memoryStats);
        return timeResult;
    }

    // is stuk?? Idk heb iets gesloopt maar weet niet wat. TODO: oplossen

    // private long benchmarkPartialIteration(Map<Integer, String> map, int size) {
    //     // Setup
    //     map.clear();
    //     Integer[] keys = generateRandomKeys(size);
    //     Arrays.sort(keys);
    //     for (int key : keys) {
    //         map.put(key, "value" + key);
    //     }

    //     // Define range for partial iteration (middle 25%)
    //     int fromIndex = size / 4;
    //     int toIndex = (3 * size) / 4;
    //     Integer fromKey = keys[fromIndex];
    //     Integer toKey = keys[toIndex];

    //     // Memory usage for partial iteration is measured for completeness
    //     MemoryStats memoryStats = measureMemoryUsage(() -> {
    //         @SuppressWarnings("unused")
    //         int counter = 0;
    //         if (map instanceof RedBlackTree) {
    //             RedBlackTree<Integer, String> rbt = (RedBlackTree<Integer, String>) map;
    //             Iterator<String> iter = rbt.iterator(fromKey, toKey);
    //             while (iter.hasNext()) {
    //                 iter.next();
    //                 counter++;
    //             }
    //         } else if (map instanceof BPlusTree) {
    //             BPlusTree<Integer, String> bpt = (BPlusTree<Integer, String>) map;
    //             Iterator<String> iter = bpt.iterator(fromKey, toKey);
    //             while (iter.hasNext()) {
    //                 iter.next();
    //                 counter++;
    //             }
    //         } else {
    //             NavigableMap<Integer, String> subMap =
    //                 ((TreeMap<Integer, String>) map).subMap(fromKey, true, toKey, false);
    //             for (@SuppressWarnings("unused")
    //             String ignored : subMap.values()) {
    //                 counter++;
    //             }
    //         }
    //     });

    //     long timeResult = benchmark(() -> {
    //         int count = 0;
    //         if (map instanceof RedBlackTree) {
    //             RedBlackTree<Integer, String> rbt = (RedBlackTree<Integer, String>) map;
    //             Iterator<String> iter = rbt.iterator(fromKey, toKey);
    //             while (iter.hasNext()) {
    //                 iter.next();
    //                 count++;
    //             }
    //         } else if (map instanceof BPlusTree) {
    //             BPlusTree<Integer, String> bpt = (BPlusTree<Integer, String>) map;
    //             Iterator<String> iter = bpt.iterator(fromKey, toKey);
    //             while (iter.hasNext()) {
    //                 iter.next();
    //                 count++;
    //             }
    //         } else {
    //             // TreeMap - use subMap
    //             NavigableMap<Integer, String> subMap =
    //                 ((TreeMap<Integer, String>) map).subMap(fromKey, true, toKey, false);
    //             for (@SuppressWarnings("unused")
    //             String ignored : subMap.values()) {
    //                 count++;
    //             }
    //         }
    //         // Prevent optimization
    //         if (count == 0) throw new RuntimeException("No values iterated");
    //     });

    //     System.out.printf("    Partial iteration memory overhead: %s\n", memoryStats);
    //     return timeResult;
    // }

    private Integer[] generateRandomKeys(int size) {
        Set<Integer> keySet = new HashSet<>();
        ThreadLocalRandom random = ThreadLocalRandom.current();

        while (keySet.size() < size) {
            keySet.add(random.nextInt(size * 10));
        }

        return keySet.toArray(new Integer[0]);
    }

    private long benchmark(Runnable operation) {
        return benchmark(() -> {
            operation.run();
            return 0L;
        });
    }

    private static class MemoryStats {

        long usedMemoryBefore;
        long usedMemoryAfter;

        public long getMemoryUsed() {
            return usedMemoryAfter - usedMemoryBefore;
        }

        @Override
        public String toString() {
            return String.format(
                "%d KB (%.2f MB)",
                getMemoryUsed() / 1024,
                getMemoryUsed() / (1024.0 * 1024.0)
            );
        }
    }

    private MemoryStats measureMemoryUsage(Runnable operation) {
        // Force garbage collection before measurement
        System.gc();
        System.gc();

        MemoryStats stats = new MemoryStats();
        MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        stats.usedMemoryBefore = heapUsage.getUsed();

        operation.run();

        // Force garbage collection after measurement to exclude temporary objects
        System.gc();
        System.gc();

        heapUsage = memoryMXBean.getHeapMemoryUsage();
        stats.usedMemoryAfter = heapUsage.getUsed();

        return stats;
    }

    private long benchmark(BenchmarkOperation operation) {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            try {
                operation.run();
            } catch (Exception e) {
                // Ignore warmup exceptions
            }
        }

        // Force garbage collection
        System.gc();

        // Actual benchmark
        long totalTime = 0;
        int successfulRuns = 0;

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            try {
                long time = operation.run();
                if (time > 0) {
                    totalTime += time;
                } else {
                    long start = System.nanoTime();
                    operation.run();
                    totalTime += System.nanoTime() - start;
                }
                successfulRuns++;
            } catch (Exception e) {
                System.err.println("Benchmark iteration failed: " + e.getMessage());
            }
        }

        return successfulRuns > 0 ? (totalTime / successfulRuns) / 1_000 : -1; // Convert to microseconds
    }

    @FunctionalInterface
    private interface BenchmarkOperation {
        long run() throws Exception;
    }
}
