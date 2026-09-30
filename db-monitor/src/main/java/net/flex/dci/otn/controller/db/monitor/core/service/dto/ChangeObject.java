package net.flex.dci.otn.controller.db.monitor.core.service.dto;

import java.io.Serializable;
import lombok.Data;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.notification.rev180718.EventType;

/**
 * @version 1.0
 * @date 2021/11/10 17:50
 */
@Data
public class ChangeObject implements Serializable {

    private String objectType;

    private String objectKeyName;

    private Document changeBody;

    private EventType eventType;

    private String neId;

    private String collectionName;
}
