package net.flex.dci.otc.controller.rpc.client.rpcs;

import net.flex.dci.otc.common.exception.CommonException;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrMonitorOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrOutput;

/**
 * @version 1.0
 * @date 2022/8/30 10:20
 */
public interface OTDRRpc {

    /**
     * otdr task rpc
     */

    /**
     * rpc for get otdr monitor
     *
     * @param adapter
     * @param nodeId
     * @return
     * @throws CommonException
     */
    GetOtdrMonitorOutput getOtdrMonitor(Adapter adapter, String nodeId)
            throws CommonException;

    StartOtdrOutput startOtdr(Adapter adapter, StartOtdrInput input) throws CommonException;


    GetOtdrResultOutput getOtdrResult(Adapter adapter, GetOtdrResultInput input)
            throws CommonException;

    String getOtdrJsonResult(Adapter adapter, GetOtdrResultInput input)
            throws CommonException;
}
