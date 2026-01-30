package com.casperswebsites.trees;

import com.casperswebsites.trees.bplustree.BPlusTree;
import com.casperswebsites.trees.redblack.RedBlackTree;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.*;

/**
 * Outputs performance results to a CSV file
 */
public class PerformanceTestStructured {

    private static final int[] DATA_SIZES = { 100, 1_000, 10_000, 100_000, 1_000_000 };
    private static final int[] BPLUS_ORDERS = { 8, 16, 32, 128, 256 };
    private static final int WARMUP_ITERATIONS = 5;
    private static final int TEST_ITERATIONS = 10;
    private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    private static final String OUTPUT_FILE = "performance_results.csv";

    private List<PerformanceResult> results = new ArrayList<>();
    private static final Random random = new Random(21);

    public static void main(String[] args) {
        System.out.println("=== Structured Performance Test ===");
        System.out.println("Running tests and generating CSV output...\n");

        PerformanceTestStructured test = new PerformanceTestStructured();
        test.runAllTests();
        test.outputToCsv();

        System.out.println("=== Test Complete ===");
        System.out.println("Results saved to: " + OUTPUT_FILE);
    }

    private void runAllTests() {
        String[] operations = {
            "Random_Lookup",
            "Sequential_Insert",
            "Random_Insert",
            "Sequential_Delete",
            "Random_Delete",
            "Full_Iteration",
            "Partial_Iteration",
        };

        for (String operation : operations) {
            System.out.println("Testing operation: " + operation);

            for (int size : DATA_SIZES) {
                // Test HashMap
                testSingleOperation("HashMap", new HashMap<>(), size, operation);

                // Test TreeMap
                testSingleOperation("TreeMap", new TreeMap<>(), size, operation);

                // Test RedBlackTree
                testSingleOperation("RedBlackTree", new RedBlackTree<>(), size, operation);

                // Test B+ Trees with different orders
                for (int order : BPLUS_ORDERS) {
                    testSingleOperation(
                        "BPlusTree_" + order,
                        new BPlusTree<>(order),
                        size,
                        operation
                    );
                }
            }

            System.out.println("  Completed operation: " + operation);
        }
    }

    private void testSingleOperation(
        String structureName,
        Map<Integer, String> map,
        int size,
        String operation
    ) {
        PerformanceResult result = null;

        switch (operation) {
            case "Random_Lookup":
                result = benchmarkRandomLookup(structureName, map, size);
                break;
            case "Sequential_Insert":
                result = benchmarkSequentialInserts(structureName, map, size);
                break;
            case "Random_Insert":
                result = benchmarkRandomInserts(structureName, map, size);
                break;
            case "Sequential_Delete":
                result = benchmarkSequentialDeletes(structureName, map, size);
                break;
            case "Random_Delete":
                result = benchmarkRandomDeletes(structureName, map, size);
                break;
            case "Full_Iteration":
                result = benchmarkFullIteration(structureName, map, size);
                break;
            case "Partial_Iteration":
                result = benchmarkPartialIteration(structureName, map, size);
                break;
        }

        if (result != null) {
            results.add(result);
        }
    }

       private PerformanceResult benchmarkRandomLookup(
        String structureName,
        Map<Integer, String> map,
        int size
    ) {
        // lookup 25% of the set size
        int lookups = size / 4;
        // not using the generateRandomKeys functions to control the min and max value
        Integer[] randomIndices = new Integer[lookups];
        for (int i = 0; i < lookups; i++) {
            randomIndices[i] = random.nextInt(0, size);
        }

        // Measure memory usage
        MemoryStats memoryStats = measureMemoryUsage(() -> {
            map.clear();
            for (int i = 0; i < size; i++) {
                map.put(i, "value" + i);
            }

            int sum = 0;
            for (int i = 0; i < lookups; i++) {
                sum += map.get(randomIndices[i]).length();
            }
            // use our sum to not optimise the loop away
            map.put(size, String.valueOf(sum));
        });

        // Measure execution time
        long timeResult = benchmark(() -> {
            map.clear();
            for (int i = 0; i < size; i++) {
                map.put(i, "value" + i);
            }
        });

        return new PerformanceResult(
            structureName,
            "Random_Lookup",
            size,
            timeResult,
            memoryStats.getMemoryUsed() / 1024
        );
    }

