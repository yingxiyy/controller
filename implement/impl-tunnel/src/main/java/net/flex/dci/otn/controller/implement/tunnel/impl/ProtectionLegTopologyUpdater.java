package net.flex.dci.otn.controller.implement.tunnel.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.ViewLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRackBuilder;

/** Database-only mutations needed after a protection-leg device removal succeeds. */
final class ProtectionLegTopologyUpdater {

    private ProtectionLegTopologyUpdater() {
    }

    static Link removeOchFromSiteLink(Link siteLink, Link ochLink) {
        Site site = siteLink.getAugmentation(Link1.class).getSite();
        Och och = ochLink.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                .getOch();
        Available released = new AvailableBuilder()
                .setLowerFrequency(och.getLowerFrequency())
                .setUpperFrequency(och.getUpperFrequency())
                .setKey(new AvailableKey(och.getLowerFrequency()))
                .build();
        FrequencyAvailable frequencies = new FrequencyAvailable(siteLink);
        frequencies.add(released);

        return new LinkBuilder(siteLink).addAugmentation(Link1.class, new Link1Builder()
                .setSite(new SiteBuilder(site)
                        .setAvailable(frequencies.getAvailableList())
                        .setSupportedLink(site.getSupportedLink().stream()
                                .filter(link -> !ochLink.getLinkId().equals(link.getLinkRef()))
                                .collect(Collectors.toList()))
                        .setBandwidth(String.valueOf(Integer.parseInt(site.getBandwidth()) + 1))
                        .build())
                .build()).build();
    }

