package net.flex.dci.otn.controller.gateway.valuemap;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;
import org.springframework.util.MultiValueMap;

/**
 * @version 1.0
 * @date 2022/2/11 10:26
 */
@Data
@Builder
public class GatewayContext implements Serializable {

    @Tolerate
    public GatewayContext() {
    }

    public static final String CACHE_GATEWAY_CONTEXT = "cache-Gateway-Context";

    private String cacheReqBody;

    private MultiValueMap<String, String> formData;

    private String path;


    private String cachedResponseBody;
}
