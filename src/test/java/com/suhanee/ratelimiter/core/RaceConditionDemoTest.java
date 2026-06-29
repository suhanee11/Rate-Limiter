package com.suhanee.ratelimiter.core;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the race condition in NaiveTokenBucket.
 *
 * Setup: a bucket with capacity 10 (no refill during the test window).
 * We fire 50 threads at it simultaneously, all calling tryConsume().
 *
 * Expected (correct) behaviour: exactly 10 should succeed.
 * Actual (buggy) behaviour: MORE than 10 often succeed, because of the
 * race condition described in NaiveTokenBucket's javadoc.
 *
 * This test is EXPECTED to sometimes fail the "<=10" assertion -- that's
 * the point. It's evidence the bug is real, not theoretical.
 */
class RaceConditionDemoTest {

    @RepeatedTest(5)
    void naiveBucket_allowsMoreThanCapacity_underConcurrentLoad() throws InterruptedException {
        int capacity = 10;
        int numberOfThreads = 50;

        NaiveTokenBucket bucket = new NaiveTokenBucket(capacity, 1, 60_000);

        AtomicInteger allowedCount = new AtomicInteger(0);
        CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

        ExecutorService pool = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            pool.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    if (bucket.tryConsume()) {
                        allowedCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await();
        pool.shutdown();

        System.out.println("Capacity was " + capacity + ". Requests allowed through: " + allowedCount.get());

        assertThat(allowedCount.get()).isLessThanOrEqualTo(capacity);
    }

    @Test
    void singleThreaded_naiveBucket_worksFineAlone() {
        NaiveTokenBucket bucket = new NaiveTokenBucket(5, 1, 60_000);
        int allowed = 0;
        for (int i = 0; i < 10; i++) {
            if (bucket.tryConsume()) allowed++;
        }
        assertThat(allowed).isEqualTo(5);
    }
}