package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import net.flex.dci.otn.controller.allocate.designer.NeDesigner;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelInput;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtReusedStrategy;
import net.flex.dci.otn.controller.allocate.link.common.BomGenerator;
import net.flex.dci.otn.controller.allocate.common.YangDataConverter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.BomInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.ne.bom.group.info.site.NeBomInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.reused.nodes.snapshot.ConfigBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.Nodes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateTunnelsInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateTunnelsOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateTunnelsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.allocate.tunnels.output.BomInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.allocate.tunnels.output.TunnelAllocateResult;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.allocate.tunnels.output.TunnelAllocateResultBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.sequence.RouteSequence;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result.ReusedNodesSnapshot;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result.ReusedNodesSnapshotBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result.Vendor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result.VendorBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result.vendor.RouteInfos;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info.RouteBundleInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TunnelAllocator {

    @Autowired
    private BomGenerator bomGenerator;

    @Autowired
    private OtReusedStrategy otReusedStrategy;

    @Autowired
    private PhyNodeDao phyNodeDao;

    @Autowired
    private NeDesigner neDesigner;


    public AllocateTunnelsOutput doIt(AllocateTunnelsInput input) throws CommonException {
        log.debug("start allocate tunnel");
        Map<String, Map<String, NeBomInfo>> totalBomInfo = new HashMap<>();//key is the same as the  key for bomMetaMap in BomMetaConfig.class, {bomMetaKey,{neId,neBomInfo}}
        List<RouteBundleInfo> routeBundleInfoList = input.getRouteBundleInfo();
        List<Nodes> reusedNodeList = new ArrayList<>();
        List<Vendor> resultByVendor = new ArrayList<>();
        for (RouteBundleInfo bundle : routeBundleInfoList) {
            try {
                //一个bundleInfo 就是一个厂家需要建立的tunnel数
                Vendor vendor = allocateBundleOfTunnel(bundle, totalBomInfo, reusedNodeList, input);
                resultByVendor.add(vendor);
            } catch (Exception e) {
                log.error("Failed to allocate tunnel for {}.", input, e);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Failed to allocate tunnel" + e.toString(), e);
            }
        }

        try {
            ReusedNodesSnapshot reusedNodesSnapshot = new ReusedNodesSnapshotBuilder()
                    .setConfig(new ConfigBuilder().setNodes(reusedNodeList).build())
                    .setOp(null).build();

            TunnelAllocateResult tunnelAllocateResult = new TunnelAllocateResultBuilder()
                    .setVendor(resultByVendor)
                    .setReusedNodesSnapshot(reusedNodesSnapshot).build();

            BomInfo bomInfo;
            try {
                bomInfo = bomGenerator.constructBomInfo(totalBomInfo);
            } catch (NeDesignerException e) {
                log.error("Failed to construct output.", e);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Failed to generate BOM." + e.getCause().getMessage(), e);
            }
            return new AllocateTunnelsOutputBuilder()
                    .setTunnelAllocateResult(tunnelAllocateResult)
                    .setBomInfo(new BomInfoBuilder(bomInfo).build())
                    .setReturnCode(RpcResultType.Success).build();
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to construct output.", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "construct output error" + e.toString(), e);
        }

    }

    private Vendor allocateBundleOfTunnel(RouteBundleInfo bundleInfo, final Map<String, Map<String, NeBomInfo>> totalBomInfo, final List<Nodes> reusedNodeSnapshotList, AllocateTunnelsInput input)
            throws NeDesignerException {

        TunnelInput tunnelInput = getTunnelInput(bundleInfo, input);

        //在这个vendor里面，以下5个值不会变，因为每次allocateBundleOfTunnel中，都是同一条siteLink
        String siteLinkSrcNodeId = tunnelInput.getSiteLinkSrcNode().getNodeId().getValue();
        String siteLinkDestNodeId = tunnelInput.getSiteLinkDestNode().getNodeId().getValue();
        String siteLinkId = tunnelInput.getSiteLinks().get(0).getLinkId().getValue();
        String srcSiteId = PhysicalNodeIdNamingRule.getSiteId(tunnelInput.getSiteLinkSrcNode().getNodeId().getValue());
        String dstSiteId = PhysicalNodeIdNamingRule.getSiteId(tunnelInput.getSiteLinkDestNode().getNodeId().getValue());

        //最早从数据库取出的镜像，留着后面输出镜像使用
        List<Node> reusedNodesInDbSrcSnapshot = new ArrayList<>(tunnelInput.getReusedNodesInDbSrc());
        List<Node> reusedNodesInDbDstSnapshot = new ArrayList<>(tunnelInput.getReusedNodesInDbDst());

        String vendorName = bundleInfo.getVendorName();
        String vendorType = bundleInfo.getProductType();

        Map<String, Node> bomNodes = new HashMap<>();//key is the nodeId
        Map<String, Node> reusedNodeInMemorySrc = new HashMap<>();//key is the nodeId
        Map<String, Node> reusedNodeInMemoryDst = new HashMap<>();//key is the nodeId
        Map<String, Node> reusedNodeInDbSrc = reusedNodesInDbSrcSnapshot.stream().collect(Collectors.toMap(item -> item.getNodeId().getValue(), Function.identity()));
        Map<String, Node> reusedNodeInDbDst = reusedNodesInDbDstSnapshot.stream().collect(Collectors.toMap(item -> item.getNodeId().getValue(), Function.identity()));
        List<RouteInfos> totalRouteInfos = new LinkedList<>();
        for (int index = 0; index < bundleInfo.getBundleNumber(); index++) {
            try {
                net.flex.dci.otn.controller.allocate.designer.model.RouteInfo info = getRouteResource(tunnelInput);
                totalRouteInfos.add(RouteYangDataConverter.getRouteInfo(info, index));

                //更新tunnelInput，供下次创建的业务使用
                updateTunnelInput(info, tunnelInput, srcSiteId, dstSiteId, reusedNodeInMemorySrc, reusedNodeInMemoryDst, reusedNodeInDbSrc, reusedNodeInDbDst, input.isIsReusedMixed(), siteLinkId);

                updateBomNodes(bomNodes, info, input.isIsReusedMixed(), siteLinkSrcNodeId, siteLinkDestNodeId, reusedNodeInDbSrc, reusedNodeInDbDst);

            } catch (CommonException ce) {
                log.error("allocate tunnel error.", ce);
                throw ce;
            } catch (Exception e) {
                log.error("allocate tunnel error.", e);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "create tunnel error: " + e.toString(), e);
            }

        }

        //更新总的reusedNode snapshot
        List<Nodes> currentReusedNodeSnapshot = getReusedNodeSnapShot(bomNodes, reusedNodesInDbSrcSnapshot, reusedNodesInDbDstSnapshot);
        reusedNodeSnapshotList.addAll(currentReusedNodeSnapshot);

        //更新总的bom信息
        totalBomInfo.putAll(bomGenerator.generateBomMap(bomNodes, vendorName, vendorType, currentReusedNodeSnapshot));

        return new VendorBuilder()
                .setSiteLinkId(siteLinkId)
                .setRouteInfos(totalRouteInfos)
                .setVendorName(vendorName).build();
    }

    private List<Nodes> getReusedNodeSnapShot(Map<String, Node> bomNodes, List<Node> reusedNodesInDbSrc, List<Node> reusedNodesInDbDst) {
        List<Nodes> output = new ArrayList<>();
        List<Node> reusedNodes = new ArrayList<>(reusedNodesInDbSrc.size() + reusedNodesInDbDst.size());
        reusedNodes.addAll(reusedNodesInDbSrc);
        reusedNodes.addAll(reusedNodesInDbDst);

        for (Node node : reusedNodes) {
            if (bomNodes.containsKey(node.getNodeId().getValue())) {
                output.add(YangDataConverter.convertToUINode(node));
            }
        }
        return output;
    }

    /**
     * 根据上次创建业务的output，需要更新以下东西：
     *
     * 1. reusedNodeInMemory
     *
     * 2. reusedNodeInDb
     *
     * 3. siteLinkSrcNode（src,dst）
     *
     * @param routeInfo
     * @param tunnelInput
     * @param srcSiteId
     * @param dstSiteId
     * @param reusedNodeInMemorySrc
     * @param reusedNodeInMemoryDst
     * @param reusedNodeInDbSrc
     * @param reusedNodeInDbDst
     * @param isReusedMixed
     * @param siteLinkId
     * @throws NeDesignerException
     */
    private void updateTunnelInput(RouteInfo routeInfo, TunnelInput tunnelInput, String srcSiteId, String dstSiteId,
            Map<String, Node> reusedNodeInMemorySrc,
            Map<String, Node> reusedNodeInMemoryDst,
            Map<String, Node> reusedNodeInDbSrc,
            Map<String, Node> reusedNodeInDbDst, Boolean isReusedMixed, String siteLinkId) throws NeDesignerException {

        List<Node> nodes = new ArrayList<>();
        nodes.addAll(routeInfo.getMain().getNodes());
        if (routeInfo.getSlave() != null && routeInfo.getSlave().getNodes() != null) {
            nodes.addAll(routeInfo.getSlave().getNodes());
        }

        for (Node node : nodes) {
            String nodeId = node.getNodeId().getValue();
            //因为输出的routeInfo也包含复用段的opc node，所以如果光电不复用，则不考虑opc node，需要从重用里面移除。
            String siteLinkSrcNodeId = tunnelInput.getSiteLinkSrcNode().getNodeId().getValue();
            String siteLinkDstNodeId = tunnelInput.getSiteLinkDestNode().getNodeId().getValue();
            if (siteLinkSrcNodeId.equals(nodeId)) {
                tunnelInput.setSiteLinkSrcNode(node);
            }
            if (siteLinkDstNodeId.equals(nodeId)) {
                tunnelInput.setSiteLinkDestNode(node);
            }
            if (!isReusedMixed) {
                if (nodeId.equals(siteLinkSrcNodeId) || node.getNodeId().getValue().equals(siteLinkDstNodeId)) {
                    continue;
                }
            }
            boolean canReused = otReusedStrategy.canReused(node, tunnelInput.getLineSignalRate(), tunnelInput.getCardType(), tunnelInput.getVendorName(), tunnelInput.getVendorType(), siteLinkId,tunnelInput.getServicetype());

            //更新srcSite重用map
            if (nodeId.contains(srcSiteId)) {
                if (reusedNodeInDbSrc.containsKey(nodeId)) {
                    if (canReused) {
                        reusedNodeInDbSrc.put(nodeId, node);//属于dbSrc，仍然可重用，更新map
                    } else {
                        reusedNodeInDbSrc.remove(nodeId);//属于dbSrc，不可重用，从map移除
                    }
                    continue;
                }
                if (canReused) {
                    reusedNodeInMemorySrc.put(nodeId, node);
                } else {
                    reusedNodeInMemorySrc.remove(nodeId);
                }
                continue;
            }

            if (nodeId.contains(dstSiteId)) {
                if (reusedNodeInDbDst.containsKey(nodeId)) {
                    if (canReused) {
                        reusedNodeInDbDst.put(nodeId, node);//属于dbDst，仍然可重用，更新map
                    } else {
                        reusedNodeInDbDst.remove(nodeId);//属于dbDst，不可重用，从map移除
                    }
                    continue;
                }
                if (canReused) {
                    reusedNodeInMemoryDst.put(nodeId, node);
                } else {
                    reusedNodeInMemoryDst.remove(nodeId);
                }
            }

        }

        tunnelInput.setReusedNodesInMemorySrc(new ArrayList<>(reusedNodeInMemorySrc.values()));
        tunnelInput.setReusedNodesInMemoryDst(new ArrayList<>(reusedNodeInMemoryDst.values()));
        tunnelInput.setReusedNodesInDbSrc(new ArrayList<>(reusedNodeInDbSrc.values()));
        tunnelInput.setReusedNodesInDbDst(new ArrayList<>(reusedNodeInDbDst.values()));

    }

    private TunnelInput getTunnelInput(RouteBundleInfo bundleInfo, AllocateTunnelsInput input) throws NeDesignerException {
        ParamCreate param = new ParamCreate();
        param.parser(input);

        String vendorName = bundleInfo.getVendorName();
        String productType = bundleInfo.getProductType();
        NodeType nodeType = bundleInfo.getNodeType();
        Integer bundleNumber = bundleInfo.getBundleNumber();
        String plane = bundleInfo.getPlaneName();
        List<Link> primarySiteLinks = new LinkedList<>();
        List<Link> secondarySiteLinks = new LinkedList<>();

        List<String> primarySiteLinkIds = getPrimarySiteLinks(bundleInfo.getRouteInfo());
        List<String> secondarySiteLinkIds = getSecondarySiteLinks(bundleInfo.getRouteInfo());

        if (!secondarySiteLinkIds.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "Only support primary route");
        }
        if (primarySiteLinkIds.size() != 1) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "primary route should only one siteLink");
        }
        if (vendorName == null || vendorName.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "vendorName is mandatory");
        }
        if (productType == null || productType.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "productType is mandatory");
        }
        if (bundleNumber <= 0) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "bundleNumber must larger than 1");
        }

        if (nodeType == null || !nodeType.equals(NodeType.TD)) {
            nodeType = NodeType.TD;
        }

        for (String linkId : primarySiteLinkIds) {
            for (Link siteLink : param.getPossibleSiteLinks()) {
                if (siteLink.getLinkId().getValue().equals(linkId)) {
                    primarySiteLinks.add(siteLink);
                }
            }
        }
        if (secondarySiteLinkIds != null) {
            for (String linkId : secondarySiteLinkIds) {
                for (Link siteLink : param.getPossibleSiteLinks()) {
                    if (siteLink.getLinkId().getValue().equals(linkId)) {
                        secondarySiteLinks.add(siteLink);
                    }
                }
            }
        }

        //暂时都只有一个
        Link siteLink = primarySiteLinks.get(0);
        String siteLinkId = siteLink.getLinkId().getValue();
        FrequencyAvailable frequencyAvailable = new FrequencyAvailable(siteLink);
        String srcNeId = PhysicalTpIdNamingRule.getNodeId(siteLink.getSource().getSourceTp().getValue());
        Node srcNe = phyNodeDao.getConfigPhyNodeById(srcNeId);
        if (srcNe == null) {
            String msg = String.format("Failed to get siteLink: %s,  source node:%s from db.", siteLink.getLinkId().getValue(), srcNeId);
            log.error(msg);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
        }
        String desNeId = PhysicalTpIdNamingRule.getNodeId(siteLink.getDestination().getDestTp().getValue());
        Node desNe = phyNodeDao.getConfigPhyNodeById(desNeId);
        if (desNe == null) {
            String msg = String.format("Failed to get siteLink: %s, dest node:%s from db.", siteLink.getLinkId().getValue(), desNeId);
            log.error(msg);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
        }

        String srcSiteId = PhysicalNodeIdNamingRule.getSiteId(srcNeId);
        String dstSiteId = PhysicalNodeIdNamingRule.getSiteId(desNeId);
        Boolean isReusedTpc = input.isIsReusedTpc();
        Boolean isReusedMixed = input.isIsReusedMixed();
        Boolean isReused = isReusedMixed || isReusedTpc;
        List<Node> reusedNodesInDbSrc = Collections.EMPTY_LIST;
        List<Node> reusedNodesInDbDst = Collections.EMPTY_LIST;
        if (isReused) {
            reusedNodesInDbSrc = otReusedStrategy
                    .getReusedNodePoolFromDb(srcSiteId, param.getLinePortSignalRate(), param.getCardType(), vendorName, productType, isReusedMixed, siteLinkId, plane,param.getServiceType());
            reusedNodesInDbDst = otReusedStrategy
                    .getReusedNodePoolFromDb(dstSiteId, param.getLinePortSignalRate(), param.getCardType(), vendorName, productType, isReusedMixed, siteLinkId, plane,param.getServiceType());
        }
        return TunnelInput.builder()
                .cardType(param.getCardType())
                .tunnelSignalRate(param.getTunnelSignalRate())
                .clientMedium(param.getClientMedium())
                .lineSignalRate(param.getLinePortSignalRate())
                .siteLinks(primarySiteLinks)
                .vendorName(vendorName)
                .vendorType(productType)
                .reusedNodesInDbSrc(reusedNodesInDbSrc)
                .reusedNodesInDbDst(reusedNodesInDbDst)
                .frequencyAvailable(frequencyAvailable)
                .siteLinkSrcNode(srcNe)
                .siteLinkDestNode(desNe)
                .plane(plane)
                .riskGroupName(param.getRiskGroupName())
                .build();
    }


    /**
     * 如果新生成的tunnel，更新了之前存的node，则替换
     *
     * @param bomNodes
     * @param info
     * @param isMixed
     * @param siteLinkSrcNodeId
     * @param siteLinkDestNodeId
     * @param reusedNodesInDbSrc
     * @param reusedNodesInDbDst
     * @return
     */
    private void updateBomNodes(Map<String, Node> bomNodes, RouteInfo info, Boolean isMixed,
            String siteLinkSrcNodeId, String siteLinkDestNodeId,
            Map<String, Node> reusedNodesInDbSrc,
            Map<String, Node> reusedNodesInDbDst) {
        List<Node> newNodes = new ArrayList<>();
        newNodes.addAll(info.getMain().getNodes());
        if (info.getSlave() != null && info.getSlave().getNodes() != null) {
            newNodes.addAll(info.getSlave().getNodes());
        }

        for (Node node : newNodes) {
            if (!isMixed) {
                //当OPC和TPC没有混用时，bom Node中要把siteLink中的node移除
                if (node.getNodeId().getValue().equals(siteLinkSrcNodeId) || node.getNodeId().getValue().equals(siteLinkDestNodeId)) {
                    continue;
                }
            } else {
                //当混用光电时，如果此复用段涉及到的node没有被重用，则也不计入bom中
                if (node.getNodeId().getValue().equals(siteLinkSrcNodeId) && !reusedNodesInDbSrc.containsKey(node.getNodeId().getValue())) {
                    continue;
                }
                if (node.getNodeId().getValue().equals(siteLinkDestNodeId) && !reusedNodesInDbDst.containsKey(node.getNodeId().getValue())) {
                    continue;
                }
            }
            bomNodes.put(node.getNodeId().getValue(), node);
        }
    }

    private net.flex.dci.otn.controller.allocate.designer.model.RouteInfo getRouteResource(TunnelInput input) {
        log.debug("build tunnel route info start...");

        net.flex.dci.otn.controller.allocate.designer.model.RouteInfo info = null;
        try {
            info = neDesigner.allocateTunnel(input);
        } catch (NeDesignerException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "error in neDesign " + e.getCause().getMessage(), e);
        }

        log.debug("build tunnel route info done.");
        return info;
    }


    private List<String> getPrimarySiteLinks(List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo> routeList) throws CommonException {
        List<String> siteLinkIds = new LinkedList<>();

        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo route : routeList) {
            siteLinkIds.addAll(getSiteLinkWithRouteSequences(route.getPrimary().getRouteSequence()));
        }
        return siteLinkIds;
    }

    private List<String> getSecondarySiteLinks(List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo> routeList) throws CommonException {
        List<String> siteLinkIds = new LinkedList<>();

        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo route : routeList) {
            if (route.getSecondary() != null) {
                siteLinkIds.addAll(getSiteLinkWithRouteSequences(route.getSecondary().getRouteSequence()));
            }
        }
        return siteLinkIds;
    }

    private Collection<? extends String> getSiteLinkWithRouteSequences(List<RouteSequence> routeSequenceList) throws CommonException {
        List<String> siteLinkIds = new LinkedList<>();
        for (RouteSequence rs : routeSequenceList) {
            if (rs.getResourceType().getImplementedInterface().getName()
                    .equals(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.Link.class.getName())) {
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.Link link =
                        (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.Link) rs.getResourceType();
                siteLinkIds.add(link.getLinkHop().getLinkId().getValue());
            } else {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "only support Link hop when create-tunnel");
            }
        }
        return siteLinkIds;
    }

}
