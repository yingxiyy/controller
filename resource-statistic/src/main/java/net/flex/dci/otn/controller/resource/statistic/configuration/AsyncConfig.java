package net.flex.dci.otn.controller.resource.statistic.configuration;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 2026/4/12
 *
 * @author musa
 * @version 1.0
 **/
@Configuration
public class AsyncConfig {

    @Bean("inventoryExecutor")
    public ThreadPoolTaskExecutor inventoryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(16);
        executor.setMaxPoolSize(48);
        executor.setQueueCapacity(4096);
        executor.setKeepAliveSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setThreadNamePrefix("inventory-export-");
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        return executor;
    }

    @Bean("exportExecutor")
    public ThreadPoolTaskExecutor exportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setThreadNamePrefix("export-task-");
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        return executor;
    }


    @Bean("queryExecutor")
    public ThreadPoolTaskExecutor queryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(30);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setThreadNamePrefix("query-task-");
        executor.setAllowCoreThreadTimeOut(true);
        executor.initialize();
        return executor;
    }
}
