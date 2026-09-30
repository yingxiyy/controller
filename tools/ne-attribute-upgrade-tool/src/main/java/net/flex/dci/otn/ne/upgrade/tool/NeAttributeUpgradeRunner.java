package net.flex.dci.otn.ne.upgrade.tool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.ne.upgrade.tool.upgrade.NeAttribute;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 2026/3/13
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Order(value = 2)
@RequiredArgsConstructor
@Slf4j
public class NeAttributeUpgradeRunner implements ApplicationRunner {

    private final NeAttribute neAttribute;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("start to upgrade ne ip formatter");
//        neAttribute.upgradeNeSubType();
        neAttribute.removeTelemetryRecord();
        log.info("Task completed, exiting application...");
        System.exit(0);
    }
}
