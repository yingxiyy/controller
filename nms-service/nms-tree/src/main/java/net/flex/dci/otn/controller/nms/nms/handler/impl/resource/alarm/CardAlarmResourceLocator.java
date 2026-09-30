package net.flex.dci.otn.controller.nms.nms.handler.impl.resource.alarm;

import static net.flex.dci.otn.controller.nms.utils.Constants.CHASSIS_INFIX;

import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
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
 * @date 8/5/2025 10:37 AM
 */
@Component
@Slf4j
public class CardAlarmResourceLocator extends AbstractAlarmResourceLocator {

    @Override
    public LocateResourcesByAlarmOutput getLocateByAlarmResourceId(String alarmResourceId) {
        log.debug("locate card alarm resources by id:{}", alarmResourceId);
        Equipments cardEquip = netconfTopology.getEquipment(
                alarmResourceId);
        if (cardEquip == null) {
            return resourceRemove(alarmResourceId);
        } else {
            return getCardAlarmLocateResource(cardEquip);
        }
    }

    /**
     * @param cardEquip
     * @return
     */
    private LocateResourcesByAlarmOutput getCardAlarmLocateResource(Equipments cardEquip) {
        log.debug("get card alarm locate resource,resource is :{}", cardEquip.getEquipmentId());
        String cardName = cardEquip.getFriendlyName();
        String cardId = cardEquip.getEquipmentId();
        MatchingResources matchingResources = buildMatchingResources(cardId, cardName,
                ResourceType.CARD);
        ParentResources parentResources = buildParentResources(cardId);
        return buildLocateResourcesOutput(matchingResources, parentResources, null);
    }

    private ParentResources buildParentResources(String cardId) {
        log.debug("build equipment(card) parent resources the tpId is:{}", cardId);
        String neId = PhysicalEqpIdNamingRule.getNodeId(cardId);
        Node phyNode = netconfTopology.getConfigPhyNode(neId);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical nePhysical = phyNode.getAugmentation(
                Node1.class).getPhysical();
        List<Equipments> equipments = nePhysical.getEquipments();

        ParentResourcesBuilder parentResourcesBuilder = new ParentResourcesBuilder();
        //only one chassis
        Equipments chassis = equipments.stream().filter(equip -> equip.getEquipmentId().contains(
                CHASSIS_INFIX)).findAny().orElseThrow(
                () -> new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                        "notFound chassis"));

        //chassis
        ResourceBuilder chassisResourceBuilder = new ResourceBuilder();
        chassisResourceBuilder.setResourceType(ResourceType.CHASSIS);
        chassisResourceBuilder.setResourceId(chassis.getEquipmentId());
        chassisResourceBuilder.setResourceName(chassis.getFriendlyName());
        //node
        ResourceBuilder neResourceBuilder = new ResourceBuilder();
        neResourceBuilder.setResourceName(nePhysical.getFriendlyName());
        neResourceBuilder.setResourceType(ResourceType.NE);
        neResourceBuilder.setResourceId(phyNode.getNodeId().getValue());
        List<Resource> resources = Arrays.asList(
                chassisResourceBuilder.build(), neResourceBuilder.build());
        parentResourcesBuilder.setResource(resources);
        return parentResourcesBuilder.build();
    }


    @Override
    public NMSResourceType locateResourceType() {
        return NMSResourceType.CARD;
    }
}
