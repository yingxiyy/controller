/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.utils;

import static net.flex.dci.otn.controller.nms.utils.Constants.OCH_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_VIEW_TOPO_KEY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.model.type.DataStoreType;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.NeSystemDefaultDao;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.OchNodeDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.RackDao;
import net.flex.dci.otc.mongo.dao.ScheduleDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import net.flex.dci.otc.mongo.dao.TopologyDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.dao.ViewLinkDao;
import net.flex.dci.otc.mongo.dao.ViewNodeDao;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import net.flex.dci.otc.mongo.dto.OchLinkBriefInfo;
import net.flex.dci.otc.mongo.dto.SortItem;
import net.flex.dci.otc.mongo.dto.TunnelRateInfo;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.nms.model.SiteLinkInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.NeSystemDefaultInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.OtdrPortDirection;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.telemetry.manager.TelemetryServer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.Aps;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRackBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRackKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNe;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.topology.type.SiteTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @date: 2021/3/31
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NetconfTopology {

    private final TopologyDao topologyDao;

    private final TunnelDao tunnelDao;

    private final SiteLinkDao siteLinkDao;

    private final TerminationPointDao terminationPointDao;

    private final CrossConnectionsDao crossConnectionsDao;

    private final PhyNodeDao phyNodeDao;
    private final SiteNodeDao siteNodeDao;
    private final OchNodeDao ochNodeDao;
    private final PhyLinkDao phyLinkDao;
    private final EquipmentsDao equipmentsDao;
    private final ViewLinkDao viewLinkDao;
    private final ViewNodeDao viewNodeDao;
    private final OchLinkDao ochLinkDao;
    private final RackDao rackDao;
    private final ScheduleDao scheduleDao;
    private final NeSystemDefaultDao neSystemDefaultDao;
    private final SubNetTreeNodeDao subNetTreeNodeDao;

    private void initTopoMap() {
        //Todo :init map
//        ToopSystemMap.instance().setNt(this);

    }


    /**
     * get network topology
     *
     * @param type
     * @return
     */
    public NetworkTopology getNetworkTopology(DataStoreType type) {
        return topologyDao.getNetworkTopology(type);
    }

    public DataObject getNetworkTopology(InstanceIdentifier<?> iid) {
        DataObject dataObject = topologyDao.getTopologyByIID(iid, DataStoreType.OPERATIONAL);
        if (dataObject == null) {
            dataObject = topologyDao.getTopologyByIID(iid, DataStoreType.CONFIG);
        }
        return dataObject;
    }

    public DataObject getNetworkTopologyNew(InstanceIdentifier<?> iid) {
        String neId = extractNeId(iid);
        if (neId != null && isOutOfControl(neId)) {
            // 网元停管 → 直接走配置库
            return topologyDao.getTopologyByIID(iid, DataStoreType.CONFIG);
        }

        // 网元受管 → 优先运行库，兜底配置库
        DataObject dataObject = topologyDao.getTopologyByIID(iid, DataStoreType.OPERATIONAL);
        if (dataObject == null) {
            dataObject = topologyDao.getTopologyByIID(iid, DataStoreType.CONFIG);
        }
        return dataObject;
    }

    private boolean isOutOfControl(String neId) {
        if (PhysicalNodeIdNamingRule.isPhyNodeId(neId)) {
            Node node = phyNodeDao.getOpPhyNodeById(neId);
            if (null == node) {
                return true;
            }
            OperStatus operationStatus = getPhyNodeOperStatus(node);
            return operationStatus == OperStatus.NeCommunicationException;
        }
        return false;
    }

    private String extractNeId(InstanceIdentifier<?> iid) {
        NodeKey nodeKey = iid.firstKeyOf(Node.class);
        if (nodeKey != null) {
            return nodeKey.getNodeId().getValue();
        }
        return null;
    }


    /**
     * merge or update topology
     *
     * @param iid
     * @param dataObject
     */
    public void updateTopology(InstanceIdentifier<?> iid, DataObject dataObject) {

        topologyDao.updateTopology(iid, dataObject);
    }


    /**
     * get tunnel info from mongo
     *
     * @param tunnelRef
     * @return
     * @throws Exception
     */
    public Tunnel getTunnel(Uri tunnelRef) throws CommonException {
        return tunnelDao.getTunnelById(tunnelRef.getValue());
    }

    public Tunnel getTunnel(String tunnelId) throws CommonException {
        return tunnelDao.getTunnelById(tunnelId);
    }

    public org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link getSiteLink(
            String linkId) {
        log.debug("get site link id is {}", linkId);
        return siteLinkDao.getSiteLinkById(linkId);
    }


    /**
     * get termination point
     *
     * @param topology
     * @param neId
     * @param mpoId
     * @return
     */
    public TerminationPoint getTerminationPoint(String topology, String neId, String mpoId) {

        TerminationPoint terminationPoint = terminationPointDao.getTerminationPointByNodeAndTpId(
                topology, neId, mpoId);
        return terminationPoint;
    }

    public TerminationPoint getTerminationPoint(String tpId) {
        String neId = PhysicalTpIdNamingRule.getNodeId(tpId);
        TerminationPoint terminationPoint = terminationPointDao.getPhyTpByNeIdAndTpId(
                neId, tpId);
        return terminationPoint;
    }

    public List<TerminationPoint> getTerminationPointIdRegex(String tpId) {
        List<TerminationPoint> terminationPoints = terminationPointDao.getAllTerminationPointByIdRegex(
                tpId);
        return terminationPoints;
    }

    public List<TerminationPoint> getTerminationPointByTypeAndRegex(String regex,
            PortType portType) {
        Map<String, List<TerminationPoint>> scanTpMap = terminationPointDao.getAllTerminationPointByTypeAndRegex(
                regex, portType);

        List<TerminationPoint> terminationPoints = new ArrayList<>();
        for (String nodeName : scanTpMap.keySet()) {
            terminationPoints.addAll(patchNodeName(scanTpMap.get(nodeName), nodeName));
        }

        //这些端口是通过OP数据库读取的， 是否已经被用了，需要查询物理连接
        List<Link> scanLinkList = phyLinkDao.getScanLinks();
        Iterator<TerminationPoint> iter = terminationPoints.iterator();
        while (iter.hasNext()) {
            TerminationPoint tp = iter.next();
            if (scanLinkList.stream()
                    .filter(x -> x.getLinkId().getValue().contains(tp.getTpId().getValue()))
                    .findAny()
                    .isPresent()) {
                //这个TP被用过了
                iter.remove();
            }
        }

        return terminationPoints;
    }

    private List<TerminationPoint> patchNodeName(List<TerminationPoint> scanTpList,
            String nodeName) {
        return scanTpList.parallelStream().map(tp -> {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr = tp.getAugmentation(
                    TerminationPoint1.class).getPhysical();
            TerminationPoint newTp = new TerminationPointBuilder(tp)
                    .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                            .setPhysical(new PhysicalBuilder(tpAttr)
                                    .setFriendlyName(nodeName + "/" + tpAttr.getFriendlyName())
                                    .build())
                            .build())
                    .build();
            return newTp;
        }).collect(Collectors.toList());
    }

    public CrossConnections getXc(TopologyId topologyRef, NodeId nodeId, Uri crossConnectionId)
            throws CommonException {

        return crossConnectionsDao.getXCByNodeIdAndXcRef(nodeId.getValue(),
                crossConnectionId.getValue());
    }

    public CrossConnections getXc(String nodeId, String crossConnectionId)
            throws CommonException {

        return crossConnectionsDao.getXCByNodeIdAndXcRef(nodeId,
                crossConnectionId);
    }

    private String getTpName(String nodeId, TpId tpId) {
        TerminationPoint tp = this.getTerminationPoint(PHY_TOPO_KEY, nodeId, tpId.getValue());

        if (tp != null && tp.getAugmentation(TerminationPoint1.class) != null
                && tp.getAugmentation(TerminationPoint1.class).getPhysical() != null) {
            return tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName();
        } else {
            String[] ids = tpId.getValue().split("#");
            return ids[ids.length - 1];
        }
    }

    private String getNodeName(String nodeId) {
        Node node = getPhyNode(nodeId);

        if (node != null && node.getAugmentation(Node1.class) != null
                && node.getAugmentation(Node1.class).getPhysical() != null) {
            return node.getAugmentation(Node1.class).getPhysical().getFriendlyName();
        } else {
            return nodeId;
        }
    }

    public Node getPhyNode(String neId) {
        Node node = phyNodeDao.getConfigPhyNodeById(neId);
        return node;
    }

    public Node getNeNode(String neId) {
        Node node = phyNodeDao.getOpPhyNodeById(neId);
        if (node == null) {
            node = phyNodeDao.getConfigPhyNodeById(neId);
        }
        return node;
    }

    private OperStatus getPhyNodeOperStatus(Node node) {
        if (node == null) {
            return OperStatus.Unknown;
        }
        Physical nodePhysical = node.getAugmentation(Node1.class).getPhysical();
        return nodePhysical.getOperationalState();
    }


    public List<Node> getPhyNodes() {
        List<Node> nodes = phyNodeDao.listPhyNodes();
        return nodes;
    }


    public Node getConfigPhyNode(String neId) {

        Node node = phyNodeDao.getConfigPhyNodeById(neId);
        return node;
    }

    public Node getOpPhyNode(String neId) {

        Node node = phyNodeDao.getOpPhyNodeById(neId);
        return node;
    }

    public Node getSiteNode(String siteId) {
        Node siteNode = siteNodeDao.getSiteNodeById(siteId);
        return siteNode;
    }

    public Topology getTopology(TopologyId topologyRef) {
        Topology topology = topologyDao.getTopologyById(topologyRef.getValue());
        return topology;
    }

    public org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link getLink(
            TopologyId topologyRef, LinkId linkRef) {
        String topoRef = topologyRef.getValue();
        String linkId = linkRef.getValue();
        Link topoLink = null;
        if (topoRef.equals(PHY_TOPO_KEY)) {
            topoLink = phyLinkDao.getPhyLinkByLinkId(linkId);
        } else if (topoRef.equals(SITE_TOPO_KEY)) {
            topoLink = siteLinkDao.getSiteLinkById(linkId);
        } else if (topoRef.equals(SITE_VIEW_TOPO_KEY)) {
            topoLink = viewLinkDao.getViewLinkById(linkId);
        } else if (topoRef.equals(OCH_TOPO_KEY)) {
            topoLink = ochLinkDao.getOchLinkByLinkId(linkId);
        }
        return topoLink;
    }

    public Equipments getEquipment(TopologyId phyTopoID, NodeId sourceNodeId, String equpRefId)
            throws CommonException {

        return equipmentsDao.getEquipmentByNodeAndEqId(sourceNodeId.getValue(), equpRefId);
    }

    public Equipments getEquipment(String nodeId, String eqpRefId)
            throws CommonException {
        PhysicalEqpIdNamingRule.getNodeId(eqpRefId);
        return equipmentsDao.getEquipmentByNodeAndEqId(nodeId, eqpRefId);
    }

    public Equipments getEquipment(String eqpRefId)
            throws CommonException {
        String nodeId = PhysicalEqpIdNamingRule.getNodeId(eqpRefId);
        return equipmentsDao.getEquipmentByNodeAndEqId(nodeId, eqpRefId);
    }

    public List<Link> listAllSiteLinkByIds(List<String> siteLinkIds)
            throws CommonException {
        return siteLinkDao.listAllSiteLinkByIds(siteLinkIds);
    }

    public TerminationPoint getTerminationPoint(TopologyId topologyRef, NodeId nodeRef,
            TpId tpRef) {
        return this
                .getTerminationPoint(topologyRef.getValue(), nodeRef.getValue(), tpRef.getValue());
    }


    public Node getNode(TopologyId topologyId, NodeId nodeRef) throws Exception {
        String nodeId = nodeRef.getValue();
        Node node = null;
        String topologyRef = topologyId.getValue();
        if (topologyRef.equals(PHY_TOPO_KEY)) {
            node = phyNodeDao.getConfigPhyNodeById(nodeId);
        } else if (topologyRef.equals(SITE_TOPO_KEY)) {
            node = siteNodeDao.getSiteNodeById(nodeId);
        } else if (topologyRef.equals(SITE_VIEW_TOPO_KEY)) {
            node = viewNodeDao.getViewNodeById(nodeId);
        }
        return node;
    }

    public SupportingRack getRack(TopologyId topologyRef, NodeId nodeRef, String rackRef)
            throws Exception {
//        SupportingRack rack = this.mongoDaoUtil
//                .getRack(topologyRef, nodeRef, rackRef, DataStoreType.OPERATIONAL);
//        if (rack == null) {
//            rack = this.mongoDaoUtil.getRack(topologyRef, nodeRef, rackRef, DataStoreType.CONFIG);
//        }
        SupportingRack rack = rackDao.getRackBySiteIdRackRef(nodeRef.getValue(), rackRef);
        return rack;
    }

    public SupportingRack getRack(String nodeId, String rackRef) {
//        SupportingRack rack = this.mongoDaoUtil
//                .getRack(topologyRef, nodeRef, rackRef, DataStoreType.OPERATIONAL);
//        if (rack == null) {
//            rack = this.mongoDaoUtil.getRack(topologyRef, nodeRef, rackRef, DataStoreType.CONFIG);
//        }
        SupportingRack rack = rackDao.getRackBySiteIdRackRef(nodeId, rackRef);
        return rack;
    }

    public CrossConnections getCrossConnection(TopologyId topologyId, NodeId nodeId, String xcId)
            throws CommonException {
        CrossConnections crossConnections = crossConnectionsDao.getXCByNodeIdAndXcRef(
                nodeId.getValue(), xcId);
        return crossConnections;
    }

    public PropertiesBuilder constructTunnelProperties(Tunnel tunnel) throws Exception {

        PropertiesBuilder propB = new PropertiesBuilder();
        List<Property> pList = new LinkedList<>();
        Properties originalList = tunnel.getProperties();
        if (originalList != null && originalList.getProperty() != null && !originalList
                .getProperty().isEmpty()) {
            pList.addAll(originalList.getProperty());
        }

        try {
            TpId srcTpId;
            TpId dstTpId;

            try {
                srcTpId = tunnel.getSourceTp().get(0).getTpRef();
                dstTpId = tunnel.getDestinationTp().get(0).getTpRef();
            } catch (NullPointerException e) {
                tunnel = getTunnel(tunnel.getTunnelId());
                srcTpId = tunnel.getSourceTp().get(0).getTpRef();
                dstTpId = tunnel.getDestinationTp().get(0).getTpRef();
            }
            String[] srcIds = srcTpId.getValue().split("#");
            String[] dstIds = dstTpId.getValue().split("#");
            String srcSiteId = srcIds[0];
            String dstSiteId = dstIds[0];
            String srcNodeId = srcIds[0] + "#" + srcIds[1];
            String dstNodeId = dstIds[0] + "#" + dstIds[1];

            additionalSiteProperties(srcSiteId, dstSiteId, pList);
            additionalSiteLinkProperties(srcNodeId, dstNodeId, pList);

            PropertyTool.putKeyValue(pList, "source-node-name", getNodeName(srcNodeId));
            PropertyTool.putKeyValue(pList, "dest-node-name", getNodeName(dstNodeId));
            PropertyTool.putKeyValue(pList, "source-tp-name", getTpName(srcNodeId, srcTpId));
            PropertyTool.putKeyValue(pList, "dest-tp-name", getTpName(dstNodeId, dstTpId));
//      PropertyTool.putKeyValue(pList, "site-link-name", tunnelSiteLinkMap.containsKey(tunnel.getTunnelId().getValue()) ? tunnelSiteLinkMap.get(tunnel.getTunnelId().getValue()) : null);

        } catch (NullPointerException e) {
            log.error("find one NullPointerException", e);
        }
        propB.setProperty(pList);
        return propB;
    }


    public Topology getSiteTopology() {
        return this.getTopology(new TopologyId(SITE_TOPO_KEY));
    }

