package net.flex.dci.otc.controller.rpc.client.rpcs.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.controller.rpc.client.httpclient.OdlRpcClientConfig;
import net.flex.dci.otc.controller.rpc.client.rpcs.OTDRRpc;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrMonitorInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrMonitorInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrMonitorOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/8/30 10:20
 */
@Component
@Slf4j
public class OTDRRpcImpl extends BasicRpc implements OTDRRpc {

    public OTDRRpcImpl() {
        this.NAMESPACE = "otdr";
        this.MODULE_NAME = "otdr-monitor";
    }

    @Override
    public GetOtdrMonitorOutput getOtdrMonitor(Adapter adapter, String nodeId)
            throws CommonException {
        GetOtdrMonitorInput input = buildGetOtdrMonitorInput(nodeId);
        log.debug("start to get otdr monitor status,the neId is:{}", nodeId);
        String requestOp = "get-otdr-monitor";
        try {
            String body = formRpcInput(requestOp, input);
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd()).build())
                    .post(getManagerUrl(adapter, requestOp), body);
            log.info("get otdr monitor result is:{}", result);
            return (GetOtdrMonitorOutput) formRpcOutPut(requestOp, result);
        } catch (Exception ex) {
            log.error("failed to execute the command for the get otdr monitor {}", ex.getMessage(),
                    ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    private GetOtdrMonitorInput buildGetOtdrMonitorInput(String nodeId) {
        return new GetOtdrMonitorInputBuilder().setNodeId(NodeId.getDefaultInstance(nodeId))
                .build();
    }

    @Override
    public StartOtdrOutput startOtdr(Adapter adapter, StartOtdrInput input) throws CommonException {
        log.debug("start to start otdr,the input is :{}", input);
        String requestOp = "start-otdr";
        try {
            String body = formRpcInput(requestOp, input);
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd()).build())
                    .post(getManagerUrl(adapter, requestOp), body);
            log.info("get start OTDR result is:{}", result);
            return (StartOtdrOutput) formRpcOutPut(requestOp, result);
        } catch (Exception ex) {
            log.error("failed to execute the command for the start otdr");
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public GetOtdrResultOutput getOtdrResult(Adapter adapter, GetOtdrResultInput input)
            throws CommonException {
        log.debug("start to get otdr from ne,the input is :{}", input);
        String requestOp = "get-otdr-result";
        try {
            String body = formRpcInput(requestOp, input);
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd()).build())
                    .post(getManagerUrl(adapter, requestOp), body);
            return (GetOtdrResultOutput) formRpcOutPut(requestOp, result);
        } catch (Exception ex) {
            log.error("failed to execute the command for the get otdr result");
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public String getOtdrJsonResult(Adapter adapter, GetOtdrResultInput input)
            throws CommonException {
        log.debug("start to get otdr json result from ne,the input is :{}", input);
        String requestOp = "get-otdr-result";
        try {
            String body = formRpcInput(requestOp, input);
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd()).build())
                    .post(getManagerUrl(adapter, requestOp), body);
            return result;
        } catch (Exception ex) {
            log.error("failed to execute the command for the get otdr result");
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

}
