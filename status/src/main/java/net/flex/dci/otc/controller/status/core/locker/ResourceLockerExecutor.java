package net.flex.dci.otc.controller.status.core.locker;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.cache.redis.utils.RedisLockUtil;
import org.redisson.api.RLock;

@Slf4j
public class ResourceLockerExecutor {

    private static final int DEFAULT_RETRY_COUNT = 5;
    private static final int DEFAULT_RETRY_BASE_INTERVAL_MS = 50;
    private static final int DEFAULT_LOCK_WAIT_TIME_SECONDS = 3;
    private static final int DEFAULT_LOCK_LEASE_TIME_SECONDS = 30;
    private static final Random RANDOM = new Random();

    public static void executeWithLock(String lockKey, Runnable action) {
        executeWithLock(lockKey, action, DEFAULT_RETRY_COUNT, DEFAULT_RETRY_BASE_INTERVAL_MS);
    }

    public static void executeWithLock(String lockKey, Runnable action, int retryCount,
            long baseIntervalMs) {
        int attempts = 0;
        while (attempts <= retryCount) {
            RLock lock = RedisLockUtil.getLock(lockKey);
            try {
                if (RedisLockUtil.tryLock(lockKey, DEFAULT_LOCK_WAIT_TIME_SECONDS,
                        DEFAULT_LOCK_LEASE_TIME_SECONDS)) {
                    try {
                        action.run();
                        return;
                    } finally {
                        if (lock.isHeldByCurrentThread()) {
                            RedisLockUtil.unlock(lock);
                        }
                    }
                } else {
                    attempts++;
                    if (attempts <= retryCount) {
                        long waitTime = calculateWaitTime(baseIntervalMs, attempts);
                        log.warn(
                                "Failed to acquire lock:{} for the {} time, retrying after {}ms...",
                                lockKey, attempts, waitTime);
                        sleep(waitTime);
                    } else {
                        log.error("Failed to acquire lock:{} after {} retries, giving up", lockKey,
                                retryCount);
                        throw new RuntimeException("Failed to acquire lock after retries: "
                                + lockKey);
                    }
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                log.error("Error executing with lock: {}", lockKey, e);
                throw new RuntimeException("Error executing with lock: " + lockKey, e);
            }
        }
    }

    public static void executeWithMultiLock(String prefix, List<String> ids, Runnable action) {
        executeWithMultiLock(prefix, ids, action, DEFAULT_RETRY_COUNT,
                DEFAULT_RETRY_BASE_INTERVAL_MS);
    }

    public static void executeWithMultiLock(String prefix, List<String> ids, Runnable action,
            int retryCount, long baseIntervalMs) {
        if (ids == null || ids.isEmpty()) {
            action.run();
            return;
        }
        List<String> lockKeys = ids.stream()
                .map(id -> prefix + id)
                .sorted()
                .collect(Collectors.toList());

        int attempts = 0;
        while (attempts <= retryCount) {
            List<RLock> locks = new ArrayList<>();
            try {
                for (String key : lockKeys) {
                    if (RedisLockUtil.tryLock(key, DEFAULT_LOCK_WAIT_TIME_SECONDS,
                            DEFAULT_LOCK_LEASE_TIME_SECONDS)) {
                        locks.add(RedisLockUtil.getLock(key));
                    } else {
                        log.warn("Failed to acquire lock:{} for the {} time", key, attempts + 1);
                        break;
                    }
                }

                if (locks.size() == lockKeys.size()) {
                    action.run();
                    return;
                } else {
                    attempts++;
                    if (attempts <= retryCount) {
                        long waitTime = calculateWaitTime(baseIntervalMs, attempts);
                        log.warn(
                                "Failed to acquire all locks for the {} time, retrying after {}ms...",
                                attempts, waitTime);
                        sleep(waitTime);
                    } else {
                        log.error(
                                "Failed to acquire all locks after {} retries, acquired {}/{}, giving up",
                                retryCount, locks.size(), lockKeys.size());
                        throw new RuntimeException(String.format(
                                "Failed to acquire all locks after retries, acquired %d/%d",
                                locks.size(), lockKeys.size()));
                    }
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception ex) {
                log.error("Error executing with multi lock", ex);
                throw new RuntimeException("Error executing with multi lock", ex);
            } finally {
                for (int i = locks.size() - 1; i >= 0; i--) {
                    RLock lock = locks.get(i);
                    if (lock.isHeldByCurrentThread()) {
                        RedisLockUtil.unlock(lock);
                    }
                }
            }
        }
    }

    private static long calculateWaitTime(long baseIntervalMs, int attempts) {
        long exponentialDelay = baseIntervalMs * (long) Math.pow(2, attempts - 1);
        long jitter = RANDOM.nextInt((int) (baseIntervalMs / 2 + 1));
        return Math.min(exponentialDelay + jitter, 1000);
    }

    private static void sleep(long milliseconds) {
        try {
            TimeUnit.MILLISECONDS.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Lock retry interrupted");
        }
    }

}
