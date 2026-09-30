package net.flex.dci.otn.controller.nms.nms.handler.impl.resource.alarm;

import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.enums.NMSResourceType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.LocateResourcesByAlarmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ResourceEntity.ResourceType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.MatchingResources;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.ParentResources;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.ParentResourcesBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.parent.resources.Resource;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.parent.resources.ResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/5/2025 10:39 AM
 */
@Component
@Slf4j
public class ChassisAlarmResourceLocator extends AbstractAlarmResourceLocator {

    @Override
    public LocateResourcesByAlarmOutput getLocateByAlarmResourceId(String alarmResourceId) {
        log.debug("locate the chassis alarm resource by id:{}", alarmResourceId);
        Equipments chassis = netconfTopology.getEquipment(
                alarmResourceId);
        if (chassis == null) {
            return resourceRemove(alarmResourceId);
        } else {
            return getChassisAlarmLocateResource(chassis);
        }
    }

    /**
     * @param chassis
     * @return
     */
    private LocateResourcesByAlarmOutput getChassisAlarmLocateResource(Equipments chassis) {
        log.debug("get chassis alarm locate resource,the chassis id :{}", chassis.getEquipmentId());

        String cardName = chassis.getFriendlyName();
        String cardId = chassis.getEquipmentId();
        MatchingResources matchingResources = buildMatchingResources(cardId, cardName,
                ResourceType.CHASSIS);
        ParentResources parentResources = buildParentResources(chassis.getEquipmentId());
        return buildLocateResourcesOutput(matchingResources, parentResources, null);

    }

    private ParentResources buildParentResources(String chassisId) {
        log.debug("get chassis alarm locate resource parent resource,chassis id:{}", chassisId);
        String neId = PhysicalEqpIdNamingRule.getNodeId(chassisId);
        log.debug("relative ne resource,ne :{}", neId);
        Node phyNode = netconfTopology.getConfigPhyNode(neId);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical nePhysical = phyNode.getAugmentation(
                Node1.class).getPhysical();
        ParentResourcesBuilder parentResourcesBuilder = new ParentResourcesBuilder();
        //node
        ResourceBuilder neResourceBuilder = new ResourceBuilder();
        neResourceBuilder.setResourceName(nePhysical.getFriendlyName());
        neResourceBuilder.setResourceType(ResourceType.NE);
        neResourceBuilder.setResourceId(phyNode.getNodeId().getValue());
        List<Resource> resources = Collections.singletonList(neResourceBuilder.build());
        parentResourcesBuilder.setResource(resources);
        return parentResourcesBuilder.build();
    }

    @Override
    public NMSResourceType locateResourceType() {
        return NMSResourceType.CHASSIS;
    }
}