    private PerformanceResult benchmarkSequentialInserts(
        String structureName,
        Map<Integer, String> map,
        int size
    ) {
        // Measure memory usage
        MemoryStats memoryStats = measureMemoryUsage(() -> {
            map.clear();
            for (int i = 0; i < size; i++) {
                map.put(i, "value" + i);
            }
        });

        // Measure execution time
        long timeResult = benchmark(() -> {
            map.clear();
            for (int i = 0; i < size; i++) {
                map.put(i, "value" + i);
            }
        });

        return new PerformanceResult(
            structureName,
            "Sequential_Insert",
            size,
            timeResult,
            memoryStats.getMemoryUsed() / 1024
        );
    }

    private PerformanceResult benchmarkRandomInserts(
        String structureName,
        Map<Integer, String> map,
        int size
    ) {
        Integer[] keys = generateRandomKeys(size);

        // Measure memory usage
        MemoryStats memoryStats = measureMemoryUsage(() -> {
            map.clear();
            for (int key : keys) {
                map.put(key, "value" + key);
            }
        });

        // Measure execution time
        long timeResult = benchmark(() -> {
            map.clear();
            for (int key : keys) {
                map.put(key, "value" + key);
            }
        });

        return new PerformanceResult(
            structureName,
            "Random_Insert",
            size,
            timeResult,
            memoryStats.getMemoryUsed() / 1024
        );
    }

    private PerformanceResult benchmarkSequentialDeletes(
        String structureName,
        Map<Integer, String> map,
        int size
    ) {
        // Measure memory reduction
        MemoryStats memoryStats = measureMemoryUsage(() -> {
            map.clear();
            for (int i = 0; i < size; i++) {
                map.put(i, "value" + i);
            }

            for (int i = 0; i < size; i++) {
                map.remove(i);
            }
        });

        // Measure execution time
        long timeResult = benchmark(() -> {
            map.clear();
            for (int i = 0; i < size; i++) {
                map.put(i, "value" + i);
            }

            long start = System.nanoTime();
            for (int i = 0; i < size; i++) {
                map.remove(i);
            }
            return System.nanoTime() - start;
        });

        return new PerformanceResult(
            structureName,
            "Sequential_Delete",
            size,
            timeResult,
            memoryStats.getMemoryUsed() / 1024
        );
    }

    private PerformanceResult benchmarkRandomDeletes(
        String structureName,
        Map<Integer, String> map,
        int size
    ) {
        Integer[] keys = generateRandomKeys(size);

        // Measure memory reduction
        MemoryStats memoryStats = measureMemoryUsage(() -> {
            map.clear();
            for (int key : keys) {
                map.put(key, "value" + key);
            }

            for (int key : keys) {
                map.remove(key);
            }
        });

        // Measure execution time
        long timeResult = benchmark(() -> {
            map.clear();
            for (int key : keys) {
                map.put(key, "value" + key);
            }

            long start = System.nanoTime();
            for (int key : keys) {
                map.remove(key);
            }
            return System.nanoTime() - start;
        });

        return new PerformanceResult(
            structureName,
            "Random_Delete",
            size,
            timeResult,
            memoryStats.getMemoryUsed() / 1024
        );
    }