//    public List<Schedule> getSchedules() {
//        List<MoSchedule> moSchedules = scheduleDao.getAll();
//        List<Schedule> schedules = moSchedules.stream().map(mo -> {
//            mo.convert(true);
//            return mo.build();
//        }).collect(Collectors.toList());
//
//        return schedules;
//    }

//    public void deleteScheduleTask(BigInteger id) {
//        this.mongoDaoUtil.deleteSchedule(id, DataStoreType.CONFIG);
//        scheduleDao.deleteSchedule(id);
//    }

    private void additionalSiteProperties(String srcSiteId, String dstSiteId,
            List<Property> pList) {
        // get all site nodes.

        Map<String, String> siteMap = DCISystemMap.instance().getSiteMap();
        if (siteMap.get(srcSiteId) == null || siteMap.get(dstSiteId) == null) {
            DCISystemMap.instance().buildSiteMap();
        }

        PropertyTool.putKeyValue(pList, "source-site-name", siteMap.get(srcSiteId));
        PropertyTool.putKeyValue(pList, "dest-site-name", siteMap.get(dstSiteId));
    }

    private void additionalSiteLinkProperties(String srcNodeId, String dstNodeId,
            List<Property> pList) {
        // get all site link.
        Map<String, SiteLinkInfo> siteLinkMap = DCISystemMap.instance()
                .getPhyNodeSiteLinkMap();

        String plane = "";
        String siteLinkFriendlyName = "";
        try {
            plane = siteLinkMap.get(srcNodeId).getPlane();
            siteLinkFriendlyName = siteLinkMap.get(srcNodeId).getName();
        } catch (NullPointerException e) {
            //the TPC node has been removed in cache, rebuild it
            DCISystemMap.instance().rebuildPhyNodeSiteLinkMap(srcNodeId);
            try {
                plane = siteLinkMap.get(srcNodeId).getPlane();
                siteLinkFriendlyName = siteLinkMap.get(srcNodeId).getName();
            } catch (NullPointerException e1) {
                //try dstNode
                try {
                    plane = siteLinkMap.get(dstNodeId).getPlane();
                    siteLinkFriendlyName = siteLinkMap.get(dstNodeId).getName();
                } catch (NullPointerException e2) {
                    DCISystemMap.instance().rebuildPhyNodeSiteLinkMap(dstNodeId);
                    try {
                        plane = siteLinkMap.get(dstNodeId).getPlane();
                        siteLinkFriendlyName = siteLinkMap.get(dstNodeId).getName();
                    } catch (NullPointerException e3) {
                        log.error("the phy node {}, {} hasn't found in phyNodeSiteLinkMap.",
                                srcNodeId,
                                dstNodeId, e3);
                    }
                }
            }
        }
        PropertyTool.putKeyValue(pList, "domain-name", plane);
        PropertyTool.putKeyValue(pList, "site-link-name", siteLinkFriendlyName);
    }


    public PropertiesBuilder constructPhyLinkProperties(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link link) {
        String srcSiteId;
        String dstSiteId;
        String srcNodeId;
        String dstNodeId;
        String[] srcIds;
        String[] dstIds;
        try {
            srcIds = link.getSource().getSourceTp().getValue().split("#");
            dstIds = link.getDestination().getDestTp().getValue().split("#");
        } catch (NullPointerException e) {
            link = getLink(new TopologyId(PHY_TOPO_KEY), link.getLinkId());
            srcIds = link.getSource().getSourceTp().getValue().split("#");
            dstIds = link.getDestination().getDestTp().getValue().split("#");
        }
        srcSiteId = srcIds[0];
        dstSiteId = dstIds[0];
        srcNodeId = srcIds[0] + "#" + srcIds[1];
        dstNodeId = dstIds[0] + "#" + dstIds[1];

        PropertiesBuilder propB = new PropertiesBuilder();
        List<Property> pList = new LinkedList<>();
        Properties originalList = link.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                .getPhysical().getProperties();
        if (originalList != null && originalList.getProperty() != null && !originalList
                .getProperty().isEmpty()) {
            pList.addAll(originalList.getProperty());
        }

        additionalSiteProperties(srcSiteId, dstSiteId, pList);
        additionalSiteLinkProperties(srcNodeId, dstNodeId, pList);

        PropertyTool.putKeyValue(pList, "source-node-name",
                getNodeName(link.getSource().getSourceNode().getValue()));
        PropertyTool.putKeyValue(pList, "dest-node-name",
                getNodeName(link.getDestination().getDestNode().getValue()));
        PropertyTool.putKeyValue(pList, "source-tp-name",
                getTpName(link.getSource().getSourceNode().getValue(),
                        link.getSource().getSourceTp()));
        PropertyTool.putKeyValue(pList, "dest-tp-name",
                getTpName(link.getDestination().getDestNode().getValue(),
                        link.getDestination().getDestTp()));

        propB.setProperty(pList);
        return propB;
    }


    public PropertiesBuilder constructSiteLinkProperties(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link link) {
        PropertiesBuilder propB = new PropertiesBuilder();
        List<Property> pList = new LinkedList<>();
        Properties originalList = link.getAugmentation(Link1.class).getSite().getProperties();
        if (originalList != null && originalList.getProperty() != null && !originalList
                .getProperty().isEmpty()) {
            pList.addAll(originalList.getProperty());
        }

        try {
            String srcSiteId;
            String dstSiteId;
            String srcNodeId;
            String dstNodeId;
            String[] srcIds;
            String[] dstIds;

            try {
                srcIds = link.getSource().getSourceTp().getValue().split("#");
                dstIds = link.getDestination().getDestTp().getValue().split("#");
            } catch (NullPointerException e) {
                link = getLink(new TopologyId(SITE_TOPO_KEY), link.getLinkId());
                srcIds = link.getSource().getSourceTp().getValue().split("#");
                dstIds = link.getDestination().getDestTp().getValue().split("#");
            }
            srcSiteId = srcIds[0];
            dstSiteId = dstIds[0];
            srcNodeId = srcIds[0] + "#" + srcIds[1];
            dstNodeId = dstIds[0] + "#" + dstIds[1];

            additionalSiteProperties(srcSiteId, dstSiteId, pList);
            additionalSiteLinkProperties(srcNodeId, dstNodeId, pList);

            PropertyTool.putKeyValue(pList, "source-node-name", getNodeName(srcNodeId));
            PropertyTool.putKeyValue(pList, "dest-node-name", getNodeName(dstNodeId));
            PropertyTool.putKeyValue(pList, "source-tp-name",
                    getTpName(srcNodeId, link.getSource().getSourceTp()));
            PropertyTool.putKeyValue(pList, "dest-tp-name",
                    getTpName(dstNodeId, link.getDestination().getDestTp()));
            PropertyTool.putKeyValue(pList, "az-active", getActivePath(dstNodeId));
            PropertyTool.putKeyValue(pList, "za-active", getActivePath(srcNodeId));

        } catch (NullPointerException e) {
            log.error("find one NullPointerException", e);
        }
        propB.setProperty(pList);
        return propB;
    }

    private String getActivePath(String dstNodeId) {
        try {
            Node ntNode = this.getNode(new TopologyId(PHY_TOPO_KEY), new NodeId(dstNodeId));
            Node1 phyNode = ntNode.getAugmentation(Node1.class);
            List<CrossConnections> xcs = phyNode.getPhysical().getCrossConnections();
            if (xcs == null) {
                return null;
            }
            for (CrossConnections xc : xcs) {
                Aps aps = xc.getAps();
                if (aps != null) {
                    if (aps.getActivePath() != null) {
                        return aps.getActivePath().name();
                    } else {
                        return ApsPath.PRIMARY.name();
                    }
                }
            }
        } catch (NullPointerException e) {
            log.error("", e);
        } catch (Exception e) {
            log.error("", e);

        }
        return "--";
    }

    public PropertiesBuilder constructPhyNodeProperties(Node node) throws Exception {
//        if (node.getTerminationPoint() == null) {
//            node = getNode(new TopologyId(TopoNameConstants.Phy_Topo_Key), node.getNodeId());
//        }

        PropertiesBuilder propB = new PropertiesBuilder();
        List<Property> pList = new LinkedList<>();
        Properties originalList = node.getAugmentation(Node1.class).getPhysical().getProperties();
        if (originalList != null && originalList.getProperty() != null && !originalList
                .getProperty().isEmpty()) {
            pList.addAll(originalList.getProperty());
        }

        try {
            NodeId nodeId = node.getNodeId();
            String siteId = nodeId.getValue().split("#")[0];

            Map<String, Adapter> adapterMap = DCISystemMap.instance().getAdapterMap();
            String adapterName;
            if (adapterMap == null || adapterMap.get(nodeId.getValue()) == null) {
                adapterName = "";
            } else {
                adapterName = adapterMap.get(nodeId.getValue()).getName().getValue();
            }
            PropertyTool.putKeyValue(pList, "adapter-id", adapterName);

            Map<String, TelemetryServer> collectorMap = DCISystemMap.instance().getCollectorMap();
            String collectorName;
            if (collectorMap == null || collectorMap.get(nodeId.getValue()) == null) {
                collectorName = "";
            } else {
                collectorName = collectorMap.get(nodeId.getValue()).getName().getValue();
            }
            PropertyTool.putKeyValue(pList, "collector-id", collectorName);

            additionalSiteLinkProperties(nodeId.getValue(), nodeId.getValue(), pList);
            Map<String, String> siteMap = DCISystemMap.instance().getSiteMap();

            if (siteMap.get(siteId) == null) {
                DCISystemMap.instance().buildSiteMap();
            }

            PropertyTool.putKeyValue(pList, "site-name", siteMap.get(siteId));


        } catch (NullPointerException e) {
            log.error("find one NullPointerException", e);
        }
        propB.setProperty(pList);
        return propB;
    }

