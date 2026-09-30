package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.enums.NeSubType;
import net.flex.dci.otn.controller.allocate.common.AllocatorConfig;
import net.flex.dci.otn.controller.allocate.common.FrequencyAllocationOrder;
import net.flex.dci.otn.controller.allocate.common.service.WdmUtilService;
import net.flex.dci.otn.controller.allocate.designer.NeDesigner;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.designer.config.ProductTypeResolver;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelNewOchInput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelNewOchOutput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelReuseOchInput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelReuseOchOutput;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtReusedStrategy;
import net.flex.dci.otn.controller.allocate.designer.tunnel.TunnelNewOchAllocate;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.link.common.BomGenerator;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.BomInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkTerminationNodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateTunnels2Input;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateTunnels2InputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateTunnels2Output;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateTunnels2OutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnel3Input;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnel3InputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.TunnelCreationAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.allocate.tunnels._2.output.BomInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.allocate.tunnels._2.output.TunnelAllocateResult2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.allocate.tunnels._2.output.TunnelAllocateResult2Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.Nodes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.OchLinksSnapshot;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.OchLinksSnapshotBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.ReusedNodesSnapshot;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.SiteLinksSnapshot;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.SiteLinksSnapshotBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.Vendor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.VendorBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.och.links.snapshot.OchLinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.site.links.snapshot.SiteLinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor.NewOchTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor.NewOchTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor.ReuesedOchTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor.ReuesedOchTunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor._new.och.tunnel.TunnelRouteInfos;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.VendorOccupationRate;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.site.link.route.PrimaryReg;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.site.link.route.SecondaryReg;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.site.link.route.ThirdReg;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TunnelAllocator2 {

    public static final String L_PORT = "OTU-Line";
    public static final String SIG_PORT = "SIG";
    private static final String LINK_MODEL_PROPERTY = "model";
    private static final String LINK_MODEL_BONE20_ONE_TO_TWO = "10";
    public static final Set<String> MIX_CARD_TYPES = new HashSet<>(
            Arrays.asList("TMUX-1"));//比如同时支持10G，100G业务，并且L口不能全部用来走10G业务

    @Autowired
    private BomGenerator bomGenerator;

    @Autowired
    private OtReusedStrategy otReusedStrategy;

    @Autowired
    private PhyNodeDao phyNodeDao;

    @Autowired
    private TunnelUtilService tunnelUtilService;

    @Autowired
    private NeDesigner neDesigner;

    @Autowired
    private SiteLinkDao siteLinkDao;

    @Autowired
    private WdmUtilService wdmUtilService;

    @Autowired
    private NEInfoConfig neInfoConfig;
    @Autowired
    private NodeUtils nodeUtils;

    @Autowired
    private TunnelNewOchAllocate tunnelNewOchAllocate;

    @Autowired
    private AllocatorConfig allocatorConfig;

    public AllocateTunnels2Output doIt(AllocateTunnels2Input input) throws CommonException {
        TempInfo tunnelAllocateResultInfo = getAllocateTunnelsResult(input, false);
        List<Node> nodeSnapShot = tunnelAllocateResultInfo.nodeSnapShot;
        Map<String, Node> totalInMemoryNode = tunnelAllocateResultInfo.totalInMemoryNode;
        TunnelAllocateResult2 tunnelAllocateResult = tunnelAllocateResultInfo.tunnelAllocateResult2;
        //build output
        try {
            //build BOM
            BomInfo bomInfo = bomGenerator.constructBomInfo(totalInMemoryNode, nodeSnapShot);

            return new AllocateTunnels2OutputBuilder()
                    .setTunnelAllocateResult2(tunnelAllocateResult)
                    .setBomInfo(new BomInfoBuilder(bomInfo).build())
                    .setReturnCode(RpcResultType.Success).build();
        } catch (Exception e) {
            log.error("Failed to construct output.", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "construct output error" + e.toString(), e);
        }
    }

    protected TempInfo getAllocateTunnelsResult(AllocateTunnels2Input input, Boolean isReallocate) {
        ParamCreate param = new ParamCreate();
        param.parser(input);

        log.debug("start allocate tunnel");
        List<VendorOccupationRate> vendorOccupationRateList = input.getVendorOccupationRate();

        WDM_Band wdmBand = null;
        String opMode = null;
        String vendorNameFlag = vendorOccupationRateList.get(0).getVendorName();
        String productTypeFlag = vendorOccupationRateList.get(0).getProductType();
        try {

            Pair<String, WDM_Band> pair = tunnelUtilService.getOpModeAndWdm(vendorNameFlag,
                    productTypeFlag, input.getFrequenceWidth(), input.getCardType(), input
                            .getServiceType());
            opMode = pair.getLeft();
            wdmBand = pair.getRight();
        } catch (NeDesignerException e) {
            log.error("Failed to get opMode or wdmBand.", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get opMode or wdmBand" + e.toString(), e);

        }

        Map<String, List<Link>> reUsedOchMap = getReusedOchMap(input, param,
                opMode);//key is vendorName and productType

        //output  init
        List<Vendor> vendors = new ArrayList<>();
        List<Node> nodeSnapShot = new ArrayList<>();
        List<Link> ochLinkSnapshot = new ArrayList<>();
        List<Link> siteLinkSnapshot = new ArrayList<>();

        Map<String, Node> totalInMemoryNode = new HashMap<>();//node已经改变，但还没入库

        List<Link> reusedOchLinksTotal = reUsedOchMap.values().stream().flatMap(List::stream)
                .collect(Collectors.toList());

        SiteLinkRoute siteLinkRouteUpdated;
        if (isReallocate) {
            siteLinkRouteUpdated = normalizeBone20OneToTwoSiteLinkRoute(input.getSiteLinkRoute());
        } else {
            siteLinkRouteUpdated = validateBundleNum_And_Fresh_SiteLinkRoute(reusedOchLinksTotal,
                    normalizeBone20OneToTwoSiteLinkRoute(input.getSiteLinkRoute()), param,
                    vendorNameFlag, productTypeFlag, wdmBand);
        }

        Set<Long> usedCentralFrequenciesTotal = new HashSet<>();
        List<Long> initSiteLinkCenFres = new ArrayList<>(
                siteLinkRouteUpdated.getCentralFrequencies());
        for (VendorOccupationRate vendorOccupationRate : vendorOccupationRateList) {
            //init freq
            siteLinkRouteUpdated.getCentralFrequencies().clear();
            siteLinkRouteUpdated.getCentralFrequencies().addAll(initSiteLinkCenFres);
            siteLinkRouteUpdated.getCentralFrequencies().removeAll(usedCentralFrequenciesTotal);
            //prepare input
            String vendorName = vendorOccupationRate.getVendorName();
            String productType = vendorOccupationRate.getProductType();
            Integer toCreateBundleNum = vendorOccupationRate.getNumber();//需要创建的业务数量
            String key = TunnelUtil.createVendorProductKey(vendorName, productType);
            Integer failureGroupNum = param.getFailureRateVendors().get(key);
            Set<String> usedNodeSet = new HashSet<>();
            int remainingFailureGroups = failureGroupNum;

            //Scenario1: 首先用，可重用och
            List<ReuesedOchTunnel> reuesedOchTunnels = new ArrayList<>();
            List<Link> reusedOchLinks = reUsedOchMap.get(key);
            Map<String, List<Link>> reusedOchLinksGroupByNodeId = Collections.emptyMap();
            if (reusedOchLinks != null && !reusedOchLinks.isEmpty()) {
                Map<String, Boolean> reusedOchTdNodeCache = new HashMap<>();

                // key is all TD nodes used by the reused OCH route, including A/Z and REG TDs.
                reusedOchLinksGroupByNodeId = reusedOchLinks.stream()
                        .collect(Collectors.groupingBy(
                                link -> getReusedOchTdNodeGroupKey(link, reusedOchTdNodeCache)));
            }

            //Scenario2: 接着用选中的路由
            NewOchTunnel newOchTunnel = null;
            List<TunnelRouteInfos> tunnelRouteInfos = new ArrayList<>();
            List<Long> usedCentralFrequencies = new ArrayList<>();
            SiteLinkRoute siteLinkRoute = siteLinkRouteUpdated;
            while (toCreateBundleNum > 0) {
                // 每次循环创建一个failure group。group内业务可以共用同一组TD设备/OCH，
                // group之间必须避开已经使用过的TD设备，满足failure-rate的设备隔离语义。
                int toCreateBundlePerGroup = getFailureGroupBundleNumber(
                        toCreateBundleNum, remainingFailureGroups);
                int currentGroupCreated = 0;
                Set<String> currentGroupNodes = new HashSet<>();

                //先给reuse och，确保不同failure group不复用同一组TD设备
                for (Map.Entry<String, List<Link>> reusedOchEntry : reusedOchLinksGroupByNodeId.entrySet()) {
                    if (toCreateBundlePerGroup == 0) {
                        break;//此failure group已经创建够了
                    }
                    // 已经属于其他failure group的TD设备不能再被当前group复用，
                    // 否则failure-rate=2时两组业务仍可能落在同一组A/Z端TD设备上。
                    if (!Collections.disjoint(Arrays.asList(reusedOchEntry.getKey().split(",")),
                            usedNodeSet)) {
                        continue;
                    }
                    List<Link> reuseOchLinkList = reusedOchEntry.getValue();

                    for (Link reuseOchLink : reuseOchLinkList) {
                        if (toCreateBundlePerGroup == 0) {
                            break;//此group已经创建够了
                        }
                        String reusedOchLinkId = reuseOchLink.getLinkId().getValue();
                        int maxTunnelOch = tunnelUtilService.getMatchedOchReusedOdu(reuseOchLink,
                                param, vendorName, productType);
                        if (maxTunnelOch <= 0) {
                            continue;
                        }

                        int toCreateNumberForOch = Math.min(toCreateBundlePerGroup, maxTunnelOch);

                        TunnelReuseOchOutput tunnelReuseOchOutput = allocateTunnelsReuseOch(
                                vendorName, productType, toCreateNumberForOch, reusedOchLinkId,
                                param, totalInMemoryNode);
                        //build output
                        ReuesedOchTunnel reuesedOchTunnel = new ReuesedOchTunnelBuilder()
                                .setLinkId(reusedOchLinkId)
                                .setTpcRoute(tunnelReuseOchOutput.getTpcRoutes())
                                .build();
                        reuesedOchTunnels.add(reuesedOchTunnel);

                        //put snapshot and inMemory node
                        ochLinkSnapshot.add(tunnelReuseOchOutput.getOchLinkSnapshot());
                        nodeSnapShot.addAll(tunnelReuseOchOutput.getTpcNodeSnapshot());
                        totalInMemoryNode.putAll(tunnelReuseOchOutput.getInMemoryTpcNode());
                        toCreateBundleNum = toCreateBundleNum - toCreateNumberForOch;
                        toCreateBundlePerGroup = toCreateBundlePerGroup - toCreateNumberForOch;
                        currentGroupCreated = currentGroupCreated + toCreateNumberForOch;
                        currentGroupNodes.addAll(Arrays.asList(reusedOchEntry.getKey().split(",")));
                        addTdNodes(currentGroupNodes, tunnelReuseOchOutput.getInMemoryTpcNode());

                    }
                }

                if (toCreateBundlePerGroup > 0) {//scenario1可重用OCH还没创够
                    if (siteLinkRoute == null) {
                        log.error(
                                "Failed to allocate tunnel, because still need to create {} tunnel, but no available route, for inpuy: {}.",
                                toCreateBundleNum, input);
                        throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                                "Failed to allocate tunnel, because no available route chosen.");
                    }
                    // 创建new OCH时有两类TD不能选：
                    // 1. 前面failure group已经用过的TD，来自usedNodeSet，用于保证group间设备隔离。
                    // 2. 当前group已经用过但没有OT板卡槽位的TD，用addNoAvailableSlotTdNodes临时补充。
                    // 两类约束只在new OCH分配时合并，不能反向污染usedNodeSet，
                    // 否则会影响同group内其它reused OCH继续复用已有时隙。
                    Set<String> newOchExcludeNodeSet = new HashSet<>(usedNodeSet);
                    addNoAvailableSlotTdNodes(newOchExcludeNodeSet, totalInMemoryNode, vendorName,
                            productType, param.getCardType());
                    Set<String> nodeIdsBeforeNewOch = new HashSet<>(totalInMemoryNode.keySet());
                    TunnelNewOchOutput tunnelNewOchOutput = allocateTunnelsNewOch(
                            toCreateBundlePerGroup, siteLinkRoute, totalInMemoryNode, vendorName,
                            productType, param, input, opMode, newOchExcludeNodeSet,
                            wdmBand);

                    //减去使用了的频率
                    siteLinkRoute.getCentralFrequencies()
                            .removeAll(tunnelNewOchOutput.getUsedCentralFrequencies());
                    usedCentralFrequencies.addAll(tunnelNewOchOutput.getUsedCentralFrequencies());

                    //put snapshot and inMemory node
                    siteLinkSnapshot.addAll(
                            new HashSet<>(tunnelNewOchOutput.getSiteLinkSnapshot()));
                    nodeSnapShot.addAll(new HashSet<>(tunnelNewOchOutput.getNodeSnapshot()));
                    totalInMemoryNode.putAll(tunnelNewOchOutput.getInMemoryNode());
                    addNewTdNodes(currentGroupNodes, nodeIdsBeforeNewOch,
                            tunnelNewOchOutput.getInMemoryNode());

                    toCreateBundleNum = toCreateBundleNum - toCreateBundlePerGroup;
                    currentGroupCreated = currentGroupCreated + toCreateBundlePerGroup;
                    tunnelRouteInfos.addAll(tunnelNewOchOutput.getTunnelRouteInfos());
                }
                // 当前group成功创建后，再把它用到的TD设备加入全局排除集合。
                // 这样不会限制group内部共享设备，但会限制后续group继续使用这些设备。
                usedNodeSet.addAll(currentGroupNodes);
                remainingFailureGroups--;
                if (currentGroupCreated == 0) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "Failed to allocate tunnel, because no available resource for failure group.");
                }
            }

            if (!tunnelRouteInfos.isEmpty()) {
                //总共使用的centralFreq
                siteLinkRoute.getCentralFrequencies().clear();
                siteLinkRoute.getCentralFrequencies().addAll(usedCentralFrequencies);
                //build output
                newOchTunnel = new NewOchTunnelBuilder()
                        .setSiteLinkRoute(
                                new SiteLinkRouteBuilder(siteLinkRoute).setCentralFrequencies(
                                        new ArrayList<>(siteLinkRoute.getCentralFrequencies()))
                                        .build())
                        .setTunnelRouteInfos(tunnelRouteInfos)
                        .setOpMode(opMode)
                        .build();
                usedCentralFrequenciesTotal.addAll(usedCentralFrequencies);
//                //add bom
//                totalBomInfo.putAll(bomGenerator.generateBomMap(bomNodes, vendorName, productType, currentReusedNodeSnapshot));
//                totalBomInfo.putAll(tunnelNewOchOutput.getBomMap());
            }

            Vendor vendor = new VendorBuilder().setVendorName(vendorName)
                    .setProductType(productType)
                    .setReuesedOchTunnel(reuesedOchTunnels)
                    .setNewOchTunnel(newOchTunnel)
                    .build();
            vendors.add(vendor);
        }
        try {
            List<ReusedNodesSnapshot> reusedNodesSnapshot = nodeSnapShot.stream()
                    .map(node -> RouteYangDataConverter.getReusedNodesSnapshot(node))
                    .collect(Collectors.toList());
            List<Nodes> nodes = totalInMemoryNode.values().stream()
                    .map(node -> RouteYangDataConverter.getTunnelNodes(node))
                    .collect(Collectors.toList());

            OchLinksSnapshot ochLinksSnapshot = new OchLinksSnapshotBuilder().setOchLinks(
                    ochLinkSnapshot.stream().map(item -> new OchLinksBuilder()
                            .setLinkId(item.getLinkId().getValue())
                            .setOch(new OchBuilder().setAvailable(
                                    item.getAugmentation(Link1.class).getOch().getAvailable())
                                    .build())
                            .build()
                    ).collect(Collectors.toList())).build();
            SiteLinksSnapshot siteLinksSnapshot = new SiteLinksSnapshotBuilder().setSiteLinks(
                    siteLinkSnapshot.stream().map(item -> new SiteLinksBuilder()
                            .setLinkId(item.getLinkId().getValue())
                            .setSite(new SiteBuilder().setAvailable(
                                    item.getAugmentation(
                                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                                            .getSite()
                                            .getAvailable()).build())
                            .build()
                    ).collect(Collectors.toList())).build();

            TunnelAllocateResult2 tunnelAllocateResult = new TunnelAllocateResult2Builder()
                    .setReusedNodesSnapshot(reusedNodesSnapshot)
                    .setOchLinksSnapshot(ochLinksSnapshot)
                    .setSiteLinksSnapshot(siteLinksSnapshot)
                    .setVendor(vendors)
                    .setNodes(nodes).build();
            return new TempInfo(tunnelAllocateResult, nodeSnapShot, totalInMemoryNode);
        } catch (Exception e) {
            log.error("Failed to get tunnelAllocateResult.", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "construct output error" + e.toString(), e);
        }
    }

    private int getFailureGroupBundleNumber(int toCreateBundleNum, int remainingFailureGroups) {
        if (remainingFailureGroups <= 1) {
            return toCreateBundleNum;
        }
        // 动态按剩余业务/剩余group取ceil，保证业务尽量平均分配；
        // 例如64条、failure-rate=2时拆成32+32，而不是先耗尽某一组资源。
        return (int) Math.ceil(toCreateBundleNum / (double) remainingFailureGroups);
    }

    private String getReusedOchTdNodeGroupKey(Link reusedOchLink,
                                              Map<String, Boolean> reusedOchTdNodeCache) {
        Set<String> tdNodeIds = getReusedOchTdNodeIds(reusedOchLink, reusedOchTdNodeCache);
        if (tdNodeIds.isEmpty()) {
            tdNodeIds.add(reusedOchLink.getSource().getSourceNode().getValue());
            tdNodeIds.add(reusedOchLink.getDestination().getDestNode().getValue());
        }
        return tdNodeIds.stream().sorted().collect(Collectors.joining(","));
    }

    private Set<String> getReusedOchTdNodeIds(Link reusedOchLink,
                                              Map<String, Boolean> reusedOchTdNodeCache) {
        Set<String> tdNodeIds = new HashSet<>();
        Link1 ochLinkAttr = reusedOchLink.getAugmentation(Link1.class);
        Och och = ochLinkAttr == null ? null : ochLinkAttr.getOch();
        if (och == null) {
            return tdNodeIds;
        }
        if (och.getExplictRoute() == null || och.getExplictRoute().getRoute() == null) {
            addTdNodeId(tdNodeIds, reusedOchLink.getSource().getSourceNode().getValue(),
                    reusedOchTdNodeCache);
            addTdNodeId(tdNodeIds, reusedOchLink.getDestination().getDestNode().getValue(),
                    reusedOchTdNodeCache);
            return tdNodeIds;
        }

        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route route
                : och.getExplictRoute().getRoute()) {
            if (route.getPrimary() != null) {
                addTdNodeIdsFromRoute(tdNodeIds, route.getPrimary().getExplicitRouteObjects(),
                        reusedOchTdNodeCache);
            }
            if (route.getSecondary() != null) {
                addTdNodeIdsFromRoute(tdNodeIds, route.getSecondary().getExplicitRouteObjects(),
                        reusedOchTdNodeCache);
            }
            if (route.getThird() != null) {
                route.getThird().forEach(third -> addTdNodeIdsFromRoute(tdNodeIds,
                        third.getExplicitRouteObjects(), reusedOchTdNodeCache));
            }
        }
        if (tdNodeIds.isEmpty()) {
            addTdNodeId(tdNodeIds, reusedOchLink.getSource().getSourceNode().getValue(),
                    reusedOchTdNodeCache);
            addTdNodeId(tdNodeIds, reusedOchLink.getDestination().getDestNode().getValue(),
                    reusedOchTdNodeCache);
        }
        return tdNodeIds;
    }

    private void addTdNodeIdsFromRoute(Set<String> tdNodeIds,
                                        List<ExplicitRouteObjects> explicitRouteObjects,
                                        Map<String, Boolean> reusedOchTdNodeCache) {
        if (explicitRouteObjects == null) {
            return;
        }
        for (ExplicitRouteObjects explicitRouteObject : explicitRouteObjects) {
            if (explicitRouteObject.getPathRouteObject() == null) {
                continue;
            }
            for (PathRouteObject pathRouteObject : explicitRouteObject.getPathRouteObject()) {
                if (!(pathRouteObject.getResourceType() instanceof Tp)) {
                    continue;
                }
                Tp tp = (Tp) pathRouteObject.getResourceType();
                if (tp.getTpHop() == null || tp.getTpHop().getTpRef() == null) {
                    continue;
                }
                String tpId = tp.getTpHop().getTpRef().getValue();
                addTdNodeId(tdNodeIds, PhysicalTpIdNamingRule.getNodeId(tpId),
                    reusedOchTdNodeCache);
            }
        }
    }

    private void addTdNodeId(Set<String> tdNodeIds, String nodeId,
                             Map<String, Boolean> reusedOchTdNodeCache) {
        if (isTdNode(nodeId, reusedOchTdNodeCache)) {
            tdNodeIds.add(nodeId);
        }
    }

    private boolean isTdNode(String nodeId, Map<String, Boolean> reusedOchTdNodeCache) {
        if (reusedOchTdNodeCache.containsKey(nodeId)) {
            return reusedOchTdNodeCache.get(nodeId);
        }
        Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
        boolean isTd = node != null
                && node.getAugmentation(Node1.class) != null
                && node.getAugmentation(Node1.class).getPhysical() != null
                && NodeType.TD.equals(node.getAugmentation(Node1.class).getPhysical().getNodeType());
        reusedOchTdNodeCache.put(nodeId, isTd);
        return isTd;
    }
    /**
     * 在同一个failure group内部，只把已经没有可用OT板卡槽位的TD加入排除集合。
     * 如果TD还有空槽位，即使它已经承载了当前group的部分业务，也允许继续在该TD上创建新的OT板卡。
     * 典型场景：先复用一张8x100G板卡剩余的2个时隙，剩余6条业务仍应优先在同一TD上新增一张OT板卡。
     */
    private void addNoAvailableSlotTdNodes(Set<String> usedNodeSet, Map<String, Node> inMemoryNode, String vendorName, String productType, String cardType) {
        Card otCardInfo;
        try {
            otCardInfo = neInfoConfig.getNeInfo(vendorName, productType, NodeType.TD.name()).getCardByCardType(cardType);
        } catch (NeDesignerException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "failed to get OT card info: " + e.getMessage(), e);
        }
        for (Node node : inMemoryNode.values()) {
            Node1 node1 = node.getAugmentation(Node1.class);
            if (node1 == null || node1.getPhysical() == null || !NodeType.TD.equals(node1.getPhysical().getNodeType())) {
                continue;
            }
            try {
                // 组内复用时，只排除已经没有可用OT板卡槽位的TD；
                // 仍有空槽的TD可以继续创建新OT板卡，支撑同一failure group内的资源复用。
                nodeUtils.pickedAvailableSlot(node, otCardInfo);
            } catch (NeDesignerException e) {
                usedNodeSet.add(node.getNodeId().getValue());
            }
        }
    }

    /**
     * 收集当前failure group实际用到的所有TD。
     * group结束后这些TD会加入全局usedNodeSet，保证后续failure group完全换一组设备。
     */
    private void addTdNodes(Set<String> usedNodeSet, Map<String, Node> inMemoryNode) {
        for (Node node : inMemoryNode.values()) {
            Node1 node1 = node.getAugmentation(Node1.class);
            if (node1 == null || node1.getPhysical() == null || !NodeType.TD.equals(node1.getPhysical().getNodeType())) {
                continue;
            }
            usedNodeSet.add(node.getNodeId().getValue());
        }
    }

    /**
     * TunnelNewOchAllocate returns the whole in-memory node map, including TDs created by
     * previous failure groups. Only TDs introduced by this new-OCH allocation belong to the
     * current group; otherwise we over-exclude candidates and break the original A/Z position
     * matching preference.
     */
    private void addNewTdNodes(Set<String> usedNodeSet, Set<String> nodeIdsBefore,
                               Map<String, Node> inMemoryNode) {
        for (Node node : inMemoryNode.values()) {
            String nodeId = node.getNodeId().getValue();
            if (nodeIdsBefore.contains(nodeId)) {
                continue;
            }
            Node1 node1 = node.getAugmentation(Node1.class);
            if (node1 == null || node1.getPhysical() == null || !NodeType.TD.equals(node1.getPhysical().getNodeType())) {
                continue;
            }
            usedNodeSet.add(nodeId);
        }
    }

    private SiteLinkRoute validateBundleNum_And_Fresh_SiteLinkRoute(List<Link> reusedOchLinks,
                                                                    SiteLinkRoute siteLinkRoute, ParamCreate param, String vendorName, String productType,
                                                                    WDM_Band wdmBand) {
        int ochBundle = 0;
        if (reusedOchLinks != null) {
            ochBundle = reusedOchLinks.stream()
                    .mapToInt(reuseOchLink -> tunnelUtilService.getMatchedOchReusedOdu(reuseOchLink,
                            param, vendorName, productType))
                    .sum();
        }
        if (siteLinkRoute == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "invalid siteLinkRoute, because is null");
        }
        List<String> primary = siteLinkRoute.getPrimary();
        if (primary == null || primary.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "invalid primary, because is null or empty");
        }
        boolean sharedBone20ProtectionSiteLink = isSharedBone20ProtectionSiteLink(siteLinkRoute);
        List<String> siteLinks = new ArrayList<>(primary);
        if (!param.getProtectionType()
                .equals(ProtectionUnprotected.class)) {//todo: 目前定死两条腿就是secondary
            List<String> secondary = siteLinkRoute.getSecondary();
            if (secondary == null || secondary.isEmpty()) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "invalid secondary, because is null or empty");
            }
            if (!sharedBone20ProtectionSiteLink && !Collections.disjoint(primary, secondary)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "invalid primary and secondary , because joint");
            }
            siteLinks.addAll(secondary);

            if (param.getProtectionType().equals(ProtectionBidir1To2.class)) {//三条腿
                List<String> third = siteLinkRoute.getThird();


/*
comment for support missing third sitelink for 1:3

                if (third == null || third.isEmpty()) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "invalid third, because is null or empty");
                }*/
                if (!sharedBone20ProtectionSiteLink && third != null && !Collections.disjoint(primary, third)) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            "invalid primary and third , because joint");
                }
                if (!sharedBone20ProtectionSiteLink && third != null && !Collections.disjoint(secondary, third)) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            "invalid secondary and third , because joint");
                }
                if (third != null) {
                    siteLinks.addAll(third);
                }
            }
        }
        siteLinks = siteLinks.stream().distinct().collect(Collectors.toList());

        List<Link> siteLinkList = siteLinkDao.listAllSiteLinkByIds(siteLinks);
        if (siteLinkList.size() != siteLinks.size()) {
            log.error("required {} sitelinks, but only found {} in db. sitelinks are:{}", siteLinks.size(),siteLinkList.size(),siteLinks);

            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "siteLink changed, please re-design.");
        }

        //validate dummy,grid,wdb,wdmBandSiteLink
        Boolean hasDummy = null;
        GridType grid = null;
        String linkGroup = null;
        for (Link siteLink : siteLinkList) {
            Site siteAddr = siteLink.getAugmentation(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite();

            //dummy
            boolean currentHasDummy = !(siteAddr.getDummyLink() == null || siteAddr.getDummyLink()
                    .isEmpty());
            if (hasDummy == null) {
                hasDummy = currentHasDummy;
            } else if (!hasDummy.equals(currentHasDummy)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "Inconsistent dummy link");
            }

            //grid
            GridType currentGrid = siteAddr.getGrid();
            if (grid == null) {
                grid = currentGrid;
            } else if (!grid.equals(currentGrid)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "Inconsistent grid");
            }

            //wdm
            String currentLinkGroup = siteAddr.getLinkGroup();
            if (linkGroup == null) {
                linkGroup = currentLinkGroup;
            } else if (!linkGroup.equals(currentLinkGroup)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "Inconsistent linkGroup");
            }
        }

        List<Long> centralFrequencies = FrequencyAvailable.getFreeCentFrequency(siteLinkList,
                wdmBand, GridType.valueOf("_" + siteLinkRoute.getFixGrid()));
        // 只调整自动创建新 OCH 的候选顺序，不改变公共频率计算和用户显式指定的频率。
        centralFrequencies = orderCentralFrequencies(centralFrequencies,
                allocatorConfig.getFrequencyAllocationOrder());
        log.info("Automatic OCH frequency allocation order is {}, candidate count is {}",
                allocatorConfig.getFrequencyAllocationOrder(), centralFrequencies.size());
        log.debug("Ordered automatic OCH frequency candidates are {}", centralFrequencies);
        int siteLinkRouteBundleNum = centralFrequencies.size() * param.clientLineRate;

        int pathTotalBundle = ochBundle + siteLinkRouteBundleNum;
        if (pathTotalBundle < param.bundleNumber) {
            String msg = String.format(
                    "Failed to allocate tunnel ,because resource is changed after planning, current route can only create %d tunnel, but required:%d ",
                    pathTotalBundle,
                    param.bundleNumber);
            log.error(msg);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
        }
        return new SiteLinkRouteBuilder(siteLinkRoute).setBundleNumber(siteLinkRouteBundleNum)
                .setCentralFrequencies(centralFrequencies).build();
    }

    /**
     * 返回独立的有序候选列表，避免部署级分配策略修改公共频率计算结果。
     */
    static List<Long> orderCentralFrequencies(List<Long> centralFrequencies,
                                               FrequencyAllocationOrder allocationOrder) {
        List<Long> ordered = new ArrayList<>(centralFrequencies);
        if (FrequencyAllocationOrder.DESCENDING.equals(allocationOrder)) {
            ordered.sort(Collections.reverseOrder());
        } else {
            ordered.sort(Long::compareTo);
        }
        return ordered;
    }

    private SiteLinkRoute normalizeBone20OneToTwoSiteLinkRoute(SiteLinkRoute siteLinkRoute) {
        if (siteLinkRoute == null || siteLinkRoute.getPrimary() == null || siteLinkRoute.getPrimary().size() != 1) {
            return siteLinkRoute;
        }
        if (siteLinkRoute.getSecondary() != null && !siteLinkRoute.getSecondary().isEmpty()) {
            return siteLinkRoute;
        }

        Link siteLink = siteLinkDao.getSiteLinkById(siteLinkRoute.getPrimary().get(0));
        if (!isBone20OneToTwoSiteLink(siteLink) || !hasInternalSecondary(siteLink)) {
            return siteLinkRoute;
        }

        SiteLinkRouteBuilder builder = new SiteLinkRouteBuilder(siteLinkRoute)
                // Bone2.0 model=10 uses one siteLink with internal OLP3-3 legs; expose the same
                // siteLinkId on secondary so new-OCH allocation creates the 1B leg.
                .setSecondary(new ArrayList<>(siteLinkRoute.getPrimary()));
        if (hasInternalThird(siteLink)) {
            builder.setThird(new ArrayList<>(siteLinkRoute.getPrimary()));
        }
        return builder.build();
    }

    private boolean isSharedBone20ProtectionSiteLink(SiteLinkRoute siteLinkRoute) {
        if (siteLinkRoute == null || siteLinkRoute.getPrimary() == null || siteLinkRoute.getPrimary().size() != 1) {
            return false;
        }
        Link siteLink = siteLinkDao.getSiteLinkById(siteLinkRoute.getPrimary().get(0));
        return isBone20OneToTwoSiteLink(siteLink);
    }

    private boolean isBone20OneToTwoSiteLink(Link siteLink) {
        Site site = siteLink.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                .getSite();
        if (site.getProperties() == null || site.getProperties().getProperty() == null) {
            return false;
        }
        String model = getSiteProperty(site, LINK_MODEL_PROPERTY);
        return LINK_MODEL_BONE20_ONE_TO_TWO.equals(model)
                && ProductTypeResolver.isBone20ProductType(site.getVendorName(), site.getProductType());
    }

    private boolean hasInternalSecondary(Link siteLink) {
        Site site = siteLink.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                .getSite();
        return site.getExplictRoute() != null
                && site.getExplictRoute().getRoute() != null
                && !site.getExplictRoute().getRoute().isEmpty()
                && site.getExplictRoute().getRoute().get(0).getSecondary() != null;
    }

    private boolean hasInternalThird(Link siteLink) {
        Site site = siteLink.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                .getSite();
        return site.getExplictRoute() != null
                && site.getExplictRoute().getRoute() != null
                && !site.getExplictRoute().getRoute().isEmpty()
                && site.getExplictRoute().getRoute().get(0).getThird() != null
                && !site.getExplictRoute().getRoute().get(0).getThird().isEmpty();
    }

    private String getSiteProperty(Site site, String name) {
        return site.getProperties().getProperty().stream()
                .filter(property -> name.equals(property.getName()))
                .map(property -> property.getValue())
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
    }

    protected TunnelNewOchOutput allocateTunnelsNewOch(Integer number, SiteLinkRoute siteLinkRoute,
                                                     Map<String, Node> totalInMemoryNode, String vendorName, String productType,
                                                     ParamCreate param,
                                                     AllocateTunnels2Input input, String opMode, Set<String> usedNodeSet, WDM_Band wdmBand) {
        String srcSiteId = param.getSrcSite().getNodeId().getValue();
        String destSiteId = param.getDesSite().getNodeId().getValue();

        try {
            Boolean isReusedTpc = input.isIsReusedTpc();
            Boolean isReusedMixed = input.isIsReusedMixed();
            Boolean isReused = isReusedMixed || isReusedTpc;
            List<Node> reusedNodesInDb = Collections.EMPTY_LIST;
            List<Node> reusedIncludeNodes = Collections.EMPTY_LIST;

            List<String> reusedIncludeNodesIds =
                    input.getReusedMandatoryNodes() == null ? Collections.EMPTY_LIST
                            : input.getReusedMandatoryNodes()
                            .stream()
                            .map(node -> node.getNodeId())
                            .collect(Collectors.toList());

            if (!reusedIncludeNodesIds.isEmpty()) {
                reusedIncludeNodes = phyNodeDao.listConfigPhyNodeByIds(reusedIncludeNodesIds);
            }

            List<String> cardTypes = new ArrayList<>();
            cardTypes.add(param.getCardType());

            List<String> portTypes = new ArrayList<>();
            portTypes.add(L_PORT);

            if (isReused) {
                Set<String> excludeNodeIds = totalInMemoryNode.values().stream()
                        .map(item -> item.getNodeId().getValue()).collect(Collectors.toSet());
                excludeNodeIds.addAll(usedNodeSet);
                if (input.getReusedExcludeNodes() != null) {
                    excludeNodeIds.addAll(
                            input.getReusedExcludeNodes().stream().map(item -> item.getNodeId())
                                    .collect(Collectors.toSet()));
                }
                if (!reusedIncludeNodesIds.isEmpty()) {
                    excludeNodeIds.addAll(new HashSet<>(reusedIncludeNodesIds));
                }
//                if (param.getIsProtected() && param.getOpCardType().equals(DEFAULT_OP3_CARD_TYPE)) {
//                    cardTypes.add(DEFAULT_OP3_CARD_TYPE);
//                    portTypes.add(SIG_PORT);
//                }
                NodeType nodeType = NodeType.TD;
                if (input.isIsReusedMixed()) {
                    nodeType = null;
                }
                reusedNodesInDb = otReusedStrategy.getReusedPortNodePoolFromDbNewOch(srcSiteId,
                        destSiteId, vendorName, cardTypes, portTypes, param.getPlaneId(), param
                                .getRiskGroupName(),
                        excludeNodeIds, NeSubType.EPC_OTM, input.getProtectionType(),
                        input.getSiteLinkRoute(), input.getCardType(), input.getServiceType());

                //handle REG reused node
                List<PrimaryReg> primaryReg = siteLinkRoute.getPrimaryReg();
                if (primaryReg != null && !primaryReg.isEmpty()) {
                    for (PrimaryReg pr : primaryReg) {
                        if (pr.getType().equals(LinkTerminationNodeType.REG.name())) {
                            List<Node> reusedRegNodesInDbPr = otReusedStrategy.getReusedNodePoolFromDbReg(
                                    pr.getSiteId(), vendorName, cardTypes, portTypes, param
                                            .getPlaneId(), param
                                            .getRiskGroupName(),
                                    excludeNodeIds);
                            reusedNodesInDb.addAll(reusedRegNodesInDbPr);
                        }
                    }
                }

                List<SecondaryReg> secondaryRegs = siteLinkRoute.getSecondaryReg();
                if (secondaryRegs != null && !secondaryRegs.isEmpty()) {
                    for (SecondaryReg sr : secondaryRegs) {
                        if (sr.getType().equals(LinkTerminationNodeType.REG.name())) {
                            List<Node> reusedRegNodesInDbSr = otReusedStrategy.getReusedNodePoolFromDbReg(
                                    sr.getSiteId(), vendorName, cardTypes, portTypes, param
                                            .getPlaneId(), param
                                            .getRiskGroupName(),
                                    excludeNodeIds);
                            reusedNodesInDb.addAll(reusedRegNodesInDbSr);
                        }
                    }
                }

                List<ThirdReg> thirdReg = siteLinkRoute.getThirdReg();
                if (thirdReg != null && !thirdReg.isEmpty()) {
                    for (ThirdReg tr : thirdReg) {
                        if (tr.getType().equals(LinkTerminationNodeType.REG.name())) {
                            List<Node> reusedRegNodesInDbPr = otReusedStrategy.getReusedNodePoolFromDbReg(
                                    tr.getSiteId(), vendorName, cardTypes, portTypes, param
                                            .getPlaneId(), param
                                            .getRiskGroupName(),
                                    excludeNodeIds);
                            reusedNodesInDb.addAll(reusedRegNodesInDbPr);
                        }
                    }
                }


              /*  List<Node> reusedNodesInDbDst = otReusedStrategy.getReusedPortNodePoolFromDbNewOch(destSiteId, vendorName, cardTypes, portTypes, param.getPlaneName(), param.getRiskGroupName(),
                        excludeNodeIds, nodeType);
                if(!reusedNodesInDbDst.isEmpty()){
                    reusedNodesInDb.addAll(reusedNodesInDbDst);
                }*/

            }

            TunnelNewOchInput tunnelNewOchInput = TunnelNewOchInput.builder()
                    .op6CardType(param.getOpCardType())
                    .cardType(param.getCardType())
                    .tunnelSignalRate(param.getTunnelSignalRate())
                    .clientMediumA(param.getClientMediumA())
                    .clientMediumZ(param.getClientMediumZ())
                    .lineSignalRate(param.getLinePortSignalRate())
                    .siteLinkRoute(siteLinkRoute)
                    .vendorName(vendorName)
                    .vendorType(productType)
                    .tunnelNumber(number)
                    .tunnelNumberPerOch(param.getClientLineRate())
//                    .clientLineRate(param.getClientLineRate())
                    .ochNumber(param.getOchNumber(number))
                    .totalInMemoryNode(totalInMemoryNode)
                    .riskGroupName(param.getRiskGroupName())
                    .plane(param.getPlaneName())
                    .planeId(param.getPlaneId())
                    .srcSite(srcSiteId)
                    .destSite(destSiteId)
                    .grid(GridType.valueOf("_" + siteLinkRoute.getFixGrid()))
                    .reusedNodesInDb(reusedNodesInDb)
                    .reusedIncludeNodes(reusedIncludeNodes)
                    .isReusedMixed(isReusedMixed)
                    .serviceType(input.getServiceType())
                    .opMode(opMode)
                    // Keep the requested protection model when a protection1to2 OCH is initially created with two legs.
                    .requestedProtectionType(param.getProtectionType())
                    .excludeNodes(usedNodeSet)
                    .wdmBand(wdmBand)
                    .selectedCardIdsBySite(getSelectedCardIdsBySite(input, vendorName, productType))
                    .build();

            return neDesigner.allocateTunnelNewOch(tunnelNewOchInput);
        } catch (NeDesignerException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "error in neDesign " + e.getCause().getMessage(), e);
        }
    }


    protected Map<String, List<String>> getSelectedCardIdsBySite(AllocateTunnels2Input input,
                                                                 String vendorName,
                                                                 String productType) {
        return Collections.emptyMap();
    }

    protected TunnelReuseOchOutput allocateTunnelsReuseOch(String vendorName, String productType,
                                                           int number, String ochLinkId, ParamCreate param, Map<String, Node> totalInMemoryNode) {
        TunnelReuseOchInput tunnelInput = TunnelReuseOchInput.builder()
                .vendorName(vendorName)
                .vendorType(productType)
                .cardType(param.getCardType())
                .lineSignalRate(param.getLinePortSignalRate())
                .clientMediumA(param.getClientMediumA())
                .clientMediumZ(param.getClientMediumZ())
                .tunnelSignalRate(param.getTunnelSignalRate())
                .number(number)
                .ochLinkId(ochLinkId)
                .tunnelOdu(param.getTunnelOdu())
                .totalInMemoryNode(totalInMemoryNode)
                .servicetype(param.getServiceType())
                .build();
        TunnelReuseOchOutput tunnelReuseOchOutput = null;
        try {
            tunnelReuseOchOutput = neDesigner.allocateTunnelReuseOch(tunnelInput);
        } catch (NeDesignerException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "error in neDesign " + e.getCause().getMessage(), e);
        }
        return tunnelReuseOchOutput;
    }

    protected Map<String, List<Link>> getReusedOchMap(AllocateTunnels2Input input, ParamCreate param,
                                                      String opMode) {
        if (!input.isIsReusedTpc()) {
            return Collections.EMPTY_MAP;
        }
        List<Link> reusedOch = tunnelUtilService.getReusedOchList(input.getSiteLinkRoute(), param,
                opMode);

        return reusedOch.stream()
                .collect(Collectors.groupingBy(ochLink -> TunnelUtil.createVendorProductKey(
                        ochLink.getAugmentation(Link1.class).getOch().getVendorName(),
                        ochLink.getAugmentation(Link1.class).getOch().getProductType())));
    }


    public CreateTunnel3Input reallocateByFrequency(CreateTunnel3Input input)
            throws NeDesignerException {
        WDM_Band wdmBand = neInfoConfig.getOtCardWdmBand(input.getCardType());
        if (wdmBand != WDM_Band.C_L) {
            return input;
        }
        if (input.getTunnelAllocateResult2().getVendor().get(0).getNewOchTunnel() == null) {
            return input;
        }

//        Boolean needReallocate = false;
//        List<Vendor> vendors = input.getTunnelAllocateResult2().getVendor();
//        SiteLinkRoute siteLinkRoute = null;
//        outerLoop:
//        for (Vendor vendor : vendors) {
//            List<TunnelRouteInfos> tunnelRouteInfos = vendor.getNewOchTunnel().getTunnelRouteInfos();
//            siteLinkRoute = vendor.getNewOchTunnel().getSiteLinkRoute();
//            List<Long> defaultCenFrequencies = siteLinkRoute.getCentralFrequencies();
//            for (int i = 0; i < tunnelRouteInfos.size(); i++) {
//                // "source-tp": [{
//                //                                                            "tp-ref": "Site-1944747727028097024#Ne-1947521288553762816#MUX-1-50#PORT-1-50-M36D36",
//                //                                                            "slot": "/frequency=186050000,186200000"
//                //                                                        }
//                //                                                    ],
//                String mdTp = tunnelRouteInfos.get(i).getOchRoute().getPrimary().getCrossConnections().get(0).getSourceTp().get(0).getTpRef().getValue();
//                if (!isValidMd(mdTp, defaultCenFrequencies.get(i), input.getFrequenceWidth())) {
//                    needReallocate = true;
//                    break outerLoop;
//                }
//            }
//        }
//        if (!needReallocate) {
//            return input;
//        }
        SiteLinkRoute siteLinkRoute = input.getTunnelAllocateResult2().getVendor().get(0)
                .getNewOchTunnel().getSiteLinkRoute();//todo: just support one vendor
        AllocateTunnels2Input inputAllocate = new AllocateTunnels2InputBuilder(
                (TunnelCreationAttributes) input)
                .setSiteLinkRoute(siteLinkRoute).build();
        TempInfo output = getAllocateTunnelsResult(inputAllocate, true);

        CreateTunnel3Input inputUpdated = new CreateTunnel3InputBuilder(input)
                .setTunnelAllocateResult2(
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.create.tunnel._3.input.TunnelAllocateResult2Builder(
                                output.tunnelAllocateResult2)
                                .build()
                ).build();
        log.info("Reallocated, update the input for creating tunnel");
        log.debug("Reallocated, update the input for creating tunnel as:{}", inputUpdated);

        return inputUpdated;
    }

    private boolean isValidMd(String mdTp, Long centFreq, GridType grid) {
        WDM_Band wdmBandPort = wdmUtilService.getWdmBandByMdPort(mdTp);
        WDM_Band wdmBandCenFre = wdmUtilService.getWdmBandByCentFre(centFreq, grid);
        return wdmBandPort == wdmBandCenFre;
    }

    class TempInfo {

        List<Node> nodeSnapShot;
        Map<String, Node> totalInMemoryNode;
        TunnelAllocateResult2 tunnelAllocateResult2;

        public TempInfo(TunnelAllocateResult2 tunnelAllocateResult2, List<Node> nodeSnapShot,
                        Map<String, Node> totalInMemoryNode) {
            this.tunnelAllocateResult2 = tunnelAllocateResult2;
            this.nodeSnapShot = nodeSnapShot;
            this.totalInMemoryNode = totalInMemoryNode;


        }
    }
}
