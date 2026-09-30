package net.flex.dci.otn.controller.cli.configuration;

import net.flex.dci.otn.controller.cli.ssh.CliWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 *
 * @version 1.0
 * @date 9/17/2025 4:10 PM
 */
@Configuration
@EnableWebSocket
@EnableScheduling
public class WebsocketConfig implements WebSocketConfigurer {

    private final CliWebSocketHandler cliWebSocketHandler;

    public WebsocketConfig(CliWebSocketHandler cliWebSocketHandler) {
        this.cliWebSocketHandler = cliWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(cliWebSocketHandler, "/cli/ws")
                .setAllowedOrigins("*");
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        // 配置线程池大小，根据实际需求调整
        scheduler.setPoolSize(Runtime.getRuntime().availableProcessors() * 2);
        scheduler.setThreadNamePrefix("cli-session-task-scheduler-");
        scheduler.initialize();
        return scheduler;
    }
}
