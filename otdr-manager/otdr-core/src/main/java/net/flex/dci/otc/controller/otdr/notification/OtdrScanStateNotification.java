package net.flex.dci.otc.controller.otdr.notification;

import net.flex.dci.otc.controller.otdr.domain.OtdrScanDetail;

/**
 * @version 1.0
 * @date 2022/9/2 15:17
 */
public interface OtdrScanStateNotification {

    void sendOtdrScanStateNotification(OtdrScanDetail otdrScanDetail);


}
