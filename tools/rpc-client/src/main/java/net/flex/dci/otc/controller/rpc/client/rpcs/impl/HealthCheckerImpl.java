package net.flex.dci.otc.controller.rpc.client.rpcs.impl;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.CommonUtil;
import net.flex.dci.otc.controller.rpc.client.dto.HealthStatusResp;
import net.flex.dci.otc.controller.rpc.client.dto.ModuleCredential;
import net.flex.dci.otc.controller.rpc.client.enums.HealthStatus;
import net.flex.dci.otc.controller.rpc.client.httpclient.OdlRpcClient;
import net.flex.dci.otc.controller.rpc.client.httpclient.OdlRpcClientConfig.Builder;
import net.flex.dci.otc.controller.rpc.client.rpcs.HealthChecker;
import net.flex.dci.otc.controller.rpc.client.utils.ModuleUtils;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import org.asynchttpclient.Response;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

/**
 *
 * @version 1.0
 * @date 12/11/2025 1:14 PM
 */
@Component
@Slf4j
public class HealthCheckerImpl implements HealthChecker {

    private final static String HEALTH_CHECK_URL_TEMPLATE = "http://%s:%d/actuator/health";

    private final Long CONNECTION_TIMEOUT = 30000L;

    private final Long READ_TIMEOUT = 30000L;

    @Autowired
    protected OdlRpcClient odlRpcClient;

    @Override
    public HealthStatus checkHealth(Adapter adapter) {

        log.debug("start to check the adapter health:{}", adapter.getName());
        try {
            String hostIp = CommonUtil.formateIpAddress(adapter.getIp());
            String url = String.format(
                    HEALTH_CHECK_URL_TEMPLATE, hostIp, adapter.getPort().getValue());
            Response response = odlRpcClient.setConfig(
                            new Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd())
                                    .connectionTimeout(CONNECTION_TIMEOUT).readTimeout(READ_TIMEOUT)
                                    .build())
                    .getRes(url);
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                String responseBody = response.getResponseBody();
                HealthStatusResp healthStatusResp = JSON.parseObject(responseBody,
                        HealthStatusResp.class);
                if (HealthStatus.UP.name().equalsIgnoreCase(healthStatusResp.getStatus())) {
                    return HealthStatus.UP;
                } else {
                    log.warn("Adapter {} health check returned non-up status:{}", adapter.getName(),
                            healthStatusResp.getStatus());
                    return HealthStatus.DOWN;
                }
            } else {
                log.warn("Adapter {} health check returned http{}", adapter.getName(),
                        response.getStatusCode());
                return HealthStatus.DOWN;
            }
        } catch (ResourceAccessException e) {
            // 连接超时、读取超时或网络不可达
            log.warn("Adapter {} health check failed due to network error: {}",
                    adapter.getName(), e.getMessage());
            return HealthStatus.UNREACHABLE;
        } catch (Exception e) {
            log.warn("Adapter {} health check encountered unexpected error: {}",
                    adapter.getName(), e.getMessage());
            return HealthStatus.UNKNOWN;
        }
    }

    @Override
    public HealthStatus checkHealth(InstanceDetails instanceDetails) {
        log.debug("check the current instance Details health status:{}", instanceDetails.getId());
        try {
            ModuleCredential moduleCredential = ModuleUtils.getCredential(instanceDetails);
            assert moduleCredential != null;
            String hostIp = CommonUtil.formateIpAddress(moduleCredential.getIp());
            String url = String.format(HEALTH_CHECK_URL_TEMPLATE, hostIp,
                    moduleCredential.getPort());
            Builder configBuilder = new Builder().connectionTimeout(CONNECTION_TIMEOUT)
                    .readTimeout(READ_TIMEOUT);
            if (moduleCredential.getPassword() != null && moduleCredential.getUsername() != null) {
                configBuilder.password(moduleCredential.getPassword());
                configBuilder.user(moduleCredential.getUsername());
            }

            Response response = odlRpcClient.setConfig(configBuilder.build()).getRes(url);
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                String responseBody = response.getResponseBody();
                HealthStatusResp healthStatusResp = JSON.parseObject(responseBody,
                        HealthStatusResp.class);
                if (HealthStatus.UP.name().equalsIgnoreCase(healthStatusResp.getStatus())) {
                    return HealthStatus.UP;
                } else {
                    log.warn("Instance {} health check returned non-up status:{}",
                            instanceDetails.getId(),
                            healthStatusResp.getStatus());
                    return HealthStatus.DOWN;
                }
            } else {
                log.warn("Instance {} health check returned http{}", instanceDetails.getId(),
                        response.getStatusCode());
                return HealthStatus.DOWN;
            }
        } catch (ResourceAccessException e) {
            // 连接超时、读取超时或网络不可达
            log.warn("Instance {} health check failed due to network error: {}",
                    instanceDetails.getId(), e.getMessage());
            return HealthStatus.UNREACHABLE;
        } catch (Exception e) {
            log.warn("Instance {} health check encountered unexpected error: {}",
                    instanceDetails.getId(), e.getMessage());
            return HealthStatus.UNKNOWN;
        }
    }
}
