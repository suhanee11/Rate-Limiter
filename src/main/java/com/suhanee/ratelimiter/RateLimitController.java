package com.suhanee.ratelimiter;

import com.suhanee.ratelimiter.core.AtomicTokenBucket;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@CrossOrigin(origins = "*") // fine for a demo project; would restrict this in real production
public class RateLimitController {

    private final AtomicTokenBucket atomicTokenBucket;

    private static final long CAPACITY = 10;
    private static final double REFILL_PER_SEC = 2.0; // 2 tokens added back per second

    public RateLimitController(AtomicTokenBucket atomicTokenBucket) {
        this.atomicTokenBucket = atomicTokenBucket;
    }

    @GetMapping("/api/ping")
    public RateLimitResponse ping(@RequestParam(defaultValue = "demo-user") String client) {
        String bucketKey = "ratelimit:bucket:" + client + ":/api/ping";
        AtomicTokenBucket.TokenBucketResult result =
                atomicTokenBucket.tryConsume(bucketKey, CAPACITY, REFILL_PER_SEC);

        return new RateLimitResponse(result.allowed(), result.tokensRemaining(), CAPACITY);
    }

    public record RateLimitResponse(boolean allowed, double tokensRemaining, long capacity) {
    }
}