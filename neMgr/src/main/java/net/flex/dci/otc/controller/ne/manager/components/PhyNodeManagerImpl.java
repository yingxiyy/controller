package net.flex.dci.otc.controller.ne.manager.components;

import static net.flex.dci.otc.common.constants.Constants.HYPHEN;
import static net.flex.dci.otc.common.util.TopoNameConstants.Phy_Topo_Key;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.DefaultRack.DEFAULT_START_RACK;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.DefaultRack.MANUAL_DEFAULT_RACK_ID_PREFIX;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.DefaultRack.MANUAL_DEFAULT_RACK_PREFIX;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.HOSTNAME;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.RackDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CommunicationStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateNeInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SupervisionStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.lldp.attributes.Lldp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.lldp.attributes.LldpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.DcnBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuClientBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRackBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNe;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNeKey;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 7/26/2023 10:27 AM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PhyNodeManagerImpl implements PhyNodeManager {

    private final PhyNodeDao phyNodeDao;

    private final SiteNodeDao siteNodeDao;

    private final RackDao rackDao;


    @Override
    public Node mergeConfNeDataFromOp(Node configNode, String neId) {
        log.debug("merge the conf ne data from op data,the ne id is:{}", neId);
        Node opNode = phyNodeDao.getOpPhyNodeById(neId);
        Physical configNodePhysical = configNode.getAugmentation(Node1.class).getPhysical();
        Physical opNodePhysical = opNode.getAugmentation(Node1.class).getPhysical();
        NodeBuilder nodeBuilder = new NodeBuilder(configNode);
        PhysicalBuilder physicalBuilder = new PhysicalBuilder(configNodePhysical);
        DcnBuilder dcnBuilder = new DcnBuilder();
        physicalBuilder.setDcn(dcnBuilder.build());
        //update the properties
        List<Property> properties = new ArrayList<>();
        properties.add(new PropertyBuilder().setName(HOSTNAME)
                .setValue(configNodePhysical.getFriendlyName()).build());
        PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
        propertiesBuilder.setProperty(properties);
        physicalBuilder.setProperties(propertiesBuilder.build());
        physicalBuilder.setAdminState(AdminStatus.Unknown);
        physicalBuilder.setCrossConnections(opNodePhysical.getCrossConnections());
        physicalBuilder.setVendorType(opNodePhysical.getVendorType());
        physicalBuilder.setImplementState(ImplementState.Allocate);
        physicalBuilder.setStuffed(false);
        physicalBuilder.setInternalLinks(opNodePhysical.getInternalLinks());
        physicalBuilder.setOperationalState(OperStatus.Unknown);
        physicalBuilder.setAlarmState(AlarmSeverity.Unknown);
        physicalBuilder.setVendorName(opNodePhysical.getVendorName());
        physicalBuilder.setNodeType(opNodePhysical.getNodeType());
        physicalBuilder.setAlignmentStatus(AlignmentStatusType.Unknown);
        List<Equipments> rebuildEquipments = rebuildDefaultEquipState(
                opNodePhysical.getEquipments());
        physicalBuilder.setEquipments(rebuildEquipments);
        physicalBuilder.setSystem(NeManagerUtils.getDefaultSystem());
        List<TerminationPoint> terminationPoints = rebuildDefaultTpState(
                opNode.getTerminationPoint());
        nodeBuilder.setTerminationPoint(terminationPoints);
        Node1Builder node1Builder = new Node1Builder(configNode.getAugmentation(Node1.class));
        node1Builder.setPhysical(physicalBuilder.build());
        nodeBuilder.addAugmentation(Node1.class, node1Builder.build());

        return nodeBuilder.build();
    }

    private List<TerminationPoint> rebuildDefaultTpState(List<TerminationPoint> terminationPoints) {
        List<TerminationPoint> rebuildTerminationPoint = terminationPoints.stream()
                .map(terminationPoint -> {
                    TerminationPointBuilder terminationPointBuilder = new TerminationPointBuilder(
                            terminationPoint);
                    TerminationPoint1 terminationPoint1 = terminationPoint.getAugmentation(
                            TerminationPoint1.class);
                    TerminationPoint1Builder terminationPoint1Builder = new TerminationPoint1Builder(
                            terminationPoint1);
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpPhysical = terminationPoint1.getPhysical();
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder physicalBuilder = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
                            tpPhysical);
                    physicalBuilder.setAdminState(AdminStatus.Unknown);
                    physicalBuilder.setAlarmState(AlarmSeverity.Unknown);
                    physicalBuilder.setImplementState(ImplementState.Allocate);
                    physicalBuilder.setOperationalState(OperStatus.Unknown);
                    physicalBuilder.setAlignmentStatus(AlignmentStatusType.Unknown);
                    physicalBuilder.setLldp(defaultLldp());
                    if (physicalBuilder.getOtuClient() != null) {
                        OtuClientBuilder otuClientBuilder = new OtuClientBuilder(
                                physicalBuilder.getOtuClient());
                        physicalBuilder.setOtuClient(otuClientBuilder.build());
                    }
                    physicalBuilder.setProperties(new PropertiesBuilder().build());
                    terminationPoint1Builder.setPhysical(physicalBuilder.build());
                    terminationPointBuilder.addAugmentation(TerminationPoint1.class,
                            terminationPoint1Builder.build());
                    return terminationPointBuilder.build();
                }).collect(Collectors.toList());
        return rebuildTerminationPoint;
    }

    private Lldp defaultLldp() {
        LldpBuilder lldpBuilder = new LldpBuilder();
        lldpBuilder.setAdminStatus(AdminStatus.Unknown);
        lldpBuilder.setOperStatus(OperStatus.Unknown);
        lldpBuilder.setNeighbor(new ArrayList<>());
        return lldpBuilder.build();
    }

    private List<Equipments> rebuildDefaultEquipState(List<Equipments> equipments) {
        List<Equipments> rebuildEquips = equipments.stream().map(equipment -> {
            EquipmentsBuilder equipmentsBuilder = new EquipmentsBuilder(equipment);
            equipmentsBuilder.setAdminState(AdminStatus.Unknown);
            equipmentsBuilder.setAlarmState(AlarmSeverity.Unknown);
            equipmentsBuilder.setAlignmentStatus(AlignmentStatusType.Unknown);
            equipmentsBuilder.setOperationalState(OperStatus.Unknown);
            equipmentsBuilder.setImplementState(ImplementState.Allocate);
            equipmentsBuilder.setProperties(new PropertiesBuilder().build());
            return equipmentsBuilder.build();
        }).collect(Collectors.toList());
        return rebuildEquips;
    }

    @Override
    public Node createNewNode(CreateNeInput input) {
        try {
            log.info("start to create a new node,the input is:{}", input);
            NodeBuilder newNodeBuilder = new NodeBuilder();
            String siteId = input.getSiteId();
            Physical physical = input.getPhysical();
            String neId = PhysicalNodeIdNamingRule.getNewNodeId(siteId);
            newNodeBuilder.setNodeId(NodeId.getDefaultInstance(neId));
            newNodeBuilder.setKey(new NodeKey(NodeId.getDefaultInstance(neId)));
            PhysicalBuilder physicalBuilder = new PhysicalBuilder(physical);
            physicalBuilder.setCreationTime(NeManagerUtils.getCurrentTime());
            physicalBuilder.setSupervisionStatus(SupervisionStatusType.Monitoring);
            physicalBuilder.setCommunicationStatus(CommunicationStatusType.Broken);
            Node1Builder physicalNodeBuilder = new Node1Builder();
            physicalNodeBuilder.setPhysical(physicalBuilder.build());

            newNodeBuilder.addAugmentation(Node1.class, physicalNodeBuilder.build());
            return newNodeBuilder.build();
        } catch (Exception ex) {
            log.error("failed to create new node,the reason is:{}", ex.getMessage(), ex);
            throw ex;
        }
    }

    @Override
    public Node mountNeToSite(String neId) {
        String siteId = PhysicalNodeIdNamingRule.getSiteId(neId);
        log.debug("start to mount ne to Site,the neId is:{} ,the site Id is:{}", neId, siteId);
        Node refSiteNode = siteNodeDao.getSiteNodeById(siteId);
        Site sitePhysical = refSiteNode.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                .getSite();
        List<SupportingRack> supportingRacks =
                sitePhysical.getSupportingRack() != null ? sitePhysical.getSupportingRack()
                        : new ArrayList<>();

        List<SupportingRack> newSupportingRacks = supportingRacks.stream()
                .filter(supportingRack -> !supportingRack.getFriendlyName()
                        .startsWith(MANUAL_DEFAULT_RACK_PREFIX)).collect(Collectors.toList());
        Optional<SupportingRack> optionalManualRack = supportingRacks.stream()
                .filter(supportingRack -> supportingRack.getFriendlyName()
                        .startsWith(MANUAL_DEFAULT_RACK_PREFIX)).findAny();
        SupportingRack manualRack = null;
        if (optionalManualRack.isPresent()) {
            manualRack = optionalManualRack.get();
            List<SupportingNe> supportingNes = manualRack.getSupportingNe();
            int minLocation = supportingRacks.stream()
                    .mapToInt(supportingNe -> Integer.parseInt(supportingNe.getLocation())).min()
                    .orElse(36);
            int newLocation = minLocation - 3;
            SupportingNeBuilder supportingNeBuilder = new SupportingNeBuilder();
            supportingNeBuilder.setNodeRef(NodeId.getDefaultInstance(neId));
            supportingNeBuilder.setKey(new SupportingNeKey(NodeId.getDefaultInstance(neId)));
            supportingNeBuilder.setLocation(String.valueOf(newLocation));
            supportingNes.add(supportingNeBuilder.build());
        } else {
            manualRack = createManualSupportingRack(neId, 1);
        }
        newSupportingRacks.add(manualRack);
        List<SupportingNode> supportingNodes = refSiteNode.getSupportingNode();
        SupportingNodeBuilder supportingNodeBuilder = new SupportingNodeBuilder();
        supportingNodeBuilder.setNodeRef(NodeId.getDefaultInstance(neId));
        supportingNodeBuilder.setTopologyRef(TopologyId.getDefaultInstance(Phy_Topo_Key));
        supportingNodes.add(supportingNodeBuilder.build());

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder siteNode1Builder = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder(
                refSiteNode.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class));
        SiteBuilder siteBuilder = new SiteBuilder(sitePhysical);
        siteBuilder.setSupportingRack(newSupportingRacks);
        siteNode1Builder.setSite(siteBuilder.build());

        NodeBuilder siteNodeBuilder = new NodeBuilder(refSiteNode);
        siteNodeBuilder.setSupportingNode(supportingNodes);
        siteNodeBuilder.addAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class,
                siteNode1Builder.build());
        siteNodeDao.rewriteSiteNode(siteNodeBuilder.build());
        return siteNodeBuilder.build();
    }

    private SupportingRack createManualSupportingRack(String neId, int id) {
        log.debug("start to create manual supporting rack ,the neId :{},rack small id is:{}", neId,
                id);
        String siteId = PhysicalNodeIdNamingRule.getSiteId(neId);
        SupportingRackBuilder supportingRackBuilder = new SupportingRackBuilder();
        supportingRackBuilder.setAlarmState(AlarmSeverity.Unknown);
        supportingRackBuilder.setRackId(
                Uri.getDefaultInstance(MANUAL_DEFAULT_RACK_ID_PREFIX + siteId + HYPHEN + id));
        supportingRackBuilder.setFriendlyName(MANUAL_DEFAULT_RACK_PREFIX + id);
        supportingRackBuilder.setGlobalIdentify(
                PhysicalNodeIdNamingRule.getSiteId(neId) + HYPHEN + MANUAL_DEFAULT_RACK_ID_PREFIX
                        + id);
        SupportingNeBuilder supportingNeBuilder = new SupportingNeBuilder();
        supportingNeBuilder.setNodeRef(NodeId.getDefaultInstance(neId));
        supportingNeBuilder.setKey(new SupportingNeKey(NodeId.getDefaultInstance(neId)));
        supportingNeBuilder.setLocation(String.valueOf(DEFAULT_START_RACK));
        supportingRackBuilder.setSupportingNe(
                Collections.singletonList(supportingNeBuilder.build()));
        return supportingRackBuilder.build();
    }


}
