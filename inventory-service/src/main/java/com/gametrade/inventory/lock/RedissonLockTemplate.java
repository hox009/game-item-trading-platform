package com.gametrade.inventory.lock;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson-backed {@link LockTemplate}. Acquires an interruptible lease-based
 * lock so a crashed holder cannot deadlock the SKU forever.
 */
@Component
public class RedissonLockTemplate implements LockTemplate {

    private static final long WAIT_MILLIS = 3_000L;
    private static final long LEASE_MILLIS = 5_000L;

    private final RedissonClient redissonClient;

    public RedissonLockTemplate(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> T withLock(String key, Supplier<T> action) {
        RLock lock = redissonClient.getLock(key);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(WAIT_MILLIS, LEASE_MILLIS, TimeUnit.MILLISECONDS);
            if (!acquired) {
                throw new IllegalStateException("failed to acquire lock: " + key);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while acquiring lock: " + key, e);
        } finally {
            if (acquired && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
