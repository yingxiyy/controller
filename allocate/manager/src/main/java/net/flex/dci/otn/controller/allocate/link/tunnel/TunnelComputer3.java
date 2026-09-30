///*
// * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
// *
// * This program and the accompanying materials are made available under the
// * terms of the Eclipse Public License v1.0 which accompanies this distribution,
// * and is available at http://www.eclipse.org/legal/epl-v10.html
// */
//
//package net.flex.dci.otn.controller.allocate.link.tunnel;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map.Entry;
//import java.util.Optional;
//import java.util.Set;
//import java.util.stream.Collectors;
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otc.common.exception.CommonException;
//import net.flex.dci.otc.common.exception.CommonExceptionType;
//import net.flex.dci.otc.common.util.TopoNameConstants;
//import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
//import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
//import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
//import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
//import net.flex.dci.otc.mongo.dao.OchLinkDao;
//import net.flex.dci.otc.mongo.dao.PhyLinkDao;
//import net.flex.dci.otc.mongo.dao.SiteLinkDao;
//import net.flex.dci.otc.mongo.dao.TopologyDao;
//import net.flex.dci.otn.controller.allocate.Dijkstra.DJNode;
//import net.flex.dci.otn.controller.allocate.Dijkstra.Dijkstra;
//import net.flex.dci.otn.controller.allocate.Dijkstra.Graph;
//import net.flex.dci.otn.controller.allocate.common.util.Constant;
//import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
//import net.flex.dci.otn.controller.allocate.designer.tunnel.TunnelUtils;
//import net.flex.dci.otn.controller.allocate.dfs.DFSGraph;
//import net.flex.dci.otn.controller.allocate.dfs.DFSNode;
//import net.flex.dci.otn.controller.allocate.dfs.DFSPath;
//import org.apache.commons.lang3.builder.HashCodeBuilder;
//import org.apache.commons.lang3.tuple.Pair;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.Available;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnels2Input;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnels2Output;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnels2OutputBuilder;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.reuesed.och.ReuesedOch;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.reuesed.och.ReuesedOchBuilder;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.reuesed.och.reuesed.och.Links;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.reuesed.och.reuesed.och.LinksBuilder;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.compute.result._2.ReusedRoute;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.compute.result._2.ReusedRouteBuilder;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.compute.result._2.ShortPath;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.compute.result._2.ShortPathBuilder;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.creation.attributes.VendorOccupationRate;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRoute;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.bundle.info._2.SiteLinkRouteBuilder;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
///**
// * For DFS
// */
//@Slf4j
//@Service
//public class TunnelComputer3 {
//
//    public static final String SITE_LINK_PREFIX = "SiteLink";
//    public static final String WSS_LINK_PREFIX = "WssLink";
//    @Value("${computed.route.limit:5}")
//    private Integer COMPUTED_ROUTE_LIMIT;
//    @Autowired
//    private TopologyDao topologyDao;
//    @Autowired
//    private SiteLinkDao siteLinkDao;
//    @Autowired
//    private OchLinkDao ochLinkDao;
//    @Autowired
//    private PhyLinkDao phyLinkDao;
//    @Autowired
//    private TunnelUtils tunnelUtils;
//
//    public ComputeTunnels2Output doIt(ComputeTunnels2Input input) throws CommonException {
//
////        validate(input);
//        ParamCompute3 param = new ParamCompute3();
//        param.parser(input);
//
//        Pair<List<ReuesedOch>, List<ReusedRoute>> reusedOchPair = getReUsedOchScenario(param);
//        List<ReusedRoute> reusedRoutes = reusedOchPair.getRight();
//
//        List<ShortPath> shortPath = getShortPath(param);
//        shortPath = removeDuplicateWithReuseRoute(shortPath, reusedRoutes);
//
//        ComputeTunnels2Output output = new ComputeTunnels2OutputBuilder()
//                .setReturnCode(RpcResultType.Success)
//                .setReuesedOch(reusedOchPair.getLeft())
//                .setReusedRoute(reusedRoutes)
//                .setShortPath(shortPath).build();
//        return output;
//    }
//
//    private List<ShortPath> removeDuplicateWithReuseRoute(List<ShortPath> shortPaths, List<ReusedRoute> reusedRoutes) {
//        if (reusedRoutes == null || reusedRoutes.isEmpty()) {
//            return shortPaths;
//        }
//
//        List<ShortPath> result = new ArrayList<>();
//        for (ShortPath shortPath : shortPaths) {
//            SiteLinkRoute shortPathSiteLinkRoute = shortPath.getSiteLinkRoute();
//            for (ReusedRoute reusedRoute : reusedRoutes) {
//                SiteLinkRoute reusedSiteLinkRoute = reusedRoute.getSiteLinkRoute();
//                if (!shortPathSiteLinkRoute.getVendorName().equals(reusedSiteLinkRoute.getVendorName())) {
//                    result.add(shortPath);
//                    continue;
//                }
//                if (!shortPathSiteLinkRoute.getCentralFrequencies().equals(reusedSiteLinkRoute.getCentralFrequencies())) {
//                    result.add(shortPath);
//                    continue;
//                }
//                if (!shortPathSiteLinkRoute.getPrimary().equals(reusedSiteLinkRoute.getPrimary())) {
//                    result.add(shortPath);
//                    continue;
//                }
//                if (shortPathSiteLinkRoute.getSecondary() != null) {
//                    if (reusedSiteLinkRoute.getSecondary() == null) {
//                        result.add(shortPath);
//                        continue;
//                    } else {
//                        if (!reusedSiteLinkRoute.getSecondary().equals(shortPathSiteLinkRoute.getSecondary())) {
//                            result.add(shortPath);
//                            continue;
//                        }
//                    }
//                } else if (reusedSiteLinkRoute.getSecondary() != null) {
//                    result.add(shortPath);
//                    continue;
//                }
//
//            }
//        }
//        return result;
//    }
//
//    private Pair<List<ReuesedOch>, List<ReusedRoute>> getReUsedOchScenario(ParamCompute3 param) {
//
//        //为了得到reused och route，所以这里是不过滤vendor，cardtype，L口速率之类的，那个留着getMatchedOchReusedOdu去做
//        List<Link> matchedOchLinks = ochLinkDao.filter(param.getSrcSite(), param.getDesSite(), param.getMandatorySiteLinkId(), param.getIsProtected(), param.getRiskGroupName(), param.getPlaneName());
//
//        //get reused och route
//        List<ReusedRoute> reusedRoutes = getReUsedOchRoute(param, matchedOchLinks);
//
//        //get reused och link
//        List<ReuesedOch> reUsedOchs = getReUsedOch(param, matchedOchLinks);
//
//        return Pair.of(reUsedOchs, reusedRoutes);
//    }
//
//    private List<ReuesedOch> getReUsedOch(ParamCompute3 param, List<Link> matchedOchLinks) {
//        List<ReuesedOch> reUsedOchs = new ArrayList<>();
//        for (VendorOccupationRate occupationRate : param.getVendorOccupationRateList()) {
//            String vendorName = occupationRate.getVendorName();
//            String productType = occupationRate.getProductType();
//
//            List<Links> linksList = new ArrayList<>();
//            for (Link ochLink : matchedOchLinks) {
//                int bundleNumber = getMatchedOchReusedOdu(ochLink, param, vendorName, productType);
//
//                if (bundleNumber > 0) {
//                    //add reusedOch
//                    Links links = new LinksBuilder().setLinkId(ochLink.getLinkId().getValue()).setBundleNumber(bundleNumber).build();
//                    linksList.add(links);
//                }
//            }
//            ReuesedOch ReuesedOch = new ReuesedOchBuilder()
//                    .setLinks(linksList)
//                    .setVendorName(vendorName)
//                    .setProductType(productType)
//                    .build();
//            reUsedOchs.add(ReuesedOch);
//        }
//        return reUsedOchs;
//    }
//
//    private List<ReusedRoute> getReUsedOchRoute(ParamCompute3 param, List<Link> matchedOchLinks) {
//        Set<ReusedRouteTemp> reUsedOchRouteTemps = new HashSet<>();
//        for (Link ochLink : matchedOchLinks) {
//            try {
//                //generate reusedOchRoute
//                Och ochLinkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();
//                Route ochRoute = ochLinkAttr.getExplictRoute().getRoute().get(0);
//                ReusedRouteTemp reusedRouteTmp = buildReusedRouteTemp(ochRoute, ochLinkAttr.getVendorName(), ochLinkAttr.getProductType());
//                if (ochLink.getDestination().getDestNode().getValue().contains(param.getSrcSite().getNodeId().getValue())) {
//                    reusedRouteTmp.reverse();
//                }
//                reUsedOchRouteTemps.add(reusedRouteTmp);
//            } catch (Exception e) {
//                log.error("Failed to get reused och route by och link:{}", ochLink.getLinkId().getValue(), e);
//                continue;
//            }
//        }
//
//        List<ReusedRoute> reusedRoutes = new ArrayList<>();
//        Iterator<ReusedRouteTemp> iterator = reUsedOchRouteTemps.iterator();
//        while (iterator.hasNext()) {
//            ReusedRouteTemp reusedRouteTemp = iterator.next();
//            ReusedRoute reusedRoute = null;
//            try {
//                reusedRoute = buildReusedRoute(reusedRouteTemp, param.getClientLineRate());
//            } catch (NeDesignerException e) {
//                log.error("Failed to build reused route by:{}", reusedRouteTemp, e);
//                continue;
//            }
//            if (reusedRoute != null) {
//                reusedRoutes.add(reusedRoute);
//            }
//        }
//        return reusedRoutes;
//    }
//
//    private boolean passMandatorySite(String mandatorySiteId, ArrayList<String> primaryRoute) {
//        if (mandatorySiteId == null) {
//            return true;
//        }
//        for (String linkId : primaryRoute) {
//            //siteLink
//            if (linkId.startsWith(SITE_LINK_PREFIX)) {
//                boolean passSite = siteLinkDao.passSite(linkId, mandatorySiteId);
//                if (passSite) {
//                    return true;
//                }
//                continue;
//            }
//            //wssLink
//            if (linkId.contains(mandatorySiteId)) {
//                return true;
//            }
//
//        }
//        return false;
//    }
//
//
//    private ReusedRoute buildReusedRoute(ReusedRouteTemp reusedRouteTemp, int clientRateNumber) throws NeDesignerException {
//        List<Long> freqs = getJointFrequency(reusedRouteTemp.primary, reusedRouteTemp.fixGrid);
//        if (freqs == null || freqs.isEmpty()) {
//            return null;//no reused route, because no joint frequency
//        }
//        if (reusedRouteTemp.secondary != null && !reusedRouteTemp.secondary.isEmpty()) {
//            List<Long> secondFreq = getJointFrequency(reusedRouteTemp.secondary, reusedRouteTemp.fixGrid);
//            freqs.retainAll(secondFreq);
//            if (freqs.isEmpty()) {
//                return null;
//            }
//        }
//        int bundleNum = freqs.size() * clientRateNumber;
//        SiteLinkRoute tunnelRoute = new SiteLinkRouteBuilder()
//                .setBundleNumber(bundleNum)
//                .setCentralFrequencies(freqs)
//                .setFixGrid(reusedRouteTemp.fixGrid)
//                .setPrimary(reusedRouteTemp.primary)
//                .setSecondary(reusedRouteTemp.secondary)
//                .setVendorName(reusedRouteTemp.vendorName)
//                .setProductType(reusedRouteTemp.productType)
//                .build();
//
//        return new ReusedRouteBuilder()
//                .setSiteLinkRoute(tunnelRoute)
//                .build();
//
//    }
//
//    private List<Long> getJointFrequency(ArrayList<String> LinkList, String fixGrid) throws NeDesignerException {
//
//        GridType fixGridType = tunnelUtils.getGridByString(fixGrid);
//
//        List<Long> commonFreqs = null;
//        for (String linkId : LinkList) {
//            if (linkId.startsWith(WSS_LINK_PREFIX)) {
//                continue;
//            }
//            Link siteLink = siteLinkDao.getSiteLinkById(linkId);
//            List<Long> centralFrequencies = new FrequencyAvailable(siteLink).getAllPossibleCentFrequency(fixGridType);
//            if (commonFreqs == null) {
//                commonFreqs = new ArrayList<>(centralFrequencies);
//            } else {
//                commonFreqs.retainAll(centralFrequencies);
//            }
//            if (commonFreqs.isEmpty()) {
//                return null;
//            }
//        }
//        return commonFreqs;
//    }
//
//    private ReusedRouteTemp buildReusedRouteTemp(Route ochRoute, String vendorName, String productType) throws NeDesignerException {
//        List<ExplicitRouteObjects> primaryExplicitRouteObjects = ochRoute.getPrimary().getExplicitRouteObjects();
//        ArrayList<String> ochPrimary = getSiteLinkRoute(primaryExplicitRouteObjects);//include siteLinkId and wssLinkId
//        GridType fixGrid = getFixGridForRoute(ochPrimary);
//        ArrayList<String> ochSecondary = null;
//        if (ochRoute.getSecondary() != null && ochRoute.getSecondary().getExplicitRouteObjects() != null) {
//            ochSecondary = getSiteLinkRoute(ochRoute.getSecondary().getExplicitRouteObjects());
//        }
//
//        return new ReusedRouteTemp(ochPrimary, ochSecondary, vendorName, productType, tunnelUtils.getGridString(fixGrid));
//    }
//
//    /**
//     * 同一条route里面，有一下几种组合： 1， 全是flex， 则fixGrid返回grid75 2. flex+mux96（grid50），则fixGrid返回grid50 3. flex+mux64（grid75），则fixGrid返回grid75 其他的就是非法的。
//     *
//     * @param route
//     * @return
//     */
//    private GridType getFixGridForRoute(ArrayList<String> route) throws NeDesignerException {
//        ArrayList<String> siteLinkIds = getSiteLinkListFromRoute(route);
//        Set<GridType> gridSet = getGridTypesForSiteLinks(siteLinkIds);
//
//        if (gridSet.size() > 2) {
//            log.error("Invalid route, because this route have more grids as: {}, route is :{}", gridSet, route);
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Invalid route, because has more than 2 different Grid.");
//        }
//
//        Iterator<GridType> gridIterator = gridSet.iterator();
//        GridType grid = gridIterator.next();
//
//        if (gridSet.size() == 1) {
//            if (grid.equals(GridType._0)) {
//                return GridType._75;
//            }
//            return grid;
//        }
//
//        if (!grid.equals(GridType._0)) {
//            return grid;
//        }
//        return gridIterator.next();
//    }
//
//    private Set<GridType> getGridTypesForSiteLinks(ArrayList<String> siteLinkIds) throws NeDesignerException {
//        Set<String> gridSet = siteLinkDao.getGrids(siteLinkIds);
//        if (gridSet == null || gridSet.isEmpty()) {
//            log.error("Got empty grid from siteLinks:{}", siteLinkIds);
//            throw new NeDesignerException("Got empty grid from siteLinks");
//        }
//        Set<GridType> result = new HashSet<>();
//        for (String gridString : gridSet) {
//            try {
//                result.add(tunnelUtils.getGridByString(gridString));
//            } catch (Exception e) {
//                log.error("Failed to get grid type for grid string :{} from siteLinks:{}", gridString, siteLinkIds, e);
//                throw new NeDesignerException("Failed to get grid type from gridString:" + gridString);
//            }
//
//        }
//
//        return result;
//    }
//
//    /**
//     * route include sitelink id and wsslink id as this way: s,w,s,w,s
//     *
//     * @param route
//     * @return
//     */
//    private ArrayList<String> getSiteLinkListFromRoute(ArrayList<String> route) {
//        ArrayList<String> result = new ArrayList<>();
//        int size = route.size();
//        for (int i = 0; i < size; i += 2) {
//            result.add(route.get(i));
//        }
//        return result;
//    }
//
//    private ArrayList<String> getSiteLinkRoute(List<ExplicitRouteObjects> primaryExplicitRouteObjects) {
//        ArrayList<String> ochPrimary = primaryExplicitRouteObjects.get(0).getPathRouteObject().stream()
//                .filter(item -> isSiteLink(item) || isWssLink(item))
//                .map(siteLink -> ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) siteLink.getResourceType()).getLinkHop()
//                        .getLinkRef().getValue()).collect(
//                        Collectors.toCollection(ArrayList::new));
//        return ochPrimary;
//    }
//
//    private boolean isSiteLink(PathRouteObject item) {
//        return item.getTopologyRef().getValue().equals(TopoNameConstants.Site_Topo_Key) &&
//                item.getResourceType().getImplementedInterface().getName()
//                        .equals(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link.class.getName());
//    }
//
//    private boolean isWssLink(PathRouteObject item) {
//        return item.getTopologyRef().getValue().equals(TopoNameConstants.Phy_Topo_Key) &&
//                item.getResourceType().getImplementedInterface().getName()
//                        .equals(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link.class.getName())
//                && ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) item.getResourceType()).getLinkHop()
//                .getLinkRef().getValue().startsWith(WSS_LINK_PREFIX);
//    }
//
//
//    private List<ShortPath> getShortPath(ParamCompute3 param) {
//        String riskGroupName = param.getRiskGroupName();
//        String planeName = param.getPlaneName();
//        String srcSiteId = param.getSrcSite().getNodeId().getValue();
//        String destSiteId = param.getDesSite().getNodeId().getValue();
//        List<String> priMandatorySiteLinkId = param.getPrimaryMandatorySiteLinkIds();
//        List<String> secMandatorySiteLinkId = param.getSecondaryMandatorySiteLinkIds();
//        List<String> matchedGrids = param.getMatchedGrids();
//
//        List<ShortPath> output = new ArrayList<>();
//
//        List<GridSiteLinkTemp> gridSiteLinksData = getGridSiteLinksFromDB(riskGroupName, planeName, matchedGrids, srcSiteId, destSiteId);
//        for (GridSiteLinkTemp gridSiteLinkTemp : gridSiteLinksData) {
////            DFSGraph dfsGraph = buildGraph(gridSiteLinkTemp, srcSiteId, destSiteId);
//            List<DFSPath> allPath = getAllPaths(gridSiteLinkTemp, srcSiteId, destSiteId);
//            if (!param.getIsProtected()) {
//                List<ShortPath> shortPathNonProtected;
//                if (mandatorySiteLinkId == null) {
//                    shortPathNonProtected = getShortPathNonProtected(fixGrid, srcSiteId, destSiteId, srcSiteLinks, destSiteLinks, siteLinks, param.getClientLineRate());
//                } else {
//                    shortPathNonProtected = getShortPathsNonProtectd_WithMandatory(param, srcSiteId, destSiteId, siteLinks, fixGrid, srcSiteLinks, destSiteLinks, mandatorySiteLinkId);
//                    if (shortPathNonProtected == null || shortPathNonProtected.isEmpty()) {
//                        continue;
//                    }
//                }
//
//                output.addAll(shortPathNonProtected);
//            } else {
//                List<ShortPath> shortPathPrimary;
//                boolean hasMandatorySiteLinkId = mandatorySiteLinkId != null ? true : false;
//                if (!hasMandatorySiteLinkId) {
//                    List<ShortPath> shortPathNonProtected = getShortPathNonProtected(fixGrid, srcSiteId, destSiteId, srcSiteLinks, destSiteLinks, siteLinks, param.getClientLineRate());
//                    if (shortPathNonProtected == null || shortPathNonProtected.isEmpty()) {
//                        continue;
//                    }
//                    //在所有最短路径中，配对primary+secondary,如果有满足的，就不重新calculate路径了
//                    List<ShortPath> shortPaths = getShortPathProtected(shortPathNonProtected, param.getClientLineRate(), param.getSrcSite().getNodeId().getValue(),
//                            param.getDesSite().getNodeId().getValue());
//                    if (!shortPaths.isEmpty()) {
//                        output.addAll(shortPaths);
//                        continue;
//                    }
//
//                    shortPathPrimary = shortPathNonProtected;//不能配对出secondary，留着后面重新calculate secondary
//                } else {
//                    shortPathPrimary = getShortPathsNonProtectd_WithMandatory(param, srcSiteId, destSiteId, siteLinks, fixGrid, srcSiteLinks, destSiteLinks, mandatorySiteLinkId);
//                    if (shortPathPrimary == null || shortPathPrimary.isEmpty()) {
//                        continue;
//                    }
//
//                }
//
//                //根据每一个primary最短路径，重新计算寻找最短路径
//                for (ShortPath shortPath : shortPathPrimary) {
//                    List<ShortPath> shortPathProtected = getShortPathProtected(fixGrid, srcSiteId, destSiteId, srcSiteLinks, destSiteLinks, siteLinks, param.getClientLineRate(), shortPath,
//                            hasMandatorySiteLinkId, param.getSrcSite().getNodeId().getValue(), param.getDesSite().getNodeId().getValue());
//                    if (shortPathProtected != null) {
//                        output.addAll(shortPathProtected);
//                    }
//                }
//            }
//        }
//
//        return output;
//    }
//
//    private List<ShortPath> getShortPathsNonProtectd_WithMandatory(ParamCompute3 param, String srcSiteId, String destSiteId, List<Link> siteLinks, String fixGrid, List<Link> srcSiteLinks,
//            List<Link> destSiteLinks,
//            String mandatorySiteLinkId) {
//        List<ShortPath> shortPathNonProtected;
//        Boolean isMandatorySiteLinkIdAsStartEnd = false;
//        if (mandatorySiteLinkId != null) {
//            if (mandatorySiteLinkId.contains(srcSiteId)) {
//                srcSiteLinks = srcSiteLinks.stream().filter(item -> item.getLinkId().getValue().equals(mandatorySiteLinkId)).collect(Collectors.toList());
//                isMandatorySiteLinkIdAsStartEnd = true;
//            } else if (mandatorySiteLinkId.contains(destSiteId)) {
//                destSiteLinks = destSiteLinks.stream().filter(item -> item.getLinkId().getValue().equals(mandatorySiteLinkId)).collect(Collectors.toList());
//                isMandatorySiteLinkIdAsStartEnd = true;
//            }
//        }
//
//        if (isMandatorySiteLinkIdAsStartEnd) {
//            shortPathNonProtected = getShortPathNonProtected(fixGrid, srcSiteId, destSiteId, srcSiteLinks, destSiteLinks, siteLinks, param.getClientLineRate());
//        } else {
//            Optional<Link> mandatorySiteLinkOptional = siteLinks.stream().filter(item -> item.getLinkId().getValue().equals(mandatorySiteLinkId)).findAny();
//            if (!mandatorySiteLinkOptional.isPresent()) {
//                log.debug("Failed to find mandatorySiteLink:{} in grid group:{}, then no path in this grid group.", mandatorySiteLinkId, fixGrid);
//                return null;
//            }
//            List<Link> mandatorySiteLinks = Arrays.asList(mandatorySiteLinkOptional.get());
//
//            //note: 这里的srcSiteId和destSiteId仍然不变，不需要替换成mandatorySiteLink的src/dest，是因为性能考虑，不想算那么多次，关于siteId的相关处理，连接两个half的时候会处理
//            List<ShortPath> shortPathFirstHalf = getShortPathNonProtected(fixGrid, srcSiteId, destSiteId, srcSiteLinks, mandatorySiteLinks, siteLinks, param.getClientLineRate());
//            if (shortPathFirstHalf.isEmpty()) {
//                log.debug("No path found from src:{} to mandatorySiteLink:{} in grid group:{}", srcSiteId, mandatorySiteLinkId, fixGrid);
//                return null;
//            }
//            List<ShortPath> shortPathLastHalf = getShortPathNonProtected(fixGrid, srcSiteId, destSiteId, mandatorySiteLinks, destSiteLinks, siteLinks, param.getClientLineRate());
//            if (shortPathLastHalf.isEmpty()) {
//                log.debug("No path found from mandatorySiteLink:{} to dest:{} in grid group:{}", mandatorySiteLinkId, destSiteId, fixGrid);
//                return null;
//            }
//            shortPathNonProtected = connectTwoHalf(shortPathFirstHalf, shortPathLastHalf, param.getClientLineRate());
//
//        }
//        return shortPathNonProtected;
//    }
//
//    private List<ShortPath> connectTwoHalf(List<ShortPath> shortPathFirstHalf, List<ShortPath> shortPathLastHalf, Integer clientLineNumber) {
//        List<ShortPath> shortPaths = new ArrayList<>();
//        for (ShortPath fistHalf : shortPathFirstHalf) {
//            SiteLinkRoute firstHalfSiteLinkRoute = fistHalf.getSiteLinkRoute();
//            List<Long> fistHalfFreq = firstHalfSiteLinkRoute.getCentralFrequencies();
//            List<String> firstPrimary = firstHalfSiteLinkRoute.getPrimary();
//            String firstPrimaryLastWssLinkSiteId = PhysicalNodeIdNamingRule.getSiteId(PhysicalLinkIdNamingRule.getNodeAId(firstPrimary.get(firstPrimary.size() - 2)));
//
//            for (ShortPath lastHalf : shortPathLastHalf) {
//                //step1: 检查frequency是否有交集
//                List<Long> commonFreqs = new ArrayList<>(fistHalfFreq);
//                commonFreqs.retainAll(lastHalf.getSiteLinkRoute().getCentralFrequencies());
//                if (commonFreqs.isEmpty()) {
//                    continue;
//                }
//                //step2: 过滤掉half连接点上，连续两个wss link在同一个site的情况
//                List<String> lastPrimary = lastHalf.getSiteLinkRoute().getPrimary();
//                String lastPrimaryFirstWssLinkSiteId = PhysicalNodeIdNamingRule.getSiteId(PhysicalLinkIdNamingRule.getNodeAId(lastPrimary.get(1)));
//                if (firstPrimaryLastWssLinkSiteId.equals(lastPrimaryFirstWssLinkSiteId)) {
//                    continue;
//                }
//                //step3: 连接前后半段，生成新的path
//                List<String> primary = new ArrayList<>(firstPrimary);
//                primary.remove(primary.size() - 1);//最后一个点肯定是连接点的sitelink，后半部分已经包含了。
//                primary.addAll(lastPrimary);
//                //todo: mandatory will refactor later, then hack here
//                int bundleNum = getBundleNum(clientLineNumber, commonFreqs.size(), commonFreqs.size());
//
//                SiteLinkRoute siteLinkRoute = new SiteLinkRouteBuilder()
//                        .setFixGrid(firstHalfSiteLinkRoute.getFixGrid())
//                        .setBundleNumber(bundleNum)
//                        .setCentralFrequencies(commonFreqs)
//                        .setVendorName(firstHalfSiteLinkRoute.getVendorName())
//                        .setPrimary(primary).build();
//
//                shortPaths.add(new ShortPathBuilder().setSiteLinkRoute(siteLinkRoute).build());
//            }
//
//        }
//        return shortPaths;
//    }
//
//    //在所有最短路径中，配对primary+secondary
//    private List<ShortPath> getShortPathProtected(List<ShortPath> shortPathTotal, int clientLineNumber, String srcSite, String desSite) {
//        List<ShortPath> output = new ArrayList<>();
//        int size = shortPathTotal.size();
//        for (int i = 0; i < size; i++) {
//            ShortPath primaryShortPath = shortPathTotal.get(i);
//            for (int j = i + 1; j < size; j++) {
//                ShortPath shortPathSecondary = shortPathTotal.get(j);
//                ShortPath shortPath = buildProtectedShortPath(primaryShortPath, shortPathSecondary, clientLineNumber, false, srcSite, desSite);
//                if (shortPath != null) {
//                    output.add(shortPath);
//                }
//            }
//        }
//        return output;
//    }
//
//    private List<ShortPath> getShortPathProtected(String fixGrid, String srcSiteId, String destSiteId, List<Link> srcSiteLinks, List<Link> destSiteLinks, List<Link> siteLinks,
//            int clientRateNumber,
//            ShortPath primaryShortPath, boolean hasMandatorySiteLinkId, String srcSite, String desSite) {
//        HashSet<String> primarySiteLinks = new HashSet<>(primaryShortPath.getSiteLinkRoute().getPrimary());
//        String primarySrcSiteLink = primaryShortPath.getSiteLinkRoute().getPrimary().get(0);
//        String primaryDestSiteLink = primaryShortPath.getSiteLinkRoute().getPrimary().get(primaryShortPath.getSiteLinkRoute().getPrimary().size() - 1);
//        List<Link> secondarySiteLinksPool = siteLinks.stream().filter(item -> !primarySiteLinks.contains(item.getLinkId().getValue())).collect(Collectors.toList());
//        List<Link> secondarySrcSiteLinks = srcSiteLinks.stream().filter(item -> !item.getLinkId().getValue().equals(primarySrcSiteLink)).collect(Collectors.toList());
//        List<Link> secondaryDestSiteLinks = destSiteLinks.stream().filter(item -> !item.getLinkId().getValue().equals(primaryDestSiteLink)).collect(Collectors.toList());
//
//        List<ShortPath> shortPathsSecondary = getShortPathNonProtected(fixGrid, srcSiteId, destSiteId, secondarySrcSiteLinks, secondaryDestSiteLinks, secondarySiteLinksPool, clientRateNumber);
//        List<ShortPath> output = new ArrayList<>();
//
//        for (ShortPath shortPathSecondary : shortPathsSecondary) {
//            ShortPath shortPath = buildProtectedShortPath(primaryShortPath, shortPathSecondary, clientRateNumber, hasMandatorySiteLinkId, srcSite, desSite);
//            if (shortPath != null) {
//                output.add(shortPath);
//            }
//        }
//        return output;
//    }
//
//    /**
//     * 两条路径可以组成primary+secondary的必要条件是：
//     *
//     * 1. 有共同的frequency
//     *
//     * 2. 途径的sitelink不重合
//     *
//     * @param primaryShortPath
//     * @param shortPathSecondary
//     * @param hasMandatorySiteLinkId
//     * @param srcSite
//     * @param desSite
//     * @return
//     */
//    private ShortPath buildProtectedShortPath(ShortPath primaryShortPath, ShortPath shortPathSecondary, int clientLineNumber, boolean hasMandatorySiteLinkId, String srcSite, String desSite) {
//        List<Long> jointFrequency = new ArrayList<>(primaryShortPath.getSiteLinkRoute().getCentralFrequencies());
//        jointFrequency.retainAll(shortPathSecondary.getSiteLinkRoute().getCentralFrequencies());
//
//        if (jointFrequency.isEmpty()) {
//            //no joint frequency
//            return null;
//        }
//        List<String> jointSiteLinks = new ArrayList<>(primaryShortPath.getSiteLinkRoute().getPrimary());
//        jointSiteLinks.retainAll(shortPathSecondary.getSiteLinkRoute().getPrimary());
//        if (!jointSiteLinks.isEmpty()) {
//            //路径有交集
//            return null;
//        }
//        int bundleNum = getBundleNum(clientLineNumber, jointFrequency.size(), jointFrequency.size());
//        int primaryBundle = primaryShortPath.getSiteLinkRoute().getBundleNumber();
//        int secondaryBundle = primaryShortPath.getSiteLinkRoute().getBundleNumber();
//        if (bundleNum > primaryBundle) {
//            bundleNum = primaryBundle;
//        }
//        if (bundleNum > secondaryBundle) {
//            bundleNum = secondaryBundle;
//        }
//        List<String> primary = primaryShortPath.getSiteLinkRoute().getPrimary();
//        List<String> secondary = shortPathSecondary.getSiteLinkRoute().getPrimary();
//
//        Set<String> primarySites = primary.stream().flatMap(item -> getSites(item, srcSite, desSite).stream()).collect(Collectors.toSet());
//        Set<String> secondarySites = secondary.stream().flatMap(item -> getSites(item, srcSite, desSite).stream()).collect(Collectors.toSet());
//        if (!Collections.disjoint(primarySites, secondarySites)) {
//            log.debug("No route, because primarySites:{} has common with secondarySites:{}", primarySites, secondarySites);
//            return null;
//        }
//
//        //put the shorter path on primary, when there is no mandatory
//        if (!hasMandatorySiteLinkId) {
//            if (primary.size() > secondary.size()) {
//                primary = shortPathSecondary.getSiteLinkRoute().getPrimary();
//                secondary = primaryShortPath.getSiteLinkRoute().getPrimary();
//            }
//        }
//
//        SiteLinkRoute siteLinkRoute = new SiteLinkRouteBuilder()
//                .setFixGrid(primaryShortPath.getSiteLinkRoute().getFixGrid())
//                .setBundleNumber(bundleNum)
//                .setCentralFrequencies(jointFrequency)
//                .setVendorName(primaryShortPath.getSiteLinkRoute().getVendorName())
//                .setPrimary(primary)
//                .setSecondary(secondary)
//                .build();
//
//        return new ShortPathBuilder().setSiteLinkRoute(siteLinkRoute).build();
//    }
//
//    private Set<String> getSites(String linkId, String srcSite, String desSite) {
//        if (linkId.startsWith(WSS_LINK_PREFIX)) {
//            Set<String> result = new HashSet<>();
//            result.add(PhysicalLinkIdNamingRule.getSiteAId(linkId));
//            return result;
//        }
//        Link siteLink = siteLinkDao.getSiteLinkById(linkId);
//        return siteLink.getAugmentation(Link1.class).getSite().getExplictRoute().getRoute().get(0).getPrimary().getExplicitRouteObjects().get(0).getPathRouteObject().stream()
//                .filter(item -> item.getResourceType().getImplementedInterface().getName()
//                        .equals(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp.class.getName()))
//                .map(item -> ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp) item.getResourceType()).getTpHop()
//                        .getSiteRef().getValue()).
//                filter(item -> !item.equals(srcSite) && !item.equals(desSite))
//                .collect(Collectors.toSet());
//    }
//
//    private boolean inSite(Link siteLink, String siteId) {
//        return siteLink.getSource().getSourceNode().getValue().equals(siteId) || siteLink.getDestination().getDestNode().getValue().equals(siteId);
//    }
//
//    private List<GridSiteLinkTemp> getGridSiteLinksFromDB(String riskGroupName, String planeName, List<String> matchedGrids, String srcSiteId, String destSiteId) {
//        //对sitelink分类
//        List<GridSiteLinkTemp> gridSiteLinkTemps = new ArrayList<>();
//        List<Link> flexGridSiteLinks = null;
//        List<Link> flexGridSiteLinksSrc = null;
//        List<Link> flexGridSiteLinksDest = null;
//        for (String matchedGrid : matchedGrids) {
//            List<Link> siteLinks = siteLinkDao.filterGridAvailable(matchedGrid, riskGroupName, planeName);
//            if (siteLinks == null || siteLinks.isEmpty()) {
//                log.debug("No siteLink found at Grid:{}, riskGroupName:{},planeName:{}", matchedGrid, riskGroupName, planeName);
//                continue;
//            }
//            List<Link> srcSiteLinks = siteLinks.stream().filter(item -> inSite(item, srcSiteId)).collect(Collectors.toList());
//            List<Link> destSiteLinks = siteLinks.stream().filter(item -> inSite(item, destSiteId)).collect(Collectors.toList());
//
//            if (matchedGrid.equals(tunnelUtils.GRID_0)) {
//                flexGridSiteLinks = siteLinks;
//                flexGridSiteLinksSrc = srcSiteLinks;
//                flexGridSiteLinksDest = destSiteLinks;
//            } else {
//                gridSiteLinkTemps.add(new GridSiteLinkTemp(matchedGrid, siteLinks, srcSiteLinks, destSiteLinks));
//            }
//        }
//
//        //没有固定频率的复用段，所有复用段都是flex的
//        if (gridSiteLinkTemps.isEmpty()) {
//            if (flexGridSiteLinks == null || flexGridSiteLinks.isEmpty()) {
//                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                        String.format("Group[%s] and plane[%s] can't find available siteLink", riskGroupName,
//                                planeName));
//            }
//            if (flexGridSiteLinksSrc == null || flexGridSiteLinksSrc.isEmpty()) {
//                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                        String.format("Group[%s] and plane[%s] can't find available siteLink in src site[%s]", riskGroupName,
//                                planeName, srcSiteId));
//            }
//            if (flexGridSiteLinksDest == null || flexGridSiteLinksDest.isEmpty()) {
//                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                        String.format("Group[%s] and plane[%s] can't find available siteLink in dest site[%s]", riskGroupName,
//                                planeName, destSiteId));
//            }
//            //all site links are flex grid
//            return Collections.singletonList(new GridSiteLinkTemp(tunnelUtils.GRID_75, flexGridSiteLinks, flexGridSiteLinksSrc, flexGridSiteLinksDest));//flex默认用75的间隔
//
//        }
//
//        //将flex加入fix，同时验证复用段中是否包含srcSite和destSite
//        List<GridSiteLinkTemp> result = new ArrayList<>();
//
//        for (GridSiteLinkTemp gridSiteLinkTemp : gridSiteLinkTemps) {
//            if (gridSiteLinkTemp.srcSiteLinks.isEmpty() && (flexGridSiteLinksSrc == null || flexGridSiteLinksSrc.isEmpty())) {
//                log.debug("No source siteLink found from site:{}, at Grid:{}, riskGroupName:{},planeName:{}", srcSiteId, gridSiteLinkTemp.fixGrid, riskGroupName, planeName);
//                continue;
//            }
//            if (gridSiteLinkTemp.destSiteLinks.isEmpty() && (flexGridSiteLinksDest == null || flexGridSiteLinksDest.isEmpty())) {
//                log.debug("No dest siteLink found from site:{}, at Grid:{}, riskGroupName:{},planeName:{}", destSiteId, gridSiteLinkTemp.fixGrid, riskGroupName, planeName);
//                continue;
//            }
//            if (flexGridSiteLinks != null && !flexGridSiteLinks.isEmpty()) {
//                gridSiteLinkTemp.siteLinks.addAll(flexGridSiteLinks);
//                gridSiteLinkTemp.srcSiteLinks.addAll(flexGridSiteLinksSrc);
//                gridSiteLinkTemp.destSiteLinks.addAll(flexGridSiteLinksDest);
//                result.add(gridSiteLinkTemp);
//            }
//        }
//
//        if (result.isEmpty()) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    String.format("Group[%s] and plane[%s] can't find available siteLink", riskGroupName,
//                            planeName));
//        }
//        return result;
//    }
//
//    /**
//     * 取出每一个srcSiteLinkId最短的路径
//     *
//     * @param fixGrid
//     * @param destSiteId
//     * @param srcSiteLinks
//     * @param destSiteLinks
//     * @param siteLinks
//     * @param clientLineRateNumber
//     * @return
//     */
//    private List<ShortPath> getShortPathNonProtected(String fixGrid, String srcSiteId, String destSiteId, List<Link> srcSiteLinks, List<Link> destSiteLinks, List<Link> siteLinks,
//            int clientLineRateNumber) {
//        List<ShortPath> result = new ArrayList<>();
//        GridType fixGridType = null;
//        try {
//            fixGridType = tunnelUtils.getGridByString(fixGrid);
//        } catch (NeDesignerException e) {
//            log.error("Failed to getShortPathNonProtected because invalid fixGrid:{}", fixGrid, e);
//            return result;
//        }
//        for (Link srcSiteLink : srcSiteLinks) {
//            //scenario1: single siteLink is the route
//            String srcSiteLinkId = srcSiteLink.getLinkId().getValue();
////            List<DJNode> shortPathNodes = new ArrayList<>();
//            String srcVendorName = srcSiteLink.getAugmentation(Link1.class).getSite().getVendorName();
//
//            DJNode srcNode = buildNode(srcSiteLink, fixGridType);
//            if (srcSiteLinkId.contains(destSiteId)) {//最短路径就是一个复用段
//                ShortPath shortPath = buildShortPath(fixGrid, Arrays.asList(srcNode), clientLineRateNumber, srcVendorName);
//                if (shortPath != null) {
//                    result.add(shortPath);//这就是这个srcSiteLink出发的最短的一条
//                }
//                continue;
//            }
//
//            List<Long> srcFreq = srcNode.getAvailableCentFrequency();
//            for (Link destSiteLink : destSiteLinks) {
//                String dstVendorName = destSiteLink.getAugmentation(Link1.class).getSite().getVendorName();
//                if (!srcVendorName.equals(dstVendorName)) {
//                    continue;
//                }
//                DJNode destNode = buildNode(destSiteLink, fixGridType);
//                List<Long> dstFreq = destNode.getAvailableCentFrequency();
//                List<Long> commonFreq = new ArrayList<>(srcFreq);
//                commonFreq.retainAll(dstFreq);
//                if (commonFreq.isEmpty()) {//frequency没有交集
//                    continue;
//                }
//                List<DJNode> shortPathNodes = calculateShortPath(srcSiteId, destSiteId, siteLinks, srcNode, destNode, new HashSet<>(commonFreq), fixGridType);
//                if (shortPathNodes.isEmpty()) {
//                    log.debug("No short path available between {} and {}.", srcSiteLinkId, destSiteLink.getLinkId().getValue());
//                    continue;
//                }
//                ShortPath shortPath = buildShortPath(fixGrid, shortPathNodes, clientLineRateNumber, srcVendorName);
//                if (shortPath == null) {
//                    continue;
//                }
//                result.add(shortPath);
//            }
//
//        }
//
//        return result;
//
//    }
//
//    private List<DJNode> calculateShortPath(String srcSiteId, String destSiteId, List<Link> siteLinks, DJNode srcNode, DJNode destNode, Set<Long> commonFreq, GridType fixGridType) {
//        Graph graph = buildGraph(srcSiteId, destSiteId, siteLinks, commonFreq, fixGridType);
//        DJNode djSourceNode = graph.getNodeByLinkId(srcNode.getSiteLinkId());
//        DJNode djDstNode = graph.getNodeByLinkId(destNode.getSiteLinkId());
//        if (djSourceNode == null || djDstNode == null) {
//            return Collections.EMPTY_LIST;
//        }
//        Dijkstra.calculateShortestPathFromSource(graph, djSourceNode);
//        List<DJNode> shortPathNodes = djDstNode.getShortestPath();
//        if (shortPathNodes.isEmpty()) {
//            return shortPathNodes;
//        }
//        shortPathNodes.add(djDstNode);//因为算出来的path不包括目的的
//        return shortPathNodes;
//    }
//
//    private ShortPath buildShortPath(String fixGrid, List<DJNode> nodes, int clientRateNumber, String vendorName) {
//        if (nodes == null || nodes.isEmpty()) {
//            return null;
//        }
//        List<String> primary = new ArrayList<>();
//        List<Long> freqs = nodes.get(0).getAvailableCentFrequency();
//        if (freqs.isEmpty()) {
//            return null;
//        }
//        List<Long> commonFreqs = new ArrayList<>(freqs);
//        int size = nodes.size();
//        String preWssLink = null;
//
//        for (int i = 0; i < size; i++) {
//            DJNode node = nodes.get(i);
//            commonFreqs.retainAll(node.getAvailableCentFrequency());
//            if (commonFreqs.isEmpty()) {
//                return null;
//            }
//            primary.add(node.getSiteLinkId());
//            if (i != size - 1) {
//                DJNode nextNode = nodes.get(i + 1);
//                String wssLinkId = node.getWssLinkId(nextNode.getSiteLinkId());
//                if (wssLinkId == null) {
//                    String msg = String.format("Build short path error, because failed to get wssLinkId from %s to %s", node.getSiteLinkId(), nextNode.getSiteLinkId());
//                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, msg);
//
//                }
//                if (preWssLink != null) {
//                    String preWssLinkSiteId = PhysicalTpIdNamingRule.getSiteId(PhysicalLinkIdNamingRule.getTpAId(preWssLink));
//                    String currentWssLinkSiteId = PhysicalTpIdNamingRule.getSiteId(PhysicalLinkIdNamingRule.getTpAId(wssLinkId));
//                    if (preWssLinkSiteId.equals(currentWssLinkSiteId)) {
//                        log.debug("Not valid path, because preWssLink:{} and currentWssLink:{} in same site.", preWssLinkSiteId, wssLinkId);
//                        return null;
//                    }
//
//                }
//                primary.add(wssLinkId);
//                preWssLink = wssLinkId;
//            }
//        }
//
//        if (commonFreqs.isEmpty()) {
//            return null;
//        }
//        Integer minBandwidth = getMinBandwidth(nodes);
//        int freqSize = freqs.size();
//        int bundleNum = getBundleNum(clientRateNumber, freqSize, minBandwidth);
//        SiteLinkRoute siteLinkRoute = new SiteLinkRouteBuilder()
//                .setFixGrid(fixGrid)
//                .setBundleNumber(bundleNum)
//                .setCentralFrequencies(commonFreqs)
//                .setVendorName(vendorName)
//                .setPrimary(primary).build();
//
//        return new ShortPathBuilder().setSiteLinkRoute(siteLinkRoute).build();
//    }
//
//    private Integer getMinBandwidth(List<DJNode> nodes) {
//        List<String> siteLinks = nodes.stream().map(DJNode::getSiteLinkId).collect(Collectors.toList());
//        Integer minBandwidth = siteLinkDao.getMinimumBandwidthByIds(siteLinks);
//        return minBandwidth;
//    }
//
//    private int getBundleNum(int clientRateNumber, int freqSize, int minBandwidth) {
//        int avaSize = freqSize > minBandwidth ? minBandwidth : freqSize;
//        return avaSize * clientRateNumber;
//    }
//
//    private List<DFSPath> getAllPaths(GridSiteLinkTemp gridSiteLinkTemp, String srcSiteId, String destSiteId) {
//        GridType fixGridType = null;
//        try {
//            fixGridType = tunnelUtils.getGridByString(gridSiteLinkTemp.fixGrid);
//        } catch (NeDesignerException e) {
//            log.error("Failed to getAllPath because invalid fixGrid:{}", gridSiteLinkTemp.fixGrid, e);
//            return Collections.EMPTY_LIST;
//        }
//        List<DFSPath> totalPath = new ArrayList<>();
//        DFSGraph graph = new DFSGraph();
//        List<String> siteLinkIds = gridSiteLinkTemp.siteLinks.stream().map(item -> item.getLinkId().getValue()).collect(Collectors.toList());
//
//        //Add node in graph and get P2P path if existed
//        for (Link siteLink : gridSiteLinkTemp.siteLinks) {
//            String siteLinkId = siteLink.getLinkId().getValue();
//            if (siteLinkId.contains(srcSiteId) && siteLinkId.contains(destSiteId)) {//P2P直接返回path，不参与graph计算
//                totalPath.add(new DFSPath(Arrays.asList(siteLinkId), new FrequencyAvailable(siteLink).getAllPossibleCentFrequency(fixGridType)));
//                continue;
//            }
//            graph.addNode(new DFSNode(siteLink.getLinkId().getValue(), new FrequencyAvailable(siteLink).getAllPossibleCentFrequency(fixGridType)));
//        }
//
//        //Add addEdge graph
//        List<Link> wssLinks = phyLinkDao.filterAllWssLink(siteLinkIds, srcSiteId, destSiteId);//剔除掉src的MUX和dest的MUX直接通过wss相连这种情况，这个路径是错误的
//
//        for (Link wssLink : wssLinks) {
//            try {
//                List<SupportedLink> supportedLink = wssLink.getAugmentation(
//                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class).getPhysical().getSupportedLink();
//                String siteLinkAId = supportedLink.get(0).getLinkRef().getValue();
//                String siteLinkZId = supportedLink.get(1).getLinkRef().getValue();
//                graph.addEdge(siteLinkAId, siteLinkZId, wssLink.getLinkId().getValue());
//            } catch (Exception e) {
//                log.error("Drop invalid wssLink when build graph.{}.", wssLink, e);
//            }
//        }
//
//        //calculate path
//        for (Link srcLink : gridSiteLinkTemp.srcSiteLinks) {
//            String srcLinkId = srcLink.getLinkId().getValue();
//            for (Link destLink : gridSiteLinkTemp.destSiteLinks) {
//                String destLinkId = destLink.getLinkId().getValue();
//                totalPath.addAll(graph.findAllPaths(srcLinkId, destLinkId));
//            }
//        }
//        return totalPath;
//
//    }
//
//    private DJNode buildNode(Link siteLink, GridType fixGridType) {
//        List<Long> centralFrequencies = new FrequencyAvailable(siteLink).getAllPossibleCentFrequency(fixGridType);
//        return new DJNode(siteLink.getLinkId().getValue(), centralFrequencies);
//    }
//
//    private int getMatchedOchReusedOdu(Link ochLink, ParamCompute3 param, String vendorName, String productType) {
//        Och ochLinkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();
//
//        if (ochLinkAttr.getAvailable() == null) {
//            return 0;
//        }
//
//        if (!ochLinkAttr.getCardType().equals(param.getCardType())) {
//            return 0;
//        }
//
//        if (!ochLinkAttr.getVendorName().equals(vendorName) || !ochLinkAttr.getProductType().equals(productType)) {
//            return 0;
//        }
//        if (!ochLinkAttr.getOdukType().equals(param.getOdukType())) {
//            return 0;
//        }
//
//        for (Available avaOdu : ochLinkAttr.getAvailable()) {
//            if (avaOdu.getSupportedOduj().equals(param.getTunnelOdu())) {
//                String available = avaOdu.getAvailableOdujSlot();
//                if (available.isEmpty()) {
//                    return 0;
//                } else {
//                    return available.split(Constant.OCH_AVAILABLE_ODU_SEPARATOR).length;
//                }
//            }
//        }
//
//        return 0;
//    }
//
//    class ReusedRouteTemp {
//
//        private ArrayList<String> primary;
//        private ArrayList<String> secondary;
//        private String vendorName;
//        private String productType;
//        private String fixGrid;
//
//        public ReusedRouteTemp(ArrayList<String> primary, ArrayList<String> secondary, String vendorName, String productType, String fixGrid) {
//            this.primary = primary;
//            this.secondary = secondary;
//            this.vendorName = vendorName;
//            this.productType = productType;
//            this.fixGrid = fixGrid;
//        }
//
//        public int hashCode() {
//            return HashCodeBuilder.reflectionHashCode(this);
//        }
//
//        public boolean equals(Object anObject) {
//            if (this == anObject) {
//                return true;
//            }
//            if (anObject instanceof ReusedRouteTemp) {
//                ReusedRouteTemp another = (ReusedRouteTemp) anObject;
//                if (!another.vendorName.equals(vendorName)) {
//                    return false;
//                }
//                if (!another.productType.equals(productType)) {
//                    return false;
//                }
//                if (!another.primary.equals(primary)) {
//                    return false;
//                }
//                if (this.secondary == null) {
//                    if (another.secondary == null) {
//                        return true;
//                    } else {
//                        return false;
//                    }
//                } else {
//                    if (another.secondary == null) {
//                        return false;
//                    } else {
//                        return this.secondary.equals(((ReusedRouteTemp) anObject).secondary);
//                    }
//                }
//            }
//            return false;
//        }
//
//
//        public void reverse() {
//            if (this.primary != null) {
//                Collections.reverse(this.primary);
//            }
//            if (this.secondary != null) {
//                Collections.reverse(this.secondary);
//            }
//        }
//    }
//
//    class GridSiteLinkTemp {
//
//        String fixGrid; //e.g. 50,75
//        List<Link> siteLinks;
//        List<Link> srcSiteLinks;
//        List<Link> destSiteLinks;
//
//        public GridSiteLinkTemp(String fixGrid, List<Link> siteLinks, List<Link> srcSiteLinks, List<Link> destSiteLinks) {
//            this.fixGrid = fixGrid;
//            this.siteLinks = siteLinks;
//            this.srcSiteLinks = srcSiteLinks;
//            this.destSiteLinks = destSiteLinks;
//        }
//    }
//}