package devicemaintenance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 定时任务调度器配置
 * 
 * 配置异步执行线程池，用于并发执行定时任务
 * 
 * 配置参数（application.yml）:
 * - task.scheduler.core-pool-size: 核心线程数（默认10）
 * - task.scheduler.max-pool-size: 最大线程数（默认20）
 * - task.scheduler.queue-capacity: 队列容量（默认100）
 */
@Configuration
@EnableAsync
@Slf4j
public class TaskSchedulerConfig {

    @Value("${task.scheduler.core-pool-size:10}")
    private int corePoolSize;

    @Value("${task.scheduler.max-pool-size:20}")
    private int maxPoolSize;

    @Value("${task.scheduler.queue-capacity:100}")
    private int queueCapacity;

    /**
     * 定时任务异步执行器
     */
    @Bean(name = "taskSchedulerExecutor")
    @Primary
    public Executor taskSchedulerExecutor() {
        log.info("初始化定时任务异步执行器: corePoolSize={}, maxPoolSize={}, queueCapacity={}", 
                 corePoolSize, maxPoolSize, queueCapacity);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数
        executor.setCorePoolSize(corePoolSize);
        
        // 最大线程数
        executor.setMaxPoolSize(maxPoolSize);
        
        // 队列容量
        executor.setQueueCapacity(queueCapacity);
        
        // 线程名称前缀
        executor.setThreadNamePrefix("scheduled-task-");
        
        // 拒绝策略：由调用线程执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 线程池关闭时等待所有任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 最多等待60秒
        executor.setAwaitTerminationSeconds(60);
        
        executor.initialize();
        
        log.info("定时任务异步执行器初始化完成");
        
        return executor;
    }
}
