package net.flex.dci.otn.controller.nms.nms.handler.impl.resource.alarm;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.enums.NMSResourceType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.LocateResourcesByAlarmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ResourceEntity.ResourceType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.locate.resources.by.alarm.output.MatchingResources;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/5/2025 10:37 AM
 */
@Component
@Slf4j
public class NeAlarmResourceLocator extends AbstractAlarmResourceLocator {

    @Override
    public LocateResourcesByAlarmOutput getLocateByAlarmResourceId(String alarmResourceId) {
        log.debug("located the relative ne alarm resource id:{}", alarmResourceId);
        Node refNode = netconfTopology.getNeNode(
                alarmResourceId);
        LocateResourcesByAlarmOutput alarmOutput = null;
        if (refNode == null) {
            alarmOutput = resourceRemove(alarmResourceId);
        } else {
            alarmOutput = locateResource(refNode);
        }
        return alarmOutput;
    }


    private LocateResourcesByAlarmOutput locateResource(Node refNode) {
        log.debug("locate alarm locate resources:{}", refNode.getNodeId().getValue());
        Physical physical = refNode.getAugmentation(
                Node1.class).getPhysical();
        String neName = physical.getFriendlyName();
        String neId = refNode.getNodeId().getValue();
        MatchingResources matchingResources = buildMatchingResources(neId, neName, ResourceType.NE);
        return buildLocateResourcesOutput(matchingResources, null, null);
    }

    @Override
    public NMSResourceType locateResourceType() {
        return NMSResourceType.NODE;
    }
}
