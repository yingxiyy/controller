package net.flex.dci.otn.controller.nms.nms.component.terminationPoint.scan;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.properties.scan.TelecomScanPortConfiguration;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @version 1.0
 * @date 11/29/2023 11:08 AM
 */
@Slf4j
public abstract class AbstractScanTerminationPoint implements IScanTerminationPoint {

    @Autowired
    protected TelecomScanPortConfiguration telecomScanPortConfiguration;


}
