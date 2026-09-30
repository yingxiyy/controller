package net.flex.dci.otn.controller.implement.site.nbi.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.implement.common.ase.AseInjectModeUpdator;
import net.flex.dci.otn.controller.implement.common.config.ImplConfig;
import net.flex.dci.otn.controller.implement.common.dto.SwitchResult;
import net.flex.dci.otn.controller.implement.common.enums.SetResultCode;
import net.flex.dci.otn.controller.implement.common.impl.ExternalLinkUpdate;
import net.flex.dci.otn.controller.implement.common.impl.ImplActionType;
import net.flex.dci.otn.controller.implement.common.impl.Implementor;
import net.flex.dci.otn.controller.implement.common.impl.LinkImplementState;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import net.flex.dci.otn.controller.implement.common.utils.CommonUtils;
import net.flex.dci.otn.controller.implement.common.utils.DebugInfo;
import net.flex.dci.otn.controller.implement.common.utils.NeManagementChecker;
import net.flex.dci.otn.controller.implement.common.utils.RouteExtractor;
import net.flex.dci.otn.controller.implement.physical.component.ApsSwitchManager;
import net.flex.dci.otn.controller.implement.site.nbi.impl.bytedance.ByteDanceSpec;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;

@Slf4j
public class SiteLinkImplementor extends Implementor {

    private final String siteLinkId;
    //  private ImplementState targetState;
    private final LifeCycleSevice lifeService;
    private final ImplConfig implConfig;


