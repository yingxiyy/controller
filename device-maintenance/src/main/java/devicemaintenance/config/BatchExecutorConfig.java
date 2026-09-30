package devicemaintenance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 批次操作线程池配置
 * 
 * 统一线程池管理所有批次操作的 RPC 并发调用：
 * - batchTaskExecutor: 用于所有批次操作（download/backup/upgrade/commit/rollback）
 * 
 * 线程池参数说明：
 * - corePoolSize: 核心线程数，一直存活
 * - maxPoolSize: 最大线程数，控制总体并发度（不区分操作类型）
 * - queueCapacity: 队列容量，超过核心线程数的任务进入队列
 * 
 * 场景示例：10000个设备，maxPoolSize=100
 * - 同时只能执行100个 RPC 调用（所有操作共享这个线程池）
 * - 剩余9900个任务在队列中等待
 * - 队列满后，触发拒绝策略（CallerRunsPolicy：调用者线程执行）
 * 
 * 注意：线程数是总体计数，不按命令分别计数
 * - download、backup、upgrade、commit、rollback 都使用这个线程池
 * - maxPoolSize 控制的是所有操作的总并发数
 */
@Configuration
@Slf4j
public class BatchExecutorConfig {

    /**
     * 批次任务执行线程池（统一线程池）
     * 用于并发下发所有类型的 RPC 操作
     */
    @Bean(name = "batchTaskExecutor")
    public ThreadPoolTaskExecutor batchTaskExecutor(
            @Value("${batch.executor.core-pool-size:20}") int corePoolSize,
            @Value("${batch.executor.max-pool-size:100}") int maxPoolSize,
            @Value("${batch.executor.queue-capacity:10000}") int queueCapacity,
            @Value("${batch.executor.thread-name-prefix:batch-rpc-}") String threadNamePrefix,
            @Value("${batch.executor.await-termination-seconds:60}") int awaitTerminationSeconds) {
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心线程数（始终存活）
        executor.setCorePoolSize(corePoolSize);
        
        // 最大线程数（控制所有 RPC 操作的总并发数）
        executor.setMaxPoolSize(maxPoolSize);
        
        // 队列容量（超过核心线程数的任务进入队列）
        executor.setQueueCapacity(queueCapacity);
        
        // 线程名称前缀（便于日志追踪）
        executor.setThreadNamePrefix(threadNamePrefix);
        
        // 拒绝策略：CallerRunsPolicy
        // 当队列满且达到最大线程数时，由调用者线程执行任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待终止时间（秒）
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        
        // 初始化
        executor.initialize();
        
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚀 批次任务线程池初始化完成");
        log.info("  核心线程数: {}", corePoolSize);
        log.info("  最大线程数: {} (所有 RPC 操作的总并发数)", maxPoolSize);
        log.info("  队列容量: {}", queueCapacity);
        log.info("  线程前缀: {}", threadNamePrefix);
        log.info("  拒绝策略: CallerRunsPolicy (调用者线程执行)");
        log.info("  支持操作: download, backup, upgrade, commit, rollback");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        return executor;
    }
}

