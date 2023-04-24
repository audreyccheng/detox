package shield.benchmarks.utils;

import java.util.concurrent.atomic.AtomicInteger;

public class CacheStats {
    public static AtomicInteger totalLayers = new AtomicInteger(0);
    public static AtomicInteger spedUpLayers = new AtomicInteger(0);
    public static AtomicInteger readSpedUpLayers = new AtomicInteger(0);
    public static AtomicInteger writeOnlyLayers = new AtomicInteger(0);
    public static AtomicInteger totalRequests = new AtomicInteger(0);
    public static AtomicInteger cachedRequests = new AtomicInteger(0);

    public static AtomicInteger totalPrefetches = new AtomicInteger(0);
    public static AtomicInteger redundantPrefetches = new AtomicInteger(0);
    public static AtomicInteger prefetchesUsed = new AtomicInteger(0);
    public static AtomicInteger redundantPrefetchesUsed = new AtomicInteger(0);

    public static void ranLayer(int cachedRequests, int totalRequests, int totalReadRequests, int totalPrefetches, int redundantPrefetches, int prefetchesUsed, int redundantPrefetchesUsed) {
        totalLayers.incrementAndGet();
        if (cachedRequests == totalRequests) spedUpLayers.incrementAndGet();
        if (cachedRequests == totalReadRequests) readSpedUpLayers.incrementAndGet();
        if (totalReadRequests == 0) writeOnlyLayers.incrementAndGet();

        CacheStats.totalRequests.addAndGet(totalRequests);
        CacheStats.cachedRequests.addAndGet(cachedRequests);

        CacheStats.totalPrefetches.addAndGet(totalPrefetches);
        CacheStats.redundantPrefetches.addAndGet(redundantPrefetches);

        CacheStats.prefetchesUsed.addAndGet(prefetchesUsed);
        CacheStats.redundantPrefetchesUsed.addAndGet(redundantPrefetchesUsed);
    }

    public static void printReport() {
        int s = spedUpLayers.get();
        int t = totalLayers.get();
        System.out.printf("%d layers sped up out of %d layers total; %.3f\n", s, t, ((float) s) / t);

        int sr = readSpedUpLayers.get();
        System.out.printf("%d layers had all reads sped up out of %d layers total; %.3f. %d of these layers were write-only.\n", sr, t, ((float) sr) / t, writeOnlyLayers.get());

        int c = cachedRequests.get();
        int r = totalRequests.get();
        System.out.printf("%d requests cached out of %d requests total; %.3f\n", c, r, ((float) c) / r);

        System.out.printf("Of %d prefetches made total, %d (%.3f) were used, %d (%.3f) were redundant, %d (%.3f) were used AND redundant",
                totalPrefetches.get(),
                prefetchesUsed.get(), ((float) prefetchesUsed.get()) / totalPrefetches.get(),
                redundantPrefetches.get(), ((float) redundantPrefetches.get()) / totalPrefetches.get(),
                redundantPrefetchesUsed.get(), ((float) redundantPrefetchesUsed.get()) / totalPrefetches.get());
    }
}
