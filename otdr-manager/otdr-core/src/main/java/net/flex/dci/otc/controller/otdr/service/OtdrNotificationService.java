package net.flex.dci.otc.controller.otdr.service;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.aspect.Log;
import net.flex.dci.otc.common.model.ObjectChangeMessage;
import net.flex.dci.otc.common.model.type.DataStoreType;
import net.flex.dci.otc.common.util.SwitchCaseUtils;
import net.flex.dci.otc.controller.otdr.domain.OtdrTaskResult;
import net.flex.dci.otn.controller.tools.kafka.service.ObjectNotifiMessager;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.notification.rev180718.EventType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ObjectType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.otn.phy.topology.type.OtnPhyTopology;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/9/2 15:43
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OtdrNotificationService {


    @Log
    public void publishNotification(EventType eventType, OtdrTaskResult otdrTaskResult) {
        log.info("start to send notification event type is {},change data is :{}", eventType,
                otdrTaskResult);
        log.debug("start to send notification event type is {},change data is :{}", eventType,
                otdrTaskResult);
        try {
            if (otdrTaskResult == null) {
                return;
            }
            String changeData = JSON.toJSONString(otdrTaskResult);
            ObjectChangeMessage objectChangeMessage = ObjectChangeMessage.builder()
                    .eventType(SwitchCaseUtils.lowerFirstCase(EventType.Update.name()))
                    .data(changeData)
                    .dataStoreType(DataStoreType.OPERATIONAL)
                    .timestamp(System.currentTimeMillis())
                    .objectType(SwitchCaseUtils.lowerFirstCase(ObjectType.Otdr.name()))
                    .topologyRef(OtnPhyTopology.QNAME.getLocalName())
                    .topologyType(OtnPhyTopology.QNAME.getLocalName())
                    .build();

            ObjectNotifiMessager.publishKafkaMessage(objectChangeMessage);
            log.info("end to send notification ");
            log.debug("end to send notification");
        } catch (Exception e) {
            log.error("Failed to send notification for iid {}", e.getMessage(), e);
        }
    }
}
