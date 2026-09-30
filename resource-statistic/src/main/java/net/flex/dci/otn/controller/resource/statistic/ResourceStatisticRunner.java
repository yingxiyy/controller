package net.flex.dci.otn.controller.resource.statistic;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.export.file.ExportFileServiceImpl;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 2026/6/10
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@Order(value = 2)
@RequiredArgsConstructor
public class ResourceStatisticRunner implements ApplicationRunner {

    private final ExportFileServiceImpl exportFileServiceImpl;


    @Override
    public void run(ApplicationArguments args) throws Exception {
        exportFileServiceImpl.initExportDir();
    }
}
