package com.gametrade.inventory.lock;

import java.util.function.Supplier;

/**
 * Abstraction over a distributed lock so the service layer stays testable
 * without a real Redis/Redisson instance.
 */
public interface LockTemplate {

    /**
     * Runs {@code action} while holding the named lock.
     *
     * @throws IllegalStateException if the lock cannot be acquired in time
     */
    <T> T withLock(String key, Supplier<T> action);
}