    private final static PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
    private final static NeManagementChecker neManagementChecker = SpringBeanFinder.getBean(
            NeManagementChecker.class);
    private final static SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);
    private final static OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);

    private final ZkResourceLock locker;
    private NeYangModel yangModel;
    private ChangedObject changedObject;
    private RouteInfo rInfo;
    private Map<String, List<String>> dummyWssXcMap = Collections.emptyMap();
    private boolean hasDummyOchBeforeDeimplement = false;


    public SiteLinkImplementor(String siteLinkId, ImplementState targetState,
            LifeCycleSevice lifeService) {
        super(siteLinkId, targetState);

        this.siteLinkId = siteLinkId;
//    this.targetState = implementState;
        this.lifeService = lifeService;

        locker = new ZkResourceLock();

        implConfig = SpringBeanFinder.getBean(ImplConfig.class);
        if (implConfig == null) {
            log.debug("impl config is null");
        }
    }

    @Override
    public void startSyncAction() {
        log.debug("startSyncAction sitelink");

        String msg = null;
        boolean siteLinkActionSuccess = false;

        changedObject = new ChangedObject();
        try {
            Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
            Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

            yangModel = CommonUtils.getYangModelInProperties(siteLinkAttr.getProperties());

            // ByteDance MPO route objects are virtual. Expand them from this SiteLink's
            // real supporting links so FMUX_32 never inherits the legacy MPO1..8 range.
            rInfo = MpoAggregatingRouteNormalizer.supports(yangModel)
                    ? new RouteInfo(new MpoAggregatingRouteNormalizer(changedObject, siteLink))
                    : new RouteInfo();
            rInfo.parse(siteLinkAttr.getExplictRoute().getRoute());
            prepareForImpl(siteLink);

            checkNodeManagement(rInfo.getNodeIdList());

            siteLinkDao.updateSiteLinkImplementState(siteLinkId,
                    ImplementState.Implement.equals(targetState)
                            ? ImplementState.Doimplementing : ImplementState.Deimplementing,
                    AdminStatus.Up);

            lockResource();
            doIt();  //no exception throw out
            siteLinkActionSuccess = true;
        } catch (Exception e) {
            log.error("update siteLink implState error", e);

            updateImplementState(rInfo.getNodeIdList(), ImplementState.PartialImplement);
            msg = ExceptionUtils.getRootCauseMessage(e);
        } finally {
            try {
                store2DB(changedObject);
                if (siteLinkActionSuccess && targetState.equals(ImplementState.Allocate)
                        && hasDummyOchBeforeDeimplement) {
                    cleanResidualWssXcAfterSiteLinkDeimplemented();
                }
            } catch (RuntimeException e) {
                log.error("siteLink post-store cleanup failed", e);
            } finally {
                lifeService.logEndLinkImpl(msg);
                locker.unlock();
                DebugInfo.print(changedObject, rInfo);
            }
        }
    }

    /**
     * impl/deImpl the link
     *
     * @return
     */
    public void doIt() {
        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);

        //开启异步操作，是否成功这里判断不了
        log.info("async update siteLink implStatus start...");
        updateLink(siteLink);
    }

    private void updateLink(Link siteLink) {
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

        log.info("start to {} sitelink: {} {}", targetState, siteLink.getLinkId(),
                siteLinkAttr.getFriendlyName());

        try {
            String friendlyName = siteLinkAttr.getFriendlyName();
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            LinkImplementState implementor = new LinkImplementState(
                    LinkImplementState.LinkType.SiteLink,
                    siteLink.getLinkId().getValue(),
                    friendlyName,
                    rInfo,
                    (success, throwable) -> {
                        if (throwable != null) {
                            String msg = String.format("%s site link failed (%s). %s",
                                    targetState.equals(ImplementState.Allocate) ? "deImplement"
                                            : "Implement",
                                    friendlyName, throwable.getMessage());
                            log.error(msg, throwable);
                            future.completeExceptionally(throwable);
                        } else {
                            future.complete(success);
                        }
                    });

            if (targetState.equals(ImplementState.Allocate)) {
                log.debug("start deImplement siteLink {}", friendlyName);

                appendActiveWssXcFromCfgOrOp();

                dummyWssXcMap = extensionWssXcFromDummyOchs(siteLink.getLinkId().getValue());
                if (!dummyWssXcMap.isEmpty()) {
                    Set<String> extendedXcIds = new HashSet<>(rInfo.getXcIdList());
                    dummyWssXcMap.values().forEach(extendedXcIds::addAll);
                    rInfo.getXcIdList().clear();
                    rInfo.getXcIdList().addAll(extendedXcIds);

                    log.info("this siteLink should set ASE XC power-control-mode as manually");
                    new AseInjectModeUpdator(changedObject).updateMCSrc2DstPowerControlModel(
                            dummyWssXcMap, siteLinkId, lifeService.getGroupId());
                    log.info("dummyOch WSS XC has been appended, start deimpl sitelink");
                }

                //在 deimpl 的时候 光放的OSC 不能关闭， 否则OSC后面的网元有可能就托管了;
                rInfo.getEqIdList().removeIf(id -> id.endsWith("OSC"));

                implementor.setActionType(ImplActionType.Deimplement)
                        .changeAs(changedObject, lifeService);
            } else {
                log.debug("start Implement siteLink {}", friendlyName);
                implementor.setActionType(ImplActionType.Implement)
                        .changeAs(changedObject, lifeService);
            }

            future.get();

            if (targetState.equals(ImplementState.Allocate)) {
                clearupDummyOch(siteLink.getLinkId().getValue());
                cleanConfigResidualDummyAseXcBeforeStore();
            } else {
                //C-flex OMSP with TMUX32 should force switch to A when only has one leg
                if (siteLinkAttr.getProtectionType().getSimpleName()
                        .equals(ProtectionBidir1To1.class.getSimpleName())) {
                    checkAndDoIt(rInfo,
                            siteLinkAttr.getExplictRoute().getRoute().get(0).getPrimary()
                                    .getCrossConnections());
                }
            }

        } catch (InterruptedException e) {
            log.error("Thread was interrupted", e);
            Thread.currentThread().interrupt(); // Restore interrupt status
            throw new RuntimeException("Thread was interrupted during action on NE");
        } catch (ExecutionException e) {
            log.error("Exception while waiting for NE action to complete", e.getCause());
            throw new RuntimeException(e.getCause().getMessage(), e.getCause());
        } catch (Exception e) {
            log.error("Exception when processing", e.getCause());
            throw e;
        }
        log.debug("siteLink action completed.");
    }

    private void checkNodeManagement(List<String> nodeIdList) {
        // Keep siteLink implement-time validation aligned with tunnel validation.
        neManagementChecker.checking(nodeIdList);
    }

    private void updateImplementState(List<String> nodeIdList, ImplementState implementState)
            throws CommonException {
        log.debug("update Node ImplementState {}", implementState.name());

        for (String nodeId : nodeIdList) {
            phyNodeDao.updateConfigNodeImplState(nodeId, implementState, AdminStatus.Up);
        }

        siteLinkDao.updateSiteLinkImplementState(siteLinkId, implementState, AdminStatus.Up);

    }

    private void prepareForImpl(Link siteLink) {
        //管理用的Phylink 不在route 中，单独处理
        for (SupportingLink sl : siteLink.getSupportingLink()) {
            String linkId = sl.getLinkRef().getValue();
            if (PhysicalLinkIdNamingRule.isCableLink(linkId)) {
                rInfo.getPhyLinkIdList().add(linkId);
                rInfo.getTpIdList().add(PhysicalLinkIdNamingRule.getTpAId(linkId));
                rInfo.getTpIdList().add(PhysicalLinkIdNamingRule.getTpZId(linkId));
            }
        }

        //外部连接名称改为IP相关
        if (targetState.equals(ImplementState.Implement)) {
            new ExternalLinkUpdate(changedObject, rInfo).start();
        }

        if (isByteDanceFamily(yangModel)) {
            setChassisType("BONE_OPC");
            if (!rInfo.isHasCMUX()) {
                //通告添加a/z External的连接到rInfo.physicalLinkList(), 后续相关的板卡，tp的就可以处理
                rInfo = new ByteDanceSpec(changedObject, rInfo, siteLink).start();
            }
        }

        // wssLink is a business OCH route resource, not a SiteLink resource. SiteLink
        // implementation must not pull in the adjacent ROADM node, otherwise a stale
        // full-node save may overwrite the real SiteLink side that is being
        // implemented by another task. Dummy OCH is local to this SiteLink route and
        // does not contain wssLink either. The wssChannel cross-connection in the
        // OCH route is separate and is still handled by the business OCH flow.
    }

    private boolean isByteDanceFamily(NeYangModel yangModel) {
        // Bone2.0 keeps the ByteDance external-link supplement flow but uses its own model.
        return NeYangModel.ByteDance.equals(yangModel) || NeYangModel.Chassis20.equals(yangModel);
    }

    /**
     * 商用 C Flex 没有 ASE dummyOch，不能走 ASE 的 MANUAL 切换流程；但设备 侧可能仍有 adminUp 的 media-channel，必须在删除
     * internal-link 前由正常 removeFlexXC 流程删除。这里只把本复用段 route TP 命中的 CFG/OP WSS XC 加入 rInfo.xcIdList，不把
     * OP 的 XC 对象写回 CFG。
     */
    private void appendActiveWssXcFromCfgOrOp() {
        Set<String> routeTpIds = new HashSet<>(rInfo.getTpIdList());
        if (routeTpIds.isEmpty()) {
            return;
        }

        Set<String> existedXcIds = new HashSet<>(rInfo.getXcIdList());
        Set<String> activeWssXcIds = new HashSet<>();
        for (String nodeId : rInfo.getNodeIdList().stream().distinct()
                .collect(Collectors.toList())) {
            collectActiveRouteWssXc(changedObject.getChangedPhyNode(nodeId), routeTpIds,
                    activeWssXcIds);
            collectActiveRouteWssXc(phyNodeDao.getOpPhyNodeById(nodeId), routeTpIds,
                    activeWssXcIds);
        }

        activeWssXcIds.removeAll(existedXcIds);
        if (activeWssXcIds.isEmpty()) {
            return;
        }

        rInfo.getXcIdList().addAll(activeWssXcIds);
        log.info("append active WSS XC from cfg/op before siteLink deimplement, siteLink={}, xc={}",
                siteLinkId, activeWssXcIds);
    }

    private void collectActiveRouteWssXc(Node node, Set<String> routeTpIds, Set<String> result) {
        if (node == null || node.getAugmentation(Node1.class) == null
                || node.getAugmentation(Node1.class).getPhysical() == null
                || node.getAugmentation(Node1.class).getPhysical().getCrossConnections() == null) {
            return;
        }

        node.getAugmentation(Node1.class).getPhysical().getCrossConnections().stream()
                .filter(xc -> xc.getCrossConnectionId() != null)
                .filter(xc -> xc.getWssChannel() != null)
                .filter(this::isActiveWssXc)
                .filter(xc -> isXcOnRouteTp(xc, routeTpIds))
                .map(xc -> xc.getCrossConnectionId().getValue())
                .forEach(result::add);
    }

    private boolean isActiveWssXc(CrossConnections xc) {
        return AdminStatus.Up.equals(xc.getAdminState())
                || ImplementState.Implement.equals(xc.getImplementState());
    }

    private boolean isXcOnRouteTp(CrossConnectionAttributes xc, Set<String> routeTpIds) {
        return sourceTpInRoute(xc.getSourceTp(), routeTpIds)
                || destinationTpInRoute(xc.getDestinationTp(), routeTpIds);
    }

    private boolean sourceTpInRoute(List<SourceTp> sourceTps, Set<String> routeTpIds) {
        if (sourceTps == null) {
            return false;
        }

        return sourceTps.stream()
                .filter(tp -> tp.getTpRef() != null && tp.getTpRef().getValue() != null)
                .map(tp -> normalizeFrequencyTp(tp.getTpRef().getValue()))
                .anyMatch(routeTpIds::contains);
    }

    private boolean destinationTpInRoute(List<DestinationTp> destinationTps,
            Set<String> routeTpIds) {
        if (destinationTps == null) {
            return false;
        }

        return destinationTps.stream()
                .filter(tp -> tp.getTpRef() != null && tp.getTpRef().getValue() != null)
                .map(tp -> normalizeFrequencyTp(tp.getTpRef().getValue()))
                .anyMatch(routeTpIds::contains);
    }

    private String normalizeFrequencyTp(String tpId) {
        int index = tpId.lastIndexOf('/');
        if (index < 0 || index == tpId.length() - 1) {
            return tpId;
        }

        String suffix = tpId.substring(index + 1);
        return suffix.matches("\\d+") ? tpId.substring(0, index) : tpId;
    }


    private void setChassisType(String chassisType) {
        for (String nodeId : rInfo.getNodeIdList()) {
            Node node = changedObject.getChangedPhyNode(nodeId);
            node = setChassisTypeOnNode(node, chassisType);
            changedObject.addChangedPhyNode(node);
        }
    }

    private Node setChassisTypeOnNode(Node node, String chassisType) {
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        List<Equipments> newEqList = nodeAttr.getEquipments().stream().map(eq -> {
            if (eq.getEquipmentId().contains("CHASSIS")) {
                // Chassis is not part of the route, so add it explicitly for device write.
                addChassisEqToRoute(eq.getEquipmentId());
                Properties newProperties = PropertyTool.addProperty(eq.getProperties(),
                        "chassis-class", chassisType);
                return new EquipmentsBuilder(eq)
                        .setProperties(newProperties)
                        .build();
            }
            return eq;
        }).collect(Collectors.toList());

        return new NodeBuilder(node)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(nodeAttr)
                                .setEquipments(newEqList)
                                .build())
                        .build())
                .build();
    }

    private void addChassisEqToRoute(String eqId) {
        if (!rInfo.getEqIdList().contains(eqId)) {
            rInfo.getEqIdList().add(eqId);
        }
    }


    private void clearupDummyOch(String siteLinkId) {
        List<String> ochLinkIds = getDummyOchLinkIdsOnSiteLink(siteLinkId);

        if (!ochLinkIds.isEmpty()) {
            log.debug("following dummy och link will be remove together {}",
                    String.join("\n", ochLinkIds));
            ochLinkIds.forEach(x -> changedObject.addRemovedOchLink(x));
        }

        // 假波记录可能已删除，但复用段仍有残留引用；即使查不到假波，也必须清空 dummyLink 并随最终存盘保存。
        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

        Link newSiteLink = new LinkBuilder(siteLink).addAugmentation(Link1.class, new Link1Builder()
                        .setSite(new SiteBuilder(siteLinkAttr)
                                .setDummyLink(null)
                                .setAdminState(AdminStatus.Down)
                                .setImplementState(ImplementState.Allocate)
                                .build())
                        .build())
                .build();
        changedObject.addChangedSiteLink(newSiteLink);
    }

    /**
     * 删除复用段时，设备上也需要删除该复用段所有 dummyOch 路由里的 WSS media-channel XC。 这里保留原 extensionWssXc 的主流程位置，但数据源改为
     * dummyOch 的 CFG route， 避免从 OP 反捞已经不属于配置库的数据。
     */
    private Map<String, List<String>> extensionWssXcFromDummyOchs(String siteLinkId) {
        List<String> ochLinkIds = getDummyOchLinkIdsOnSiteLink(siteLinkId);
        if (ochLinkIds.isEmpty()) {
            hasDummyOchBeforeDeimplement = false;
            return Collections.emptyMap();
        }
        hasDummyOchBeforeDeimplement = true;

        Map<String, Set<String>> nodeDummyXcMap = new HashMap<>();
        for (String ochLinkId : ochLinkIds) {
            Link dummyOch = ochLinkDao.getOchLinkByLinkId(ochLinkId);
            if (dummyOch == null || dummyOch.getAugmentation(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                    == null) {
                log.error("cannot find dummyOch when extending WSS XC from route: {}", ochLinkId);
                continue;
            }

            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och ochAttr =
                    dummyOch.getAugmentation(
                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                            .getOch();
            if (ochAttr == null || ochAttr.getExplictRoute() == null
                    || ochAttr.getExplictRoute().getRoute() == null) {
                log.error("dummyOch has no route when extending WSS XC: {}", ochLinkId);
                continue;
            }

            RouteExtractor.extractorXc(ochAttr.getExplictRoute().getRoute()).stream()
                    .filter(xc -> xc.getWssChannel() != null)
                    .map(xc -> xc.getCrossConnectionId().getValue())
                    .forEach(xcId -> nodeDummyXcMap
                            .computeIfAbsent(PhysicalXcIdNamingRule.getNodeId(xcId),
                                    key -> new HashSet<>())
                            .add(xcId));
        }

        Map<String, List<String>> result = nodeDummyXcMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> new ArrayList<>(entry.getValue())
                ));
        log.debug("append dummyOch WSS XC from route on siteLink {} \n {}", siteLinkId, result);
        return result;
    }

    private List<String> getDummyOchLinkIdsOnSiteLink(String siteLinkId) {
        List<String> siteLinkIds = new ArrayList<>();
        siteLinkIds.add(siteLinkId);

        List<String> ochLinkIds = ochLinkDao.retrieveAllOchLinkBySupportingLinkIds(siteLinkIds);
        ochLinkIds.removeIf(OchLinkIdNamingRule::isOchBusinessLink);
        return ochLinkIds;
    }

    /**
     * make siteLink and related serverLink, node's implState = required
     *
     * @param siteLink
     * @param targetState
     */
    private void updateSiteLinkImplementState(Link siteLink, ImplementState targetState) {
        log.debug("update siteLink ImplementState {} {}", siteLink.getLinkId().getValue(),
                targetState.name());

        AdminStatus adminStatus;
        if (targetState.equals(ImplementState.Allocate)) {
            adminStatus = AdminStatus.Down;
        } else {
            adminStatus = AdminStatus.Up;
        }

        SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);
        siteLinkDao.updateSiteLinkImplementState(siteLink.getLinkId().getValue(), targetState,
                adminStatus);

        //siteLink 的server层全是phyLink
        PhyLinkDao phyLinkDao = SpringBeanFinder.getBean(PhyLinkDao.class);
        rInfo.getPhyLinkIdList().forEach(
                phyLinkId -> phyLinkDao.updateLinkImplementState(phyLinkId, targetState,
                        adminStatus));

        rInfo.getNodeIdList().forEach(
                nodeId -> phyNodeDao.updateConfigNodeImplState(nodeId, targetState,
                        adminStatus.Up));
    }

    /**
     * lock siteLink and related node's
     *
     * @param
     * @throws CommonException
     */
    private void lockResource() {
        locker.addResource(siteLinkId);
        rInfo.getNodeIdList().forEach(locker::addResource);

        locker.getLock();
        log.info("resource has locked");
    }

    @Builder
    private static class SwitchParam {

        String nodeId;
        String apsName;
        ApsPath targetPath;
        String apsCrossConnectionId;
    }

    private void checkAndDoIt(RouteInfo rInfo,
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> primaryXcList) {
        List<SwitchParam> switchParams = getSwitchParam(rInfo, primaryXcList);

        if (switchParams != null) {
            ApsSwitchManager apsSwitchManager = SpringBeanFinder.getBean(ApsSwitchManager.class);

            switchParams.forEach(param -> forceSwitch2A(apsSwitchManager, param));
        }
    }

    private void forceSwitch2A(ApsSwitchManager apsSwitchManager, SwitchParam switchParam) {
        String neId = switchParam.nodeId;
        String apsName = switchParam.apsName;
        ApsPath targetPath = switchParam.targetPath;
        String apsCrossConnectionId = switchParam.apsCrossConnectionId;

        SwitchResult switchResult = apsSwitchManager
                .executeApsSwitch(neId, apsName, apsCrossConnectionId, targetPath,
                        lifeService.getTaskInfoMessage());

        if (switchResult.code == SetResultCode.FAILED) {
            log.error(
                    "force switch to A failed for apsCrossConnectionId {}, apsName {} ,the reason is {}",
                    apsCrossConnectionId, apsName, switchResult.message);
        }
    }

    private List<SwitchParam> getSwitchParam(RouteInfo rInfo,
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> xcList) {
        //find out APS xc at first
        List<CrossConnectionAttributes> apsXcList = xcList.stream()
                .filter(xc -> xc.getDescription().contains("APS"))
                .collect(Collectors.toList());

        if (apsXcList.isEmpty()) {
            return null;
        }

        List<SwitchParam> params = new ArrayList<>();
        for (CrossConnectionAttributes apsXc : apsXcList) {
            boolean foundA = rInfo.getPhyLinkIdList().stream()
                    .anyMatch(phyLinkId -> phyLinkId.contains(
                            apsXc.getDestinationTp().get(0).getTpRef().getValue()));
            boolean foundB = rInfo.getPhyLinkIdList().stream()
                    .anyMatch(phyLinkId -> phyLinkId.contains(
                            apsXc.getDestinationTp().get(1).getTpRef().getValue()));

            if (foundA && foundB) {
                return null;
            } else {
                params.add(SwitchParam.builder()
                        .nodeId(apsXc.getNodeRef().getValue())
                        .apsName(apsXc.getDescription())
                        .apsCrossConnectionId(apsXc.getCrossConnectionId().getValue())
                        .targetPath(ApsPath.PRIMARY)
                        .build());
            }
        }
        if (params.size() != 2) {
            log.error("the OMSP protection param size must be 2");
            return null;
        }

        return params;
    }

    private void cleanResidualWssXcAfterSiteLinkDeimplemented() {
        // 复用段删除结果先入库并 merge 到 OP，再基于新的 OP 检查设备可能仍残留的
        // WSS media-channel。这样前面已经删除、但旧 OP 尚未刷新导致的对象不会再次加入。
        // 这里不吞异常，让外层把复用段删除标成失败/partial。
        new ResidualWssXcCleaner(siteLinkId, rInfo.getNodeIdList(), lifeService.getGroupId())
                .cleanAfterSiteLinkDeimplemented();
    }

    private void cleanConfigResidualDummyAseXcBeforeStore() {
        // 复用段删除成功后，CFG 中不应再保留本复用段假波的 ASEXC。
        // 这里只清理未被任何剩余 dummyOch route 引用的 ASE-* 交叉；业务 WSS
        // 交叉即使使用 ASEXC 前缀，也不会因为本次 siteLink 删除被误删。
        new ResidualWssXcCleaner(siteLinkId, rInfo.getNodeIdList(), lifeService.getGroupId())
                .cleanConfigResidualDummyAseXc(changedObject);
    }

}
