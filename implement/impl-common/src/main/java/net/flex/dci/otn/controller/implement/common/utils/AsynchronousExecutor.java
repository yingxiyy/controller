package net.flex.dci.otn.controller.implement.common.utils;

import com.google.common.util.concurrent.ListenableFuture;
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
                        "implementor-asynchronous-pool"));
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

    /**
     * 提交Runnable任务并返回ListenableFuture，支持异步回调和等待
     *
     * @param runnable 任务
     * @return ListenableFuture<Void>
     */
    public static ListenableFuture<Void> submit(Runnable runnable) {
        return executorService.submit(runnable, null);
    }

    /**
     * 提交Callable任务并返回ListenableFuture
     *
     * @param callable 任务
     * @param <T> 返回类型
     * @return ListenableFuture<T>
     */
    public static <T> ListenableFuture<T> submit(Callable<T> callable) {
        return executorService.submit(callable);
    }

}
