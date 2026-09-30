package net.flex.dci.otn.controller.implement.common.ase;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.OchXcProtoType;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class DummyOchAllocatorOverTunnel {
    private Tunnel tunnel;
    private long taskGroupId;

    //记录一条tunnel对应的OCH link 穿过的所有siteLink 处理对象
    private Map<String, DummyListManagerWrapOnOch> siteLinkDummyInfoMap;  //string is siteLinkId

    private ChangedObject changedObject;
    private OchXcProtoType protoXc = new OchXcProtoType();

    private List<String> newBindingSiteLinkIds;

    @Getter
    private Map<Link, DummyListManagerWrapOnOch> resultMap;  //一个OCH 在ROADM由多个siteLink支撑，所以需要修改ASE 的内容也是每个siteLink 不一样

    public DummyOchAllocatorOverTunnel(Tunnel tunnel, long taskGroupId, String newBindingSiteLinkId) {
        this(tunnel, taskGroupId, newBindingSiteLinkId == null
                ? Collections.emptyList()
                : Collections.singletonList(newBindingSiteLinkId));
    }

    public DummyOchAllocatorOverTunnel(Tunnel tunnel, long taskGroupId, List<String> newBindingSiteLinkIds) {
        this.tunnel = tunnel;
        this.taskGroupId = taskGroupId;
        this.newBindingSiteLinkIds = newBindingSiteLinkIds == null
                ? Collections.emptyList()
                : new ArrayList<>(newBindingSiteLinkIds);

        resultMap = new HashMap<>();
        siteLinkDummyInfoMap = new HashMap<>();
    }

    public void setCache(ChangedObject changedObject) {
        this.changedObject = changedObject;
    }

    //释放假波，意味着有业务OCH需要占用
    public void releaseAseOch() {
        List<String> ochLinkIdList = tunnel.getSupportingLink().stream().map(sl -> sl.getLinkRef().getValue()).collect(Collectors.toList());

        ochLinkIdList.forEach(ochLinkId -> {
            Link ochLink = changedObject.getChangedOchLink(ochLinkId);
            Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
            if (!ochLinkAttr.getImplementState().equals(ImplementState.Implement)) {
                log.debug("before business och insert, calculate how to remove dummy och");
                //找出对应的site link, 逐一释放dummy och。 一个OCH穿越几个复用段
                long lower = ochLinkAttr.getLowerFrequency().getValue().longValue();
                long upper = ochLinkAttr.getUpperFrequency().getValue().longValue();

                if (ochLinkAttr.getImplementState().equals(ImplementState.PartialImplement)) {
                    // partial 场景：上次 dummy 写入/删除失败会留下 allocate 状态。
                    // 只清理当前业务频率范围内的 allocate dummy；partial dummy 可能仍在设备上，
                    // 必须保留给本次 retry 继续删除。
                    if (!newBindingSiteLinkIds.isEmpty()) {
                        newBindingSiteLinkIds.forEach(siteLinkId ->
                                cleanupAllocateDummyOchOnSiteLink(siteLinkId, lower, upper));
                    } else {
                        List<String> siteLinkIdList = getSiteLinkId(ochLink);
                        siteLinkIdList.forEach(siteLinkId ->
                                cleanupAllocateDummyOchOnSiteLink(siteLinkId, lower, upper));
                    }
                }

                if (!newBindingSiteLinkIds.isEmpty()) {
                    // 如果 bind 3rd leg 用到新的复用段，只释放新增复用段上的假波；
                    // ROADM 场景下新增腿可能跨多个 siteLink，需要全部处理。
                    newBindingSiteLinkIds.forEach(siteLinkId -> releaseDummyOchOnSiteLink(siteLinkId, lower, upper));
                } else {
                    List<String> siteLinkIdList = getSiteLinkId(ochLink);
                    siteLinkIdList.forEach(siteLinkId -> releaseDummyOchOnSiteLink(siteLinkId, lower, upper));
                }
            }
        });

        log.debug("unimpacted ochList {}:", ochLinkIdList);
        //remove unchanged OchLink in changedObject
        removeUnImpactedOch(ochLinkIdList);
    }

    private void cleanupAllocateDummyOchOnSiteLink(String siteLinkId, long lower, long upper) {
        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
        Site siteLinkAttr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
        List<String> dummyLinkIds = siteLinkAttr.getDummyLink();
        if (dummyLinkIds == null || dummyLinkIds.isEmpty()) {
            return;
        }

        List<String> snapshot = new ArrayList<>(dummyLinkIds);
        DummyOchLinkConstructor constructor = new DummyOchLinkConstructor(changedObject);
        snapshot.forEach(ochLinkId -> {
            Link ochLink = changedObject.getChangedOchLink(ochLinkId);
            if (ochLink == null) {
                return;
            }
            Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
            if (ImplementState.Allocate.equals(ochLinkAttr.getImplementState())
                    && frequencyOverlap(lower, upper,
                    ochLinkAttr.getLowerFrequency().getValue().longValue(),
                    ochLinkAttr.getUpperFrequency().getValue().longValue())) {
                constructor.remove(ochLink);
            }
        });
    }

    private boolean frequencyOverlap(long lower, long upper, long otherLower, long otherUpper) {
        return lower < otherUpper && otherLower < upper;
    }

    //插入假波，意味着有业务OCH 已经释放空间
    public void insertAseOch() {
        log.debug("start insert ASE och");

        List<String> ochLinkIdList = tunnel.getSupportingLink().stream().map(sl -> sl.getLinkRef().getValue()).collect(Collectors.toList());

        ochLinkIdList.forEach(ochLinkId -> {
            Link ochLink = changedObject.getChangedOchLink(ochLinkId);
            Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
            log.debug("This business ochlink has been removed and status {}: {}", ochLinkId, ochLinkAttr.getImplementState());
            List<String> siteLinkIdList = getAseInsertSiteLinkIds(ochLink, newBindingSiteLinkIds);
            if (!siteLinkIdList.isEmpty()) {
                //找出对应的site link, 逐一释放dummy och。 一个OCH穿越几个复用段
                log.debug("after business och removed, calculate how to insert dummy och");
                long lower = ochLinkAttr.getLowerFrequency().getValue().longValue();
                long upper = ochLinkAttr.getUpperFrequency().getValue().longValue();

                // Remove-leg keeps the business OCH implemented. An explicit scope restores
                // dummy OCH only on the deleted leg; normal deallocation keeps the old rule.
                siteLinkIdList.forEach(siteLinkId -> insertDummyOchOnSiteLink(siteLinkId, lower, upper, ochLink));
            }
        });

        //remove unchanged OchLink in changedObject
        log.debug("unimpacted ochList {}:", ochLinkIdList);
        removeUnImpactedOch(ochLinkIdList);
    }

    static List<String> getAseInsertSiteLinkIds(Link ochLink, List<String> scopedSiteLinkIds) {
        if (scopedSiteLinkIds != null && !scopedSiteLinkIds.isEmpty()) {
            return new ArrayList<>(scopedSiteLinkIds);
        }
        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
        if (!ImplementState.Allocate.equals(ochLinkAttr.getImplementState())) {
            return Collections.emptyList();
        }
        return ochLink.getSupportingLink().stream()
                .map(supportingLink -> supportingLink.getLinkRef().getValue())
                .filter(SiteLinkIdNamingRule::isSiteLink)
                .collect(Collectors.toList());
    }


    private List<String> getSiteLinkId(Link ochLink) {
        List<String> siteLinkIdList = new ArrayList<>();
        ochLink.getSupportingLink().forEach(sl -> {
            String linkId = sl.getLinkRef().getValue();
            if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                siteLinkIdList.add(linkId);
            }
        });
        return siteLinkIdList;
    }

    //找出需要提前释放/创建的假波，
    private void releaseDummyOchOnSiteLink(String siteLinkId, long lower, long upper) {
        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
        if (!isFlexGridSiteLink(siteLink)) {
            return;
        }
        DummyListManagerWrapOnOch dummyListManagerWrapOnOch = new DummyListManagerWrapOnOch(changedObject, siteLink);
        siteLinkDummyInfoMap.put(siteLinkId, dummyListManagerWrapOnOch);

        dummyListManagerWrapOnOch.addBusinessOch(lower, upper);  //important logic here

        resultMap.put(siteLink, dummyListManagerWrapOnOch);
    }

    //找出需要占用空间的 创建假波，
    private void insertDummyOchOnSiteLink(String siteLinkId, long lower, long upper, Link businessOchLink) {
        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
        if (!isFlexGridSiteLink(siteLink)) {
            return;
        }
        DummyListManagerWrapOnOch dummyListManagerWrapOnOch = new DummyListManagerWrapOnOch(changedObject, siteLink);
        // 补假波会新建 DGE WSS XC，需要沿用触发它的业务 OCH 方向；删除假波只使用 dummy OCH 自身 route。
        dummyListManagerWrapOnOch.useBusinessOchDirection(businessOchLink);

        siteLinkDummyInfoMap.put(siteLinkId, dummyListManagerWrapOnOch);

        dummyListManagerWrapOnOch.delBusinessOch(lower, upper);

        resultMap.put(siteLink, dummyListManagerWrapOnOch);
    }

    private void removeUnImpactedOch(List<String> keepingOchLinkIds) {
        List<String> impactedOchLinkIds = new ArrayList<>(keepingOchLinkIds);
        resultMap.keySet().stream().forEach(siteLink -> {
            List<String> needRemovedDummyOchIds = resultMap.get(siteLink).fetchRemovedDummyOch();

            impactedOchLinkIds.addAll(needRemovedDummyOchIds);
        });


        for (String key : changedObject.getChangedOchLinkList().keySet()) {
            if (impactedOchLinkIds.contains(key)) {
                continue;
            }
            changedObject.unsetOchLink(key);
        }
    }


    private boolean isFlexGridSiteLink(Link siteLink) {
        Site linkAttr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
        if (linkAttr.getGrid().equals(GridType._0)) {
            return true;
        }
        return false;
    }

    //用于配合业务OCH调整dummyOCH的方法， dummyOch下发配置 或者dummyOch配置删除，然后删除dummyOch
    public void actionOnAseOch() throws Exception {
        // One pool spans both delete and insert phases; later calls get a fresh usable pool.
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            actionOnAseOch(executor);
        } finally {
            // Also release workers on failure without interrupting submitted device writes.
            executor.shutdown();
        }
    }

    private void actionOnAseOch(ExecutorService executor) throws Exception {
        log.debug("\n\n######\nstart action (del/add) dummy och on NE");
        if (!resultMap.keySet().isEmpty()) {

            List<Link> insertedOchLinks = new ArrayList<>();
            List<Link> removedOchLinks = new ArrayList<>();

            List<AseOchActionContext> actionContexts = resultMap.keySet().stream()
                    .map(AseOchActionContext::new)
                    .collect(Collectors.toList());

            List<CompletableFuture<Void>> removeFutures = actionContexts.stream()
                    .map(context -> CompletableFuture.runAsync(() -> {
                        // 每个 context 对应一个复用段，不同复用段的网元不重叠，可以并行写设备。
                        log.debug("process ASE link in siteLink {}, delete {}",
                                context.siteLinkFriendLyName,
                                context.needRemovedDummyOchLink.stream().map(x -> x.getLinkId().getValue()).collect(Collectors.toList()));

                        DummyOchImplementor dummyOchImplementor = new DummyOchImplementor(changedObject, context.siteLink);
                        dummyOchImplementor.deImplementAll(taskGroupId, context.needRemovedDummyOchLink);
                    }, executor))
                    .collect(Collectors.toList());

            try {
                CompletableFuture.allOf(removeFutures.toArray(new CompletableFuture[0])).join();
            } catch (CompletionException e) {
                log.error("error happen when delete dummy och", e.getCause());
                if (e.getCause() instanceof Exception) {
                    throw (Exception) e.getCause();
                }
                throw new RuntimeException(e.getCause());
            }

            removedOchLinks.addAll(actionContexts.stream()
                    .flatMap(context -> context.needRemovedDummyOchLink.stream())
                    .collect(Collectors.toList()));

            actionContexts.forEach(AseOchActionContext::prepareInsertDummyOchLinks);

            List<CompletableFuture<Void>> insertFutures = actionContexts.stream()
                    .map(context -> CompletableFuture.runAsync(() -> {
                        context.needInsertedDummyOchLink.forEach(ochLink -> {
                            log.debug("process ASE link in siteLink {}, start to insert dummy Och: {}",
                                    context.siteLinkFriendLyName, ochLink.getLinkId().getValue());

                            RouteInfo rInfo = new RouteInfo();
                            rInfo.parse(ochLink.getAugmentation(Link1.class).getOch().getExplictRoute().getRoute());
                            restoreXcAttribute(rInfo, true);
                        });

                        DummyOchImplementor dummyOchImplementor = new DummyOchImplementor(changedObject, context.siteLink);
                        dummyOchImplementor.implementAll(taskGroupId, context.needInsertedDummyOchLink);
                    }, executor))
                    .collect(Collectors.toList());

            try {
                CompletableFuture.allOf(insertFutures.toArray(new CompletableFuture[0])).join();
            } catch (CompletionException e) {
                log.error("error happen when insert dummy och", e.getCause());
                if (e.getCause() instanceof Exception) {
                    throw (Exception) e.getCause();
                }
                throw new RuntimeException(e.getCause());
            }

            insertedOchLinks.addAll(actionContexts.stream()
                    .flatMap(context -> context.needInsertedDummyOchLink.stream())
                    .collect(Collectors.toList()));

            removedOchLinks.forEach(x->changedObject.addRemovedOchLink(x.getLinkId().getValue()));
            log.debug("total insert dummy och: {}", insertedOchLinks.stream().map(x->x.getLinkId().getValue()).collect(Collectors.toList()));
            log.debug("total remove dummy och: {}", removedOchLinks.stream().map(x->x.getLinkId().getValue()).collect(Collectors.toList()));

        }

        log.debug("ASE related OCH infos have updated\n#####\n\n");
    }

    private class AseOchActionContext {
        private final Link siteLink;
        private final String siteLinkFriendLyName;
        private final List<Link> needRemovedDummyOchLink;
        private List<Link> needInsertedDummyOchLink;

        private AseOchActionContext(Link siteLink) {
            this.siteLink = siteLink;
            this.siteLinkFriendLyName = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite().getFriendlyName();
            this.needRemovedDummyOchLink = resultMap.get(siteLink).fetchRemovedDummyOch().stream()
                    .map(ochLinkId -> changedObject.getChangedOchLink(ochLinkId))
                    .collect(Collectors.toList());
            this.needInsertedDummyOchLink = new ArrayList<>();
        }

        private void prepareInsertDummyOchLinks() {
            this.needInsertedDummyOchLink = resultMap.get(siteLink).fetchCreatedDummyOch();
        }
    }

    public void restoreXcAttribute(RouteInfo rInfo, boolean isAse) {
        log.debug("restore XC attr from protoType");

        protoXc.restore(changedObject, rInfo.getXcIdList(), isAse);  //copy all required params
        //update target-dest-port-output-optical-power, target-source-port-output-optical-power
        //new WssChannelChannelPowerCalculator().updatePower(changedObject, rInfo, isAse);  //假波
    }

    public void deImplementTunnel(RouteInfo rInfo, LifeCycleSevice lifeService) {
        TunnelImplementInSequence implementor = new TunnelImplementInSequence(changedObject, lifeService);
        implementor.addTunnel(tunnel, rInfo);
        implementor.start(true);
    }

    public void implementTunnel(RouteInfo rInfo, LifeCycleSevice lifeService) {
        TunnelImplementInSequence implementor = new TunnelImplementInSequence(changedObject, lifeService);
        implementor.addTunnel(tunnel, rInfo);
        implementor.start(false);
    }

    public void updateAseControlModeAs(Tunnel tunnel, Long taskGroupId, String mode) {
        updateAseControlModeAs(tunnel, taskGroupId, mode, null);
    }

    /** Uses an exact XC scope for remove-leg without changing existing add-leg callers. */
    public void updateAseControlModeAs(Tunnel tunnel, Long taskGroupId, String mode,
            Collection<String> limitedXcIds) {
        String ochLinkId = tunnel.getSupportingLink().get(0).getLinkRef().getValue();
        String tunnelId = tunnel.getTunnelId().getValue();

        AseInjectModeUpdator updator = new AseInjectModeUpdator(changedObject);
        updator.setAseControlModel(tunnelId, ochLinkId, taskGroupId, mode,
                newBindingSiteLinkIds, limitedXcIds);
    }

    public void copyOchXcPrototypeFromBusinessOch(String businessOchLinkId) {
        log.debug("copy Business XC parameter from OCH: {}", businessOchLinkId);

        ChangedObject cache = new ChangedObject();
        Link businessOchLink = cache.getChangedOchLink(businessOchLinkId);
        Och ochLinkAttr = businessOchLink.getAugmentation(Link1.class).getOch();

        RouteInfo rInfo = new RouteInfo();
        rInfo.parse(ochLinkAttr.getExplictRoute().getRoute());

        protoXc.backup(changedObject, rInfo.getXcIdList());
    }

    //把需要删除的dummyOch[0] 涉及的交叉作为原型， 它的参数，在新的业务波，假波上将被应用
    public void copyOchXcPrototypeFromDummyOch() {
        resultMap.keySet().forEach(siteLink->{
            DummyListManagerWrapOnOch manager = resultMap.get(siteLink);
            if (manager.getResult().getRemoved() != null && !manager.getResult().getRemoved().isEmpty()) {
                DummyListManager.FrequencyRange firstOne = manager.getResult().getRemoved().get(0);
                String range = String.format("%d-%d", firstOne.getLower(), firstOne.getUpper());

                Site siteLinkAttr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
                String protoOchLinkId = siteLinkAttr.getDummyLink().stream().filter(id->id.contains(range)).findAny().orElse(null);
                if (protoOchLinkId == null) {
                    log.error("hasn't find the prototype OCH {} in siteLink dummy list", range);
                    return;
                }
                Link protoOchLink = changedObject.getChangedOchLink(protoOchLinkId);
                Och protoOchLinkAttr = protoOchLink.getAugmentation(Link1.class).getOch();

                RouteInfo rInfo = new RouteInfo();
                rInfo.parse(protoOchLinkAttr.getExplictRoute().getRoute());

                log.debug("backup XC parameter from OCH: {}", protoOchLink.getLinkId().getValue());
                protoXc.backup(changedObject, rInfo.getXcIdList());
            }
        });
    }

    private List<CrossConnections> getXcFromOpNode(RouteInfo removedOchRouteInfo) {
        ChangedObject cache = new ChangedObject();

        List<CrossConnections> xcFromOpNode  = removedOchRouteInfo.getXcIdList().stream().map(xcId -> {
            String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
            Node opNode = cache.getChangedPhyOpNode(nodeId);
            if (opNode == null) {
                Node tmp = changedObject.getChangedPhyNode(nodeId);
                Physical tmpAttr = tmp.getAugmentation(Node1.class).getPhysical();
                throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                        String.format("the node %s (%s)has not been managed by adapter",
                                tmpAttr.getFriendlyName(), tmpAttr.getIp()));
            }
            Physical opNodeAttr = opNode.getAugmentation(Node1.class).getPhysical();
            CrossConnections xc = opNodeAttr.getCrossConnections().stream()
                    .filter(x -> x.getCrossConnectionId().getValue().equals(xcId)).findAny()
                    .orElseThrow(() -> new CommonException(
                            CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "Cannot find XC in node: " + xcId));
            return xc;
        }).collect(Collectors.toList());

        return xcFromOpNode;
    }

//    public static class Result {
//        private List<Link> needRemovedDummyOchLink;
//        private List<Link> needInsertedDummyOchLink;
//
//        public Result(List<Link> needRemovedDummyOchLink, List<Link> needInsertedDummyOchLink) {
//            this.needRemovedDummyOchLink = needRemovedDummyOchLink;
//            this.needInsertedDummyOchLink = needInsertedDummyOchLink;
//        }
//    }
}
