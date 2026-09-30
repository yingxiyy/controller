package net.flex.dci.otn.controller.subnet.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 2026/1/10
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@Order(value = 2)
@RequiredArgsConstructor
public class SubNetManagerApplicationRunner implements ApplicationRunner {

    private final SubNetManagerInitializer subNetManagerInitializer;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("subnet node initializer");
        subNetManagerInitializer.initializing();
    }
}
