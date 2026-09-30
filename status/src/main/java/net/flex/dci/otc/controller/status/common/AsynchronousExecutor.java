package net.flex.dci.otc.controller.status.common;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import net.flex.dci.otc.common.util.SimpleThreadPoolExecutor;


public class AsynchronousExecutor {

    private static final ListeningExecutorService executorService;

    private static final int THREAD_NUMBER = 10;
    private static final int MAX_POOL_SIZE = 32;
    private static final int MAX_QUEUE_SIZE = 256;

    static {
        executorService = MoreExecutors.listeningDecorator(
                newFixedThreadPool(THREAD_NUMBER, MAX_POOL_SIZE,
                        "status-task-pool"));
    }

    public AsynchronousExecutor() {

    }

    private static ExecutorService newFixedThreadPool(int corePoolSize, int maxPoolSize,
            String poolname) {
        ExecutorService executorService = new SimpleThreadPoolExecutor(corePoolSize, maxPoolSize,
                0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(MAX_QUEUE_SIZE),
                new AbortPolicy(), poolname);
        return executorService;
    }

    public static void execute(Callable callable) {
        executorService.submit(callable);
    }

    public static void execute(Runnable runnable) {
        executorService.submit(runnable);
    }

}
