/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.INMSRetrieveOperations;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.NmsRetrieverOperationsResolver;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.OchLink;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.PhyLink;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.SiteLink;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.SiteTunnel;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.PhyEquipment;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.PhyNe;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.PhyTp;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.SiteNode;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.SiteRack;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.SiteTp;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NMSUtils;
import net.flex.dci.otn.controller.nms.utils.NetConfConvertors;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import net.flex.dci.otn.controller.nms.utils.PagedList;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyNodeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyNodePagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteNodeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteNodePagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.UpdateNodeLocationInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.UpdateNodeLocationOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.UpdateNodeLocationOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @date: 2021/4/8
 */
@Slf4j
@Component
public class NodeHandler extends AbstractBaseHandler {


    @Autowired
    private NmsRetrieverOperationsResolver nmsRetrieverOperationsResolver;

    public NodeHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public UpdateNodeLocationOutput updateNodeLocation(UpdateNodeLocationInput input)
            throws Exception {
        UpdateNodeLocationOutputBuilder outputBuilder = new UpdateNodeLocationOutputBuilder();

        NMSUtils.checkUpdateNodeInput(input);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node siteNode = netconfTopology
                .getSiteNode(input.getSiteId());
        if (siteNode != null) {
            SupportingRack siteRack = netconfTopology
                    .getRack(new TopologyId(Constants.SITE_TOPO_KEY), new NodeId(input.getSiteId()),
                            input.getRackId());
            if (siteRack == null) {
                throw new Exception("Error Rack Info");
            }

            if (input.getUptRackId() == null || "".equals(input.getUptRackId())) {
                for (SupportingNe supNe : siteRack.getSupportingNe()) {
                    if (supNe.getLocation().equals(input.getUptLocation())) {
                        throw new Exception(
                                "Duplicate location");
                    }
                }
                netconfTopology
                        .mergeNeLocation(input.getSiteId(), input.getRackId(), input.getNodeId(),
                                input.getUptLocation());
            } else {
                if (!input.getRackId().equals(input.getUptRackId())) {
                    SupportingRack uptSiteRack = netconfTopology
                            .getRack(new TopologyId(Constants.SITE_TOPO_KEY),
                                    new NodeId(input.getSiteId()),
                                    input.getUptRackId());
                    if (uptSiteRack == null) {
                        throw new Exception(
                                "Error update Rack Info");
                    }

                    for (SupportingNe supNe : uptSiteRack.getSupportingNe()) {
                        if (supNe.getLocation().equals(input.getUptLocation())) {
                            throw new Exception(
                                    "Duplicate location");
                        }
                    }
                    netconfTopology.mergeRackNeLocation(input.getSiteId(), input.getUptRackId(),
                            uptSiteRack,
                            input.getNodeId(), input.getUptLocation());
                    netconfTopology.removeNeLocation(input.getSiteId(), input.getRackId(),
                            input.getNodeId());
                } else {
                    for (SupportingNe supNe : siteRack.getSupportingNe()) {
                        if (supNe.getLocation().equals(input.getUptLocation())) {
                            throw new Exception(
                                    "Duplicate location");
                        }
                    }
                    netconfTopology.mergeNeLocation(input.getSiteId(), input.getRackId(),
                            input.getNodeId(),
                            input.getUptLocation());
                }
            }
        } else {
            throw new Exception("Error Site Info");
        }

        outputBuilder.setReturnCode(RpcResultType.Success);
        return outputBuilder.build();
    }

    @Override
    public PagedList getSiteNodePaged(GetSiteNodePagedInput input) throws Exception {
        TopologyId topologyRef = input.getTopologyRef();
        NodeId nodeRef = input.getNodeRef();
        String rackRef = input.getRackRef();
        String equipRef = input.getEquipmentRef();
        TpId tpRef = input.getTpRef();
        LinkId linkRef = input.getLinkRef();
        String tunnelRef = input.getTunnelRef();
        List<Node> nodes = getSiteNode(topologyRef, nodeRef, rackRef, equipRef, tpRef, linkRef,
                tunnelRef);

        PagedList pagedList = new PagedList(nodes);
        pagedList.setFilter(input.getFilter());
        pagedList.sort(input.getSortInfos());
        return pagedList;
    }

