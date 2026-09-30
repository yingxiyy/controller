package net.flex.dci.otc.controller.otdr.utils;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @version 1.0
 * @date 2022/8/30 14:28
 */
@Slf4j
public class AsynchronousExecutor {

    private static final ListeningExecutorService executorService;
    private static final ScheduledExecutorService monitorExecutor;
    private static final ThreadPoolExecutor threadPool;

    private static final int CORE_POOL_SIZE = 10;
    private static final int MAX_POOL_SIZE = 50;
    private static final long KEEP_ALIVE_TIME = 60L;

    private static final AtomicLong submitTasks = new AtomicLong(0);

    private static final AtomicLong completedTasks = new AtomicLong(0);
    private static final AtomicLong rejectedTasks = new AtomicLong(0);

    static {
        threadPool = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new CustomThreadFactory("otdr-task-pool"),
                new CallerRunsPolicy()
        );
        executorService = MoreExecutors.listeningDecorator(threadPool);
        monitorExecutor = Executors.newSingleThreadScheduledExecutor(
                new CustomThreadFactory("otdr-monitor-pool"));
        startPoolMonitor();
    }

    public AsynchronousExecutor() {

    }

    private static void startPoolMonitor() {
        monitorExecutor.scheduleAtFixedRate(() -> {
            ThreadPoolExecutor pool = threadPool;
            int activeCount = pool.getActiveCount();
            long completedTaskCount = pool.getCompletedTaskCount();
            long taskCount = pool.getTaskCount();
            int queueSize = pool.getQueue().size();
            int poolSize = pool.getPoolSize();
            int maximumPoolSize = pool.getMaximumPoolSize();
            log.info(
                    "OTDR ThreadPool Stats - Active: {}/{}, Queue: {}, Completed: {}/{}, " +
                            "PoolSize: {}/{}, Rejected: {}",
                    activeCount, maximumPoolSize, queueSize, completedTaskCount, taskCount,
                    poolSize, maximumPoolSize, rejectedTasks.get()
            );

        }, 30, 30, TimeUnit.SECONDS); // 每30秒监控一次
    }

    public static <T> ListenableFuture<T> execute(Callable<T> callable) {
        submitTasks.incrementAndGet();
        try {
            ListenableFuture<T> listenableFuture = executorService.submit(callable);
            Futures.addCallback(listenableFuture, new FutureCallback<T>() {
                @Override
                public void onSuccess(@Nullable T t) {
                    completedTasks.incrementAndGet();
                }

                @Override
                public void onFailure(Throwable throwable) {
                    completedTasks.incrementAndGet();
                    log.error("OTDR task execution failed", throwable);
                }
            }, MoreExecutors.directExecutor());
            return listenableFuture;
        } catch (Exception ex) {
            rejectedTasks.incrementAndGet();
            log.error("Failed to submit OTDR callable task to thread poll", ex);
            return executeInCurrentThread(callable);
        }
    }


    public static ListenableFuture<?> execute(Runnable runnable) {
        submitTasks.incrementAndGet();
        try {
            ListenableFuture<?> listenableFuture = executorService.submit(runnable);
            Futures.addCallback(listenableFuture, new FutureCallback<Object>() {
                @Override
                public void onSuccess(@Nullable Object o) {
                    completedTasks.incrementAndGet();
                }

                @Override
                public void onFailure(Throwable t) {
                    completedTasks.incrementAndGet();
                    log.error("Task runnable task execution failed", t);
                }
            }, MoreExecutors.directExecutor());
            return listenableFuture;
        } catch (Exception ex) {
            rejectedTasks.incrementAndGet();
            log.error("Failed to submit OTDR runnable task to thread pool", ex);
            return executeInCurrentThread(() -> {
                runnable.run();
                return null;
            });
        }
    }

    private static <T> ListenableFuture<T> executeInCurrentThread(Callable<T> callable) {
        SettableFuture<T> future = SettableFuture.create();
        try {
            T result = callable.call();
            future.set(result);
        } catch (Exception e) {
            future.setException(e);
        }
        return future;
    }

    private static class CustomThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        private final String namePrefix;

        private final ThreadGroup group;

        public CustomThreadFactory(String poolName) {
            SecurityManager securityManager = System.getSecurityManager();
            this.namePrefix = poolName + "-thread-";
            this.group = (securityManager != null) ? securityManager.getThreadGroup()
                    : Thread.currentThread()
                            .getThreadGroup();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
}
