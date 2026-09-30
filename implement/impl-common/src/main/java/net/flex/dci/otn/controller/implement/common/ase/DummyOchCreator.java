package net.flex.dci.otn.controller.implement.common.ase;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.config.ImplConfig;
import net.flex.dci.otn.controller.implement.common.utils.CommonUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

//almost functions is same as allocate module
//为创建dummy och 查找提供必要信息
@Slf4j
public class DummyOchCreator {
    private List<String> otmNodeList;
    private String siteLinkId;

    private List<String> amplifierXC_C;
    private List<String> amplifierXC_L;

    private String ochSrcTp;
    private String ochDstTp;
    private List<FmuxAsePath> fmuxAsePaths = new ArrayList<>();

    private ChangedObject changedObject;

    public DummyOchCreator(String siteLinkId, ChangedObject changedObject) {
        this.changedObject = changedObject;
        this.siteLinkId = siteLinkId;
        this.otmNodeList = new ArrayList<>();

        amplifierXC_C = new ArrayList<>();
        amplifierXC_L = new ArrayList<>();

        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
        init(siteLink);
    }

    private void init(Link siteLink) {

        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

        RouteInfo rInfo = new RouteInfo();
        rInfo.parse(siteLinkAttr.getExplictRoute().getRoute());
        distinguishBandC_L(rInfo.getXcIdList());   //update amplifierXC_C and amplifierXC_L

        findoutOTM(siteLink);       //update otmNodeList;

        String srcTp = siteLink.getSource().getSourceTp().getValue();
        String dstTp = siteLink.getDestination().getDestTp().getValue();
        Optional<String> srcExp33 = findExp33(srcTp);
        Optional<String> dstExp33 = findExp33(dstTp);
        if (srcExp33.isPresent() && dstExp33.isPresent()) {
            ochSrcTp = srcExp33.get();
            ochDstTp = dstExp33.get();
            return;
        }

        // Bone2.0 Flex terminates on MUXPANEL MPO, but its ASE source is FMUX_32 EXP33.
        NeYangModel yangModel = CommonUtils.getYangModelInProperties(siteLinkAttr.getProperties());
        if (NeYangModel.Chassis20.equals(yangModel)) {
            fmuxAsePaths = resolveFmuxAsePaths(siteLink);
            if (!fmuxAsePaths.isEmpty()) {
                return;
            }
        }

        ochSrcTp = getExp33(srcTp);
        ochDstTp = getExp33(dstTp);
    }


    public Link createAseOchLink(long lower, long upper) {
        FrequencyType fLower = new FrequencyType(BigInteger.valueOf(lower));
        FrequencyType fUpper = new FrequencyType(BigInteger.valueOf(upper));
        Available ava = new AvailableBuilder().setKey(new AvailableKey(fLower))
                .setLowerFrequency(fLower)
                .setUpperFrequency(fUpper)
                .build();

        Link ochLink;
        Optional<FmuxAsePath> fmuxPath = findFmuxAsePath(lower, upper);
        if (fmuxPath.isPresent()) {
            FmuxAsePath path = fmuxPath.get();
            ochLink = DummyOchLinkConstructor.defaultConstructor(changedObject)
                    .createWithAseXcEndpoints(path.getATp(), path.getZTp(), ava,
                            path.getAseXcEndpoints(), amplifierXcList(lower, upper),
                            otmNodeList, new LinkId(siteLinkId));
        } else if (inBandC(lower, upper)) {
            ochLink = DummyOchLinkConstructor.defaultConstructor(changedObject).create(ochSrcTp, ochDstTp, ava, amplifierXC_C, otmNodeList, new LinkId(siteLinkId));
        } else {
            ochLink = DummyOchLinkConstructor.defaultConstructor(changedObject).create(ochSrcTp, ochDstTp, ava, amplifierXC_L, otmNodeList, new LinkId(siteLinkId));
        }
        insertIntoSiteLink(ochLink);
        return ochLink;
    }

