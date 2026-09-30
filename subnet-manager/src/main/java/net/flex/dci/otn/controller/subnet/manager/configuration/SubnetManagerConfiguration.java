package net.flex.dci.otn.controller.subnet.manager.configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 2026/2/14
 *
 * @author musa
 * @version 1.0
 **/
@Configuration
@EnableAsync
public class SubnetManagerConfiguration {

    @Bean(name = "subnetManagerExecutor")
    public Executor subnetManagerExecutor() {
        return new ThreadPoolExecutor(
                20,
                50,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),
                r -> new Thread(r, "subnet-manager-async-thread-" + r.hashCode()),
                new CallerRunsPolicy()
        );
    }

}
