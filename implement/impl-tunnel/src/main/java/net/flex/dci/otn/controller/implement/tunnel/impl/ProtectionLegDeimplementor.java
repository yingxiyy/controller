package net.flex.dci.otn.controller.implement.tunnel.impl;

import java.util.ArrayList;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.enums.NeSubType;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otn.controller.implement.common.ase.DummyOchAllocatorOverTunnel;
import net.flex.dci.otn.controller.implement.common.ase.AseInjectModeUpdator;
import net.flex.dci.otn.controller.implement.common.config.ImplConfig;
import net.flex.dci.otn.controller.implement.common.impl.ImplActionType;
import net.flex.dci.otn.controller.implement.common.impl.LinkImplementState;
import net.flex.dci.otn.controller.implement.common.utils.ApsProtectionState;
import net.flex.dci.otn.controller.implement.common.impl.OcmUpdator;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import net.flex.dci.otn.controller.implement.common.utils.DebugInfo;
import net.flex.dci.otn.controller.implement.common.utils.RouteExtractor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;

/**
 * 保护腿删除的设备侧专用执行器。
 *
 * <p>implement 在设备操作前写入 remove-leg marker，并保留完整 OCH 路由和所有资源。
 * 本类从 marker 中识别待删的第二/第三腿，构造仅包含该腿的 RouteInfo 后下发设备。
 * 设备撤销成功后由上层 implement 流程回收数据库资源。禁止使用客户 Tunnel 的完整 route，
 * 否则会撤销 C 侧资源，而减腿不应影响客户业务。</p>
 */
@Slf4j
public class ProtectionLegDeimplementor {

    private static final String PROTECTION_CHANGE_ACTION = "protection-change-action";
    private static final String PROTECTION_CHANGE_TO_LEG_COUNT = "protection-change-to-leg-count";
    private static final String PROTECTION_CHANGE_ACTION_REMOVE_LEG = "remove-leg";

    private final String tunnelId;
    private final LifeCycleSevice lifeService;
    private final TunnelDao tunnelDao;
    private final OchLinkDao ochLinkDao;
    private final SiteLinkDao siteLinkDao;
    private final CrossConnectionsDao crossConnectionsDao;
    private final MultipleTransaction multipleTransaction;
    private final ProtectionLegResourceManager removalResources;
    private final ImplConfig implConfig;