    private PerformanceResult benchmarkFullIteration(
        String structureName,
        Map<Integer, String> map,
        int size
    ) {
        // Setup
        map.clear();
        Integer[] keys = generateRandomKeys(size);
        for (int key : keys) {
            map.put(key, "value" + key);
        }

        // Measure memory overhead
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
                // For HashMap, TreeMap and other standard Map implementations
                for (@SuppressWarnings("unused")
                String value : map.values()) {
                    counter++;
                }
            }
        });

        // Measure execution time
        long timeResult = benchmark(() -> {
            int count = 0;
            if (map instanceof RedBlackTree || map instanceof BPlusTree) {
                @SuppressWarnings("unchecked")
                Iterable<String> iterable = (Iterable<String>) ((Map<?, String>) map);
                for (@SuppressWarnings("unused")
                String value : iterable) {
                    count++;
                }
            } else {
                // For HashMap, TreeMap and other standard Map implementations
                for (@SuppressWarnings("unused")
                String value : map.values()) {
                    count++;
                }
            }
            if (count != size) throw new RuntimeException("Iteration count mismatch");
        });

        return new PerformanceResult(
            structureName,
            "Full_Iteration",
            size,
            timeResult,
            memoryStats.getMemoryUsed() / 1024
        );
    }

    private PerformanceResult benchmarkPartialIteration(
        String structureName,
        Map<Integer, String> map,
        int size
    ) {
        // Setup
        map.clear();
        Integer[] keys = generateRandomKeys(size);
        Arrays.sort(keys);
        for (int key : keys) {
            map.put(key, "value" + key);
        }

        // Define range for partial iteration (middle 25%)
        int fromIndex = size / 4;
        int toIndex = (3 * size) / 4;
        Integer fromKey = keys[fromIndex];
        Integer toKey = keys[toIndex];

        // Measure memory overhead
        MemoryStats memoryStats = measureMemoryUsage(() -> {
            @SuppressWarnings("unused")
            int counter = 0;
            if (map instanceof RedBlackTree) {
                RedBlackTree<Integer, String> rbt = (RedBlackTree<Integer, String>) map;
                Iterator<String> iter = rbt.iterator(fromKey, toKey);
                while (iter.hasNext()) {
                    iter.next();
                    counter++;
                }
            } else if (map instanceof BPlusTree) {
                BPlusTree<Integer, String> bpt = (BPlusTree<Integer, String>) map;
                Iterator<String> iter = bpt.iterator(fromKey, toKey);
                while (iter.hasNext()) {
                    iter.next();
                    counter++;
                }
            } else if (map instanceof TreeMap) {
                NavigableMap<Integer, String> subMap =
                    ((TreeMap<Integer, String>) map).subMap(fromKey, true, toKey, false);
                for (@SuppressWarnings("unused")
                String ignored : subMap.values()) {
                    counter++;
                }
            } else {
                // For HashMap, partial iteration is not meaningful as it's unordered
                // We'll iterate through all values and count a subset
                int targetCount = (toIndex - fromIndex);
                for (@SuppressWarnings("unused")
                String value : map.values()) {
                    if (counter >= targetCount) break;
                    counter++;
                }
            }
        });

        // Measure execution time
        long timeResult = benchmark(() -> {
            int count = 0;
            if (map instanceof RedBlackTree) {
                RedBlackTree<Integer, String> rbt = (RedBlackTree<Integer, String>) map;
                Iterator<String> iter = rbt.iterator(fromKey, toKey);
                while (iter.hasNext()) {
                    iter.next();
                    count++;
                }
            } else if (map instanceof BPlusTree) {
                BPlusTree<Integer, String> bpt = (BPlusTree<Integer, String>) map;
                Iterator<String> iter = bpt.iterator(fromKey, toKey);
                while (iter.hasNext()) {
                    iter.next();
                    count++;
                }
            } else if (map instanceof TreeMap) {
                NavigableMap<Integer, String> subMap =
                    ((TreeMap<Integer, String>) map).subMap(fromKey, true, toKey, false);
                for (@SuppressWarnings("unused")
                String ignored : subMap.values()) {
                    count++;
                }
            } else {
                // For HashMap, partial iteration is not meaningful as it's unordered
                // We'll iterate through all values and count a subset
                int targetCount = (toIndex - fromIndex);
                for (@SuppressWarnings("unused")
                String value : map.values()) {
                    if (count >= targetCount) break;
                    count++;
                }
            }
            if (count == 0) throw new RuntimeException("No values iterated");
        });

        return new PerformanceResult(
            structureName,
            "Partial_Iteration",
            size,
            timeResult,
            memoryStats.getMemoryUsed() / 1024
        );
    }

    private Integer[] generateRandomKeys(int size) {
        Set<Integer> keySet = new HashSet<>();

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

    private static class MemoryStats {

        long usedMemoryBefore;
        long usedMemoryAfter;

        public long getMemoryUsed() {
            return usedMemoryAfter - usedMemoryBefore;
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

    @FunctionalInterface
    private interface BenchmarkOperation {
        long run() throws Exception;
    }

    private static class PerformanceResult {

        String dataStructure;
        String operation;
        int dataSize;
        long timeMicroseconds;
        long memoryKB;

        public PerformanceResult(
            String dataStructure,
            String operation,
            int dataSize,
            long timeMicroseconds,
            long memoryKB
        ) {
            this.dataStructure = dataStructure;
            this.operation = operation;
            this.dataSize = dataSize;
            this.timeMicroseconds = timeMicroseconds;
            this.memoryKB = memoryKB;
        }

        public String toCsvRow() {
            return String.format(
                "%s,%s,%d,%d,%d",
                dataStructure,
                operation,
                dataSize,
                timeMicroseconds,
                memoryKB
            );
        }
    }

    private void outputToCsv() {
        try (FileWriter writer = new FileWriter(OUTPUT_FILE)) {
            // Write header
            writer.write("DataStructure,Operation,DataSize,Time_Microseconds,Memory_KB\n");

            // Write data rows
            for (PerformanceResult result : results) {
                writer.write(result.toCsvRow() + "\n");
            }

            writer.flush();
        } catch (IOException e) {
            System.err.println("Error writing CSV file: " + e.getMessage());
        }
    }
}
