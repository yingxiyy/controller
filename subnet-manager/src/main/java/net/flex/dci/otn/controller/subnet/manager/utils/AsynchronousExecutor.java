package net.flex.dci.otn.controller.subnet.manager.utils;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AsynchronousExecutor {

    private static final ListeningExecutorService executorService;

    private static final ScheduledExecutorService monitorExecutor;

    private static final ThreadPoolExecutor threadPool;


    private static final int CORE_POOL_SIZE = 20;
    private static final int MAX_POOL_SIZE = 128;
    private static final int MAX_QUEUE_SIZE = 1024;

    private static final long KEEP_ALIVE_TIME = 60L;


    static {
        threadPool = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(MAX_QUEUE_SIZE),
                new NamedThreadFactory("ne-manager-task-pool"),
                new CallerRunsPolicy()
        );
        executorService = MoreExecutors.listeningDecorator(threadPool);
        monitorExecutor = Executors.newSingleThreadScheduledExecutor(
                new CustomThreadFactory("ne-manager-monitor-pool"));
        startPoolMonitor();
    }

    public AsynchronousExecutor() {

    }

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    public static <T> ListenableFuture<T> submit(Callable<T> task) {
        return executorService.submit(task);
    }

    public static ListenableFuture<?> submit(Runnable task) {
        return executorService.submit(wrapTask(task));
    }

    public static void execute(Runnable task) {
        executorService.submit(wrapTask(task));
    }

    @Deprecated
    public static void execute(Callable<?> callable) {
        executorService.submit(callable);
    }


    private static Runnable wrapTask(Runnable task) {
        return () -> {
            String threadName = Thread.currentThread().getName();
            try {
                log.debug("Starting async task in thread :{}", threadName);
                task.run();
                log.debug("Completely async task in thread:{}", threadName);
            } catch (Exception e) {
                log.error("Async task failed in thread: {}", threadName, e);
            }
        };
    }

    private static void startPoolMonitor() {
        monitorExecutor.scheduleAtFixedRate(AsynchronousExecutor::printPoolStatus, 30, 30,
                TimeUnit.SECONDS); // 每30秒监控一次
    }

    public static void printPoolStatus() {
        ThreadPoolExecutor executor = threadPool;
        log.info("ThreadPool Status - Active: {}, Pool: {}, Core: {}, Max: {}, Queue: {}/{}",
                executor.getActiveCount(),
                executor.getPoolSize(),
                executor.getCorePoolSize(),
                executor.getMaximumPoolSize(),
                executor.getQueue().size(),
                MAX_QUEUE_SIZE);
    }

    public static void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("Forcing shutdown of AsynchronousExecutor...");
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("AsynchronousExecutor did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
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