    @Override
    public PageResult<Node> getSiteNodePagedNew(RetrieveTopologyDto retrieveDto) {
        log.info("start to get phy link paged pageNum:{},pageSize:{}", retrieveDto.getPageNum(),
                retrieveDto.getPageSize());
        INMSRetrieveOperations operations = nmsRetrieverOperationsResolver.resolve(retrieveDto);
        PageResult<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> pageResult = operations.retrieveAllNodePaged(
                retrieveDto);
        PageResult<Node> result = new PageResult<>();
        //todo:test for cache
//        result.setList(NetConfConvertors.convertSiteNode2OutputNode(pageResult.getList()));
        result.setList(nmsOutputConverters.convert2NmsOutput(pageResult.getList()));
        result.setTotal(pageResult.getTotal());
        result.setPageNum(pageResult.getPageNum());
        result.setPages(pageResult.getPages());
        result.setPageSize(pageResult.getPageSize());
        return result;
    }

    @Override
    public List<Node> getSiteNode(RetrieveTopologyDto retrieveDto) {
        log.info("start to get phy link paged pageNum:{},pageSize:{}", retrieveDto.getPageNum(),
                retrieveDto.getPageSize());
        INMSRetrieveOperations operations = nmsRetrieverOperationsResolver.resolve(retrieveDto);
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> queryResult = operations.retrieveAllNode(
                retrieveDto);
        //todo:test for cache
        List<Node> result = new ArrayList<>(nmsOutputConverters.convert2NmsOutput(queryResult));
        return result;
    }


    @Override
    public List<Node> getSiteNode(GetSiteNodeInput input) throws Exception {
        TopologyId topologyRef = input.getTopologyRef();
        NodeId nodeRef = input.getNodeRef();
        String rackRef = input.getRackRef();
        String equipRef = input.getEquipmentRef();
        TpId tpRef = input.getTpRef();
        LinkId linkRef = input.getLinkRef();
        String tunnelRef = input.getTunnelRef();
        return getSiteNode(topologyRef, nodeRef, rackRef, equipRef, tpRef, linkRef, tunnelRef);
    }

    @Override
    public PagedList getPhyNodePaged(GetPhyNodePagedInput input) throws Exception {
        TopologyId topologyRef = input.getTopologyRef();
        NodeId nodeRef = input.getNodeRef();
        String rackRef = input.getRackRef();
        String equipRef = input.getEquipmentRef();
        TpId tpRef = input.getTpRef();
        LinkId linkRef = input.getLinkRef();
        String tunnelRef = input.getTunnelRef();
        List<Node> nodes = this
                .getPhyNode(topologyRef, nodeRef, rackRef, equipRef, tpRef, linkRef, tunnelRef);
        PagedList pagedList = new PagedList(nodes);
        pagedList.setFilter(input.getFilter());
        pagedList.sort(input.getSortInfos());
        return pagedList;
    }

    @Override
    public PageResult<Node> getPhyNodePagedNew(RetrieveTopologyDto retrieveDto)
            throws Exception {
        long totalStart = System.currentTimeMillis();
        log.info("get phy node paged");
        INMSRetrieveOperations operations = nmsRetrieverOperationsResolver.resolve(retrieveDto);

        long dbStart = System.currentTimeMillis();
        PageResult<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> phyNodePaged = operations
                .retrieveAllNodePaged(retrieveDto);
        long dbCost = System.currentTimeMillis() - dbStart;

        long convertStart = System.currentTimeMillis();
        PageResult<Node> result = new PageResult<>();
        result.setList(nmsOutputConverters.convert2NmsOutput(phyNodePaged.getList()));
        long convertCost = System.currentTimeMillis() - convertStart;

        result.setTotal(phyNodePaged.getTotal());
        result.setPageNum(phyNodePaged.getPageNum());
        result.setPages(phyNodePaged.getPages());
        result.setPageSize(phyNodePaged.getPageSize());

        long totalCost = System.currentTimeMillis() - totalStart;
        log.info(
                "getPhyNodePagedNew cost: {}ms, MongoDB cost: {}ms,convert cost: {}ms, totalSize: {}",
                totalCost, dbCost, convertCost, phyNodePaged.getList().size());
        return result;
    }


    @Override
    public List<Node> listAllPhyNodes(RetrieveTopologyDto retrieveDto) {
        log.info("get phy node paged");
        INMSRetrieveOperations operations = nmsRetrieverOperationsResolver.resolve(retrieveDto);
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> phyNodes = operations
                .retrieveAllNode(retrieveDto);
        List<Node> refNodes = nmsOutputConverters.convert2FullNmsOutput(phyNodes);
        return refNodes;
    }

