package net.flex.dci.otc.controller.status.configuration;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 2025/12/27
 *
 * @author musa
 * @version 1.0
 **/
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("alarmMessageExecutor")
    public Executor alarmMessageExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("AlarmMessage-");
        executor.setKeepAliveSeconds(60);
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }


    @Bean("alarmStateExecutor")
    public ExecutorService alarmStateExecutor() {
        return new ThreadPoolExecutor(
                40,
                100,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(10000),
                new ThreadFactoryBuilder().setNameFormat("alarm-state-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

//    @Bean("neStatusProcessExecutor")
//    public ExecutorService neStatusProcessExecutor() {
//        return new ThreadPoolExecutor(
//                20, 40, 60, TimeUnit.SECONDS,
//                new LinkedBlockingQueue<>(5000),
//                new ThreadFactoryBuilder().setNameFormat("ne-process-%d").build(),
//                new ThreadPoolExecutor.CallerRunsPolicy()
//        );
//    }

    @Bean("stateUpdateExecutor")
    public ExecutorService stateUpdateExecutor() {
        return new ThreadPoolExecutor(
                8, 16, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(500),
                new ThreadFactoryBuilder().setNameFormat("state-update-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean("nodeStateExecutor")
    public ExecutorService nodeStateExecutor() {
        return new ThreadPoolExecutor(
                8, 16, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new ThreadFactoryBuilder().setNameFormat("node-state-update-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }


    @Bean("linkStateExecutor")
    public ExecutorService linkStateExecutor() {
        return new ThreadPoolExecutor(
                16, 32, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                new ThreadFactoryBuilder().setNameFormat("link-state-update-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean("alarmProcessExecutor")
    public ExecutorService alarmProcessExecutor() {
        return new ThreadPoolExecutor(
                16, 32, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(5000),
                new ThreadFactoryBuilder().setNameFormat("alarm-process-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean("siteAggregateExecutor")
    public ExecutorService siteAggregateExecutor() {
        return new ThreadPoolExecutor(
                4, 8, 60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                new ThreadFactoryBuilder().setNameFormat("site-aggregate-%d").build(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
