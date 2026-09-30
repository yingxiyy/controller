package net.flex.dci.otn.controller.allocate.link.site.insertnode;

import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.enums.NeSubType;
import net.flex.dci.otn.controller.allocate.link.common.Route;
import net.flex.dci.otn.controller.allocate.common.namingrule.PhyNodeFriendlyName;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.site.SiteInput;
import net.flex.dci.otn.controller.allocate.designer.model.site.SiteNodeInput;
import net.flex.dci.otn.controller.allocate.designer.ne.LinkRepo;
import net.flex.dci.otn.controller.allocate.designer.site.SiteRepo;
import net.flex.dci.otn.controller.allocate.link.phy.PhyLinkUtil;
import net.flex.dci.otn.controller.allocate.link.view.ViewLink;
import net.flex.dci.otn.controller.allocate.node.phy.PhyNodeUtil;
import net.flex.dci.otn.controller.allocate.node.site.SiteNodeCorrelateResource;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkTerminationNodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjectsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObjectBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObjectKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.SecondaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.ThirdBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

abstract class BaseInsertNodeInSiteLinkOperation {
    protected final InsertNodeInSiteLinkContext context;
    protected final ChangedObject changedObject;
    private static final String LINK_MODEL_P2P = "2";

    BaseInsertNodeInSiteLinkOperation(InsertNodeInSiteLinkContext context, ChangedObject changedObject) {
        this.context = context;
        this.changedObject = changedObject;
    }

    public void execute() {
        // ILA/DGE 共用同一套资源拆分顺序，避免两个分支以后各自维护 siteLink/phyLink/rack/OCH route 造成模型不一致。
        removeOldOtsLink();
        // 这里只创建新 ILA/DGE 节点自身默认资源；DGE 业务波/假波 WSS XC 必须等新节点和 LINE TP 存在后再补。
        createInsertedNode();
        updateOchXcsForRoute();
        createSplitOtsLinks();
        updateSiteNodeRack();
        updateSiteLinkRoute();
        updateOchRoutes();
    }

    protected void removeOldOtsLink() {
        // 复用现有 phyLink 删除逻辑，确保旧 OTS phyLink、两端 TP 状态、以及对应 OtsLink viewLink, internalLink一起被清理。
        new PhyLinkUtil(changedObject).removePhyLinkKeepXc(context.phyLinkId, context.planeId);
    }

    protected void updateOchXcsForRoute() {
        // ILA 没有额外 OCH XC；DGE 在新节点创建后补业务波/假波 WSS XC，并缓存 OCH->XC 映射供 updateOchRoutes() 使用。
    }