    static void removePhyLinkKeepXc(ChangedObject changedObject, String linkId) {
        Link link = changedObject.getChangedPhyLink(linkId);
        if (link == null) {
            return;
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical
                linkPhysical = link.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                .getPhysical();
        if (LinkType.OtsLink.equals(linkPhysical.getLinkType())
                || LinkType.OsLink.equals(linkPhysical.getLinkType())) {
            // Match the original phy-link cleanup: derived view links cannot outlive their physical link.
            ViewLinkDao viewLinkDao = SpringBeanFinder.getBean(ViewLinkDao.class);
            viewLinkDao.getViewLinkBySupportingLinks(Collections.singletonList(linkId))
                    .forEach(changedObject::addRemovedViewLink);
        }
        changedObject.addRemovedPhyLink(linkId);
        cleanLinkEndpoint(changedObject, PhysicalLinkIdNamingRule.getTpAId(linkId), linkId);
        cleanLinkEndpoint(changedObject, PhysicalLinkIdNamingRule.getTpZId(linkId), linkId);
    }

    private static void cleanLinkEndpoint(ChangedObject changedObject, String tpId, String linkId) {
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        Node node = changedObject.getChangedPhyNode(nodeId);
        if (node == null) {
            return;
        }
        List<TerminationPoint> tps = node.getTerminationPoint() == null
                ? Collections.emptyList() : node.getTerminationPoint().stream()
                .map(tp -> tpId.equals(tp.getTpId().getValue()) ? cleanTpState(tp) : tp)
                .collect(Collectors.toList());
        Physical physical = node.getAugmentation(Node1.class).getPhysical();
        List<InternalLinks> internalLinks = physical.getInternalLinks() == null
                ? Collections.emptyList() : physical.getInternalLinks().stream()
                .filter(internalLink -> !linkId.equals(internalLink.getLinkRef()))
                .collect(Collectors.toList());
        changedObject.addChangedPhyNode(new NodeBuilder(node).setTerminationPoint(tps)
                .addAugmentation(Node1.class, new Node1Builder(node.getAugmentation(Node1.class))
                        .setPhysical(new PhysicalBuilder(physical).setInternalLinks(internalLinks).build())
                        .build()).build());
    }

    private static TerminationPoint cleanTpState(TerminationPoint tp) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical old =
                tp.getAugmentation(TerminationPoint1.class).getPhysical();
        Properties retained = null;
        if (old.getProperties() != null && old.getProperties().getProperty() != null) {
            List<Property> slotFrequency = old.getProperties().getProperty().stream()
                    .filter(property -> "slot".equals(property.getName())
                            && property.getValue() != null
                            && property.getValue().contains("/frequency"))
                    .map(PropertyBuilder::new).map(PropertyBuilder::build)
                    .collect(Collectors.toList());
            if (!slotFrequency.isEmpty()) {
                retained = new PropertiesBuilder().setProperty(slotFrequency).build();
            }
        }
        return new TerminationPointBuilder(tp)
                .addAugmentation(TerminationPoint1.class,
                        new TerminationPoint1Builder(tp.getAugmentation(TerminationPoint1.class))
                                .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(old)
                                        .setAdminState(AdminStatus.Unknown)
                                        .setConnectionStatus(ConnectionStatus.Idle)
                                        .setOtuLine(null).setOtuClient(null)
                                .setProperties(retained == null
                                        ? new PropertiesBuilder().setProperty(Collections.emptyList()).build()
                                        : retained)
                                .build())
                                .build())
                .build();
    }

    static Node removeXc(Node node,
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes removed) {
        Physical physical = node.getAugmentation(Node1.class).getPhysical();
        String removedId = removed.getCrossConnectionId().getValue();
        String neId = removedId.endsWith("MPO") ? expandedMpoXcId(removed) : removedId;
        List<CrossConnections> xcs = physical.getCrossConnections() == null
                ? Collections.emptyList() : physical.getCrossConnections().stream()
                .filter(xc -> !neId.equals(xc.getCrossConnectionId().getValue()))
                .collect(Collectors.toList());
        return withPhysical(node, new PhysicalBuilder(physical).setCrossConnections(xcs).build());
    }

    private static String expandedMpoXcId(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes xc) {
        List<String> tpIds = new ArrayList<>();
        xc.getSourceTp().forEach(tp -> addExpandedMpoTp(tp.getTpRef().getValue(), tpIds));
        xc.getDestinationTp().forEach(tp -> addExpandedMpoTp(tp.getTpRef().getValue(), tpIds));
        Collections.sort(tpIds);
        return tpIds.stream().collect(Collectors.joining("-", "XC-", ""));
    }

    private static void addExpandedMpoTp(String tpId, List<String> result) {
        if (!tpId.endsWith("MPO")) {
            result.add(tpId);
            return;
        }
        for (int index = 1; index <= 8; index++) {
            result.add(tpId.replaceAll("MPO$", "MPO" + index));
        }
    }

    static Node markRegEquipmentEmpty(Node node, String equipmentId) {
        Physical physical = node.getAugmentation(Node1.class).getPhysical();
        String transceiverPrefix = equipmentId.replaceFirst("LINECARD", "TRANSCEIVER");
        List<Equipments> equipments = physical.getEquipments().stream().map(equipment -> {
            if (!equipmentId.equals(equipment.getEquipmentId())) {
                return equipment;
            }
            return new EquipmentsBuilder()
                    .setEquipmentId(equipmentId).setEquipType(EquipType.EMPTY)
                    .setImplementState(ImplementState.Allocate).setAdminState(AdminStatus.Up)
                    .setSlot(equipment.getSlot()).setShelf(equipment.getShelf())
                    .setCreationTime(equipment.getCreationTime()).setEquipTypeConfiged("BLANK")
                    .setEquipTypeVendorSpecific("BLANK").setOperationalState(OperStatus.Unknown)
                    .setAlarmState(AlarmSeverity.Unknown).setAlignmentStatus(AlignmentStatusType.Unknown)
                    .setFriendlyName(String.format("SLOT-%s-%s", equipment.getShelf(), equipment.getSlot()))
                    .setEmpty(true).setNodeRef(equipment.getNodeRef())
                    .setProperties(new PropertiesBuilder().setProperty(new ArrayList<>()).build())
                    .build();
        }).filter(Objects::nonNull)
                // A released REG line card must not leave its generated transceiver equipment behind.
                .filter(equipment -> !equipment.getEquipmentId().startsWith(transceiverPrefix))
                .collect(Collectors.toList());
        List<CrossConnections> xcs = (physical.getCrossConnections() == null
                ? Collections.<CrossConnections>emptyList() : physical.getCrossConnections()).stream()
                .filter(xc -> !xc.getCrossConnectionId().getValue().contains(equipmentId))
                .collect(Collectors.toList());
        List<InternalLinks> internalLinks = (physical.getInternalLinks() == null
                ? Collections.<InternalLinks>emptyList() : physical.getInternalLinks()).stream()
                .filter(link -> !link.getLinkRef().contains(equipmentId)).collect(Collectors.toList());
        List<TerminationPoint> tps = (node.getTerminationPoint() == null
                ? Collections.<TerminationPoint>emptyList() : node.getTerminationPoint()).stream()
                .filter(tp -> !tp.getTpId().getValue().contains(equipmentId)).collect(Collectors.toList());
        return new NodeBuilder(node).setTerminationPoint(tps)
                .addAugmentation(Node1.class, new Node1Builder(node.getAugmentation(Node1.class))
                        .setPhysical(new PhysicalBuilder(physical).setEquipments(equipments)
                                .setCrossConnections(xcs).setInternalLinks(internalLinks)
                                .setStuffed(false).build()).build()).build();
    }

    static boolean isEmptyNode(Node node) {
        if (node == null) {
            return true;
        }
        Physical physical = node.getAugmentation(Node1.class).getPhysical();
        return physical.getEquipments() == null || physical.getEquipments().stream()
                .filter(equipment -> equipment.getEquipmentId().contains("LINECARD"))
                .allMatch(equipment -> equipment.getEquipType() == null
                        || EquipType.EMPTY.equals(equipment.getEquipType()));
    }

    static boolean hasIp(Node node) {
        return node != null && node.getAugmentation(Node1.class).getPhysical().getIp() != null;
    }

    static Node removeNodeFromSite(Node siteNode, String nodeId) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1 siteAug =
                siteNode.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site site =
                siteAug.getSite();
        List<SupportingNode> supportingNodes = siteNode.getSupportingNode() == null
                ? Collections.emptyList() : siteNode.getSupportingNode().stream()
                .filter(node -> !nodeId.equals(node.getNodeRef().getValue())).collect(Collectors.toList());
        List<SupportingRack> racks = site.getSupportingRack() == null
                ? Collections.emptyList() : site.getSupportingRack().stream().map(rack -> {
                    List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNe> nes =
                            rack.getSupportingNe() == null ? Collections.emptyList()
                                    : rack.getSupportingNe().stream()
                                    .filter(ne -> !nodeId.equals(ne.getNodeRef().getValue()))
                                    .collect(Collectors.toList());
                    return new SupportingRackBuilder(rack).setSupportingNe(nes).build();
                }).filter(rack -> !rack.getSupportingNe().isEmpty()).collect(Collectors.toList());
        return new NodeBuilder(siteNode).setSupportingNode(supportingNodes)
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder(siteAug)
                                .setSite(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder(site)
                                        .setSupportingRack(racks).build()).build())
                .build();
    }

    private static Node withPhysical(Node node, Physical physical) {
        return new NodeBuilder(node).addAugmentation(Node1.class,
                new Node1Builder(node.getAugmentation(Node1.class)).setPhysical(physical).build()).build();
    }
}
