package net.flex.dci.otc.controller.ne.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.components.consistent.MapperConsistent;
import net.flex.dci.otc.controller.ne.manager.components.consistent.TelemetryNtpConfConsistent;
import net.flex.dci.otc.controller.ne.manager.task.DynamicSliceTaskManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 2025/6/11
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@Order(value = 2)
@RequiredArgsConstructor
public class NeManagerApplicationRunner implements ApplicationRunner {


    private final MapperConsistent mapperConsistent;
    private final DynamicSliceTaskManager dynamicSliceTaskManager;
    private final TelemetryNtpConfConsistent telemetryNtpConfConsistent;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.trace("initializing the synchronizeMapperState");
        telemetryNtpConfConsistent.ensureConfigConsistent();
        mapperConsistent.cleanInConsistentMapper();
        dynamicSliceTaskManager.initTasks();
    }
}
