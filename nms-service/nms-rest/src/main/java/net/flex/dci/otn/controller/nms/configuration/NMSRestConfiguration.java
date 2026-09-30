package net.flex.dci.otn.controller.nms.configuration;

import static net.flex.dci.otn.controller.nms.utils.NetConfWildCard.NETWORK_TOPOLOGY_CONFIG_WILDCARD;
import static net.flex.dci.otn.controller.nms.utils.NetConfWildCard.NETWORK_TOPOLOGY_OPERATIONAL_WILDCARD;
import static net.flex.dci.otn.controller.nms.utils.NetConfWildCard.NMS_RPC_REQUEST_WILDCARD;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.interceptor.NMSOperationInterceptor;
import net.flex.dci.otn.controller.nms.interceptor.NetworkTopologyInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * @version 1.0
 * @date 2022/4/13 22:21
 */
@Configuration
@Slf4j
@RequiredArgsConstructor
public class NMSRestConfiguration extends WebMvcConfigurationSupport {

    private final NetworkTopologyInterceptor networkTopologyInterceptor;

    private final NMSOperationInterceptor nmsOperationInterceptor;


    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(networkTopologyInterceptor)
                .addPathPatterns(NETWORK_TOPOLOGY_CONFIG_WILDCARD,
                        NETWORK_TOPOLOGY_OPERATIONAL_WILDCARD);
        registry.addInterceptor(nmsOperationInterceptor).addPathPatterns(NMS_RPC_REQUEST_WILDCARD);
    }
}
