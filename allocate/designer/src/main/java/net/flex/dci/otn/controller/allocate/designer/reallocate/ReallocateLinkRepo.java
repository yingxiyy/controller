package net.flex.dci.otn.controller.allocate.designer.reallocate;

import java.util.Map;
import java.util.Set;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NEIdGenerator;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReallocateLinkRepo implements ReallocateInterface<Link> {

    @Autowired
    private ReallocateTpRepo reallocateTpRepo;

    @Override
    public Link reallocate(Link link, Set<String> oldEquipIds, Map<String, String> reallocateEquipMap) {
        String linkIdValue = link.getLinkId().getValue();
        Source source = link.getSource();
        Destination destination = link.getDestination();
        LinkType linkType = link.getAugmentation(Link1.class).getPhysical().getLinkType();

        for (String oldEquipId : oldEquipIds) {
            String newEquipId = reallocateEquipMap.get(oldEquipId);

            String sourceTp = source.getSourceTp().getValue();
            //cableLink需要单独处理，因为cableLink中的panel需要跟着mux/muxPanel一起改变
            if (linkType.equals(LinkType.CableLink)) {
                if (linkIdValue.contains(oldEquipId)) {
                   String newSourceTp = reallocateTpRepo.getNewPanelTpId(sourceTp, newEquipId);
                    source = new SourceBuilder()
                            .setSourceTp(new TpId(newSourceTp))
                            .setSourceNode(new NodeId(NEIdGenerator.getNodeIdByTpId(newSourceTp)))
                            .build();
                    linkIdValue = linkIdValue.replace(sourceTp, newSourceTp);
                }
            }
            if (sourceTp.contains(oldEquipId)) {
                String newSourceTp = reallocateTpRepo.getNewTpId(sourceTp, newEquipId);
                source = new SourceBuilder()
                        .setSourceTp(new TpId(newSourceTp))
                        .setSourceNode(new NodeId(NEIdGenerator.getNodeIdByTpId(newSourceTp)))
                        .build();
                linkIdValue = linkIdValue.replace(sourceTp, newSourceTp);
                continue;
            }


            String destTp = destination.getDestTp().getValue();
            if (destTp.contains(oldEquipId)) {
                String newDestTp = reallocateTpRepo.getNewTpId(destTp, newEquipId);

                destination = new DestinationBuilder()
                        .setDestTp(new TpId(newDestTp))
                        .setDestNode(new NodeId(NEIdGenerator.getNodeIdByTpId(newDestTp)))
                        .build();
                linkIdValue = linkIdValue.replace(destTp, newDestTp);

            }
        }

        //Link
        LinkId linkId = new LinkId(linkIdValue);
        return new LinkBuilder().setLinkId(linkId)
                .setKey(new LinkKey(linkId))
                .setSource(source)
                .setDestination(destination)
                .addAugmentation(Link1.class, link.getAugmentation(Link1.class))
                .build();
    }

    @Override
    public String getId(Link item) {
        return item.getLinkId().getValue();
    }
}
