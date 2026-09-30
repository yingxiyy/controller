/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.discovery.common;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

@Service
public class MyExecutor {
  private ListeningExecutorService commonScheduler;

  private static final int THREAD_NUMBER = 10;
  private static final int MAX_POOL_SIZE = 32;
  private static final int MAX_QUEUE_SIZE = 256;

  public MyExecutor() {
    commonScheduler = MoreExecutors.listeningDecorator(newFixedThreadPool(THREAD_NUMBER,MAX_POOL_SIZE, "controller-allocator-pool"));
  }

  private ExecutorService newFixedThreadPool(int corePoolSize, int maxPoolSize, String poolname){
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(poolname+"-%d").build();
    //common thread pool
    ExecutorService pool = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 0L, TimeUnit.MILLISECONDS
            , new LinkedBlockingQueue<>(MAX_QUEUE_SIZE), threadFactory, new ThreadPoolExecutor.AbortPolicy());

    return pool;
  }

  public void lazyDo(Runnable task) {
    commonScheduler.submit(task);
  }

}
