/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.common.impl;

import com.google.common.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otn.controller.implement.common.config.ImplConfig;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
public class Step<V> {
    private static final int DEFAULT_THREAD_NUMBER = 48;
    private static final int DEFAULT_MAX_POOL_SIZE = 64;
    private static final int DEFAULT_MAX_QUEUE_SIZE = 256;
    private static final int MAX_ALLOWED_POOL_SIZE = 128;
    private static final int MAX_ALLOWED_QUEUE_SIZE = 512;

    private final static ListeningExecutorService service = MoreExecutors.listeningDecorator(newStepThreadPool());

    private static ExecutorService newStepThreadPool() {
        int corePoolSize = DEFAULT_THREAD_NUMBER;
        int maxPoolSize = DEFAULT_MAX_POOL_SIZE;
        int queueSize = DEFAULT_MAX_QUEUE_SIZE;

        try {
            ImplConfig implConfig = SpringBeanFinder.getBean(ImplConfig.class);
            corePoolSize = implConfig.getStepPoolCoreSize();
            maxPoolSize = implConfig.getStepPoolMaxSize();
            queueSize = implConfig.getStepPoolQueueSize();
        } catch (Exception e) {
            log.warn("cannot load implement step pool config, use defaults", e);
        }

        corePoolSize = normalize("corePoolSize", corePoolSize, DEFAULT_THREAD_NUMBER, 1, MAX_ALLOWED_POOL_SIZE);
        maxPoolSize = normalize("maxPoolSize", maxPoolSize, DEFAULT_MAX_POOL_SIZE, corePoolSize, MAX_ALLOWED_POOL_SIZE);
        queueSize = normalize("queueSize", queueSize, DEFAULT_MAX_QUEUE_SIZE, 1, MAX_ALLOWED_QUEUE_SIZE);

        log.info("implement step pool config: core={}, max={}, queue={}", corePoolSize, maxPoolSize, queueSize);

        ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueSize),
                new ThreadFactoryBuilder().setNameFormat("implementor-step-pool-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

    private static int normalize(String name, int value, int defaultValue, int min, int max) {
        if (value < min || value > max) {
            log.warn("invalid implement step pool {}, use default: value={}, default={}, range=[{}, {}]",
                    name, value, defaultValue, min, max);
            return defaultValue;
        }
        return value;
    }

    private StepToe stepToe;

    public Step(StepToe toe) {
        this.stepToe = toe;
    }

    public void start(FutureCallback<List<V>> endingWork) {
        List<ListenableFuture<V>> listenableFutures = new LinkedList<>();
        for (Callable doIt : stepToe.getToeList()) {
            listenableFutures.add(service.submit(doIt));
        }

        ListenableFuture<List<V>> futures = Futures.allAsList(listenableFutures);
        Futures.addCallback(futures, endingWork, service);

    }

}
