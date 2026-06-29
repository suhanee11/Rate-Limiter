package com.suhanee.ratelimiter.core;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * The fixed, distributed-safe token bucket.
 *
 * Unlike NaiveTokenBucket, this class holds NO mutable state itself -- the
 * token count lives in Redis, shared by every instance of this app that's
 * running. The actual race-condition fix lives in token_bucket.lua -- this
 * class is just a thin Java wrapper that calls it.
 */
@Component
public class AtomicTokenBucket {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> tokenBucketScript;

    public AtomicTokenBucket(StringRedisTemplate redisTemplate,
                             DefaultRedisScript<List> tokenBucketScript) {
        this.redisTemplate = redisTemplate;
        this.tokenBucketScript = tokenBucketScript;
    }

    public TokenBucketResult tryConsume(String bucketKey, long capacity, double refillPerSec) {
        long nowMillis = System.currentTimeMillis();

        @SuppressWarnings("unchecked")
        List<Object> result = redisTemplate.execute(
                tokenBucketScript,
                Collections.singletonList(bucketKey),
                String.valueOf(capacity),
                String.valueOf(refillPerSec),
                String.valueOf(nowMillis),
                "1"
        );

        double tokensRemaining = Double.parseDouble((String) result.get(0));
        boolean allowed = "1".equals(String.valueOf(result.get(1)));

        return new TokenBucketResult(allowed, tokensRemaining);
    }

    public record TokenBucketResult(boolean allowed, double tokensRemaining) {
    }
}