    @Override
    public List<Node> getPhyNode(GetPhyNodeInput input) throws Exception {
        TopologyId topologyRef = input.getTopologyRef();
        NodeId nodeRef = input.getNodeRef();
        String rackRef = input.getRackRef();
        String equipRef = input.getEquipmentRef();
        TpId tpRef = input.getTpRef();
        LinkId linkRef = input.getLinkRef();
        String tunnelRef = input.getTunnelRef();
        return getPhyNode(topologyRef, nodeRef, rackRef, equipRef, tpRef, linkRef, tunnelRef);
    }


    public List<Node> getPhyNode(TopologyId topologyRef, NodeId nodeRef, String rackRef,
            String equipRef,
            TpId tpRef,
            LinkId linkRef, String tunnelRef) throws Exception {
        log.debug(
                "start get all PHY Node. topology:{}, node:{}, rack:{}, equip:{}, tp:{}, link:{}, tunnel:{}",
                topologyRef == null ? "null" : topologyRef.getValue(),
                nodeRef == null ? "null" : nodeRef.getValue(),
                rackRef == null ? "null" : rackRef, equipRef == null ? "null" : equipRef,
                tpRef == null ? "null" : tpRef.getValue(),
                linkRef == null ? "null" : linkRef.getValue(),
                tunnelRef == null ? "null" : tunnelRef);

        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> ntNodes = null;
        if (topologyRef == null) {
            throw new Exception("topologyRef is mandatory.");
        } else if (topologyRef.getValue().contains(TopoNameConstants.Phy_Topo_Key)
                && nodeRef == null && rackRef == null
                && equipRef == null && tpRef == null && linkRef == null && tunnelRef == null) {
            ntNodes = netconfTopology.getPhyNodes();
        } else if (nodeRef != null && rackRef == null && equipRef == null && tpRef == null) {
            if (topologyRef.getValue().contains(TopoNameConstants.Site_Topo_Key)) {
                ntNodes = new SiteNode(netconfTopology).getPhyNodes(topologyRef, nodeRef);
            } else if (topologyRef.getValue().contains(TopoNameConstants.Phy_Topo_Key)) {
                ntNodes = new PhyNe(netconfTopology).getPhyNodes(topologyRef, nodeRef);
            } else {
                throw new Exception(
                        "not supported parameter compose.");
            }
        } else if (topologyRef.getValue().contains(TopoNameConstants.Site_Topo_Key)
                && nodeRef != null && rackRef != null) {
            ntNodes = new SiteRack(netconfTopology).getPhyNodes(topologyRef, nodeRef, rackRef);
        } else if (topologyRef.getValue().contains(TopoNameConstants.Phy_Topo_Key)
                && nodeRef != null && rackRef == null
                && equipRef != null) {
            ntNodes = new PhyEquipment(netconfTopology).getPhyNodes(topologyRef, nodeRef, equipRef);
        } else if (nodeRef != null && rackRef == null && tpRef != null) {
            if (topologyRef.getValue().equals(TopoNameConstants.Site_Topo_Key)) {
                ntNodes = new SiteTp(netconfTopology).getPhyNodes(topologyRef, nodeRef, tpRef);
            } else if (topologyRef.getValue().equals(TopoNameConstants.Phy_Topo_Key)) {
                ntNodes = new PhyTp(netconfTopology).getPhyNodes(topologyRef, nodeRef, tpRef);
            } else {
                throw new Exception(
                        "not supported parameter compose.");
            }
        } else if (nodeRef == null && rackRef == null && tpRef == null && linkRef != null) {
            if (topologyRef.getValue().contains(TopoNameConstants.Site_Topo_Key)) {
                ntNodes = new SiteLink(netconfTopology).getPhyNodes(topologyRef, linkRef);
            } else if (topologyRef.getValue().contains(TopoNameConstants.Phy_Topo_Key)) {
                ntNodes = new PhyLink(netconfTopology).getPhyNodes(topologyRef, linkRef);
            } else if (topologyRef.getValue().contains(TopoNameConstants.Och_Topo_Key)) {
                ntNodes = new OchLink(netconfTopology).getPhyNodes(topologyRef, linkRef);
            } else {
                throw new Exception(
                        "not supported parameter compose.");
            }
        } else if (topologyRef.getValue().contains(TopoNameConstants.Site_Topo_Key)
                && nodeRef == null && rackRef == null && tpRef == null && tunnelRef != null) {
            ntNodes = new SiteTunnel(netconfTopology).getPhyNodes(topologyRef, tunnelRef);
        } else {
            throw new Exception(
                    "not supported parameter compose.");
        }

        return NetConfConvertors.convertNtNode2OutputNode(ntNodes, this.netconfTopology);
    }

