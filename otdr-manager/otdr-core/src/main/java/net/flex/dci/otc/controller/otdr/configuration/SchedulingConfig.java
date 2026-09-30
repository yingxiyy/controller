package net.flex.dci.otc.controller.otdr.configuration;

import static net.flex.dci.otc.controller.otdr.utils.OtdrConstants.SCHEDULE_JOB_PREFIX;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;


@Configuration
public class SchedulingConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(10);
        taskScheduler.setRemoveOnCancelPolicy(true);
        taskScheduler.setThreadNamePrefix(SCHEDULE_JOB_PREFIX);
        return taskScheduler;
    }
}