//    public DataObject read(DataStoreType type, InstanceIdentifier<?> iid) {
//        return this.mongoDao.readData(iid, type);
//    }

//    public void put(DataStoreType type, InstanceIdentifier<?> path, DataObject dataObject) {
//        this.mongoDao.saveData(path, dataObject, type, YangOperationType.PUT);
//    }


    public void mergeRackNeLocation(String siteId, String uptRackId, SupportingRack uptSiteRack,
            String neId, String location) {
        log.info("mergeNeLocation start");
        InstanceIdentifier<SupportingRack> mergePath = InstanceIdentifier
                .builder(NetworkTopology.class)
                .child(Topology.class,
                        new TopologyKey(new TopologyId(SiteTopology.QNAME.getLocalName())))
                .child(Node.class, new NodeKey(new NodeId(siteId)))
                .augmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                .child(Site.class)
                .child(SupportingRack.class, new SupportingRackKey(new Uri(uptRackId)))
                .build();

        List<SupportingNe> uptNes = new ArrayList<SupportingNe>();
        uptNes.addAll(uptSiteRack.getSupportingNe());
        SupportingNe supNe = new SupportingNeBuilder()
                .setKey(new SupportingNeKey(new NodeId(neId)))
                .setLocation(location)
                .setNodeRef(new NodeId(neId))
                .build();
        uptNes.add(supNe);

        SupportingRack uptRack = new SupportingRackBuilder()
                .setKey(uptSiteRack.getKey())
                .setRackId(uptSiteRack.getRackId())
                .setSupportingNe(uptNes)
                .build();

//        this.mongoDao.saveData(mergePath, uptRack, DataStoreType.CONFIG, YangOperationType.MERGE);
//        this.mongoDao
//                .saveData(mergePath, uptRack, DataStoreType.OPERATIONAL, YangOperationType.MERGE);
        rackDao.saveRack2Site(siteId, uptRack);
        log.info("mergeNeLocation end");
    }

    public void removeNeLocation(String siteId, String rackId, String neId) {
        log.info("removeNeLocation start");
//        InstanceIdentifier<SupportingNe> deletePath = InstanceIdentifier
//                .builder(NetworkTopology.class)
//                .child(Topology.class,
//                        new TopologyKey(new TopologyId(SiteTopology.QNAME.getLocalName())))
//                .child(Node.class, new NodeKey(new NodeId(siteId)))
//                .augmentation(
//                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
//                .child(Site.class)
//                .child(SupportingRack.class, new SupportingRackKey(new Uri(rackId)))
//                .child(SupportingNe.class, new SupportingNeKey(new NodeId(neId)))
//                .build();
//
//        this.mongoDao.deleteData(deletePath, DataStoreType.CONFIG);
//        this.mongoDao.deleteData(deletePath, DataStoreType.OPERATIONAL);
        log.info("removeNeLocation end");

    }

    public void mergeNeLocation(String siteId, String rackId, String neId, String location)
            throws Exception {
        log.info("mergeNeLocation start");
//        InstanceIdentifier<SupportingNe> mergePath = InstanceIdentifier
//                .builder(NetworkTopology.class)
//                .child(Topology.class,
//                        new TopologyKey(new TopologyId(SiteTopology.QNAME.getLocalName())))
//                .child(Node.class, new NodeKey(new NodeId(siteId)))
//                .augmentation(
//                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
//                .child(Site.class)
//                .child(SupportingRack.class, new SupportingRackKey(new Uri(rackId)))
//                .child(SupportingNe.class, new SupportingNeKey(new NodeId(neId)))
//                .build();
//
//        SupportingNe oldNe = (SupportingNe) this.mongoDao.readData(mergePath, DataStoreType.CONFIG);
//        if (oldNe == null) {
//            throw new Exception("Error Ne's Info");
//        }
//
//        SupportingNe supNe = new SupportingNeBuilder()
//                .setKey(new SupportingNeKey(new NodeId(neId)))
//                .setLocation(location)
//                .setNodeRef(new NodeId(neId))
//                .build();
//
//        this.mongoDao.saveData(mergePath, supNe, DataStoreType.CONFIG, YangOperationType.MERGE);
//        this.mongoDao
//                .saveData(mergePath, supNe, DataStoreType.OPERATIONAL, YangOperationType.MERGE);
        log.info("mergeNeLocation end");
    }


    public void updateNode(DataStoreType type, Node node) {
//        InstanceIdentifier<Node> path = InstanceIdentifier.builder(NetworkTopology.class)
//                .child(Topology.class,
//                        new TopologyKey(new TopologyId(TopoNameConstants.Phy_Topo_Key)))
//                .child(Node.class, new NodeKey(node.getNodeId()))
//                .build();
//        this.mongoDao.saveData(path, node, type, YangOperationType.PUT);
//        phyNodeDao.rewriteConfigPhyNode();PhyNode(node, type);
        if (DataStoreType.OPERATIONAL.equals(type)) {
            phyNodeDao.rewriteOpPhyNode(node);
        } else {
            phyNodeDao.rewriteConfigPhyNode(node);
        }
    }

    public void removeNode(Node node) {
//        InstanceIdentifier<Node> path = InstanceIdentifier.builder(NetworkTopology.class)
//                .child(Topology.class,
//                        new TopologyKey(new TopologyId(TopoNameConstants.Phy_Topo_Key)))
//                .child(Node.class, new NodeKey(node.getNodeId()))
//                .build();
//        this.mongoDao.deleteData(path, DataStoreType.CONFIG);
//        this.mongoDao.deleteData(path, DataStoreType.OPERATIONAL);
        phyNodeDao.deletePhyNodeById(node.getNodeId().getValue());
    }

    public void removeEquip(Node node, Equipments eq) {
//        InstanceIdentifier<Equipments> path = InstanceIdentifier.builder(NetworkTopology.class)
//                .child(Topology.class,
//                        new TopologyKey(new TopologyId(TopoNameConstants.Phy_Topo_Key)))
//                .child(Node.class, new NodeKey(node.getNodeId()))
//                .augmentation(Node1.class)
//                .child(Physical.class)
//                .child(Equipments.class, new EquipmentsKey(eq.getEquipmentId()))
//                .build();

//        this.mongoDao.deleteData(path, DataStoreType.CONFIG);
//        this.mongoDao.deleteData(path, DataStoreType.OPERATIONAL);
        equipmentsDao.removeEquipmentsByNodeAndEqId(node.getNodeId().getValue(),
                eq.getEquipmentId());
    }

    public void updateEquip(Node node, Equipments eq, DataStoreType type) {
//        InstanceIdentifier<?> path = InstanceIdentifier.builder(NetworkTopology.class)
//                .child(Topology.class,
//                        new TopologyKey(new TopologyId(TopoNameConstants.Phy_Topo_Key)))
//                .child(Node.class, new NodeKey(node.getNodeId()))
//                .augmentation(Node1.class)
//                .child(Physical.class)
//                .child(Equipments.class, new EquipmentsKey(eq.getEquipmentId()))
//                .build();
//        mongoDao.saveData(path, eq, type, YangOperationType.MERGE);
        equipmentsDao.updateEquipments(node, eq, type);
    }

    public Physical getPhysical(String neId) {
//        InstanceIdentifier<Physical> iid = InstanceIdentifier.builder(NetworkTopology.class).build()
//                .child(Topology.class,
//                        new TopologyKey(new TopologyId(OtnPhyTopology.QNAME.getLocalName())))
//                .child(Node.class, new NodeKey(new NodeId(neId))).augmentation(Node1.class)
//                .child(Physical.class);
//        return (Physical) mongoDao.readData(iid, type);
        return phyNodeDao.getPhysicalByNode(neId);
    }

    public NeSystemDefaultInfo getNeSystemDefault() {
//        InstanceIdentifier<NeSystemDefaultInfo> opName = InstanceIdentifier
//                .create(NeSystemDefaultInfo.class);
//        return (NeSystemDefaultInfo) this.mongoDao.readData(opName, DataStoreType.CONFIG);
        return neSystemDefaultDao.getNeSystemDefault();
    }

    public Link getOchLink(String linkId) {
        return ochLinkDao.getOchLinkByLinkId(linkId);
    }

    public Link getOchLinkByLinePort(String nodeId, String portId) {
        return ochLinkDao.getOchLinkByNodeIdAndLinePort(nodeId, portId);
    }

    public Link getPhyLink(String linkId) {
        return phyLinkDao.getPhyLinkByLinkId(linkId);
    }

    public List<Link> getPhyLinkUnderPhyNode(String nodeId) {
        return phyLinkDao.listAllPhyLinksUnderPhyNode(nodeId);
    }

    public List<Link> listSiteLink() {
        return siteLinkDao.getSiteLinks();
    }

    public List<Link> listPhyLink() {
        return phyLinkDao.listPhyLinks();
    }

    public List<Node> listSiteNodes() {
        return siteNodeDao.listSiteNodes();
    }


    public List<Node> getPhyNodesByIds(List<String> phyNodeIds) {
        return phyNodeDao.listConfigPhyNodeByIds(phyNodeIds);
    }

    public List<Node> listRuntimePhyNodesByIds(List<String> phyNodeIds) {
        List<Node> runtimePhyNodes = phyNodeDao.listOperPhyNodeByIds(phyNodeIds);
        List<String> runtimePhyNodeIds = runtimePhyNodes.stream().map(NodeAttributes::getNodeId)
                .map(Uri::getValue).collect(
                        Collectors.toList());
        List<String> missPhyNodeIds = phyNodeIds.stream()
                .filter(neId -> !runtimePhyNodeIds.contains(neId)).collect(
                        Collectors.toList());
        List<Node> configNodes = new ArrayList<>();
        if (!missPhyNodeIds.isEmpty()) {
            configNodes = phyNodeDao.listConfigPhyNodeByIds(missPhyNodeIds);
        }

        List<Node> mergedNodes = new ArrayList<>();
        mergedNodes.addAll(runtimePhyNodes);
        mergedNodes.addAll(configNodes);

        return mergedNodes;
    }

    public List<Node> listPhyNodeUnderSite(String siteNodeId) {
        return phyNodeDao.listConfigPhyNodeBySiteNodeId(siteNodeId);
    }


    /**
     * get all physical link with physical node
     *
     * @param subPhyNodes
     * @return
     */
    public List<Link> listPhyLinkByNodes(List<Node> subPhyNodes) {
        List<String> nodeIds = subPhyNodes.stream().map(node -> node.getNodeId().getValue())
                .collect(
                        Collectors.toList());
        log.debug("list phy link by nodes id in :{}", nodeIds);

        return phyLinkDao.getAllPhyLinksByNodeIds(nodeIds);
    }

    public List<String> listPhyLinkIdsByNodeIds(List<String> nodeIds) {

        log.debug("retrieve list phy link by nodes id in :{}", nodeIds);
        return phyLinkDao.retrieveAllPhyLinkIdsByPhyNodeIds(nodeIds);
    }

    public List<Link> listPhyLinkUnderSiteNode(Node siteNode) {
        log.debug("list all physical link under the site node :{}",
                siteNode.getNodeId().getValue());
        List<Link> links = phyLinkDao.listAllPhyLinkBySiteNode(siteNode.getNodeId().getValue());
        return links;
    }

    public List<Node> getUnStuffedPhyNode(String siteNodeId) {
        return phyNodeDao.getUnStuffedNode(siteNodeId);
    }

    public PageResult<Link> retrieveAllSiteLinkPaged(
            Integer pageNum, Integer pageSize) {
        return siteLinkDao.listAllSiteLinkPaged(pageNum, pageSize);
    }

    public PageResult<Link> retrieveAllSiteLinkPaged(
            Integer pageNum, Integer pageSize, List<FilterItem> filterItems,
            List<SortItem> sortItems) {
        return siteLinkDao.listAllSiteLinkPaged(pageNum, pageSize, filterItems, sortItems);
    }

    public PageResult<Link> retrieveAllPhyLinkPaged(Integer pageNum, Integer pageSize) {
        return phyLinkDao.listAllPhyLinkPaged(pageNum, pageSize);
    }

    public PageResult<Link> retrieveAllPhyLinkByLinkIdsPaged(List<String> refPhyLinkIds,
            Integer pageNum, Integer pageSize) {
        return phyLinkDao.listAllPhyLinkByLinksPaged(refPhyLinkIds, pageNum, pageSize);
    }

    public PageResult<Link> retrieveAllPhyLinkByLinkIdsPaged(List<String> refPhyLinkIds,
            Integer pageNum, Integer pageSize, List<FilterItem> filterItems,
            List<SortItem> sortItems) {
        return phyLinkDao.listAllPhyLinkByLinksPaged(refPhyLinkIds, pageNum, pageSize, filterItems,
                sortItems);
    }

    public PageResult<Link> retrieveAllPhyLinkUnderByNodeIdPaged(String nodeId,
            Integer pageNum, Integer pageSize) {
        return phyLinkDao.listAllPhyLinkUnderByNodeIdPaged(nodeId, pageNum, pageSize);
    }

    public PageResult<Link> retrieveAllPhyLinkUnderByNodeIdPaged(String nodeId,
            Integer pageNum, Integer pageSize, List<FilterItem> filterItems,
            List<SortItem> sortItems) {
        return phyLinkDao.listAllPhyLinkUnderByNodeIdPaged(nodeId, pageNum, pageSize, filterItems,
                sortItems);
    }

    public List<Link> getOchLinksByIds(Set<String> ochLinkIds) {
        return ochLinkDao.getByLinkIds(ochLinkIds);
    }

    public PageResult<Link> retrievePhyLinksUnderByEquipPaged(String equipId, Integer pageNum,
            Integer pageSize) {
        return phyLinkDao.listAllPhyLinksUnderEquipPaged(equipId, pageNum, pageSize);
    }

    public PageResult<Link> retrievePhyLinksUnderByEquipPaged(String equipId, Integer pageNum,
            Integer pageSize, List<FilterItem> filterItems, List<SortItem> sortItems) {
        return phyLinkDao.listAllPhyLinksUnderEquipPaged(equipId, pageNum, pageSize, filterItems,
                sortItems);
    }

    public PageResult<Link> retrievePhyLinksUnderByTpPaged(String tpId, Integer pageNum,
            Integer pageSize) {
        return phyLinkDao.listAllPhyLinksUnderTpPaged(tpId, pageNum, pageSize);
    }

    public PageResult<Tunnel> retrieveAllTunnelPaged(
            Integer pageNum, Integer pageSize) {
        return tunnelDao.listAllTunnelPaged(pageNum, pageSize);
    }

    public PageResult<Tunnel> retrieveAllTunnelPaged(
            Integer pageNum, Integer pageSize, List<FilterItem> filterItems,
            List<SortItem> sortItems) {
        return tunnelDao.listAllTunnelPaged(pageNum, pageSize, filterItems, sortItems);
    }

    public PageResult<Tunnel> retrieveTunnelPagedByTunnelIds(List<String> supportTunnelIds,
            Integer pageNum, Integer pageSize) {
        return tunnelDao.listTunnelPagedByTunnelIds(supportTunnelIds, pageNum, pageSize);
    }

    public PageResult<Tunnel> retrieveTunnelPagedByTunnelIds(List<String> supportTunnelIds,
            Integer pageNum, Integer pageSize, List<FilterItem> filterItems,
            List<SortItem> sortItems) {
        return tunnelDao.listTunnelPagedByTunnelIds(supportTunnelIds, pageNum, pageSize,
                filterItems, sortItems);
    }

    public List<Link> getEquipmentRefPhyLinks(
            String equipmentId) {
        return phyLinkDao.listAllPhyLinksUnderEquip(equipmentId);
    }

    public List<Link> getTpRefPhyLinks(String tpId) {
        return phyLinkDao.listAllPhyLinksUnderTp(tpId);
    }

    public PageResult<Link> retrieveAllSiteLinkByIdsPaged(List<String> ids, Integer pageNum,
            Integer pageSize) {
        return siteLinkDao.listAllSiteLinkPagedByIds(ids, pageNum, pageSize);
    }

    public PageResult<Link> retrieveAllSiteLinkByIdsPaged(List<String> ids, Integer pageNum,
            Integer pageSize, List<FilterItem> filterItems, List<SortItem> sortItems) {
        return siteLinkDao.listAllSiteLinkPagedByIds(ids, pageNum, pageSize, filterItems,
                sortItems);
    }

    public Link getViewLink(String linkId) {
        return viewLinkDao.getViewLinkById(linkId);
    }

    public PageResult<Link> retrieveAllOchLinkPaged(Integer pageNum, Integer pageSize) {
        return ochLinkDao.listAllOchLinkPaged(pageNum, pageSize);
    }

    public PageResult<Node> retrieveAllPhyNodePaged(Integer pageNum, Integer pageSize) {
        return phyNodeDao.listConfigPhyNodesPaged(pageNum, pageSize);
    }

    public PageResult<Node> retrieveAllPhyNodePaged(Integer pageNum, Integer pageSize,
            List<FilterItem> filterItems, List<SortItem> sortItems) {
        return phyNodeDao.listConfigPhyNodesPaged(pageNum, pageSize, filterItems, sortItems);
    }

    public PageResult<Node> retrieveAllSiteNodePaged(Integer pageNum, Integer pageSize) {
        return siteNodeDao.listAllSiteNodePaged(pageNum, pageSize);
    }

    public PageResult<Node> retrieveAllSiteNodePaged(Integer pageNum, Integer pageSize,
            List<FilterItem> filterItems, List<SortItem> sortItems) {
        return siteNodeDao.listAllSiteNodePaged(pageNum, pageSize, filterItems, sortItems);
    }

    public List<Node> retrieveAllSiteNode(List<FilterItem> filterItems, List<SortItem> sortItems) {
        return siteNodeDao.listAllSiteNode(filterItems, sortItems);
    }

    public PageResult<Link> retrieveAllPhyLinkPaged(Integer pageNum, Integer pageSize,
            List<FilterItem> filterItems, List<SortItem> sortItems) {
        return phyLinkDao.listAllPhyLinkPaged(pageNum, pageSize, filterItems, sortItems);
    }

    public PageResult<Node> retrieveAllSiteNodePaged(List<String> nodes, Integer pageNum,
            Integer pageSize,
            List<FilterItem> filterItems, List<SortItem> sortItems) {
        return siteNodeDao.listAllSiteNodeByIdsPaged(nodes, pageNum, pageSize, filterItems,
                sortItems);
    }

    public PageResult<Node> retrieveAllPhyNodePagedByIds(List<String> refPhyNodeIds,
            Integer pageNum, Integer pageSize) {
        return phyNodeDao.listConfigPhyNodesPagedByIds(refPhyNodeIds, pageNum, pageSize);
    }

    public PageResult<Node> retrieveAllPhyNodePagedByIds(List<String> refPhyNodeIds,
            Integer pageNum, Integer pageSize, List<FilterItem> filterItems, List<SortItem> sorts) {
        return phyNodeDao.listConfigPhyNodesPagedByIds(refPhyNodeIds, pageNum, pageSize,
                filterItems, sorts);
    }

    public List<Link> getPhyLinksByIds(List<String> ids) {
        return phyLinkDao.getAllPhyLinksByIds(ids);
    }

    public PageResult<Node> retrieveAllSiteNodePagedByIds(List<String> nodeIds, Integer pageNum,
            Integer pageSize) {
        return siteNodeDao.listAllSiteNodeByIdsPaged(nodeIds, pageNum, pageSize);
    }

    public List<Node> retrieveAllSiteNodeByIds(List<String> nodeIds) {
        return siteNodeDao.listAllNodeByIds(nodeIds);
    }

    public List<Link> listAllViewLinks() {
        return viewLinkDao.listViewLinks();
    }

    public List<Node> listAllViewNodes() {
        return viewNodeDao.listViewNodes();
    }


    public List<Link> getSiteLinksBetweenSites(String sourceSiteId, String destSiteId) {
        return siteLinkDao.getSiteLinkBetweenTwoSites(sourceSiteId, destSiteId);
    }

    public List<Link> getSiteLinksByNeIdAndLinePort(String neId, String tpId) {
        return siteLinkDao.getSiteLinksByNeIdAndLinePort(neId, tpId);
    }

    public List<Link> getOchLinksUnderSiteLinkId(String siteLinkId) {
        return ochLinkDao.getAllOchLinksUnderSiteLinkIds(Collections.singletonList(siteLinkId));
    }

    public List<Link> getOchLinksUnderSiteLinks(List<String> siteLinkIds) {
        return ochLinkDao.getAllOchLinksUnderSiteLinkIds(siteLinkIds);
    }

    public List<Link> getBusinessOchLinksUnderSiteLinks(List<String> siteLinkIds) {
        return ochLinkDao.getAllBusinessOchLinksUnderSiteLinkIds(siteLinkIds);
    }

    public List<Link> getOchLinksBasedOnPhyLinks(List<String> phyLinkIds) {
        log.debug("start to get och links based on phyLink");
        List<String> siteLinkIds = siteLinkDao.retrieveAllSiteLinkIdsBySupportingLinkIds(
                phyLinkIds);
        List<String> supportLinkIds =
                CollectionUtils.isEmpty(siteLinkIds) ? phyLinkIds : siteLinkIds;
        //first get och link by phy link
//        Set<Link> ochLinks = new HashSet<>();
//        List<Link> phyLinkRefOchLinks = ochLinkDao.getAllOchLinksUnderPhyLinkIds(
//                phyLinkIds);
//
//        //get och link by phy link => site link => och link
//        List<Link> siteLinks = siteLinkDao.listAllSiteLinkBasedOnPhyLinks(phyLinkIds);
//        List<String> siteLinkIds = siteLinks.stream()
//                .map(siteLink -> siteLink.getLinkId().getValue()).collect(
//                        Collectors.toList());
//        List<Link> siteLinkRefOchLinks = ochLinkDao.getAllOchLinksUnderSiteLinkIds(
//                siteLinkIds);
//        ochLinks.addAll(phyLinkRefOchLinks);
//        ochLinks.addAll(siteLinkRefOchLinks);
//        return new ArrayList<>(ochLinks);
        return ochLinkDao.findBySupportingLinkRef(supportLinkIds);

    }


    public List<Tunnel> getTunnelsBaseOnOchLinks(List<String> ochLinkIds) {
        return tunnelDao.getAllTunnelsUnderOchLink(ochLinkIds);
    }


    public List<LinkStateDto> getTunnelStateBaseOnOchLinks(List<String> ochLinkIds) {
        return tunnelDao.getAllTunnelsStateUnderOchLink(ochLinkIds);
    }


    public List<Tunnel> retrieveAllTunnel(List<FilterItem> filterItems, List<SortItem> sortItems) {
        return tunnelDao.listAllTunnel(filterItems, sortItems);
    }


    public PageResult<Link> retrievePhyLinksUnderByTpPaged(String tpId, Integer pageNum,
            Integer pageSize, List<FilterItem> filterItems, List<SortItem> sortItems) {
        return phyLinkDao.listAllPhyLinksUnderTpPaged(tpId, pageNum, pageSize, filterItems,
                sortItems);
    }

    public List<Node> retrieveAllPhyNodeByIds(List<String> refPhyNodeIds) {
        return phyNodeDao.listConfigPhyNodeByIds(refPhyNodeIds);
    }

    public List<Node> retrieveAllRealPhyNodeByIds(List<String> refPhyNodeIds) {
        return phyNodeDao.listOperPhyNodeByIds(refPhyNodeIds);
    }

    public List<String> getTerminationPointIdByIdRegexAndPortType(String refElementId,
            PortType portType) {
        return terminationPointDao.getAllTerminationPointIdByIdRegexAndPortType(refElementId,
                portType);
    }

    public List<String> listAllOperationalEquipmentByIdRegexAndEquipTypes(String idRegex,
            EquipType[] equipTypes) {
        List<EquipType> equipTypeList = Arrays.asList(equipTypes);
        return equipmentsDao.listAllOperationalEquipmentByIdRegexAndEquipTypes(idRegex,
                equipTypeList);
    }

    public List<String> getTerminationPointIdByRefEquipIdsAndPortType(List<String> refEquipmentIds,
            PortType portType) {
        return terminationPointDao.getAllTerminationPointIdByRefEquipmentIdsAndPortType(
                refEquipmentIds, portType);
    }

    public List<TerminationPoint> getAllTerminationPointByIds(List<String> refTerminationPointIds) {
        return terminationPointDao.getAllTerminationPointByIds(refTerminationPointIds);
    }

    public List<Link> listAllPhyLinkUnderEquipments(Set<String> refEquipmentIds) {
        List<Link> refPhyLinks = refEquipmentIds.stream().map(phyLinkDao::listAllPhyLinksUnderEquip)
                .flatMap(
                        Collection::stream).collect(Collectors.toList());
        return refPhyLinks;
    }

    public List<TerminationPoint> getTerminationPoint(String tpId, PortType portType,
            OtdrPortDirection direction) {
//        return phyNodeDao.getTerminationPoint(tpId, portType, direction);
        return new ArrayList<>();
    }

    public List<Link> listAllViewLinkByLinkLevel(ViewLinkType viewLinkType) {
        if (viewLinkType == null) {
            return viewLinkDao.listViewLinks();
        } else {
            return viewLinkDao.listViewLinksByLinkLeve(viewLinkType);
        }
    }

    public List<Link> listAllSiteLinkBasedOnPhyLinks(List<String> phyLinkIds) {
        return siteLinkDao.listAllSiteLinkBasedOnPhyLinks(phyLinkIds);
    }

    public List<String> listAllSiteLinkRefPlane() {
        return siteLinkDao.listAllRefPlane();
    }

    public List<String> listAllOchLinkRefPlane() {
        return ochLinkDao.listAllRefPlane();
    }

    public List<Link> listAllSiteLinkByPlane(String plane) {
        return siteLinkDao.listAllSiteLinkByPlaneName(plane);
    }

    public List<Link> listAllSiteLinkByPlaneNameStartwith(String plane) {
        return siteLinkDao.listAllSiteLinkByPlaneNameStartwith(plane);
    }

    public List<Link> listAllOchLinkByPlane(String plane) {
        return ochLinkDao.listAllOchLinkByPlane(plane);
    }

    public List<Link> listAllOchLinkBySiteLinkIds(List<String> siteLinkIds) {
        return ochLinkDao.getAllOchLinksUnderSiteLinkIds(siteLinkIds);
    }

    public List<Link> getRefViewLinkByLinkIds(List<String> refLinkIds, ViewLinkType viewLinkType) {
        return viewLinkDao.listAllViewLinkBySupportLinkIdsAndLevel(refLinkIds, viewLinkType);
    }

    public List<TerminationPoint> getTerminationPointIdByRefEquipIds(String cardId) {
        return terminationPointDao.getAllTerminationPointByIdRegex(cardId);
    }

    public List<Link> getTpsRefPhyLinks(List<String> refTpIds) {
        return phyLinkDao.listAllPhyLinksUnderTps(refTpIds);
    }

    public Link getTunnelRefOchLink(
            String tunnelId) {
        return ochLinkDao.getOchLinkByTunnelId(tunnelId);
    }

    public List<String> getPlaneDescendants(String subnetId) {
        List<SubNetTreeNode> descendantNodes = subNetTreeNodeDao.getAllDescendants(subnetId);
        List<String> descendantIds = descendantNodes.stream()
                .map(SubNetTreeNode::getSubNetId).collect(
                        Collectors.toList());
        return descendantIds;
    }

    public List<Node> listAllViewNodesInSubnetHierarchy(List<String> subnetHierarchy) {
        return viewNodeDao.listAllViewNodesInSubnetHierarchy(subnetHierarchy);
    }

    public List<Link> listAllViewLinkBySubnetAndLinkLevel(List<String> subnetIds,
            ViewLinkType linkLevel) {
        return viewLinkDao.listAllViewLinksInSubnetHierarchyAndViewLinkType(subnetIds, linkLevel);
    }

    public List<Node> listAllViewNodesBySiteId(String viewNodeRefSiteId) {
        return viewNodeDao.listViewNodesBySiteId(viewNodeRefSiteId);
    }

    public void updateViewNode(Node node) {
        viewNodeDao.saveViewNode(node);
    }

    public List<Link> retrieveAllRelatedSiteLinkBySiteAndSubnet(String siteId,
            String subnetId) {
        return siteLinkDao.listAllSiteRelatedSiteLinksBySubnetIdAndSite(siteId, subnetId);
    }


    public List<String> getTunnelIdsUnderRefTp(String tpId) {
        List<LinkStateDto> tunnelInfos = tunnelDao.getAllTunnelsStateUnderTp(tpId);
        return tunnelInfos.stream().map(LinkStateDto::getId).collect(Collectors.toList());
    }

    public List<String> listAllOchLinkIdsBySiteLinkIds(List<String> siteLinkIds) {
        return ochLinkDao.retrieveAllOchLinkBySupportingLinkIds(siteLinkIds);
    }

    public boolean existedSubnet(String subnetId) {
        return subNetTreeNodeDao.existsBySubNetId(subnetId);
    }

    public SubNetTreeNode getSubnetBySubnetId(String subnetId) {
        return subNetTreeNodeDao.findBySubNetId(subnetId).orElse(null);
    }

    public List<SubNetTreeNode> retrieveAllSubnets() {
        return subNetTreeNodeDao.findAll();
    }

    public boolean existedSite(String siteId) {
        return siteNodeDao.existSiteBySiteId(siteId);
    }

    public PageResult<Tunnel> retrieveAllTunnelBetweenSitePaged(String sourceSiteId,
            String destSiteId, int pageNum, int pageSize, List<FilterItem> filterItems,
            List<SortItem> sortItems) {
        return tunnelDao.listAllTunnelBetweenSitePaged(sourceSiteId, destSiteId, pageNum, pageSize,
                filterItems, sortItems);
    }

    public List<String> getOccupiedOchLinkIdsBetweenTwoSite(String sourceSiteId, String destSiteId,
            String subnetId) {
        return ochLinkDao.getOccupiedOchLinkIdsBetweenSiteBySubnetId(sourceSiteId, destSiteId,
                subnetId);
    }

    public List<TunnelRateInfo> retrieveAllTunnelRateInfoByOchLinkIds(
            List<String> occupiedOchLinkIds) {
        return tunnelDao.retrieveAllTunnelRateInfoByOchLinkIds(occupiedOchLinkIds);
    }

    public Node getViewNodeByViewNodeId(String viewNodeId) {
        return viewNodeDao.getViewNodeById(viewNodeId);
    }


    public Map<String, Node> batchGetNeNodes(List<String> neIds) {
        long t0 = System.currentTimeMillis();
        List<Node> opNodes = phyNodeDao.listOperPhyNodeByIds(neIds);
        long t1 = System.currentTimeMillis();
        Map<String, Node> result = opNodes.stream()
                .collect(Collectors.toMap(node -> node.getNodeId().getValue(), n -> n));
        Set<String> missingNeIds = CommonUtils.getDifferenceSetByGuava(new HashSet<>(neIds),
                result.keySet());
        long t2 = System.currentTimeMillis();
        if (CollectionUtils.isEmpty(missingNeIds)) {
            log.info("[TIMING] batchGetNeNodes: db={}ms, convert={}ms, total={}ms, count={}",
                    t1 - t0, t2 - t1, t2 - t0, neIds.size());
            return result;
        }
        List<Node> configNodes = phyNodeDao.listConfigPhyNodeByIds(missingNeIds);
        configNodes.forEach(n ->
                result.putIfAbsent(n.getNodeId().getValue(), n));
        log.info("[TIMING] batchGetNeNodes: op={}ms, config={}ms, total={}ms, count={}",
                t1 - t0, System.currentTimeMillis() - t2, System.currentTimeMillis() - t0,
                neIds.size());
        return result;
    }

    public Map<String, Node> batchGetNeNodesLight(List<String> neIds) {
        long t0 = System.currentTimeMillis();
        List<Node> opNodes = phyNodeDao.listLightOpPhyNodeByIds(neIds);
        long t1 = System.currentTimeMillis();
        Map<String, Node> result = opNodes.stream()
                .collect(Collectors.toMap(node -> node.getNodeId().getValue(), n -> n));
        Set<String> missingNeIds = CommonUtils.getDifferenceSetByGuava(new HashSet<>(neIds),
                result.keySet());
        long t2 = System.currentTimeMillis();
        if (CollectionUtils.isEmpty(missingNeIds)) {
            log.info("[TIMING] batchGetNeNodes: db={}ms, convert={}ms, total={}ms, count={}",
                    t1 - t0, t2 - t1, t2 - t0, neIds.size());
            return result;
        }
        List<Node> configNodes = phyNodeDao.listLightConfigPhyNodeByIds(missingNeIds);
        configNodes.forEach(n ->
                result.putIfAbsent(n.getNodeId().getValue(), n));
        log.info("[TIMING] batchGetNeNodes: op={}ms, config={}ms, total={}ms, count={}",
                t1 - t0, System.currentTimeMillis() - t2, System.currentTimeMillis() - t0,
                neIds.size());
        return result;
    }

    public Map<String, Node> batchGetSiteNodesLight(List<String> siteIds) {
        List<Node> siteNode = siteNodeDao.listAllLightNodeByIds(siteIds);
        Map<String, Node> result = siteNode.stream()
                .collect(Collectors.toMap(node -> node.getNodeId().getValue(), n -> n));
        return result;
    }

    public Map<String, Link> batchGetPhyLinks(Set<String> phyLinkIds) {
        List<Link> phyLinks = phyLinkDao.getAllPhyLinksByIds(new ArrayList<>(phyLinkIds));
        Map<String, Link> result = phyLinks.stream()
                .collect(Collectors.toMap(link -> link.getLinkId().getValue(), link -> link));
        return result;
    }

    public Map<String, Link> batchGetPhyLinksLight(Set<String> phyLinkIds) {
        List<Link> phyLinks = phyLinkDao.getAllPhyLinksByIds(new ArrayList<>(phyLinkIds));
        Map<String, Link> result = phyLinks.stream()
                .collect(Collectors.toMap(link -> link.getLinkId().getValue(), link -> link));
        return result;
    }

    public Map<String, Link> batchGetSiteLinksLight(Set<String> siteLinkIds) {
        List<Link> siteLinks = siteLinkDao.listAllSiteLinkLightByIds(new ArrayList<>(siteLinkIds));
        Map<String, Link> result = siteLinks.stream()
                .collect(Collectors.toMap(link -> link.getLinkId().getValue(), link -> link));
        return result;
    }

    public Map<String, List<String>> aggregateSiteLinkXcs(
            Set<String> siteLinkIds) {
        Map<String, List<String>> crossConnectionsMap = siteLinkDao.batchGetSiteLinkXcs(
                siteLinkIds);
        return crossConnectionsMap;
    }

    public List<OchLinkBriefInfo> getBriefOchLinksBasedOnPhyLinks(List<String> refPhyLinkIds) {
        log.debug("start to get och links based on phyLink");
        List<String> siteLinkIds = siteLinkDao.retrieveAllSiteLinkIdsBySupportingLinkIds(
                refPhyLinkIds);
        List<String> supportLinkIds =
                CollectionUtils.isEmpty(siteLinkIds) ? refPhyLinkIds : siteLinkIds;
        return ochLinkDao.retrieveAllOchLinkBriefInfoByOchLinkIds(supportLinkIds);
    }

    public Map<String, Map<String, TerminationPoint>> batchGetNeTpMap(Set<String> neIds,
            Set<String> tpIds) {
        log.debug("batch get ne tp Map the neIds:{} and tpIds:{}", neIds, tpIds);
        Map<String, Map<String, TerminationPoint>> result = terminationPointDao.batchGetOpNeTpMap(
                neIds, tpIds);
        Set<String> missingNeIds = new HashSet<>(neIds);
        missingNeIds.removeAll(result.keySet());
        if (!missingNeIds.isEmpty()) {
            log.debug("batchGetNeTpMap missing in op, fallback to config: {}", missingNeIds);
            Map<String, Map<String, TerminationPoint>> configMap =
                    terminationPointDao.batchGetConfigNeTpMap(missingNeIds, tpIds);
            configMap.forEach(result::putIfAbsent);
        }
        return result;
    }

    public Map<String, Map<String, Equipments>> batchGetNeEquipMap(Set<String> neIds,
            Set<String> equipIds) {
        log.debug("batch get ne tp Map the neIds:{} and eQIds:{}", neIds, equipIds);
        Map<String, Map<String, Equipments>> result = equipmentsDao.batchGetOpNeEqMap(
                neIds, equipIds);
        Set<String> missingNeIds = new HashSet<>(neIds);
        missingNeIds.removeAll(result.keySet());
        if (!missingNeIds.isEmpty()) {
            log.debug("batchGetNeEquipMap missing in op, fallback to config: {}", missingNeIds);
            Map<String, Map<String, Equipments>> configMap =
                    equipmentsDao.batchGetConfigNeEqMap(missingNeIds, equipIds);
            configMap.forEach(result::putIfAbsent);
        }
        return result;
    }

    public PageResult<Link> retrieveAllOchLinkPaged(Integer pageNum, Integer pageSize,
            List<FilterItem> filterItems, List<SortItem> sortItems) {
        return ochLinkDao.listAllOchLinkPaged(pageNum, pageSize, filterItems, sortItems);
    }

//    public List<Node> retrieveAllSiteNodeByIds(List<String> nodeIds) {
//        return siteNodeDao.listAllNodeByIds(nodeIds);
//    }
//    public String getSiteLinkProperty(Link siteLink, String key) {
//        Properties properites = siteLink.getAugmentation(Link1.class).getSite().getProperties();
//        if (properites != null) {
//            Optional<Property> propOp = properites.getProperty().stream().filter(x -> x.getName().equalsIgnoreCase(key)).findFirst();
//            if (propOp.isPresent()) {
//                return propOp.get().getValue();
//            }
//        }
//        return null;
//    }
}