    public List<Node> getSiteNode(TopologyId topologyRef, NodeId nodeRef, String rackRef,
            String equipRef,
            TpId tpRef,
            LinkId linkRef, String tunnelRef) throws Exception {
        log.debug(
                "start get all Site Node topology:{}, node:{}, rack:{}, equip:{}, tp:{}, link:{}, tunnel:{}",
                topologyRef == null ? "null" : topologyRef.getValue(),
                nodeRef == null ? "null" : nodeRef.getValue(),
                rackRef == null ? "null" : rackRef, equipRef == null ? "null" : equipRef,
                tpRef == null ? "null" : tpRef.getValue(),
                linkRef == null ? "null" : linkRef.getValue(),
                tunnelRef == null ? "null" : tunnelRef);

        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> ntNodes = null;
        if (topologyRef == null) {
            throw new Exception("topologyRef is mandatory.");
        } else if (topologyRef.getValue().contains(TopoNameConstants.Site_Topo_Key)
                && nodeRef == null && rackRef == null
                && equipRef == null && tpRef == null && linkRef == null && tunnelRef == null) {
//            Topology topo = netconfTopology.getTopology(topologyRef);
//            if (topo == null) {
//                throw new Exception(
//                        "cannot find required Topology.");
//            }
            ntNodes = netconfTopology.listSiteNodes();
        } else if (nodeRef != null && rackRef == null && equipRef == null && tpRef == null) {
            if (topologyRef.getValue().contains(TopoNameConstants.Site_Topo_Key)) {
                ntNodes = new SiteNode(netconfTopology).getSiteNodes(topologyRef, nodeRef);
            } else if (topologyRef.getValue().contains(TopoNameConstants.Phy_Topo_Key)) {
                ntNodes = new PhyNe(netconfTopology).getSiteNodes(topologyRef, nodeRef);
            } else {
                throw new Exception(
                        "not supported parameter compose.");
            }
        } else if (topologyRef.getValue().contains(TopoNameConstants.Site_Topo_Key)
                && nodeRef != null && rackRef != null) {
            ntNodes = new SiteRack(netconfTopology).getSiteNodes(topologyRef, nodeRef, rackRef);
        } else if (topologyRef.getValue().contains(TopoNameConstants.Phy_Topo_Key)
                && nodeRef != null && rackRef == null
                && equipRef != null) {
            ntNodes = new PhyEquipment(netconfTopology)
                    .getSiteNodes(topologyRef, nodeRef, equipRef);
        } else if (nodeRef != null && rackRef == null && tpRef != null) {
            if (topologyRef.getValue().equals(TopoNameConstants.Site_Topo_Key)) {
                ntNodes = new SiteTp(netconfTopology).getSiteNodes(topologyRef, nodeRef, tpRef);
            } else if (topologyRef.getValue().equals(TopoNameConstants.Phy_Topo_Key)) {
                ntNodes = new PhyTp(netconfTopology).getSiteNodes(topologyRef, nodeRef, tpRef);
            } else {
                throw new Exception(
                        "not supported parameter compose.");
            }
        } else if (nodeRef == null && rackRef == null && tpRef == null && linkRef != null) {
            if (topologyRef.getValue().contains(TopoNameConstants.Site_Topo_Key)) {
                ntNodes = new SiteLink(netconfTopology).getSiteNodes(topologyRef, linkRef);
            } else if (topologyRef.getValue().contains(TopoNameConstants.Phy_Topo_Key)) {
                ntNodes = new PhyLink(netconfTopology).getSiteNodes(topologyRef, linkRef);
            } else if (topologyRef.getValue().contains(TopoNameConstants.Och_Topo_Key)) {
                ntNodes = new OchLink(netconfTopology).getSiteNodes(topologyRef, linkRef);
            } else {
                throw new Exception(
                        "not supported parameter compose.");
            }
        } else if (topologyRef.getValue().contains(TopoNameConstants.Site_Topo_Key)
                && nodeRef == null && rackRef == null && tpRef == null && tunnelRef != null) {
            ntNodes = new SiteTunnel(netconfTopology).getSiteNodes(topologyRef, tunnelRef);
        } else {
            throw new Exception(
                    "not supported parameter compose.");
        }

        return nmsOutputConverters.convert2NmsOutput(ntNodes);
    }

    @Override
    public List<Node> getUnStuffedPhyNode(String siteNodeId) throws Exception {
        return NetConfConvertors.convertNtNode2OutputNode(
                netconfTopology.getUnStuffedPhyNode(siteNodeId), this.netconfTopology);
    }


}
