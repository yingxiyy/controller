package net.flex.dci.otn.controller.nms.cache.manager;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.cache.redis.core.RedisCacheOperation;
import net.flex.dci.otn.controller.nms.cache.utils.AsynchronousCacheRefreshExecutor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @version 1.0
 * @date 2022/3/20 10:39
 */
@Slf4j
public abstract class AbstractCacheManager<T> {

    @Autowired
    protected RedisCacheOperation cacheOperation;

    protected final String SIGN_KEY = "_sign";

    private final String LOCK_SUFFIX = "_LOCK";


    protected final int EXPIRE_TIME_OUT = 1800;

    public String generateLockKey(String id) {
        return id + LOCK_SUFFIX;
    }

    public String generateSignKey(String id) {
        return id + SIGN_KEY;
    }

    public T getValue(String id) throws ExecutionException, InterruptedException {
        return null;
    }

    public T removeCache(String id) throws ExecutionException, InterruptedException {
        return null;
    }


    protected T refreshCache(String id) throws ExecutionException, InterruptedException {
        log.info("start to refresh site link cache ,link id is {}", id);
        return null;
    }


    protected <T> T refreshAndGetCache(String id, Callable<T> callable)
            throws ExecutionException, InterruptedException {
        log.info("start to refresh cache ,id is {}", id);
        ListenableFuture<T> listenableFuture = AsynchronousCacheRefreshExecutor.execute(callable
        );
        return listenableFuture.get();
    }

}
