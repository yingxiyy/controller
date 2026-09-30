package net.flex.dci.otn.controller.db.monitor.core.service;

import static net.flex.dci.otc.common.util.YangConstants.PHY_TOPO_KEY;

import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.aspect.Log;
import net.flex.dci.otc.common.model.ne.NeStatus;
import net.flex.dci.otc.common.model.ne.NeStatusMessage;
import net.flex.dci.otc.common.model.type.NeStatusType;
import net.flex.dci.otn.controller.db.monitor.core.service.dto.NetConfEventChangeDto;
import net.flex.dci.otn.controller.tools.kafka.service.StatusMessageSender;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.notification.rev180718.EventType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2023/3/8 11:29
 */
@Component
@Slf4j
public class NeStatusChangeNotificationService {

    @Log
    public void publishNotification(EventType eventType, NetConfEventChangeDto eventChangeDto) {
//        log.info("start to send notification event type is {},change data is :{}", eventType,
//                eventChangeDto);
        log.debug("start to send notification event type is {},change data is :{}", eventType,
                eventChangeDto);
        try {
            if (eventChangeDto == null) {
                return;
            }
            String topologyRef = eventChangeDto.getTopologyRef();
            String objectType = eventChangeDto.getObjectType();
            if (!(topologyRef.equals(PHY_TOPO_KEY) && objectType.equals(ObjectType.Node.name()))) {
                log.debug("don't support the type eventChange notification");
                return;
            }
            NeStatusMessage neStatusMessage = null;
            if (eventType.equals(EventType.Remove)) {
                String changeObjectKey = eventChangeDto.getObjectKeyName();
                Document data = eventChangeDto.getChangeData();
                String changeObjectId = data.getString(changeObjectKey);
                NeStatus neStatus = NeStatus.builder().neStatus(NeStatusType.REMOVED)
                        .neId(changeObjectId).build();
                neStatusMessage = NeStatusMessage.builder().neStatuses(
                        Collections.singletonList(neStatus)).build();
            }
            StatusMessageSender.sendMessage(neStatusMessage);
            log.info("end to send notification ");
            log.debug("end to send notification");
        } catch (Exception e) {
            log.error("Failed to send notification for iid {}", e);
//                    eventChangeDto.getIid().getTargetType().getName(), e);
        }
    }
}
