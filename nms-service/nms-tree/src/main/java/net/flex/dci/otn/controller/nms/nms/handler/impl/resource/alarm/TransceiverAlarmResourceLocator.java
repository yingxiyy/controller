package net.flex.dci.otn.controller.nms.nms.handler.impl.resource.alarm;

import static net.flex.dci.otn.controller.nms.utils.Constants.CHASSIS_INFIX;

import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.enums.NMSResourceType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.LocateResourcesByAlarmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ResourceEntity.ResourceType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.MatchingResources;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.ParentResources;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.ParentResourcesBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.parent.resources.Resource;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.parent.resources.ResourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/5/2025 1:55 PM
 */
@Component
@Slf4j
public class TransceiverAlarmResourceLocator extends AbstractAlarmResourceLocator {

    @Override
    public LocateResourcesByAlarmOutput getLocateByAlarmResourceId(String alarmResourceId) {
        log.debug("get transceiver alarm resource locator id:{}", alarmResourceId);
        Equipments transceiver = netconfTopology.getEquipment(
                alarmResourceId);
        if (transceiver == null) {
            return resourceRemove(alarmResourceId);
        } else {
            return getTransceiverAlarmResource(transceiver);
        }

    }

    private LocateResourcesByAlarmOutput getTransceiverAlarmResource(Equipments transceiver) {
        log.debug("get transceiver alarm resource resource:{}", transceiver.getEquipmentId());
        MatchingResources matchingResources = buildMatchingResources(transceiver.getEquipmentId(),
                transceiver.getFriendlyName(), ResourceType.TRANSCEIVER);
        ParentResources parentResources = buildTransceiverParentResource(
                transceiver.getEquipmentId());
        return buildLocateResourcesOutput(matchingResources, parentResources, null);
    }

    private ParentResources buildTransceiverParentResource(String transceiverId) {
        log.debug("build transceiver parent resource transceiver id :{}", transceiverId);
        String transceiverRefPortId = PhysicalTpIdNamingRule.getPortIdFromTransceiverId(
                transceiverId);
        String neId = PhysicalTpIdNamingRule.getNodeId(transceiverRefPortId);
        String equipId = PhysicalTpIdNamingRule.getEquipId(transceiverRefPortId);
        Node ne = netconfTopology.getConfigPhyNode(neId);
        Physical nePhysical = ne.getAugmentation(
                Node1.class).getPhysical();
        TerminationPoint refTp = ne.getTerminationPoint().stream()
                .filter(terminationPoint -> terminationPoint.getTpId().getValue()
                        .equals(transceiverRefPortId)).findAny()
                .orElseThrow(() -> new CommonException(
                        CommonExceptionType.NOT_FOUND_ERROR,
                        "notFound tp: " + transceiverRefPortId));
        List<Equipments> equipments = nePhysical.getEquipments();
        Equipments equipment = equipments.stream()
                .filter(equip -> equip.getEquipmentId().equals(equipId))
                .findAny().orElseThrow(
                        () -> new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                                "notFound: " + equipId));
        //only one chassis
        Equipments chassis = equipments.stream().filter(equip -> equip.getEquipmentId().contains(
                CHASSIS_INFIX)).findAny().orElseThrow(
                () -> new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                        "notFound chassis"));
        //tp->equipment ->chassis -> node
        ParentResourcesBuilder parentResourcesBuilder = new ParentResourcesBuilder();
        //tp
        ResourceBuilder terminationPointResourceBuilder = new ResourceBuilder();
        terminationPointResourceBuilder.setResourceId(refTp.getTpId().getValue());
        terminationPointResourceBuilder.setResourceType(ResourceType.TERMINATIONPOINT);
        terminationPointResourceBuilder.setResourceName(
                refTp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName());
        //equipment
        ResourceBuilder equipmentResourceBuilder = new ResourceBuilder();
        equipmentResourceBuilder.setResourceId(equipment.getEquipmentId());
        equipmentResourceBuilder.setResourceType(ResourceType.CARD);
        equipmentResourceBuilder.setResourceName(equipment.getFriendlyName());
        //chassis
        ResourceBuilder chassisResourceBuilder = new ResourceBuilder();
        chassisResourceBuilder.setResourceType(ResourceType.CHASSIS);
        chassisResourceBuilder.setResourceId(chassis.getEquipmentId());
        chassisResourceBuilder.setResourceName(chassis.getFriendlyName());
        //node
        ResourceBuilder neResourceBuilder = new ResourceBuilder();
        neResourceBuilder.setResourceName(nePhysical.getFriendlyName());
        neResourceBuilder.setResourceType(ResourceType.NE);
        neResourceBuilder.setResourceId(ne.getNodeId().getValue());

        List<Resource> resources = Arrays.asList(terminationPointResourceBuilder.build(),
                equipmentResourceBuilder.build(),
                chassisResourceBuilder.build(), neResourceBuilder.build());
        parentResourcesBuilder.setResource(resources);
        return parentResourcesBuilder.build();
    }

    @Override
    public NMSResourceType locateResourceType() {
        return NMSResourceType.TRANSCEIVER;
    }
}
