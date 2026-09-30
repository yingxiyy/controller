package net.flex.dci.otn.controller.allocate.link.och;

import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.CrossConnectionSlotNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.common.util.ocm.LinkRoute;
import net.flex.dci.otc.common.util.ocm.OcmTool;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class OcmUpdater {

    private ChangedObject changedObject;

    public OcmUpdater(ChangedObject changedObject) {
        this.changedObject = changedObject;
    }

    //TODO--------yyx
    public void updateOcmGroup(Link ochLink, CrossConnectionAttributes xc, boolean remove) {
//        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
//        String nodeId = xc.getNodeRef().getValue();
//        Node node = changedObject.getChangedPhyNode(nodeId);
//        String ocmId = CrossConnectionSlotNamingRule.getOcmNameOverAmplifierXc(xc);
//        if (remove == true) {
//            node = OcmTool.removeOcmOnNe(node, ocmId, ochLinkAttr.getLowerFrequency());
//        } else {
//            node = OcmTool.addOcmOnNe(node, ocmId, ochLinkAttr.getLowerFrequency(), ochLinkAttr.getUpperFrequency(), updatedOcmGroup.get(nodeId));
//        }
//
//        changedObject.addChangedPhyNode(node);
    }

    private Map<String, List<OCMGripGroups>> updatedOcmGroup = new HashMap<>();

    public OcmUpdater setOldOcmGroupMap(Map<String, List<OCMGripGroups>> updatedOcmGroup) {
        this.updatedOcmGroup = updatedOcmGroup;
        return this;
    }

    public void updateOcmGroup(Link ochLink, boolean remove) {
        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();

        List<CrossConnectionAttributes> amplifierXCs = new ArrayList<>();
        for (SupportingLink sl : ochLink.getSupportingLink()) {
            if (SiteLinkIdNamingRule.isSiteLink(sl.getLinkRef().getValue())) {
                Link siteLink = changedObject.getChangedSiteLink(sl.getLinkRef().getValue());
                Site siteLinkAttr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
                if (!siteLinkAttr.getGrid().equals(GridType._0)) {
                    //only flex grid need remove or add
                    continue;
                }

                amplifierXCs.addAll(LinkRoute.getAmplifierXCList(siteLink));
                for (CrossConnectionAttributes xc : amplifierXCs) {
//                    String ocmId = CrossConnectionSlotNamingRule.getOcmNameOverAmplifierXc(xc);
//                    String nodeId = PhysicalEqpIdNamingRule.getNodeId(xc.getNodeRef().getValue());
//                    Node node = changedObject.getChangedPhyNode(nodeId);
//                    if (remove) {
//                        node = OcmTool.removeOcmOnNe(node, ocmId, ochLinkAttr.getLowerFrequency());
//                    } else {
//                        node = OcmTool.addOcmOnNe(node, ocmId, ochLinkAttr.getLowerFrequency(), ochLinkAttr.getUpperFrequency(), updatedOcmGroup.get(nodeId));
//                    }
//                    changedObject.addChangedPhyNode(node);
                }
            }
        }
    }
}
