package com.example.chat;

import java.util.concurrent.ConcurrentHashMap;

public class RateLimiter {
    private int requestsLimit = 10;
    private long refillIntervalMs = 1000;
    private ConcurrentHashMap<String, Bucket> bucketPerUser;

    private RateLimiter(int requestsLimit, long refillIntervalMs) {
        this.bucketPerUser = new ConcurrentHashMap<String, Bucket>();
        this.requestsLimit = requestsLimit;
        this.refillIntervalMs = refillIntervalMs;
    }

    public static RateLimiter create(int requestsLimit) {
        return new RateLimiter(requestsLimit, 1000);
    }

    public static RateLimiter create(int requestsLimit, long refillIntervalMs) {
        return new RateLimiter(requestsLimit, refillIntervalMs);
    }

    public boolean receive(String requestId) {
        Bucket userBucket;

        userBucket = this.bucketPerUser.get(requestId);

        if (userBucket == null) {
            userBucket = new Bucket(requestsLimit, requestsLimit, requestId, refillIntervalMs);
            this.bucketPerUser.putIfAbsent(requestId, userBucket);
        }

        boolean hasTokens = userBucket.check();
        if (hasTokens) {
            userBucket.spend();
            return true;
        }

        this.bucketPerUser.putIfAbsent(requestId, userBucket);
        return false;
    }
}

final class Bucket {
    private int capacity = 10;
    private int tokens;
    private String id;
    private long refillIntervalMs;
    private long lastRefill = System.currentTimeMillis();

    protected Bucket(int capacity, int tokens, String id, long refillIntervalMs) {
        this.capacity = capacity;
        this.tokens = tokens;
        this.id = id;
        this.refillIntervalMs = refillIntervalMs;
    }

    protected void spend() {
        if (this.tokens > 0) {
            this.tokens -= 1;
        }
    }

    private void refill() {
        long now = System.currentTimeMillis();
        if (now - this.lastRefill < this.refillIntervalMs) {
            return;
        }
        this.tokens += this.capacity;
        this.lastRefill = now;
    }

    protected boolean check() {
        if (this.tokens > 0) {
            return true;
        }

        if (this.tokens == 0) {
            this.refill();
        }

        return false;
    }

    protected String getId() {
        return this.id;
    }
}
