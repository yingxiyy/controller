package net.flex.dci.otn.controller.db.monitor.core.service.dto;

import java.io.Serializable;
import lombok.Data;
import net.flex.dci.otc.common.model.type.DataStoreType;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.notification.rev180718.EventType;

/**
 * @version 1.0
 * @date 2021/11/4 13:36
 */
@Data
public class NetConfEventChangeDto implements Serializable {

    private String topologyRef;

    private String topologyType;

    private String objectType;

    private String objectKeyName;

    private Document changeData;

    private String objectId;

    private DataStoreType dataStoreType;

    private EventType eventType;
}