    public Link createAseOchLink(long lower, long upper, Link businessOchLink) {
        FrequencyType fLower = new FrequencyType(BigInteger.valueOf(lower));
        FrequencyType fUpper = new FrequencyType(BigInteger.valueOf(upper));
        Available ava = new AvailableBuilder().setKey(new AvailableKey(fLower))
                .setLowerFrequency(fLower)
                .setUpperFrequency(fUpper)
                .build();

        // 业务触发的假波补充需要沿用业务 OCH 上 DGE WSS XC 的方向，避免同一 DGE 上假波/业务波 source-dest 相反。
        DummyOchLinkConstructor constructor = DummyOchLinkConstructor.businessOchConstructor(changedObject, businessOchLink);
        Link ochLink;
        Optional<FmuxAsePath> fmuxPath = findFmuxAsePath(lower, upper);
        if (fmuxPath.isPresent()) {
            FmuxAsePath path = fmuxPath.get();
            ochLink = constructor.createWithAseXcEndpoints(path.getATp(), path.getZTp(), ava,
                    path.getAseXcEndpoints(), amplifierXcList(lower, upper),
                    otmNodeList, new LinkId(siteLinkId));
        } else if (inBandC(lower, upper)) {
            ochLink = constructor.create(ochSrcTp, ochDstTp, ava, amplifierXC_C, otmNodeList, new LinkId(siteLinkId));
        } else {
            ochLink = constructor.create(ochSrcTp, ochDstTp, ava, amplifierXC_L, otmNodeList, new LinkId(siteLinkId));
        }
        insertIntoSiteLink(ochLink);
        return ochLink;
    }

    private void findoutOTM(Link siteLink) {
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

        siteLinkExternalChecking(siteLinkAttr.getAExternal().getAddDropLink());
        siteLinkExternalChecking(siteLinkAttr.getZExternal().getAddDropLink());
    }

    private void siteLinkExternalChecking(List<AddDropLink> addDropLink) {
        if (addDropLink == null || addDropLink.isEmpty()) {
            return;
        }

        if (addDropLink.get(0).getConnnectorType().name().contains(EquipType.MUX.name())) {
            String aTpId = PhysicalLinkIdNamingRule.getTpAId(addDropLink.get(0).getLinkRef());
            String zTpId = PhysicalLinkIdNamingRule.getTpZId(addDropLink.get(0).getLinkRef());

            String muxTpId;
            if (aTpId.contains(EquipType.MUX.name())) {
                muxTpId = aTpId;
            } else {
                muxTpId = zTpId;
            }
            String nodeId = PhysicalTpIdNamingRule.getNodeId(muxTpId);
            otmNodeList.add(nodeId);
        }
    }

    private void insertIntoSiteLink(Link ochLink) {
        Link siteLink = changedObject.getChangedSiteLink(siteLinkId); //refresh

        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
        List<String> ochLinkIdList = siteLinkAttr.getDummyLink();
        if (ochLinkIdList == null) {
            ochLinkIdList = new ArrayList<>();
        } else {
            ochLinkIdList = new ArrayList<>(ochLinkIdList);
        }
        List<String> beforeDummyLinks = new ArrayList<>(ochLinkIdList);
        ochLinkIdList.add(ochLink.getLinkId().getValue());

        Link newSiteLink = new LinkBuilder(siteLink).addAugmentation(Link1.class, new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                .setSite(new SiteBuilder(siteLinkAttr).setDummyLink(ochLinkIdList).build())
                .build()).build();
        siteLink = newSiteLink;

        changedObject.addChangedSiteLink(siteLink);
        // The created dummy OCH and siteLink.dummyLink relation must be saved together by ChangedObject.
        log.info("DUMMY_RELATION_UPDATE siteLink={} addDummy={} before={} after={}",
                siteLinkId, ochLink.getLinkId().getValue(), beforeDummyLinks, ochLinkIdList);
    }

