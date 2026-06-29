package com.suhanee.ratelimiter.core;

public class NaiveTokenBucket {

    private final long capacity;
    private final long refillTokensPerInterval;
    private final long refillIntervalMillis;

    private double availableTokens;
    private long lastRefillTimestamp;

    public NaiveTokenBucket(long capacity,long refillTokensPerInterval,long refillIntervalMillis){
        this.capacity=capacity;
        this.refillIntervalMillis=refillIntervalMillis;
        this.refillTokensPerInterval=refillTokensPerInterval;
        this.availableTokens=capacity;
        this.lastRefillTimestamp=System.currentTimeMillis();

    }

    private void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTimestamp;
        if (elapsed <= 0) return;

        double tokensToAdd = (elapsed / (double) refillIntervalMillis) * refillTokensPerInterval;
        if (tokensToAdd > 0) {
            availableTokens = Math.min(capacity, availableTokens + tokensToAdd);
            lastRefillTimestamp = now;
        }
    }

    public boolean tryConsume() {
        refill();
        if (availableTokens >= 1) {
            availableTokens -= 1;
            return true;
        }
        return false;
    }

    public double getAvailableTokens() {
        return availableTokens;
    }
}
