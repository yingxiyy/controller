package net.flex.dci.otn.controller.db.monitor.core.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.notification.rev180718.EventType;

/**
 * @version 1.0
 * @date 7/10/2025 2:31 PM
 */
@Getter
public enum OperationMethod {
    a("set", new ArrayList<>(), EventType.Update),
    u("set", Arrays.asList(SubOperationMethod.ne, SubOperationMethod.to, SubOperationMethod.oe),
            EventType.Update),
    i("insert", new ArrayList<>(), EventType.Update),
    d("$unset", new ArrayList<>(), EventType.Update);


    private final String mongodbOp;

    private final List<SubOperationMethod> subOperationMethods;

    private final EventType eventType;

    OperationMethod(String mongodbOp, List<SubOperationMethod> subOperationMethods,
            EventType eventType) {
        this.mongodbOp = mongodbOp;
        this.subOperationMethods = subOperationMethods;
        this.eventType = eventType;
    }

}
