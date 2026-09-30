package net.flex.dci.otc.controller.ne.manager.utils;

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

    private static final ExecutorService REGISTER_EXECUTOR;
    private static final ExecutorService SYNC_EXECUTOR;
    private static final ExecutorService TELEMETRY_EXECUTOR;
    private static final ExecutorService CLEANUP_EXECUTOR;
    private static final ExecutorService NTP_EXECUTOR;

    // 专用线程池的 Listening 装饰器
    private static final ListeningExecutorService registerListeningExecutor;
    private static final ListeningExecutorService syncListeningExecutor;
    private static final ListeningExecutorService telemetryListeningExecutor;
    private static final ListeningExecutorService cleanupListeningExecutor;
    private static final ListeningExecutorService ntpListeningExecutor;


    private static final int CORE_POOL_SIZE = 40;
    private static final int MAX_POOL_SIZE = 128;
    private static final int MAX_QUEUE_SIZE = 200;

    private static final int REGISTER_QUEUE_SIZE = 100;
    private static final int SYNC_QUEUE_SIZE = 100;
    private static final int TELEMETRY_QUEUE_SIZE = 100;
    private static final int CLEANUP_QUEUE_SIZE = 100;
    private static final int NTP_QUEUE_SIZE = 100;

    private static final long KEEP_ALIVE_TIME = 60L;


    private static ThreadPoolExecutor registerExecutor;
    private static ThreadPoolExecutor syncExecutor;
    private static ThreadPoolExecutor telemetryExecutor;
    private static ThreadPoolExecutor cleanupExecutor;
    private static ThreadPoolExecutor ntpExecutor;


    static {
        // 通用线程池
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

        registerExecutor = new ThreadPoolExecutor(
                20, 40, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(REGISTER_QUEUE_SIZE),
                new NamedThreadFactory("ne-register-pool"),
                new CallerRunsPolicy()
        );
        REGISTER_EXECUTOR = registerExecutor;
        registerListeningExecutor = MoreExecutors.listeningDecorator(registerExecutor);

        syncExecutor = new ThreadPoolExecutor(
                30, 60, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(SYNC_QUEUE_SIZE),
                new NamedThreadFactory("ne-sync-pool"),
                new CallerRunsPolicy()
        );
        SYNC_EXECUTOR = syncExecutor;
        syncListeningExecutor = MoreExecutors.listeningDecorator(syncExecutor);

        telemetryExecutor = new ThreadPoolExecutor(
                15, 30, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(TELEMETRY_QUEUE_SIZE),
                new NamedThreadFactory("ne-telemetry-pool"),
                new CallerRunsPolicy()
        );
        TELEMETRY_EXECUTOR = telemetryExecutor;
        telemetryListeningExecutor = MoreExecutors.listeningDecorator(telemetryExecutor);

        cleanupExecutor = new ThreadPoolExecutor(
                5, 10, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(CLEANUP_QUEUE_SIZE),
                new NamedThreadFactory("ne-cleanup-pool"),
                new CallerRunsPolicy()
        );
        CLEANUP_EXECUTOR = cleanupExecutor;
        cleanupListeningExecutor = MoreExecutors.listeningDecorator(cleanupExecutor);

        ntpExecutor = new ThreadPoolExecutor(
                10, 20, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(NTP_QUEUE_SIZE),
                new NamedThreadFactory("ne-ntp-pool"),
                new CallerRunsPolicy()
        );
        NTP_EXECUTOR = ntpExecutor;
        ntpListeningExecutor = MoreExecutors.listeningDecorator(ntpExecutor);
    }

    public static ExecutorService getRegisterExecutor() {
        return REGISTER_EXECUTOR;
    }

    public static ExecutorService getSyncExecutor() {
        return SYNC_EXECUTOR;
    }

    public static ExecutorService getTelemetryExecutor() {
        return TELEMETRY_EXECUTOR;
    }

    public static ExecutorService getCleanupExecutor() {
        return CLEANUP_EXECUTOR;
    }

    public static ExecutorService getNtpExecutor() {
        return NTP_EXECUTOR;
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


    public static <T> ListenableFuture<T> submit(Callable<T> task, ExecutorService executor) {
        return MoreExecutors.listeningDecorator(executor).submit(task);
    }

    public static ListenableFuture<?> submit(Runnable task, ExecutorService executor) {
        return MoreExecutors.listeningDecorator(executor).submit(wrapTask(task));
    }

    public static void execute(Runnable task, ExecutorService executor) {
        MoreExecutors.listeningDecorator(executor).submit(wrapTask(task));
    }

    // 专用线程池的便捷方法
    public static <T> ListenableFuture<T> submitToRegister(Callable<T> task) {
        return registerListeningExecutor.submit(task);
    }

    public static ListenableFuture<?> submitToRegister(Runnable task) {
        return registerListeningExecutor.submit(wrapTask(task));
    }

    public static void executeInRegister(Runnable task) {
        registerListeningExecutor.submit(wrapTask(task));
    }

    public static <T> ListenableFuture<T> submitToSync(Callable<T> task) {
        return syncListeningExecutor.submit(task);
    }

    public static ListenableFuture<?> submitToSync(Runnable task) {
        return syncListeningExecutor.submit(wrapTask(task));
    }

    public static void executeInSync(Runnable task) {
        syncListeningExecutor.submit(wrapTask(task));
    }

    public static <T> ListenableFuture<T> submitToTelemetry(Callable<T> task) {
        return telemetryListeningExecutor.submit(task);
    }

    public static ListenableFuture<?> submitToTelemetry(Runnable task) {
        return telemetryListeningExecutor.submit(wrapTask(task));
    }

    public static void executeInTelemetry(Runnable task) {
        telemetryListeningExecutor.submit(wrapTask(task));
    }

    public static <T> ListenableFuture<T> submitToCleanup(Callable<T> task) {
        return cleanupListeningExecutor.submit(task);
    }

    public static ListenableFuture<?> submitToCleanup(Runnable task) {
        return cleanupListeningExecutor.submit(wrapTask(task));
    }

    public static void executeInCleanup(Runnable task) {
        cleanupListeningExecutor.submit(wrapTask(task));
    }

    public static <T> ListenableFuture<T> submitToNtp(Callable<T> task) {
        return ntpListeningExecutor.submit(task);
    }

    public static ListenableFuture<?> submitToNtp(Runnable task) {
        return ntpListeningExecutor.submit(wrapTask(task));
    }

    public static void executeInNtp(Runnable task) {
        ntpListeningExecutor.submit(wrapTask(task));
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
        // 通用线程池
        ThreadPoolExecutor executor = threadPool;
        log.info("General Pool Status - Active: {}, Pool: {}, Core: {}, Max: {}, Queue: {}/{}",
                executor.getActiveCount(),
                executor.getPoolSize(),
                executor.getCorePoolSize(),
                executor.getMaximumPoolSize(),
                executor.getQueue().size(),
                MAX_QUEUE_SIZE);

        // 注册线程池
        log.info("Register Pool Status - Active: {}, Pool: {}, Core: {}, Max: {}, Queue: {}/{}",
                registerExecutor.getActiveCount(),
                registerExecutor.getPoolSize(),
                registerExecutor.getCorePoolSize(),
                registerExecutor.getMaximumPoolSize(),
                registerExecutor.getQueue().size(),
                REGISTER_QUEUE_SIZE);

        // 同步线程池
        log.info("Sync Pool Status - Active: {}, Pool: {}, Core: {}, Max: {}, Queue: {}/{}",
                syncExecutor.getActiveCount(),
                syncExecutor.getPoolSize(),
                syncExecutor.getCorePoolSize(),
                syncExecutor.getMaximumPoolSize(),
                syncExecutor.getQueue().size(),
                SYNC_QUEUE_SIZE);

        // 遥测线程池
        log.info("Telemetry Pool Status - Active: {}, Pool: {}, Core: {}, Max: {}, Queue: {}/{}",
                telemetryExecutor.getActiveCount(),
                telemetryExecutor.getPoolSize(),
                telemetryExecutor.getCorePoolSize(),
                telemetryExecutor.getMaximumPoolSize(),
                telemetryExecutor.getQueue().size(),
                TELEMETRY_QUEUE_SIZE);

        // 清理线程池
        log.info("Cleanup Pool Status - Active: {}, Pool: {}, Core: {}, Max: {}, Queue: {}/{}",
                cleanupExecutor.getActiveCount(),
                cleanupExecutor.getPoolSize(),
                cleanupExecutor.getCorePoolSize(),
                cleanupExecutor.getMaximumPoolSize(),
                cleanupExecutor.getQueue().size(),
                CLEANUP_QUEUE_SIZE);

        // NTP 线程池
        log.info("NTP Pool Status - Active: {}, Pool: {}, Core: {}, Max: {}, Queue: {}/{}",
                ntpExecutor.getActiveCount(),
                ntpExecutor.getPoolSize(),
                ntpExecutor.getCorePoolSize(),
                ntpExecutor.getMaximumPoolSize(),
                ntpExecutor.getQueue().size(),
                NTP_QUEUE_SIZE);
    }

    public static void shutdown() {
        // 关闭通用线程池
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("Forcing shutdown of General Executor...");
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("General Executor did not terminate");
                }
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // 关闭专用线程池
        shutdownExecutor(registerExecutor, "Register Executor");
        shutdownExecutor(syncExecutor, "Sync Executor");
        shutdownExecutor(telemetryExecutor, "Telemetry Executor");
        shutdownExecutor(cleanupExecutor, "Cleanup Executor");
        shutdownExecutor(ntpExecutor, "NTP Executor");
    }

    private static void shutdownExecutor(ExecutorService executor, String name) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("Forcing shutdown of {}...", name);
                executor.shutdownNow();
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("{} did not terminate", name);
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
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
