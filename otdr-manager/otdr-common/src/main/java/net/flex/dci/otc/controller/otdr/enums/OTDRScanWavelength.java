package net.flex.dci.otc.controller.otdr.enums;

import lombok.Getter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.ScanWavelength;

/**
 * 2025/8/3
 *
 * @author musa
 * @version 1.0
 **/
@Getter
public enum OTDRScanWavelength {

    WT1510(ScanWavelength.WT1510),

    /**
     * wavelength 1620
     */
    WT1620(ScanWavelength.WT1620),

    /**
     * wavelength 1610
     */
    WT1610(ScanWavelength.WT1610);

    private final ScanWavelength scanWavelength;

    OTDRScanWavelength(ScanWavelength scanWavelength) {
        this.scanWavelength = scanWavelength;
    }
}