    public ProtectionLegDeimplementor(String tunnelId, LifeCycleSevice lifeService) {
        this.tunnelId = tunnelId;
        this.lifeService = lifeService;
        this.tunnelDao = SpringBeanFinder.getBean(TunnelDao.class);
        this.ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);
        this.siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);
        this.crossConnectionsDao = SpringBeanFinder.getBean(CrossConnectionsDao.class);
        this.multipleTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
        this.removalResources = SpringBeanFinder.getBean(ProtectionLegResourceManager.class);
        this.implConfig = SpringBeanFinder.getBean(ImplConfig.class);
    }

    public void startSyncAction() {
        String error = null;
        try {
            Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
            lifeService.updateTaskInfo(tunnel.getTunnelId().getValue(),
                net.flex.dci.otc.common.model.TaskInfoMessage.ResourceType.tunnel, tunnel.getFriendlyName());
            Link ochLink = getOchLink(tunnel);
            RouteInfo removedLegRoute = buildRemovedLegRoute(ochLink);
            // The persisted marker is the retry-stable source for resources no longer visible in the route.
            removalResources.extendPreparedRemoveLegRouteInfo(tunnelId, removedLegRoute);
            Map<String, String> retainedApsC = getRetainedApsCTransitions(ochLink);

            // Only 2->1 requires a force switch; 3->2 keeps protection traffic unchanged.
            forceSwitchToPrimaryBeforeRemoveToSingleLeg(ochLink);
            sanitizeRemovedLegResources(ochLink, removedLegRoute);
            log.info("Start device deimplement for protection leg, tunnelId={}, route={}, retainedApsC={}",
                    tunnelId, removedLegRoute, retainedApsC);
            deimplementLegResources(tunnel, ochLink, removedLegRoute, retainedApsC);
        } catch (Exception e) {
            error = e.getMessage() == null ? e.toString() : e.getMessage();
            log.error("Failed to deimplement protection leg for tunnelId={}", tunnelId, e);
        } finally {
            lifeService.logEndLinkImpl(error);
        }
    }

    /**
     * 根据 marker 的目标腿数从完整 OCH route 取出待删腿，并映射到 RouteInfo 的 primary 槽位。
     * RouteInfo 解析器以 primary 为入口；这里的 primary 只是执行器内部的单腿载体，不改变原始 OCH route。
     */
    private RouteInfo buildRemovedLegRoute(Link ochLink) {
        Och och = ochLink.getAugmentation(Link1.class).getOch();
        Properties properties = och.getProperties();
        String action = PropertyTool.getValue(properties, PROTECTION_CHANGE_ACTION);
        if (!PROTECTION_CHANGE_ACTION_REMOVE_LEG.equals(action)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "Protection-leg device deimplement requires remove-leg marker, actual action=" + action);
        }
        int toLegCount = getIntProperty(properties, PROTECTION_CHANGE_TO_LEG_COUNT);
        Route sourceRoute = och.getExplictRoute().getRoute().get(0);
        Route removedLegOnly;
        if (toLegCount == 2 && sourceRoute.getThird() != null && !sourceRoute.getThird().isEmpty()) {
            Third removedThird = sourceRoute.getThird().get(0);
            removedLegOnly = new RouteBuilder().setPrimary(new PrimaryBuilder()
                .setExplicitRouteObjects(removedThird.getExplicitRouteObjects())
                .setCrossConnections(removedThird.getCrossConnections())
                .build()).build();
        } else if (toLegCount == 1 && sourceRoute.getSecondary() != null) {
            Secondary removedSecondary = sourceRoute.getSecondary();
            removedLegOnly = new RouteBuilder().setPrimary(new PrimaryBuilder()
                .setExplicitRouteObjects(removedSecondary.getExplicitRouteObjects())
                .setCrossConnections(removedSecondary.getCrossConnections())
                .build()).build();
        } else {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "Remove-leg marker does not match a removable OCH route, toLegCount=" + toLegCount);
        }
        RouteInfo removedLegRoute = new RouteInfo().parse(Collections.singletonList(removedLegOnly));
        extendWithAssociatedTunnelLegResources(ochLink, toLegCount, removedLegRoute);
        return removedLegRoute;
    }

    private void extendWithAssociatedTunnelLegResources(Link ochLink, int toLegCount, RouteInfo removedLegRoute) {
        for (Tunnel tunnel : tunnelDao.getAllTunnelsUnderOchLink(Collections.singletonList(
                ochLink.getLinkId().getValue()))) {
            Route tunnelLeg = getRemovedTunnelLegRoute(tunnel, toLegCount);
            if (tunnelLeg != null) {
                removedLegRoute.extend(new RouteInfo().parse(Collections.singletonList(tunnelLeg)));
            }
        }
    }

    private Route getRemovedTunnelLegRoute(Tunnel tunnel, int toLegCount) {
        if (tunnel.getExplictRoute() == null || tunnel.getExplictRoute().getRoute() == null
                || tunnel.getExplictRoute().getRoute().isEmpty()) {
            return null;
        }
        Route route = tunnel.getExplictRoute().getRoute().get(0);
        if (toLegCount == 2 && route.getThird() != null && !route.getThird().isEmpty()) {
            Third third = route.getThird().get(0);
            return new RouteBuilder().setPrimary(new PrimaryBuilder()
                    .setExplicitRouteObjects(third.getExplicitRouteObjects())
                    .setCrossConnections(third.getCrossConnections())
                    .build()).build();
        }
        if (toLegCount == 1 && route.getSecondary() != null) {
            Secondary secondary = route.getSecondary();
            return new RouteBuilder().setPrimary(new PrimaryBuilder()
                    .setExplicitRouteObjects(secondary.getExplicitRouteObjects())
                    .setCrossConnections(secondary.getCrossConnections())
                    .build()).build();
        }
        return null;
    }

    /**
     * 将单腿 RouteInfo 裁剪为真正允许撤销的资源集合。客户侧 C 资源、设备/收发器、SiteLink
     * 外部 OMS 及保留腿仍在使用的资源都不能进入设备撤销命令。
     */
    private void sanitizeRemovedLegResources(Link ochLink, RouteInfo removedLegRoute) {
        // The removed branch may contain customer-facing C resources shared with the retained service.
        // Keep only protection-leg resources in the device deimplement payload.
        removeClientSideResources(ochLink, removedLegRoute);
        // 站点外部 OMS 是接入侧公共资源，不能撤销。
        removeExternalOmsResources(removedLegRoute);
        // 主腿、保留保护腿仍在使用的资源不能重复下发撤销。
        removeResourcesStillUsedByRetainedLegs(ochLink, removedLegRoute);
    }

    private void removeClientSideResources(Link ochLink, RouteInfo routeInfo) {
        Set<String> clientTpIds = new HashSet<>();
        for (Tunnel tunnel : tunnelDao.getAllTunnelsUnderOchLink(Collections.singletonList(
                ochLink.getLinkId().getValue()))) {
            if (tunnel.getSourceTp() != null) {
                tunnel.getSourceTp().forEach(tp -> clientTpIds.add(tp.getTpRef().getValue()));
            }
            if (tunnel.getDestinationTp() != null) {
                tunnel.getDestinationTp().forEach(tp -> clientTpIds.add(tp.getTpRef().getValue()));
            }
        }
        routeInfo.getTpIdList().removeIf(clientTpIds::contains);
        // 客户侧 XC 的 ID 以其端口 ID 为组成部分；只按本 OCH 下业务的明确 C TP 作排除，
        // 不以模糊的 "C" 字符串判断，避免误伤线路侧资源。
        routeInfo.getXcIdList().removeIf(xcId -> clientTpIds.stream().anyMatch(xcId::contains));
    }

    private void removeExternalOmsResources(RouteInfo routeInfo) {
        Set<String> externalOmsLinkIds = new HashSet<>();
        for (String siteLinkId : routeInfo.getLogicServerLinkIdList()) {
            if (!SiteLinkIdNamingRule.isSiteLink(siteLinkId)) {
                continue;
            }
            Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
            if (siteLink == null) {
                continue;
            }
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1 augmentation =
                    siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class);
            if (augmentation == null || augmentation.getSite() == null) {
                continue;
            }
            Site site = augmentation.getSite();
            collectExternalOms(site.getAExternal() == null ? null : site.getAExternal().getAddDropLink(), externalOmsLinkIds);
            collectExternalOms(site.getZExternal() == null ? null : site.getZExternal().getAddDropLink(), externalOmsLinkIds);
        }
        if (externalOmsLinkIds.isEmpty()) {
            return;
        }
        Set<String> externalOmsTpIds = new HashSet<>();
        externalOmsLinkIds.forEach(linkId -> {
            externalOmsTpIds.add(PhysicalLinkIdNamingRule.getTpAId(linkId));
            externalOmsTpIds.add(PhysicalLinkIdNamingRule.getTpZId(linkId));
        });
        routeInfo.getPhyLinkIdList().removeIf(externalOmsLinkIds::contains);
        routeInfo.getTpIdList().removeIf(externalOmsTpIds::contains);
    }

    private void collectExternalOms(List<AddDropLink> addDropLinks, Set<String> result) {
        if (addDropLinks == null) {
            return;
        }
        for (AddDropLink addDropLink : addDropLinks) {
            if (addDropLink.getLinkRef() != null && PhysicalLinkIdNamingRule.isOmsLink(addDropLink.getLinkRef())) {
                result.add(addDropLink.getLinkRef());
            }
        }
    }

    private void removeResourcesStillUsedByRetainedLegs(Link ochLink, RouteInfo removedLegRoute) {
        Och och = ochLink.getAugmentation(Link1.class).getOch();
        Route sourceRoute = och.getExplictRoute().getRoute().get(0);
        int toLegCount = getIntProperty(och.getProperties(), PROTECTION_CHANGE_TO_LEG_COUNT);
        Route retained = buildRetainedRoute(sourceRoute, toLegCount);
        RouteInfo retainedRoute = new RouteInfo().parse(Collections.singletonList(retained));
        removedLegRoute.getTpIdList().removeIf(retainedRoute.getTpIdList()::contains);
        removedLegRoute.getXcIdList().removeIf(retainedRoute.getXcIdList()::contains);
        removedLegRoute.getPhyLinkIdList().removeIf(retainedRoute.getPhyLinkIdList()::contains);
        removedLegRoute.getEqIdList().removeIf(retainedRoute.getEqIdList()::contains);
    }

    static Route buildRetainedRoute(Route sourceRoute, int toLegCount) {
        RouteBuilder retained = new RouteBuilder().setPrimary(sourceRoute.getPrimary());
        if (toLegCount >= 2 && sourceRoute.getSecondary() != null) {
            // 3->2 retains secondary; 2->1 must deimplement secondary instead of filtering it out.
            retained.setSecondary(sourceRoute.getSecondary());
        }
        return retained.build();
    }

    /**
     * 3->2 retains the primary APS XC, but its C member must return to LOP and
     * the C port must be adminDown. Keep this transition outside the removed route.
     */
    private Map<String, String> getRetainedApsCTransitions(Link ochLink) {
        Och och = ochLink.getAugmentation(Link1.class).getOch();
        return buildRetainedApsCTransitions(
                getIntProperty(och.getProperties(), PROTECTION_CHANGE_TO_LEG_COUNT),
                RouteExtractor.extractorXc(och.getExplictRoute().getRoute()));
    }

    static Map<String, String> buildRetainedApsCTransitions(int toLegCount,
            Collection<? extends CrossConnectionAttributes> crossConnections) {
        if (toLegCount != 2) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (CrossConnectionAttributes xc : crossConnections) {
            if (xc.getAps() == null || xc.getSourceTp() == null || xc.getSourceTp().isEmpty()) {
                continue;
            }
            String sourceTpId = xc.getSourceTp().get(0).getTpRef().getValue();
            if (!sourceTpId.endsWith("SIG")) {
                continue;
            }
            // APS source is the SIG member; only replace that terminal role so
            // LINECARD and PORT sequence numbers remain unchanged.
            result.put(xc.getCrossConnectionId().getValue(),
                    sourceTpId.substring(0, sourceTpId.length() - "SIG".length()) + "C");
        }
        return result;
    }

    /** 仅在 2→1 前将 APS 倒换回主路；3→2 保留保护能力，不能强制改当前业务路径。 */
    private void forceSwitchToPrimaryBeforeRemoveToSingleLeg(Link ochLink) {
        Och och = ochLink.getAugmentation(Link1.class).getOch();
        if (getIntProperty(och.getProperties(), PROTECTION_CHANGE_TO_LEG_COUNT) != 1) {
            return;
        }
        List<CrossConnectionAttributes> apsXcs = RouteExtractor.extractorXc(och.getExplictRoute().getRoute())
                .stream().filter(xc -> xc.getAps() != null).collect(Collectors.toList());
        if (apsXcs.isEmpty()) {
            return;
        }
        try {
            // impl-tunnel 不直接依赖 impl-physical，避免形成模块循环；运行时由 Spring 提供 APS 管理器。
            Class<?> managerClass = Class.forName(
                    "net.flex.dci.otn.controller.implement.physical.component.ApsSwitchManager");
            Object manager = SpringBeanFinder.getBean(managerClass);
            Method switchMethod = Arrays.stream(managerClass.getMethods())
                    .filter(method -> "executeApsSwitch".equals(method.getName())
                            && method.getParameterTypes().length == 5)
                    .findFirst()
                    .orElseThrow(() -> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "ApsSwitchManager.executeApsSwitch is unavailable"));
            Object primary = Arrays.stream(switchMethod.getParameterTypes()[3].getEnumConstants())
                    .filter(value -> "PRIMARY".equals(value.toString()))
                    .findFirst()
                    .orElseThrow(() -> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "APS PRIMARY path is unavailable"));
            Method getCode = null;
            Method getMessage = null;
            for (CrossConnectionAttributes apsXc : apsXcs) {
                // 逐个 APS XC 下发倒换，任何一个失败都阻止后续裁库。
                String apsXcId = apsXc.getCrossConnectionId().getValue();
                // OP XC may omit the APS name/description; force-switch must use the persisted CONFIG XC.
                CrossConnections configApsXc = crossConnectionsDao.getConfigXC(
                        apsXc.getNodeRef().getValue(), apsXcId);
                Object result = switchMethod.invoke(manager,
                        apsXc.getNodeRef().getValue(), getApsName(apsXc, configApsXc),
                        apsXcId, primary, lifeService.getTaskInfoMessage());
                if (getCode == null) {
                    getCode = result.getClass().getMethod("getCode");
                    getMessage = result.getClass().getMethod("getMessage");
                }
                if ("FAILED".equals(String.valueOf(getCode.invoke(result)))) {
                    throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                            "Force APS switch to primary failed for " + apsXc.getCrossConnectionId().getValue()
                                    + ": " + getMessage.invoke(result));
                }
            }
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    "Force APS switch to primary failed", e);
        }
    }

    static String getApsName(CrossConnectionAttributes routeApsXc,
                             CrossConnectionAttributes configApsXc) {
        // Route extraction can drop the XC description; use the persisted config XC as the stable source.
        CrossConnectionAttributes source = configApsXc == null ? routeApsXc : configApsXc;
        return source.getAps() != null && source.getAps().getName() != null
                ? source.getAps().getName() : source.getDescription();
    }

    /**
     * 仅撤销 marker 指定保护腿的设备资源。
     *
     * <p>第一步：按单腿 RouteInfo 装配 XC、TP、设备、内部/物理链路；按这条腿经过的
     * SiteLink 重算并加入 OCM 节点。第二步：使用资源级 {@link LinkImplementState} 下发，
     * 其汇总类型不会写 Tunnel、OCH 或 SiteLink 的状态。第三步：只有所有节点设备动作成功
     * 才持久化物理节点状态；route 与拓扑资源由 implement 的最终 trim 统一删除。</p>
     */
    private void deimplementLegResources(Tunnel tunnel, Link ochLink, RouteInfo removedLegRoute,
                                         Map<String, String> retainedApsC) throws Exception {
        // 装入目标腿经过的 SiteLink，作为 OCM 的唯一影响范围。
        ChangedObject changedObject = new ChangedObject();
        retainOnlyRegEquipment(changedObject, removedLegRoute);
        List<Link> targetSiteLinks = removedLegRoute.getLogicServerLinkIdList().stream()
                .filter(SiteLinkIdNamingRule::isSiteLink)
                .map(changedObject::getChangedSiteLink)
                .collect(Collectors.toList());

        // 只移除目标腿对应的 OCM 波道。
        OcmUpdator ocmUpdator = new OcmUpdator(changedObject, targetSiteLinks);
        ocmUpdator.remove(ochLink);
        Collection<Node> affectedOcmNodes = ocmUpdator.getUpdatedNode();
        affectedOcmNodes.forEach(changedObject::addChangedPhyNode);

        boolean restoreDummyOch = requiresDummyOchRestore(targetSiteLinks);
        DummyOchAllocatorOverTunnel dummyOchAllocator = null;
        if (restoreDummyOch) {
            List<String> removedSiteLinkIds = targetSiteLinks.stream()
                    .map(siteLink -> siteLink.getLinkId().getValue())
                    .collect(Collectors.toList());
            // 业务 OCH 在减腿期间仍为 Implement；显式 scope 保证只补被删除腿的复用段。
            dummyOchAllocator = new DummyOchAllocatorOverTunnel(tunnel,
                    lifeService.getGroupId(), removedSiteLinkIds);
            dummyOchAllocator.setCache(changedObject);
            dummyOchAllocator.copyOchXcPrototypeFromBusinessOch(ochLink.getLinkId().getValue());
        }

        // 输出本次保护腿撤销涉及的完整资源快照，便于核对设备下发范围。
        DebugInfo.printTunnelTable(changedObject, removedLegRoute);

        // First remove only resources owned by the deleted leg.
        executeProtectionLegState(ochLink, changedObject, removedLegRoute,
                ImplActionType.Deimplement,
                affectedOcmNodes.stream().map(node -> node.getNodeId().getValue())
                        .collect(Collectors.toSet()),
                Collections.emptyMap(), "deimplement protection leg resources");

        if (dummyOchAllocator != null) {
            // 与普通 Tunnel 退配一致：业务资源释放成功后才补并下发假波。
            log.info("Restore dummy OCH after protection leg removal, ochLinkId={}, siteLinks={}",
                    ochLink.getLinkId().getValue(),
                    targetSiteLinks.stream().map(siteLink -> siteLink.getLinkId().getValue())
                            .collect(Collectors.toList()));
            dummyOchAllocator.insertAseOch();
            dummyOchAllocator.actionOnAseOch();
        }

        // Then update the retained APS through write2Ne. Mixing it into the
        // Deimplement payload would call remove2Ne for an in-service APS XC.
        executeProtectionLegState(ochLink, changedObject, new RouteInfo(),
                ImplActionType.Implement, Collections.emptySet(), retainedApsC,
                "restore retained two-leg APS state");

        // ConfigNeSequence persists TP admin/implement state, but XC property-only
        // updates are not copied by its generic state updater. Persist C.enabled
        // explicitly so retry and later operations read the same state as the device.
        persistRetainedApsCState(changedObject, retainedApsC);

        if (dummyOchAllocator != null) {
            String sourceTpId = ochLink.getSource().getSourceTp().getValue();
            String destTpId = ochLink.getDestination().getDestTp().getValue();
            boolean sourceUp = tpAdminUp(changedObject, sourceTpId);
            boolean destUp = tpAdminUp(changedObject, destTpId);
            log.info("remove protection leg ASE disable endpoint check, ochLinkId={}, "
                            + "sourceTp={} adminUp={}, destTp={} adminUp={}",
                    ochLink.getLinkId().getValue(), sourceTpId, sourceUp, destTpId, destUp);
            boolean aseSpecific = NeYangModel.ByteDance.equals(implConfig.getYangModel())
                    || NeYangModel.Chassis20.equals(implConfig.getYangModel());
            if (shouldDisableAseControlMode(aseSpecific, sourceUp, destUp)) {
                // Match add-leg finalization after all scoped leg and shared APS actions succeeded.
                dummyOchAllocator.updateAseControlModeAs(tunnel, lifeService.getGroupId(),
                        AseInjectModeUpdator.ASE_CONTROL_MODE_DISABLE,
                        removedLegRoute.getXcIdList());
            }
        }

        // 设备全部成功后保存物理状态及目标腿假波变化，业务拓扑留给最终 trim。
        Set<String> persistedNodeIds = new HashSet<>(removedLegRoute.getNodeIdList());
        persistedNodeIds.addAll(affectedOcmNodes.stream()
                .map(node -> node.getNodeId().getValue()).collect(Collectors.toSet()));
        retainedApsC.keySet().stream().map(PhysicalXcIdNamingRule::getNodeId)
                .forEach(persistedNodeIds::add);
        persistProtectionLegDeviceState(changedObject, ochLink.getLinkId().getValue(),
                targetSiteLinks, persistedNodeIds, restoreDummyOch);
    }

    /** Only REG equipment is released by remove-leg; shared optical/electric cards remain untouched. */
    private void retainOnlyRegEquipment(ChangedObject changedObject, RouteInfo routeInfo) {
        routeInfo.getEqIdList().removeIf(equipmentId -> {
            Node node = changedObject.getChangedPhyNode(PhysicalEqpIdNamingRule.getNodeId(equipmentId));
            Physical physical = node == null || node.getAugmentation(Node1.class) == null
                    ? null : node.getAugmentation(Node1.class).getPhysical();
            return physical == null
                    || !NeSubType.EPC_REG.getSubTypeName().equals(physical.getCustomedType());
        });
    }

    private boolean hasDummyOch(Link siteLink) {
        Site site = siteLink.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                .getSite();
        return site.getDummyLink() != null && !site.getDummyLink().isEmpty();
    }

    private boolean requiresDummyOchRestore(List<Link> targetSiteLinks) {
        boolean anyInjected = targetSiteLinks.stream().anyMatch(this::hasDummyOch);
        boolean allInjected = targetSiteLinks.stream().allMatch(this::hasDummyOch);
        if (anyInjected && !allInjected) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "The removed protection leg siteLinks must use the same ASE injection mode");
        }
        return anyInjected;
    }

    private void executeProtectionLegState(Link ochLink, ChangedObject changedObject,
                                             RouteInfo routeInfo, ImplActionType actionType,
                                             Set<String> ocmNodeIds,
                                             Map<String, String> retainedApsC,
                                             String operation) {
        CompletableFuture<Boolean> completed = new CompletableFuture<>();
        LinkImplementState implementState = new LinkImplementState(
                LinkImplementState.LinkType.ProtectionLeg,
                ochLink.getLinkId().getValue(),
                ochLink.getAugmentation(Link1.class).getOch().getFriendlyName(),
                routeInfo,
                (success, throwable) -> {
                    if (throwable != null || !success) {
                        completed.completeExceptionally(throwable == null
                                ? new CommonException(CommonExceptionType.DEVICE_ERROR,
                                operation + " failed") : throwable);
                    } else {
                        completed.complete(true);
                    }
                });
        implementState.setOcmNodeIds(ocmNodeIds);
        implementState.setRetainedProtectionApsC(retainedApsC);
        implementState.setActionType(actionType).changeAs(changedObject, lifeService);

        try {
            completed.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Interrupted while " + operation, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    operation + " failed: " + cause.getMessage(), cause);
        }
    }

    private void persistRetainedApsCState(ChangedObject changedObject,
                                          Map<String, String> retainedApsC) {
        retainedApsC.forEach((apsXcId, cTpId) -> {
            String nodeId = PhysicalXcIdNamingRule.getNodeId(apsXcId);
            Node node = changedObject.getChangedPhyNode(nodeId);
            changedObject.addChangedPhyNode(
                    ApsProtectionState.restoreTwoLegState(node, apsXcId, cTpId));
        });
    }

    static boolean shouldDisableAseControlMode(boolean aseSpecific,
                                               boolean sourceUp, boolean destUp) {
        return aseSpecific && sourceUp && destUp;
    }

    private boolean tpAdminUp(ChangedObject changedObject, String tpId) {
        Node node = changedObject.getChangedPhyNode(PhysicalTpIdNamingRule.getNodeId(tpId));
        TerminationPoint tp = node.getTerminationPoint().stream()
                .filter(value -> value.getTpId().getValue().equals(tpId))
                .findAny().orElse(null);
        if (tp == null || tp.getAugmentation(TerminationPoint1.class) == null
                || tp.getAugmentation(TerminationPoint1.class).getPhysical() == null) {
            log.warn("remove protection leg ASE disable cannot find physical tp {}", tpId);
            return false;
        }
        return AdminStatus.Up.equals(
                tp.getAugmentation(TerminationPoint1.class).getPhysical().getAdminState());
    }

    /**
     * 设备成功后保存物理状态；ASE 减腿额外保存目标 SiteLink 和 dummy OCH 变化。
     * 业务 OCH、Tunnel 和物理链路仍由 implement 的最终 trim 统一处理。
     */
    private void persistProtectionLegDeviceState(ChangedObject changedObject, String ochLinkId,
                                                   List<Link> targetSiteLinks,
                                                   Set<String> persistedNodeIds,
                                                   boolean persistDummyOchChanges) {
        changedObject.unsetOchLink(ochLinkId);
        if (!persistDummyOchChanges) {
            targetSiteLinks.forEach(siteLink ->
                    changedObject.unsetSiteLink(siteLink.getLinkId().getValue()));
        }
        new ArrayList<>(changedObject.getChangedPhyLinkList().keySet())
                .forEach(changedObject::unsetPhyLink);
        new ArrayList<>(changedObject.getChangedTunnelList().keySet())
                .forEach(changedObject::unsetTunnel);
        // Prototype lookup may load nodes from retained legs; never persist them during a scoped leg removal.
        new ArrayList<>(changedObject.getChangedPhyNodeList().keySet()).stream()
                .filter(nodeId -> !persistedNodeIds.contains(nodeId))
                .forEach(changedObject::unsetPhyNode);
        multipleTransaction.save(changedObject);
    }

    private Link getOchLink(Tunnel tunnel) {
        String ochLinkId = tunnel.getSupportingLink().stream()
            .findFirst()
            .map(link -> link.getLinkRef().getValue())
            .orElseThrow(() -> new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "Tunnel has no supporting OCH link: " + tunnelId));
        return ochLinkDao.getOchLinkByLinkId(ochLinkId);
    }

    private int getIntProperty(Properties properties, String propertyName) {
        String value = PropertyTool.getValue(properties, propertyName);
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "Invalid remove-leg marker " + propertyName + ": " + value, e);
        }
    }
}
