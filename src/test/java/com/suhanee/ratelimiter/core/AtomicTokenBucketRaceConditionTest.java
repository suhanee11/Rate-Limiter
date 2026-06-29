package com.suhanee.ratelimiter.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The companion to RaceConditionDemoTest.
 *
 * Same attack: 50 threads fire tryConsume() at the exact same instant.
 * Same capacity: 10.
 *
 * Difference: this hits AtomicTokenBucket, which routes the
 * read-check-write through the Lua script in Redis. Because Redis executes
 * the whole script as one atomic unit, no two requests can interleave --
 * so this test should ALWAYS show exactly 10 allowed, never more.
 */
@SpringBootTest
class AtomicTokenBucketRaceConditionTest {

    @Autowired
    private AtomicTokenBucket atomicTokenBucket;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final String testKey = "ratelimit:bucket:test-client:/demo";

    @BeforeEach
    void cleanUp() {
        redisTemplate.delete(testKey);
    }

    @AfterEach
    void tearDown() {
        redisTemplate.delete(testKey);
    }

    @RepeatedTest(5)
    void atomicBucket_neverAllowsMoreThanCapacity_underConcurrentLoad() throws InterruptedException {
        int capacity = 10;
        int numberOfThreads = 50;

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
                    AtomicTokenBucket.TokenBucketResult result =
                            atomicTokenBucket.tryConsume(testKey, capacity, 0);
                    if (result.allowed()) {
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

        System.out.println("[Atomic bucket] Capacity was " + capacity + ". Requests allowed: " + allowedCount.get());

        assertThat(allowedCount.get()).isEqualTo(capacity);
    }
}
