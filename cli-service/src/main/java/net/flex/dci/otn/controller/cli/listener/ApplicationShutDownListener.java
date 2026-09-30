package net.flex.dci.otn.controller.cli.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.cli.component.DeviceSessionFactory;
import net.flex.dci.otn.controller.cli.component.SessionManager;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 *
 * 2025/9/20
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class ApplicationShutDownListener implements ApplicationListener<ContextClosedEvent> {


    private final DeviceSessionFactory deviceSessionFactory;

    private final SessionManager sessionManager;


    @Override
    public void onApplicationEvent(ContextClosedEvent contextClosedEvent) {
        log.info("Application is shutting down,cleaning up all current sessions");
        deviceSessionFactory.cleanupAllSessions();

        log.info("Clean up {} sessions durring shutdown ", sessionManager.getActiveSession());
    }
}
