package net.flex.dci.otn.controller.implement.site.nbi.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.ase.AseInjectModeUpdator;
import net.flex.dci.otn.controller.implement.common.config.ImplConfig;
import net.flex.dci.otn.controller.implement.common.utils.RouteExtractor;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RemoveResourceOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.ne.result.FailObj;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ResidualWssXcCleaner {

    private final String siteLinkId;
    private final List<String> siteLinkRouteNodeIds;
    private final Long taskGroupId;

    private final PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
    private final OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);
    private final NeManagerRpc neManagerRpc = SpringBeanFinder.getBean(NeManagerRpc.class);
    private final ImplConfig implConfig = SpringBeanFinder.getBean(ImplConfig.class);

    public ResidualWssXcCleaner(String siteLinkId, List<String> siteLinkRouteNodeIds,
            Long taskGroupId) {
        this.siteLinkId = siteLinkId;
        this.siteLinkRouteNodeIds = siteLinkRouteNodeIds == null ? Collections.emptyList() : new ArrayList<>(siteLinkRouteNodeIds);
        this.taskGroupId = taskGroupId;
    }

    public void cleanAfterSiteLinkDeimplemented() {
        Map<String, List<String>> residualWssXcMap = findResidualWssXcFromOpNode();
        if (residualWssXcMap.isEmpty()) {
            return;
        }

        log.info("clean residual wssChannel XC after siteLink deimplement, siteLink={}, xc={}",
                siteLinkId, residualWssXcMap);

        setMCSrc2DstPowerControlModel(residualWssXcMap);
        residualWssXcMap.forEach(this::deleteResidualWssXcFromNe);
    }

    public void cleanConfigResidualDummyAseXc(ChangedObject changedObject) {
        Set<String> referencedDummyAseXcIds = collectReferencedDummyAseXcIds(changedObject);
        for (String nodeId : siteLinkRouteNodeIds.stream().distinct().collect(Collectors.toList())) {
            Node cfgNode = changedObject.getChangedPhyNode(nodeId);
            Node cleanedNode = removeUnreferencedDummyAseXcs(cfgNode, referencedDummyAseXcIds);
            if (cleanedNode != cfgNode) {
                log.info("clean residual dummy ASE XC in config node after siteLink deimplement, siteLink={}, node={}",
                        siteLinkId, nodeId);
                changedObject.addChangedPhyNode(cleanedNode);
            }
        }
    }

    private Set<String> collectReferencedDummyAseXcIds(ChangedObject changedObject) {
        Set<String> referencedXcIds = new HashSet<>();
        for (String nodeId : siteLinkRouteNodeIds.stream().distinct().collect(Collectors.toList())) {
            List<Link> ochLinks = ochLinkDao.queryWithNode(nodeId);
            for (Link ochLink : ochLinks) {
                String ochLinkId = ochLink.getLinkId().getValue();
                if (OchLinkIdNamingRule.isOchBusinessLink(ochLinkId) || changedObject.isRemovedOchLink(ochLinkId)) {
                    continue;
                }
                referencedXcIds.addAll(extractRouteXcIds(ochLink));
            }
        }
        return referencedXcIds;
    }

    private Set<String> extractRouteXcIds(Link ochLink) {
        if (ochLink == null
                || ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class) == null) {
            return Collections.emptySet();
        }

        Och och = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();
        if (och == null || och.getExplictRoute() == null || och.getExplictRoute().getRoute() == null) {
            return Collections.emptySet();
        }

        return RouteExtractor.extractorXc(och.getExplictRoute().getRoute()).stream()
                .map(xc -> xc.getCrossConnectionId().getValue())
                .collect(Collectors.toSet());
    }

    static Node removeUnreferencedDummyAseXcs(Node node, Set<String> referencedDummyAseXcIds) {
        Physical physical = getPhysicalFromNode(node);
        if (physical == null || physical.getCrossConnections() == null) {
            return node;
        }

        List<CrossConnections> xcs = physical.getCrossConnections();
        List<CrossConnections> cleanedXcs = xcs.stream()
                .filter(xc -> !isUnreferencedDummyAseXc(xc, referencedDummyAseXcIds))
                .collect(Collectors.toList());
        if (cleanedXcs.size() == xcs.size()) {
            return node;
        }

        return new NodeBuilder(node)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(physical)
                                .setCrossConnections(cleanedXcs)
                                .build())
                        .build())
                .build();
    }

    private static boolean isUnreferencedDummyAseXc(CrossConnectionAttributes xc,
            Set<String> referencedDummyAseXcIds) {
        if (xc == null || xc.getCrossConnectionId() == null) {
            return false;
        }

        String xcId = xc.getCrossConnectionId().getValue();
        String description = xc.getDescription();
        // Dummy ASE XCs are generated for fake OCHs with descriptions such as
        // ASE-IRA_CL/ASE-DGE_CL. Business WSS XCs can also use the ASEXC prefix,
        // so the description guard prevents deleting business OCH WSS data.
        return xcId.startsWith("ASEXC-")
                && description != null
                && description.startsWith("ASE-")
                && !referencedDummyAseXcIds.contains(xcId);
    }

    private Map<String, List<String>> findResidualWssXcFromOpNode() {
        return siteLinkRouteNodeIds.stream()
                .distinct()
                .map(nodeId -> new java.util.AbstractMap.SimpleEntry<>(nodeId, findResidualWssXcOnNode(nodeId)))
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<String> findResidualWssXcOnNode(String nodeId) {
        Node opNode = phyNodeDao.getOpPhyNodeById(nodeId);
        return findResidualWssXcOnNode(opNode);
    }

    static List<String> findResidualWssXcOnNode(Node opNode) {
        Physical opPhysical = getPhysicalFromNode(opNode);
        if (opPhysical == null || opPhysical.getCrossConnections() == null) {
            return Collections.emptyList();
        }

        return opPhysical.getCrossConnections().stream()
                .filter(xc -> xc.getWssChannel() != null)
                .filter(xc -> AdminStatus.Up.equals(xc.getAdminState()))
                // This final cleanup runs after the normal delete result has been
                // stored and merged to OP, so only WSS XCs still active in OP are
                // treated as device residues that need another delete attempt.
                .filter(xc -> xc.getCrossConnectionId() != null)
                .map(xc -> xc.getCrossConnectionId().getValue())
                .collect(Collectors.toList());
    }

    private void setMCSrc2DstPowerControlModel(Map<String, List<String>> residualWssXcMap) {
        // 兜底删除仍然需要先置 MANUAL，否则设备功率调整中可能拒绝删除 media-channel。
        // 使用独立 ChangedObject，避免 OP 残留对象被重新保存回 CFG/OP 数据库。
        new AseInjectModeUpdator(new ChangedObject()).updateMCSrc2DstPowerControlModel(
                residualWssXcMap,
                siteLinkId,
                taskGroupId);
        log.debug("residual wssChannel XC power-control-mode set to {} before delete",
                AseInjectModeUpdator.MC_POWER_CONTROL_MODEL_MANUAL);
    }

    private void deleteResidualWssXcFromNe(String nodeId, List<String> xcIds) {
        Node deleteNode = buildDeleteNode(nodeId, new HashSet<>(xcIds));
        if (deleteNode == null) {
            return;
        }

        Physical physical = getPhysical(deleteNode);
        if (physical.getIp() == null && implConfig != null && implConfig.isWriteWithoutIP()) {
            log.info("no IP, skip residual wssChannel XC delete on node {}", nodeId);
            return;
        }

        //残留交叉必须通过 removeResource 真正删除。
        RemoveResourceOutput output = neManagerRpc.removeResource(deleteNode);
        if (output != null && output.getFailObj() != null
                && output.getFailObj().getObject() != null
                && !output.getFailObj().getObject().isEmpty()) {
            String error = convertFailObj(output.getFailObj());
            log.error("delete residual wssChannel XC failed, node={}, error={}", nodeId, error);
            throw new RuntimeException(String.format(
                    "delete residual wssChannel XC failed, node=%s, error=%s", nodeId, error));
        }
    }

    private Node buildDeleteNode(String nodeId, Set<String> xcIds) {
        Node opNode = phyNodeDao.getOpPhyNodeById(nodeId);
        Physical opPhysical = getPhysical(opNode);
        if (opNode == null || opPhysical == null || opPhysical.getCrossConnections() == null) {
            return null;
        }

        List<CrossConnections> deleteXcList = opPhysical.getCrossConnections().stream()
                .filter(xc -> xc.getWssChannel() != null)
                .filter(xc -> xcIds.contains(xc.getCrossConnectionId().getValue()))
                .map(this::buildDeleteXc)
                .collect(Collectors.toList());
        if (deleteXcList.isEmpty()) {
            return null;
        }

        return new NodeBuilder()
                .setNodeId(opNode.getNodeId())
                .setKey(opNode.getKey())
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder()
                                .setCrossConnections(deleteXcList)
                                .setIp(opPhysical.getIp())
                                .setFriendlyName(opPhysical.getFriendlyName())
                                .setNodeType(opPhysical.getNodeType())
                                .build())
                        .build())
                .build();
    }

    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections buildDeleteXc(CrossConnectionAttributes xc) {
        return new CrossConnectionsBuilder(xc)
                .setAdminState(AdminStatus.Down)
                .setImplementState(ImplementState.Allocate)
                .setOperationalState(null)
                .setProperties(null)
                .setAmplifier(null)
                .setAps(null)
                .setWssChannel(null)
                .build();
    }

    private Physical getPhysical(Node node) {
        return getPhysicalFromNode(node);
    }

    private static Physical getPhysicalFromNode(Node node) {
        if (node == null || node.getAugmentation(Node1.class) == null) {
            return null;
        }
        return node.getAugmentation(Node1.class).getPhysical();
    }

    private String convertFailObj(FailObj failObj) {
        if (failObj == null || failObj.getObject() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        failObj.getObject().forEach(obj -> sb.append(obj.getObjectType().name())
                .append(": ")
                .append(obj.getObjectId())
                .append(", ")
                .append(obj.getMessageInfo())
                .append("\n"));
        return sb.toString();
    }
}
