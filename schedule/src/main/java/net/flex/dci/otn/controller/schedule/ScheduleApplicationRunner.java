package net.flex.dci.otn.controller.schedule;

import lombok.RequiredArgsConstructor;
import net.flex.dci.otn.controller.schedule.task.DefaultTaskInitializer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 2026/6/2
 *
 * @author musa
 * @version 1.0
 **/
@Order(2)
@Component
@RequiredArgsConstructor
public class ScheduleApplicationRunner implements CommandLineRunner {

    private final DefaultTaskInitializer defaultTaskInitializer;

    @Override
    public void run(String... args) throws Exception {
        defaultTaskInitializer.initialSystemTask();
    }
}
