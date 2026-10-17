package com.miniRedis.store;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConcurrencyTest {

    @Test
    void testConcurrentIncrements() throws InterruptedException {
        Store store = new Store();
        int threadCount = 50;
        int incrementsPerThread = 100;
        int expectedTotal = threadCount * incrementsPerThread;

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        for (int j = 0; j < incrementsPerThread; j++) {
                            store.incr("shared_counter");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Unleash all threads simultaneously
            startLatch.countDown();

            boolean finished = doneLatch.await(5, TimeUnit.SECONDS);
            assertTrue(finished, "All increment threads should finish within timeout");
        }

        assertEquals(String.valueOf(expectedTotal), store.get("shared_counter"));
    }
}
