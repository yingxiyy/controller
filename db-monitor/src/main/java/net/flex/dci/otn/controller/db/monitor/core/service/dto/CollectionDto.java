package net.flex.dci.otn.controller.db.monitor.core.service.dto;

import java.lang.reflect.Method;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2021/11/4 14:28
 */
@Data
@Builder
public class CollectionDto {

    private String keyName;

    private Method method;

    private String topologyRef;

    private String topologyType;

    private String objectType;

    private String objectKeyName;

    private String dataKey;

}
