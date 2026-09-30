package net.flex.dci.otc.controller.otdr.components;

import net.flex.dci.otc.controller.otdr.model.LatestOtdrResult;
import net.flex.dci.otc.controller.otdr.model.OtdrResultOutput;

/**
 * @version 1.0
 * @date 2022/9/1 11:41
 */
public interface OtdrLatestResult {

    OtdrResultOutput getLatestOtdrResultByMonitorTpId(String monitorTpId);

    /**
     * two direction for otdr scan z->a a->z
     *
     * @param monitorTypId
     * @return
     */
    LatestOtdrResult getLatestOtdrResByMonitorTpId(String monitorTypId);
}
