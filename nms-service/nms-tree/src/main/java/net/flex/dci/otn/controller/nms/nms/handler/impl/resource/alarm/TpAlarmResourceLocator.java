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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/5/2025 10:36 AM
 */
@Component
@Slf4j
public class TpAlarmResourceLocator extends AbstractAlarmResourceLocator {


    @Override
    public LocateResourcesByAlarmOutput getLocateByAlarmResourceId(String alarmResourceId) {
        log.debug("locate resources tp by alarmResourceId:{}", alarmResourceId);
        TerminationPoint terminationPoint = netconfTopology.getTerminationPoint(alarmResourceId);
        if (terminationPoint == null) {
            return resourceRemove(alarmResourceId);
        } else {
            return getTerminationPointLocateResource(terminationPoint);
        }
    }

    private LocateResourcesByAlarmOutput getTerminationPointLocateResource(
            TerminationPoint terminationPoint) {
        log.debug("located termination point");
        Physical tpPhysical = terminationPoint.getAugmentation(
                TerminationPoint1.class).getPhysical();
        String tpId = terminationPoint.getTpId().getValue();
        log.debug("located termination point is:{} name is:{}", tpId, tpPhysical.getFriendlyName());

        MatchingResources matchingResources = buildMatchingResources(tpId,
                tpPhysical.getFriendlyName(), ResourceType.TERMINATIONPOINT);
        ParentResources parentResources = buildTpParentResources(tpId);
        return buildLocateResourcesOutput(matchingResources, parentResources, null);
    }

    /**
     * build tp parent resources
     *
     * @param tpId
     * @return
     */
    private ParentResources buildTpParentResources(String tpId) {
        log.debug("build tp parent resources the tpId is:{}", tpId);
        String refNeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        String equipId = PhysicalTpIdNamingRule.getEquipId(tpId);
        Node phyNode = netconfTopology.getConfigPhyNode(refNeId);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical nePhysical = phyNode.getAugmentation(
                Node1.class).getPhysical();
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
        //equipment ->chassis -> node
        ParentResourcesBuilder parentResourcesBuilder = new ParentResourcesBuilder();
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
        neResourceBuilder.setResourceId(phyNode.getNodeId().getValue());

        List<Resource> resources = Arrays.asList(equipmentResourceBuilder.build(),
                chassisResourceBuilder.build(), neResourceBuilder.build());
        parentResourcesBuilder.setResource(resources);
        return parentResourcesBuilder.build();
    }


    @Override
    public NMSResourceType locateResourceType() {
        return NMSResourceType.TERMINATIONPOINT;
    }
}