    private String getExp33(String tpId) {
        return findExp33(tpId).orElseThrow(() -> {
            String eqId = PhysicalTpIdNamingRule.getEquipId(tpId);
            return new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("cannot find EXP 33 on eq %s", eqId));
        });
    }

    private Optional<String> findExp33(String tpId) {
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        String eqId = PhysicalTpIdNamingRule.getEquipId(tpId);
        Node node = changedObject.getChangedPhyNode(nodeId);
        if (node == null || node.getTerminationPoint() == null) {
            return Optional.empty();
        }
        Optional<TerminationPoint> tpOp = node.getTerminationPoint().stream().filter(tp -> tp.getTpId().getValue().contains("EXP33") && tp.getTpId().getValue().contains(eqId)).findAny();
        return tpOp.map(tp -> tp.getTpId().getValue());
    }

    private List<FmuxAsePath> resolveFmuxAsePaths(Link siteLink) {
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
        List<FmuxGroup> aGroups = resolveFmuxGroups(siteLinkAttr.getAExternal().getAddDropLink());
        List<FmuxGroup> zGroups = resolveFmuxGroups(siteLinkAttr.getZExternal().getAddDropLink());
        if (aGroups.isEmpty() || aGroups.size() != zGroups.size()) {
            return Collections.emptyList();
        }

        aGroups.sort(Comparator.comparingInt(FmuxGroup::getMinPanelMpo));
        zGroups.sort(Comparator.comparingInt(FmuxGroup::getMinPanelMpo));
        long lower = siteLinkAttr.getAvailable().get(0).getLowerFrequency().getValue().longValue();
        long upper = siteLinkAttr.getAvailable().get(0).getUpperFrequency().getValue().longValue();

        Optional<FmuxGroup> aApsGroup = findApsFmuxGroup(aGroups);
        Optional<FmuxGroup> zApsGroup = findApsFmuxGroup(zGroups);
        if (aApsGroup.isPresent() && zApsGroup.isPresent()) {
            List<DummyOchLinkConstructor.AseXcEndpoint> endpoints = new ArrayList<>();
            endpoints.addAll(resolveFmuxAseXcEndpoints(aApsGroup.get(), siteLinkAttr));
            endpoints.addAll(resolveFmuxAseXcEndpoints(zApsGroup.get(), siteLinkAttr));
            // OMSP Flex64 injects from the FMUX connected to APS, not from both cascade boards.
            List<FmuxAsePath> paths = Collections.singletonList(new FmuxAsePath(lower, upper,
                    aApsGroup.get().getExp33Tp(), zApsGroup.get().getExp33Tp(), endpoints));
            log.info("resolved Bone2.0 APS FMUX ASE paths on siteLink {}: {}", siteLinkId, paths);
            return paths;
        }

        long width = (upper - lower) / aGroups.size();
        List<FmuxAsePath> paths = new ArrayList<>();
        for (int i = 0; i < aGroups.size(); i++) {
            long pathUpper = upper - width * i;
            long pathLower = i == aGroups.size() - 1 ? lower : pathUpper - width;
            FmuxGroup aGroup = aGroups.get(i);
            FmuxGroup zGroup = zGroups.get(i);
            List<DummyOchLinkConstructor.AseXcEndpoint> endpoints = new ArrayList<>();
            endpoints.addAll(resolveFmuxAseXcEndpoints(aGroup, siteLinkAttr));
            endpoints.addAll(resolveFmuxAseXcEndpoints(zGroup, siteLinkAttr));
            paths.add(new FmuxAsePath(pathLower, pathUpper, aGroup.getExp33Tp(),
                    zGroup.getExp33Tp(), endpoints));
        }
        log.info("resolved Bone2.0 FMUX ASE paths on siteLink {}: {}", siteLinkId, paths);
        return paths;
    }

    private List<FmuxGroup> resolveFmuxGroups(List<AddDropLink> addDropLinks) {
        if (addDropLinks == null) {
            return Collections.emptyList();
        }
        List<FmuxGroup> groups = new ArrayList<>();
        for (AddDropLink link : addDropLinks) {
            String linkRef = link.getLinkRef();
            String aTp = PhysicalLinkIdNamingRule.getTpAId(linkRef);
            String zTp = PhysicalLinkIdNamingRule.getTpZId(linkRef);
            String panelTp = isMuxPanelTp(aTp) ? aTp : isMuxPanelTp(zTp) ? zTp : null;
            String fmuxTp = isMuxPanelTp(aTp) ? zTp : isMuxPanelTp(zTp) ? aTp : null;
            if (panelTp == null || fmuxTp == null) {
                continue;
            }
            String fmuxEq = PhysicalTpIdNamingRule.getEquipId(fmuxTp);
            Optional<String> exp33 = findExp33(fmuxTp);
            if (!exp33.isPresent()) {
                continue;
            }
            FmuxGroup group = groups.stream()
                    .filter(item -> item.getEqId().equals(fmuxEq))
                    .findAny()
                    .orElseGet(() -> {
                        FmuxGroup created = new FmuxGroup(fmuxEq, exp33.get());
                        groups.add(created);
                        return created;
                    });
            group.addPanelMpo(extractMpoIndex(panelTp));
        }
        return groups;
    }

    private Optional<FmuxGroup> findApsFmuxGroup(List<FmuxGroup> groups) {
        return groups.stream().filter(this::hasApsXc).findAny();
    }

    private boolean hasApsXc(FmuxGroup group) {
        Node node = changedObject.getChangedPhyNode(PhysicalTpIdNamingRule.getNodeId(group.getExp33Tp()));
        if (node == null || node.getAugmentation(Node1.class) == null
                || node.getAugmentation(Node1.class).getPhysical() == null
                || node.getAugmentation(Node1.class).getPhysical().getCrossConnections() == null) {
            return false;
        }
        return node.getAugmentation(Node1.class).getPhysical().getCrossConnections().stream()
                .anyMatch(xc -> xc.getAps() != null && xcBelongsToEquipment(xc, group.getEqId()));
    }

    private boolean xcBelongsToEquipment(CrossConnections xc, String eqId) {
        if (xc.getSourceTp() != null && xc.getSourceTp().stream()
                .anyMatch(tp -> eqId.equals(PhysicalTpIdNamingRule.getEquipId(tp.getTpRef().getValue())))) {
            return true;
        }
        return xc.getDestinationTp() != null && xc.getDestinationTp().stream()
                .anyMatch(tp -> eqId.equals(PhysicalTpIdNamingRule.getEquipId(tp.getTpRef().getValue())));
    }

    private List<DummyOchLinkConstructor.AseXcEndpoint> resolveFmuxAseXcEndpoints(
            FmuxGroup group, Site siteLinkAttr) {
        Node node = changedObject.getChangedPhyNode(PhysicalTpIdNamingRule.getNodeId(group.getExp33Tp()));
        Physical physical = node.getAugmentation(Node1.class).getPhysical();
        Equipments fmux = findEquipment(physical, group.getEqId());
        TerminationPoint exp33 = findTp(node, group.getExp33Tp());

        List<TerminationPoint> tilaLineEastTps = isBone2Olp3ProtectionModel(
                PropertyTool.getValue(siteLinkAttr.getProperties(), "model"))
                ? resolveOlp3LocalTilaLineEastTp(node, physical, group.getExp33Tp())
                : resolveRouteTilaLineEastTps(siteLinkAttr, siteIdOfTp(group.getExp33Tp()));
        List<DummyOchLinkConstructor.AseXcEndpoint> endpoints = tilaLineEastTps.stream()
                .map(tp -> new DummyOchLinkConstructor.AseXcEndpoint(exp33, tp, fmux))
                .collect(java.util.stream.Collectors.toList());
        if (endpoints.isEmpty()) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "cannot find Bone2.0 TILA LINE_EAST for ASE source " + group.getExp33Tp());
        }
        // FMUX_32 injects media-channel noise from EXP33 toward TILA LINE_EAST.
        return endpoints;
    }

    private List<TerminationPoint> resolveOlp3LocalTilaLineEastTp(
            Node node, Physical physical, String exp33Tp) {
        List<TerminationPoint> localTilaLineEastTps = node.getTerminationPoint().stream()
                .filter(tp -> isLineEastTp(tp.getTpId().getValue()))
                .filter(tp -> isTilaLineEastTp(physical, tp))
                .collect(java.util.stream.Collectors.toList());
        if (localTilaLineEastTps.size() > 1) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "find multiple local TILA LINE_EAST for OLP3-3 ASE source " + exp33Tp);
        }
        return localTilaLineEastTps;
    }

    private List<TerminationPoint> resolveRouteTilaLineEastTps(Site siteLinkAttr, String siteId) {
        Set<String> routeTps = new LinkedHashSet<>();
        if (siteLinkAttr.getExplictRoute() == null || siteLinkAttr.getExplictRoute().getRoute() == null) {
            return Collections.emptyList();
        }
        for (Route route : siteLinkAttr.getExplictRoute().getRoute()) {
            if (route.getPrimary() != null) {
                collectRouteLineEastTps(route.getPrimary().getExplicitRouteObjects(), siteId, routeTps);
            }
            if (route.getSecondary() != null) {
                collectRouteLineEastTps(route.getSecondary().getExplicitRouteObjects(), siteId, routeTps);
            }
            if (route.getThird() != null) {
                route.getThird().forEach(third ->
                        collectRouteLineEastTps(third.getExplicitRouteObjects(), siteId, routeTps));
            }
        }
        return routeTps.stream()
                .map(this::findTpById)
                .filter(this::isTilaLineEastTp)
                .collect(java.util.stream.Collectors.toList());
    }

    private void collectRouteLineEastTps(
            List<ExplicitRouteObjects> eros, String siteId, Set<String> routeTps) {
        if (eros == null) {
            return;
        }
        for (ExplicitRouteObjects ero : eros) {
            if (ero.getPathRouteObject() == null) {
                continue;
            }
            for (PathRouteObject pathRouteObject : ero.getPathRouteObject()) {
                if (!(pathRouteObject.getResourceType() instanceof Tp)) {
                    continue;
                }
                Tp tp = (Tp) pathRouteObject.getResourceType();
                if (tp.getTpHop() == null || tp.getTpHop().getSiteRef() == null
                        || tp.getTpHop().getTpRef() == null) {
                    continue;
                }
                String tpId = tp.getTpHop().getTpRef().getValue();
                if (siteId.equals(tp.getTpHop().getSiteRef().getValue()) && isLineEastTp(tpId)) {
                    routeTps.add(tpId);
                }
            }
        }
    }

    private Equipments findEquipment(Physical physical, String eqId) {
        return physical.getEquipments().stream()
                .filter(eq -> eq.getEquipmentId().equals(eqId))
                .findAny()
                .orElseThrow(() -> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "cannot find EQ " + eqId));
    }

    private TerminationPoint findTp(Node node, String tpId) {
        return node.getTerminationPoint().stream()
                .filter(tp -> tp.getTpId().getValue().equals(tpId))
                .findAny()
                .orElseThrow(() -> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "cannot find TP " + tpId));
    }

    private TerminationPoint findTpById(String tpId) {
        Node node = changedObject.getChangedPhyNode(PhysicalTpIdNamingRule.getNodeId(tpId));
        return findTp(node, tpId);
    }

    private boolean isMuxPanelTp(String tpId) {
        return tpId != null && tpId.contains("#MUX-") && tpId.contains("-MPO");
    }

    private boolean isTilaEquipment(Equipments eq) {
        return "TILA".equals(eq.getEquipTypeVendorSpecific()) || "TILA".equals(eq.getEquipTypeConfiged());
    }

    private boolean isTilaLineEastTp(TerminationPoint tp) {
        String tpId = tp.getTpId().getValue();
        Node node = changedObject.getChangedPhyNode(PhysicalTpIdNamingRule.getNodeId(tpId));
        Physical physical = node.getAugmentation(Node1.class).getPhysical();
        return isTilaLineEastTp(physical, tp);
    }

    private boolean isTilaLineEastTp(Physical physical, TerminationPoint tp) {
        String eqId = PhysicalTpIdNamingRule.getEquipId(tp.getTpId().getValue());
        return physical.getEquipments().stream()
                .filter(eq -> eqId.equals(eq.getEquipmentId()))
                .findAny()
                .map(this::isTilaEquipment)
                .orElse(false);
    }

    private String siteIdOfTp(String tpId) {
        return PhysicalTpIdNamingRule.getNodeId(tpId).split("#Ne-")[0];
    }

    static boolean isLineEastTp(String tpId) {
        return tpId != null && tpId.endsWith("-LINE_EAST");
    }

    static boolean isBone2Olp3ProtectionModel(String model) {
        return "10".equals(model);
    }

    private int extractMpoIndex(String tpId) {
        return Integer.parseInt(tpId.replaceFirst(".*-MPO", ""));
    }

    private Optional<FmuxAsePath> findFmuxAsePath(long lower, long upper) {
        return fmuxAsePaths.stream()
                .filter(path -> lower >= path.getLower() && upper <= path.getUpper())
                .findAny();
    }

    private static class FmuxGroup {
        private final String eqId;
        private final String exp33Tp;
        private final Set<Integer> panelMpos = new LinkedHashSet<>();

        FmuxGroup(String eqId, String exp33Tp) {
            this.eqId = eqId;
            this.exp33Tp = exp33Tp;
        }

        void addPanelMpo(int index) {
            panelMpos.add(index);
        }

        String getEqId() {
            return eqId;
        }

        String getExp33Tp() {
            return exp33Tp;
        }

        int getMinPanelMpo() {
            return panelMpos.stream().min(Integer::compareTo).orElse(Integer.MAX_VALUE);
        }
    }

    private static class FmuxAsePath {
        private final long lower;
        private final long upper;
        private final String aTp;
        private final String zTp;
        private final List<DummyOchLinkConstructor.AseXcEndpoint> aseXcEndpoints;

        FmuxAsePath(long lower, long upper, String aTp, String zTp,
                List<DummyOchLinkConstructor.AseXcEndpoint> aseXcEndpoints) {
            this.lower = lower;
            this.upper = upper;
            this.aTp = aTp;
            this.zTp = zTp;
            this.aseXcEndpoints = aseXcEndpoints;
        }

        long getLower() {
            return lower;
        }

        long getUpper() {
            return upper;
        }

        String getATp() {
            return aTp;
        }

        String getZTp() {
            return zTp;
        }

        List<DummyOchLinkConstructor.AseXcEndpoint> getAseXcEndpoints() {
            return aseXcEndpoints;
        }

        @Override
        public String toString() {
            return "FmuxAsePath{" + lower + "," + upper + "," + aTp + "," + zTp
                    + ",xc=" + aseXcEndpoints.size() + "}";
        }
    }


    private void distinguishBandC_L(List<String> xcIdList) {
        xcIdList.forEach(xcId -> {
            if (xcId.contains("/C"))  {
                amplifierXC_C.add(xcId);
            } else {
                amplifierXC_L.add(xcId);
            }
        });
    }


    private boolean inBandC(long lower, long upper) {
        ImplConfig implConfig = SpringBeanFinder.getBean(ImplConfig.class);
        return FrequencyAvailable.inBandC(implConfig.getYangModel(), lower, upper);
    }

    private List<String> amplifierXcList(long lower, long upper) {
        return inBandC(lower, upper) ? amplifierXC_C : amplifierXC_L;
    }
}