    protected void createInsertedNode() {
        Site siteLinkAttr = context.siteLink.getAugmentation(Link1.class).getSite();
        try {
            SiteInput siteInput = createSiteInput(siteLinkAttr);
            SiteNodeInput siteNodeInput = SiteNodeInput.builder()
                    .siteId(context.siteNodeId)
                    .nodeType(getDesignerNodeType(context.nodeType))
                    .neSubType(getNeSubType(context.nodeType))
                    .build();
            // 插入 ILA/DGE 只需要创建一个完整孤立 OD 节点；复用 SiteRepo 单节点逻辑，不重新设计原 siteLink 多路由。
            Node insertedNode = SpringBeanFinder.getBean(SiteRepo.class).createSingleNode(siteInput, siteNodeInput,
                    false);
            changedObject.addChangedPhyNode(insertedNode);
            context.setInsertedPhyNode(insertedNode);
        } catch (NeDesignerException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "neDesigner error when create inserted node: " + e.getMessage(), e);
        }
    }

    private SiteInput createSiteInput(Site siteLinkAttr) {
        String linkModel = getSiteLinkProperty(siteLinkAttr, "model");
        return SiteInput.builder()
                .vendorName(siteLinkAttr.getVendorName())
                .vendorType(siteLinkAttr.getProductType())
                .grid(siteLinkAttr.getGrid().getIntValue())
                .isProtected(false)
                .protectionType(ProtectionUnprotected.class)
                .plane(siteLinkAttr.getPlaneName())
                .planeId(siteLinkAttr.getPlaneId())
                .riskGroupName(siteLinkAttr.getRiskGroupName())
                // 老 siteLink 可能没有 model 属性；插入节点默认按 P2P 模型选 ILA/DGE 板卡。
                .linkModel(linkModel == null ? LINK_MODEL_P2P : linkModel)
                .wdmBand(WDM_Band.fromString(siteLinkAttr.getLinkGroup()))
                .build();
    }

    private String getSiteLinkProperty(Site siteLinkAttr, String name) {
        if (siteLinkAttr.getProperties() == null || siteLinkAttr.getProperties().getProperty() == null) {
            return null;
        }
        return siteLinkAttr.getProperties().getProperty().stream()
                .filter(property -> name.equals(property.getName()))
                .map(property -> property.getValue())
                .findFirst()
                .orElse(null);
    }

    private String getDesignerNodeType(LinkTerminationNodeType nodeType) {
        if (nodeType == LinkTerminationNodeType.DGE) {
            return "D";
        }
        return "I";
    }

    private NeSubType getNeSubType(LinkTerminationNodeType nodeType) {
        if (nodeType == LinkTerminationNodeType.DGE) {
            return NeSubType.OPC_DGE;
        }
        return NeSubType.OPC_ILA;
    }

    protected void createSplitOtsLinks() {
        // 拆分 OTS 和 internal-link/TP busy 是同一组物理资源变更，必须放在一起，避免 A/N/B 节点状态和 phyLink 不一致。
        try {
            Link aToN = createSplitOtsLink(context.oldPhyLink.getSource().getSourceTp().getValue(),
                    getInsertedLineTp("LINE_WEST").getTpId().getValue());
            Link nToZ = createSplitOtsLink(getInsertedLineTp("LINE_EAST").getTpId().getValue(),
                    context.oldPhyLink.getDestination().getDestTp().getValue());

            PhyLinkUtil phyLinkUtil = new PhyLinkUtil(changedObject);
            Site siteLinkAttr = context.siteLink.getAugmentation(Link1.class).getSite();
            phyLinkUtil.addPhyLink(aToN, siteLinkAttr.getPlaneName(), siteLinkAttr.getPlaneId());
            phyLinkUtil.addPhyLink(nToZ, siteLinkAttr.getPlaneName(), siteLinkAttr.getPlaneId());

            context.aToInsertedOtsLink = changedObject.getChangedPhyLink(aToN.getLinkId().getValue());
            context.insertedToZOtsLink = changedObject.getChangedPhyLink(nToZ.getLinkId().getValue());
            ViewLink viewLink = new ViewLink(changedObject, siteLinkAttr.getPlaneId());
            viewLink.create(context.aToInsertedOtsLink);
            viewLink.create(context.insertedToZOtsLink);
            updateSplitOtsInternalLinks(context.aToInsertedOtsLink, context.insertedToZOtsLink);
        } catch (NeDesignerException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "neDesigner error when create split OTS links: " + e.getMessage(), e);
        }
    }

    private Link createSplitOtsLink(String sourceTpId, String destTpId) throws NeDesignerException {
        Link link = new LinkRepo().createLink(sourceTpId, destTpId, LinkType.OtsLink);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical oldPhyAttr =
                context.oldPhyLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class).getPhysical();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical newPhyAttr =
                link.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class).getPhysical();
        return new LinkBuilder(link)
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder()
                                .setPhysical(new PhysicalBuilder(newPhyAttr)
                                        .setProvider(oldPhyAttr.getProvider())
                                        .setProperties(oldPhyAttr.getProperties())
                                        .setPlaneName(oldPhyAttr.getPlaneName())
                                        .setPlaneId(oldPhyAttr.getPlaneId())
                                        // split 出来的两条 OTS 仍然承载原 siteLink，必须保留 phyLink -> siteLink 的反向支撑关系。
                                        .setSupportedLink(buildPhySupportedLinks())
                                        .setImplementState(ImplementState.Allocate)
                                        .build())
                                .build())
                .build();
    }

    private List<SupportedLink> buildPhySupportedLinks() {
        List<SupportedLink> supportedLinks = new ArrayList<>();
        supportedLinks.add(new SupportedLinkBuilder()
                .setTopologyRef(new TopologyId(TopoNameConstants.Site_Topo_Key))
                .setLinkRef(new LinkId(context.siteLinkId))
                .build());
        return supportedLinks;
    }

    private void updateSplitOtsInternalLinks(Link aToN, Link nToZ) {
        LinkRepo linkRepo = new LinkRepo();

        Node aNode = changedObject.getChangedPhyNode(aToN.getSource().getSourceNode().getValue());
        Node zNode = changedObject.getChangedPhyNode(nToZ.getDestination().getDestNode().getValue());
        Node insertedNode = context.insertedPhyNode;
        if (aNode == null || zNode == null || insertedNode == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "cannot update split OTS internal links because A/Z/inserted node is missing");
        }

        InternalLinks aInternalLink = linkRepo.createInternalLink(aNode.getNodeId().getValue(), aToN);
        InternalLinks nLeftInternalLink = linkRepo.createInternalLink(insertedNode.getNodeId().getValue(), aToN);
        InternalLinks nRightInternalLink = linkRepo.createInternalLink(insertedNode.getNodeId().getValue(), nToZ);
        InternalLinks zInternalLink = linkRepo.createInternalLink(zNode.getNodeId().getValue(), nToZ);

        // A/B 只占用各自外联 TP；N 同时占用 WEST/EAST 两个新外联 TP。
        changedObject.addChangedPhyNode(PhyNodeUtil.addInternalLinksAndBusyTps(aNode,
                Collections.singletonList(aInternalLink),
                Collections.singleton(aToN.getSource().getSourceTp().getValue())));
        context.setInsertedPhyNode(PhyNodeUtil.addInternalLinksAndBusyTps(insertedNode,
                Arrays.asList(nLeftInternalLink, nRightInternalLink),
                new HashSet<>(Arrays.asList(aToN.getDestination().getDestTp().getValue(),
                        nToZ.getSource().getSourceTp().getValue()))));
        changedObject.addChangedPhyNode(context.insertedPhyNode);
        changedObject.addChangedPhyNode(PhyNodeUtil.addInternalLinksAndBusyTps(zNode,
                Collections.singletonList(zInternalLink),
                Collections.singleton(nToZ.getDestination().getDestTp().getValue())));
    }

    private TerminationPoint getInsertedLineTp(String portSuffix) {
        if (context.insertedPhyNode == null || context.insertedPhyNode.getTerminationPoint() == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "cannot create split OTS links before inserted node is created");
        }
        return context.insertedPhyNode.getTerminationPoint().stream()
                .filter(tp -> tp.getTpId().getValue().endsWith(portSuffix))
                .findFirst()
                .orElseThrow(() -> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "cannot find inserted node line tp " + portSuffix + " on "
                                + context.insertedPhyNode.getNodeId().getValue()));
    }

    protected void updateSiteNodeRack() {
        Node siteNode = changedObject.getChangedSiteNode(context.siteNodeId);
        if (siteNode == null || context.insertedPhyNode == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "cannot update site node rack because siteNode or inserted phyNode is missing");
        }
        String rackName = context.siteLink.getAugmentation(Link1.class).getSite().getFriendlyName();

        // One rack represents one siteLink multiplex section in a site.
        SiteNodeCorrelateResource correlateResource = new SiteNodeCorrelateResource(siteNode);
        correlateResource.insertIntoSiteLinkRack(context.siteLinkId, rackName,
                context.insertedPhyNode);
        changedObject.addChangedSiteNode(updateInsertedSiteNodeType(correlateResource.getSiteNode()));
        markInsertedNodeEquipmentsUsed();

        // 在 rack 更新完成后更新 friendlyName。
        // createFriendlyName() 依赖 phyNode 已在 siteNode rack 中，因此必须放在 insertRack 之后。
        Node updatedSiteNode = changedObject.getChangedSiteNode(context.siteNodeId);
        PhyNodeFriendlyName phyNodeFriendlyNameGenerator = SpringBeanFinder.getBean(PhyNodeFriendlyName.class);
        context.setInsertedPhyNode(phyNodeFriendlyNameGenerator.updateFriendlyName(context.insertedPhyNode, updatedSiteNode));
        changedObject.addChangedPhyNode(context.insertedPhyNode);
    }

    private Node updateInsertedSiteNodeType(Node siteNode) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site siteAttr =
                siteNode.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class).getSite();
        SiteType insertedType = context.nodeType == LinkTerminationNodeType.DGE ? SiteType.DGE : SiteType.ILA;
        if (siteAttr.getSiteType() != null && siteAttr.getSiteType().compareTo(insertedType) >= 0) {
            return siteNode;
        }
        // 正常 create siteLink 会根据 segment 类型提升 siteNode type；插入 ILA/DGE 也需要同步这个站点级类型。
        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder(siteNode)
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder()
                                .setSite(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder(siteAttr)
                                        .setSiteType(insertedType)
                                        .build())
                                .build())
                .build();
    }

    private void markInsertedNodeEquipmentsUsed() {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                context.insertedPhyNode.getAugmentation(Node1.class).getPhysical();
        if (physical.getEquipments() == null || physical.getEquipments().isEmpty()) {
            return;
        }
        List<String> equipmentIds = physical.getEquipments().stream()
                .filter(equipment -> equipment.getEquipType() != null
                        && !org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType.EMPTY.equals(equipment.getEquipType()))
                .map(equipment -> equipment.getEquipmentId())
                .collect(java.util.stream.Collectors.toList());
        if (equipmentIds.isEmpty()) {
            return;
        }
        // 插入的新 ILA/DGE 板卡已经被 A--N/N--B 和默认 XC 占用，需和正常 create siteLink 一样标记 used。
        context.setInsertedPhyNode(PhyNodeUtil.addEquipUsedSymbol(context.insertedPhyNode, equipmentIds, true));
        changedObject.addChangedPhyNode(context.insertedPhyNode);
    }

    protected void updateSiteLinkRoute() {
        Site oldSiteAttr = context.siteLink.getAugmentation(Link1.class).getSite();
        ExplictRoute updatedRoute = updateSiteExplicitRoute(oldSiteAttr.getExplictRoute());
        List<SupportingLink> updatedSupportingLinks = updateSiteSupportingLinks(context.siteLink.getSupportingLink());

        // 插入 ILA/DGE 后，原 siteLink 仍表示 A/Z 之间的业务复用段。
        // 如果原 siteLink 及其端点节点全是 Allocate（未下发），新增资源也是 Allocate，整体应保持 Allocate；
        // 否则新增 N 及 A--N/N--B 处于 allocate，已有资源可能已下发，整体变为部分实现。
        ImplementState targetState = oldSiteAttr.getImplementState().equals(ImplementState.Allocate)
                ? ImplementState.Allocate
                : ImplementState.PartialImplement;

        Link updatedSiteLink = new LinkBuilder(context.siteLink)
                .setSupportingLink(updatedSupportingLinks)
                .addAugmentation(Link1.class, new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                        .setSite(new SiteBuilder(oldSiteAttr)
                                .setImplementState(targetState)
                                .setExplictRoute(updatedRoute)
                                .build())
                        .build())
                .build();
        // Preserve the original snapshot for non-route diffs. MultipleTransaction
        // writes the complete route separately after the other changes.
        changedObject.addChangedSiteLink(updatedSiteLink);
    }

    private ExplictRoute updateSiteExplicitRoute(ExplictRoute oldRoute) {
        if (oldRoute == null || oldRoute.getRoute() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "siteLink has no explict route: " + context.siteLinkId);
        }
        boolean oldOtsFound = false;
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route> newRoutes =
                new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route route : oldRoute.getRoute()) {
            RouteBuilder routeBuilder = new RouteBuilder(route);
            if (route.getPrimary() != null) {
                oldOtsFound = oldOtsFound || containsOldOts(route.getPrimary().getExplicitRouteObjects());
                routeBuilder.setPrimary(updatePrimaryRoute(route.getPrimary()));
            }
            if (route.getSecondary() != null) {
                oldOtsFound = oldOtsFound || containsOldOts(route.getSecondary().getExplicitRouteObjects());
                routeBuilder.setSecondary(updateSecondaryRoute(route.getSecondary()));
            }
            if (route.getThird() != null) {
                List<Third> newThirds = new ArrayList<>();
                for (Third third : route.getThird()) {
                    oldOtsFound = oldOtsFound || containsOldOts(third.getExplicitRouteObjects());
                    newThirds.add(updateThirdRoute(third));
                }
                routeBuilder.setThird(newThirds);
            }
            newRoutes.add(routeBuilder.build());
        }
        if (!oldOtsFound) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "siteLink route does not contain old OTS phyLink: " + context.phyLinkId);
        }
        return new ExplictRouteBuilder(oldRoute).setRoute(newRoutes).build();
    }

    private Primary updatePrimaryRoute(Primary oldPrimary) {
        boolean routeNeedInsertedNode = containsOldOts(oldPrimary.getExplicitRouteObjects());
        if (!routeNeedInsertedNode) {
            return oldPrimary;
        }
        List<ExplicitRouteObjects> updatedEros = updateExplicitRouteObjects(oldPrimary.getExplicitRouteObjects());
        return new PrimaryBuilder(oldPrimary)
                .setExplicitRouteObjects(updatedEros)
                .setCrossConnections(mergeInsertedAmplifierXcs(oldPrimary.getCrossConnections(), updatedEros))
                .build();
    }

    private Secondary updateSecondaryRoute(Secondary oldSecondary) {
        boolean routeNeedInsertedNode = containsOldOts(oldSecondary.getExplicitRouteObjects());
        if (!routeNeedInsertedNode) {
            return oldSecondary;
        }
        List<ExplicitRouteObjects> updatedEros = updateExplicitRouteObjects(oldSecondary.getExplicitRouteObjects());
        return new SecondaryBuilder(oldSecondary)
                .setExplicitRouteObjects(updatedEros)
                .setCrossConnections(mergeInsertedAmplifierXcs(oldSecondary.getCrossConnections(), updatedEros))
                .build();
    }

    private Third updateThirdRoute(Third oldThird) {
        boolean routeNeedInsertedNode = containsOldOts(oldThird.getExplicitRouteObjects());
        if (!routeNeedInsertedNode) {
            return oldThird;
        }
        List<ExplicitRouteObjects> updatedEros = updateExplicitRouteObjects(oldThird.getExplicitRouteObjects());
        return new ThirdBuilder(oldThird)
                .setExplicitRouteObjects(updatedEros)
                .setCrossConnections(mergeInsertedAmplifierXcs(oldThird.getCrossConnections(), updatedEros))
                .build();
    }

    private boolean containsOldOts(List<ExplicitRouteObjects> eros) {
        if (eros == null) {
            return false;
        }
        for (ExplicitRouteObjects ero : eros) {
            if (ero.getPathRouteObject() == null) {
                continue;
            }
            for (PathRouteObject routeObject : ero.getPathRouteObject()) {
                if (isOldOtsLinkHop(routeObject)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<ExplicitRouteObjects> updateExplicitRouteObjects(List<ExplicitRouteObjects> oldEros) {
        if (oldEros == null) {
            return null;
        }
        List<ExplicitRouteObjects> newEros = new ArrayList<>();
        for (ExplicitRouteObjects ero : oldEros) {
            newEros.add(new ExplicitRouteObjectsBuilder(ero)
                    .setPathRouteObject(replaceOldOtsRouteObjects(ero.getPathRouteObject()))
                    .build());
        }
        return newEros;
    }

    List<PathRouteObject> replaceOldOtsRouteObjects(List<PathRouteObject> oldObjects) {
        if (oldObjects == null) {
            return null;
        }
        List<PathRouteObject> newObjects = new ArrayList<>();
        long index = 1;
        for (int position = 0; position < oldObjects.size(); position++) {
            PathRouteObject oldObject = oldObjects.get(position);
            if (isOldOtsLinkHop(oldObject)) {
                index = appendSplitOtsRouteObjects(newObjects, index,
                        isOldOtsTraversedForward(oldObjects, position));
            } else {
                newObjects.add(reindexPathRouteObject(oldObject, index++));
            }
        }
        return newObjects;
    }

    private long appendSplitOtsRouteObjects(List<PathRouteObject> newObjects, long index,
            boolean forward) {
        Link firstLink = forward ? context.aToInsertedOtsLink : context.insertedToZOtsLink;
        Link secondLink = forward ? context.insertedToZOtsLink : context.aToInsertedOtsLink;
        String firstInsertedTp = getInsertedLineTp(forward ? "LINE_WEST" : "LINE_EAST")
                .getTpId().getValue();
        String secondInsertedTp = getInsertedLineTp(forward ? "LINE_EAST" : "LINE_WEST")
                .getTpId().getValue();

        newObjects.add(Route.getLinkPathRouteObject(phyTopologyId(),
                new LinkId(firstLink.getLinkId().getValue()), index++));
        newObjects.add(Route.getTpPathRouteObject(phyTopologyId(),
                new TpId(firstInsertedTp), index++));
        newObjects.add(Route.getTpPathRouteObject(phyTopologyId(),
                new TpId(secondInsertedTp), index++));
        newObjects.add(Route.getLinkPathRouteObject(phyTopologyId(),
                new LinkId(secondLink.getLinkId().getValue()), index++));
        return index;
    }

    private boolean isOldOtsTraversedForward(List<PathRouteObject> routeObjects,
            int linkPosition) {
        String previousTp = getRouteTpRef(routeObjects, linkPosition - 1);
        String nextTp = getRouteTpRef(routeObjects, linkPosition + 1);
        String sourceTp = context.oldPhyLink.getSource().getSourceTp().getValue();
        String destinationTp = context.oldPhyLink.getDestination().getDestTp().getValue();

        if (sourceTp.equals(previousTp) && destinationTp.equals(nextTp)) {
            return true;
        }
        if (destinationTp.equals(previousTp) && sourceTp.equals(nextTp)) {
            return false;
        }
        throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "old OTS link route neighbors do not match its endpoints: link="
                        + context.phyLinkId + ", previousTp=" + previousTp + ", nextTp=" + nextTp);
    }

    private String getRouteTpRef(List<PathRouteObject> routeObjects, int position) {
        if (position < 0 || position >= routeObjects.size()) {
            return null;
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.ResourceType resourceType =
                routeObjects.get(position).getResourceType();
        if (!(resourceType instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp)) {
            return null;
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.tp.TpHop tpHop =
                ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp) resourceType)
                        .getTpHop();
        return tpHop == null || tpHop.getTpRef() == null ? null : tpHop.getTpRef().getValue();
    }

    private boolean isOldOtsLinkHop(PathRouteObject routeObject) {
        if (routeObject.getResourceType() instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link link = (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) routeObject.getResourceType();
            return (link != null && link.getLinkHop() != null
                    && context.phyLinkId.equals(link.getLinkHop().getLinkRef().getValue()));
        }
        return false;
    }

    private PathRouteObject reindexPathRouteObject(PathRouteObject oldObject, long index) {
        return new PathRouteObjectBuilder(oldObject)
                .setIndex(index)
                .setKey(new PathRouteObjectKey(index))
                .build();
    }

    private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> mergeInsertedAmplifierXcs(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> oldXcs,
            List<ExplicitRouteObjects> updatedEros) {
        return RouteCrossConnectionOrder.mergeWithNeighborFallback(oldXcs, getInsertedAmplifierXcs(),
                RouteCrossConnectionOrder.physicalTps(updatedEros));
    }

    private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> getInsertedAmplifierXcs() {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical physical =
                context.insertedPhyNode.getAugmentation(Node1.class).getPhysical();
        if (physical.getCrossConnections() == null) {
            return Collections.emptyList();
        }
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> amplifierXcs =
                new ArrayList<>();
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections xc : physical.getCrossConnections()) {
            if (xc.getAmplifier() != null) {
                amplifierXcs.add(xc);
            }
        }
        return amplifierXcs;
    }

    private List<SupportingLink> updateSiteSupportingLinks(List<SupportingLink> oldSupportingLinks) {
        List<SupportingLink> newSupportingLinks = new LinkedList<>();
        boolean oldOtsFound = false;
        if (oldSupportingLinks != null) {
            for (SupportingLink supportingLink : oldSupportingLinks) {
                if (supportingLink.getLinkRef() != null
                        && context.phyLinkId.equals(supportingLink.getLinkRef().getValue())) {
                    oldOtsFound = true;
                } else {
                    newSupportingLinks.add(supportingLink);
                }
            }
        }
        if (!oldOtsFound) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "siteLink supporting-link does not contain old OTS phyLink: " + context.phyLinkId);
        }
        newSupportingLinks.add(buildSupportingLink(context.aToInsertedOtsLink.getLinkId().getValue()));
        newSupportingLinks.add(buildSupportingLink(context.insertedToZOtsLink.getLinkId().getValue()));
        return newSupportingLinks;
    }

    private SupportingLink buildSupportingLink(String linkId) {
        return new SupportingLinkBuilder()
                .setLinkRef(new LinkId(linkId))
                .setKey(new SupportingLinkKey(new LinkId(linkId)))
                .build();
    }

    private TopologyId phyTopologyId() {
        return new TopologyId(TopoNameConstants.Phy_Topo_Key);
    }

    protected void updateOchRoutes() {
        // ILA 只放大光信号，不产生业务波 WSS channel XC；OCH route 保持原 SiteLink 抽象跳不变。
        // DGE 子类会把 updateOchXcsForRoute() 创建的业务波 XC 加入对应 OCH route。
    }

    protected CommonException underConstruction(String step) {
        return new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "insert-node-in-site-link design step is not implemented yet: " + step);
    }
}
