package net.flex.dci.otn.controller.nms.nms.handler.impl.resource.alarm;

import net.flex.dci.otn.controller.nms.nms.enums.NMSResourceType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.LocateResourcesByAlarmOutput;

/**
 * 2025/8/4
 *
 * @author musa
 * @version 1.0
 **/
public interface AlarmResourceLocator {

    LocateResourcesByAlarmOutput locateAlarmResource(String resourceId,
            NMSResourceType nmsResourceType);
}
