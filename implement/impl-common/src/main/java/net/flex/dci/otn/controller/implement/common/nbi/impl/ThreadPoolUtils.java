package net.flex.dci.otn.controller.implement.common.nbi.impl;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ThreadPoolUtils {
    private static AtomicLong tag = new AtomicLong(1);
    private static final int MAX_POOL_SIZE = 256;
    private static final int MAX_QUEUE_SIZE = 1000;
    private static final String POOL_NAME = "impl-pool-%d";
    private static final String SCH_POOL_NAME = "impl-schedule-pool-%d";
    
//    private static ExecutorService service = ThreadPoolUtils.newFixedThreadPool(5);
//
//    public static ExecutorService getCommonPool(){
//        return service;
//    }
    
//    public static ExecutorService newFixedThreadPool(int corePoolSize,int maxPoolSize, String poolname){
//        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(poolname+"-%d").build();
//        //common thread pool
//        ExecutorService pool = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 0L, TimeUnit.MILLISECONDS
//                , new LinkedBlockingQueue<>(MAX_QUEUE_SIZE), threadFactory, new ThreadPoolExecutor.AbortPolicy());
//        return pool;
//    }
//
//    public static ExecutorService newSingleThreadPool(String poolname) {
//        return newFixedThreadPool(1,1,poolname);
//    }
    
    public static ScheduledThreadPoolExecutor newScheduleThreadPool(int corePoolSize, String poolname,boolean isDeamon){
        return new ScheduledThreadPoolExecutor(corePoolSize,
                new BasicThreadFactory.Builder().namingPattern(poolname+"-%d").daemon(isDeamon).build());
    }

    public static ScheduledExecutorService newScheduleThreadPool(int corePoolSize, String poolname){
        return newScheduleThreadPool(corePoolSize,poolname,false);
    }

    public static ScheduledExecutorService newScheduleThreadPool(int corePoolSize){
        return newScheduleThreadPool(corePoolSize,String.format(SCH_POOL_NAME,tag.getAndIncrement()));
    }
}
