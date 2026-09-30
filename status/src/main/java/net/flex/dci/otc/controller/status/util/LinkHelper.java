package net.flex.dci.otc.controller.status.util;

import static net.flex.dci.otc.controller.status.util.Constants.SITE_LINK_MODEL_KEY;
import static net.flex.dci.otc.controller.status.util.Constants.SITE_LINK_PROTECT_MODELS;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.dto.ProtectedLinkDto;
import net.flex.dci.otc.controller.status.dto.ProtectionMode;
import net.flex.dci.otc.controller.status.enums.ProtectionActivePathRole;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.SiteLinkCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.KvDefine;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yangtools.yang.binding.DataContainer;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/4/8 10:56
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LinkHelper {

    private final CrossConnectionsDao crossConnectionsDao;

    private final DciTopologyCacheManager dciTopologyCacheManager;

    public Boolean isSiteLinkInProtectedMode(Link link) {
        log.debug("is current site link in protected mode or not,linkId:{}",
                link.getLinkId().getValue());
        Site site = link.getAugmentation(Link1.class).getSite();
        Class<? extends ProtectionType> protectionType = site.getProtectionType();
        return protectionType != null && (protectionType.isAssignableFrom(ProtectionBidir1To1.class)
                || protectionType.isAssignableFrom(ProtectionBidir1To2.class));
//        Properties properties = link.getAugmentation(Link1.class).getSite().getProperties();
//        boolean isProtectedMode = false;
//        if (properties != null) {
//            isProtectedMode = detectedIsProtectedMode(properties.getProperty());
//        }
//        return isProtectedMode;
    }

    /**
     * detected is protected mode
     *
     * @param properties
     * @return
     */
    private boolean detectedIsProtectedMode(List<Property> properties) {
        Map<String, String> propertiesMap = properties.stream()
                .collect(Collectors.toMap(property -> property.getKey().getName(),
                        KvDefine::getValue));
        String siteLinkModel = propertiesMap.get(SITE_LINK_MODEL_KEY);
        return SITE_LINK_PROTECT_MODELS.contains(siteLinkModel);

    }

    public ProtectedLinkDto getProtectedSiteLinkDetailInfo(Link link) {
        String linkId = link.getLinkId().getValue();
        log.debug("detected the protected site link work detail info ,link id is:{}", linkId);

//        Site site = link.getAugmentation(Link1.class).getSite();
//        Route route = site.getExplictRoute().getRoute().get(0);
//        List<CrossConnections> xcs = route.getPrimary().getCrossConnections();
        Site site = link.getAugmentation(Link1.class).getSite();
        Class<? extends ProtectionType> protectionType = site.getProtectionType();
        Route route = site.getExplictRoute().getRoute().get(0);
        List<String> apsXcIds = getLinkRefApsCrossConnection(route);
        List<CrossConnections> realApsCrossConnections = getRealApsCrossConnections(apsXcIds);
        List<String> primaryRouteRefLinkIds = new ArrayList<>();
        List<String> secondaryRouteRefLinkIds = new ArrayList<>();
        List<String> tertiaryLinkIds = new ArrayList<>();
        //assemble the route have only have one

        if (protectionType.isAssignableFrom(ProtectionBidir1To1.class)) {
            primaryRouteRefLinkIds = getRouteRefLinks(route.getPrimary().getExplicitRouteObjects());
            secondaryRouteRefLinkIds = getRouteRefLinks(
                    route.getSecondary().getExplicitRouteObjects());
        } else if (protectionType.isAssignableFrom(ProtectionBidir1To2.class)) {
            primaryRouteRefLinkIds = getRouteRefLinks(route.getPrimary().getExplicitRouteObjects());
            secondaryRouteRefLinkIds = getRouteRefLinks(
                    route.getSecondary().getExplicitRouteObjects());
            //current third only have olp
            List<Third> thirds = route.getThird();
            tertiaryLinkIds = CollectionUtils.isEmpty(thirds) ? new ArrayList<>()
                    : getRouteRefLinks(thirds.get(0).getExplicitRouteObjects());
        }

//        List<String> primaryRouteRefLinkIds = getRouteRefLinks(
//                route.getPrimary().getExplicitRouteObjects());
//        List<String> secondaryRouteRefLinkIds = getRouteRefLinks(
//                route.getSecondary().getExplicitRouteObjects());

        ProtectionMode protectionMode = detectedProtectionMode(realApsCrossConnections);
        return ProtectedLinkDto.builder().primaryLinkIds(primaryRouteRefLinkIds)
                .activePathRoles(protectionMode.getActivePathRoles())
                .secondaryLinkIds(secondaryRouteRefLinkIds)
                .tertiaryLinkIds(tertiaryLinkIds).build();
//        List<CrossConnections> realApsCrossConnections = apsXcIds.stream().map(id -> {
////                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections realXc = phyNodeDao.getRefXcByNodeIdAndXcPref(
////                            refNodeId, xcId
//                    List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> realXcs = crossConnectionsDao.getXCByXcIdRegexLike(
//                            id);
//                    return realXcs == null ? null : realXcs.isEmpty() ? null : realXcs.get(0);
//                }).filter(Objects::nonNull)
//                .map(xc -> new CrossConnectionsBuilder(xc).build()).collect(
//                        Collectors.toList());
//        if (xcs != null) {
//            for (CrossConnections xc : xcs) {
//                String tpRef = xc.getSourceTp().get(0).getTpRef().getValue();
//                String ids[] = tpRef.split("#");
//                NodeId nodeId = new NodeId(ids[0] + "#" + ids[1]);
//                Aps aps = xc.getAps();
//                if (aps != null) {
//                    if (xc.getDestinationTp() != null) {
//                        for (DestinationTp tp : xc.getDestinationTp()) {
//                            switchPorts.add(tp.getTpRef().getValue());
//                        }
//                    }
//                    if (aps.getActivePath() == ActivePath.PRIMARY) {
//                        selectedPorts.add(xc.getDestinationTp().get(0).getTpRef().getValue());
//                        isWorkOnPrimary = true;
//                    } else if (aps.getActivePath() == ActivePath.SECONDARY) {
//                        selectedPorts.add(xc.getDestinationTp().get(1).getTpRef().getValue());
//                        isWorkOnSecondary = true;
//                    }
//                }
//            }
//        List<CrossConnections> apsCrossConnections = xcs.stream()
//                .filter(crossConnections -> crossConnections.getAps() != null).collect(
//                        Collectors.toList());
//        List<CrossConnections> realApsCrossConnections = apsCrossConnections.stream()
//                .map(crossConnections -> {
////                    String refNodeId = crossConnections.getNodeRef().getValue();
//                    String xcId = crossConnections.getCrossConnectionId().getValue();
////                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections realXc = phyNodeDao.getRefXcByNodeIdAndXcPref(
////                            refNodeId, xcId
//                    List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> realXcs = crossConnectionsDao.getXCByXcIdRegexLike(
//                            xcId);
//                    return realXcs == null ? null : realXcs.isEmpty() ? null : realXcs.get(0);
//                }).filter(Objects::nonNull)
//                .map(xc -> new CrossConnectionsBuilder(xc).build()).collect(
//                        Collectors.toList());
//        ProtectedLinkDto
//                protectedLinkDto = extractApsProtectedInfo(realApsCrossConnections);
////        }
//        ProtectedSiteLinkWorkModel workModel = ProtectedSiteLinkWorkModel.UNKNOWN;
//        if (protectedLinkDto.isPrimary() && protectedLinkDto.isSecondary()) {
//            workModel = ProtectedSiteLinkWorkModel.MIX_TYPE;
//        } else if (protectedLinkDto.isPrimary() && !protectedLinkDto.isSecondary()) {
//            workModel = ProtectedSiteLinkWorkModel.PRIMARY;
//        } else if (!protectedLinkDto.isPrimary() && protectedLinkDto.isSecondary()) {
//            workModel = ProtectedSiteLinkWorkModel.SECONDARY;
//        }
//        return ProtectedSiteLinkDto.builder().siteLinkId(linkId)
//                .selectedPortId(protectedLinkDto.getSelectedPorts())
//                .switchPortId(protectedLinkDto.getSwitchPorts()).workModel(workModel).build();
    }

    /**
     * detected OCHP Och link work mode like mix_type primary secondary
     *
     * @param ochLink
     * @return
     */
    public ProtectedLinkDto getOCHPOchWorkModeDetailInfo(Link ochLink) {
        String ochLinkId = ochLink.getLinkId().getValue();
        log.debug("start to detected the ochp och work mode detail info,the och link id is:{}",
                ochLinkId);

        Och ochPhysical = ochLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                .getOch();
        Class<? extends ProtectionType> protectionType = ochPhysical.getProtectionType();

        Route route = ochPhysical.getExplictRoute()
                .getRoute().get(0);
        List<String> routeApsXcIds = getLinkRefApsCrossConnection(route);
        List<CrossConnections> realApsCrossConnections = getRealApsCrossConnections(routeApsXcIds);
        List<String> primaryRouteRefLinkIds = new ArrayList<>();
        List<String> secondaryRouteRefLinkIds = new ArrayList<>();
        List<String> tertiaryLinkIds = new ArrayList<>();

        if (protectionType.isAssignableFrom(ProtectionBidir1To1.class)) {
            primaryRouteRefLinkIds = getRouteRefLinks(route.getPrimary().getExplicitRouteObjects());
            secondaryRouteRefLinkIds = getRouteRefLinks(
                    route.getSecondary().getExplicitRouteObjects());
        } else if (protectionType.isAssignableFrom(ProtectionBidir1To2.class)) {
            primaryRouteRefLinkIds = getRouteRefLinks(route.getPrimary().getExplicitRouteObjects());
            secondaryRouteRefLinkIds = getRouteRefLinks(
                    route.getSecondary().getExplicitRouteObjects());
            //current third only have olp
            List<Third> thirds = route.getThird();
            tertiaryLinkIds = CollectionUtils.isEmpty(thirds) ? new ArrayList<>()
                    : getRouteRefLinks(thirds.get(0).getExplicitRouteObjects());
        }
        ProtectionMode protectionMode = detectedProtectionMode(realApsCrossConnections);

        return ProtectedLinkDto.builder().primaryLinkIds(primaryRouteRefLinkIds)
                .activePathRoles(protectionMode.getActivePathRoles())
                .secondaryLinkIds(secondaryRouteRefLinkIds)
                .tertiaryLinkIds(tertiaryLinkIds).build();
    }

    /**
     * detected Protection Mode
     *
     * @param realApsCrossConnections
     * @return
     */
    private ProtectionMode detectedProtectionMode(List<CrossConnections> realApsCrossConnections) {

        Set<ProtectionActivePathRole> activePathRoles = new HashSet<>();
        realApsCrossConnections.forEach(xc -> {
            ApsPath activePath = xc.getAps().getActivePath();
            if (activePath == ApsPath.PRIMARY) {
                activePathRoles.add(ProtectionActivePathRole.Primary);
            }
            if (activePath == ApsPath.SECONDARY) {
                activePathRoles.add(ProtectionActivePathRole.Secondary);
            }
            if (activePath == ApsPath.THIRD) {
                activePathRoles.add(ProtectionActivePathRole.Tertiary);
            }

        });
        return ProtectionMode.builder().activePathRoles(activePathRoles)
                .build();
    }

    /**
     * get route ref relative route
     *
     * @param routeObjects
     * @return
     */
    private List<String> getRouteRefLinks(List<ExplicitRouteObjects> routeObjects) {
        log.trace("get all route ref links ,route object is:{}", routeObjects);
        ExplicitRouteObjects explicitRouteObjects = routeObjects.get(0);
        List<PathRouteObject> pathRouteObject = explicitRouteObjects.getPathRouteObject();
        return pathRouteObject.stream().filter(pro -> {
            Class<? extends DataContainer> clazz = pro.getResourceType()
                    .getImplementedInterface();
            return clazz.isAssignableFrom(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link.class);
        }).map(linkPro -> {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link
                    linkHop = (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) linkPro.getResourceType();
            return linkHop.getLinkHop().getLinkRef().getValue();
        }).collect(Collectors.toList());
    }

    private List<CrossConnections> getRealApsCrossConnections(List<String> apsXcIds) {
        log.trace("get the real aps cross connections,the aps xcIds is:{}", apsXcIds);
        List<CrossConnections> realApsCrossConnections = apsXcIds.stream().map(id -> {
//                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections realXc = phyNodeDao.getRefXcByNodeIdAndXcPref(
//                            refNodeId, xcId
                    List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> realXcs = crossConnectionsDao.getXCByXcIdRegexLike(
                            id);
                    return realXcs == null ? null : realXcs.isEmpty() ? null : realXcs.get(0);
                }).filter(Objects::nonNull)
                .map(xc -> new CrossConnectionsBuilder(xc).build()).collect(
                        Collectors.toList());
        return realApsCrossConnections;
    }

    private List<String> getLinkRefApsCrossConnection(Route route) {
        log.debug("get och link ref aps cross connection");
        Stream<CrossConnections> primaryStream = route.getPrimary().getCrossConnections().stream();
        Stream<CrossConnections> secondaryStream = (route.getSecondary() != null)
                ? route.getSecondary().getCrossConnections().stream()
                : Stream.empty();
        Stream<CrossConnections> thirdStream = (route.getThird() != null)
                ? route.getThird().stream()
                .flatMap(thirdRoute -> thirdRoute.getCrossConnections().stream())
                : Stream.empty();
        List<CrossConnections> crossConnections = Stream.of(primaryStream, secondaryStream,
                        thirdStream)
                .flatMap(s -> s).collect(Collectors.toList());
        List<String> refApsCrossConnections = crossConnections.stream()
                .filter(xc -> xc.getAps() != null).map(xc -> xc.getCrossConnectionId().getValue())
                .collect(
                        Collectors.toList());
        return refApsCrossConnections;
    }

    private List<String> getSiteLinkRefApsCrossConnections(String linkId) {

        SiteLinkCache siteLinkCache = dciTopologyCacheManager.getValue(linkId);
        List<String> apsIds = new ArrayList<>();
        apsIds.add(siteLinkCache.getDestinationApsXCId());
        apsIds.add(siteLinkCache.getSourceApsXCId());
        return apsIds;
    }

//    private ProtectedLinkDto extractApsProtectedInfo(
//            List<CrossConnections> realApsCrossConnections) {
//        log.debug("extract aps protected info");
//        boolean isPrimary = false;
//        boolean isSecondary = false;
//        List<String> switchPort = new ArrayList<>();
//        List<String> selectedPort = new ArrayList<>();
//        for (CrossConnections xc : realApsCrossConnections) {
//            Aps aps = xc.getAps();
//            if (aps != null) {
//                if (xc.getDestinationTp() != null) {
//                    switchPort.addAll(xc.getDestinationTp().stream()
//                            .map(destinationTp -> destinationTp.getTpRef().getValue()).collect(
//                                    Collectors.toList()));
//                }
//                if (aps.getActivePath() == ActivePath.PRIMARY) {
//                    selectedPort.add(xc.getDestinationTp().get(0).getTpRef().getValue());
//                    isPrimary = true;
//                } else if (aps.getActivePath() == ActivePath.SECONDARY) {
//                    selectedPort.add(xc.getDestinationTp().get(1).getTpRef().getValue());
//                    isSecondary = true;
//                }
//            }
//        }
//        return ProtectedLinkDto.builder().isPrimary(isPrimary).isSecondary(isSecondary)
//                .switchPorts(switchPort).selectedPorts(selectedPort).build();
//    }


    public List<PathRouteObject> sortPath(List<PathRouteObject> pathRouteObjects) {
        pathRouteObjects.sort(new Comparator<PathRouteObject>() {
            @Override
            public int compare(PathRouteObject p1, PathRouteObject p2) {
                if (p1.getIndex().longValue() == p2.getIndex().longValue()) {
                    return 0;
                } else if (p1.getIndex() < p2.getIndex()) {
                    return -1;
                } else if (p1.getIndex() > p2.getIndex()) {
                    return 1;
                }
                return 0;
            }
        });
        return pathRouteObjects;
    }

    /**
     * judge the och link is in protected mode or not
     *
     * @param ochLink
     * @return
     */
    public Boolean isOchLinkInProtectedMode(Link ochLink) {
        log.debug("is the och link in protected mode or not ?");
        Och refOchAttribute = ochLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                .getOch();
        Class<? extends ProtectionType> protectionType = refOchAttribute.getProtectionType();
        return protectionType != null && (protectionType.isAssignableFrom(ProtectionBidir1To1.class)
                || protectionType.isAssignableFrom(ProtectionBidir1To2.class));
    }
}
