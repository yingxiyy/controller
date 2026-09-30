package net.flex.dci.otn.controller.nms.nms.component.route;

import static net.flex.dci.otn.controller.nms.utils.Constants.LOGIC_DESCRIPTION_XC;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import net.flex.dci.otn.controller.nms.nms.component.route.retriever.RouteRetriever;
import net.flex.dci.otn.controller.nms.utils.CrossConnectionUtils;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.SecondaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.ThirdBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 2022/5/23 14:18
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractLinkRoute implements LinkRoute {

    protected final NetconfTopology netconfTopology;

    protected final RouteRetriever routeRetriever;


    @Autowired
    protected CrossConnectionsDao crossConnectionsDao;

    private static final Pattern MPO_SUFFIX_PATTERN = Pattern.compile(
            "(.*)(Site-[^#]+#Ne-[^#]+#LINECARD-[^#]+#PORT-[^#]+-MPO)$");

    public abstract String linkType();


    /**
     * replace the cross connection for the real one
     *
     * @param routeList
     * @return
     */
    protected List<Route> getRealRoute(List<Route> routeList) {
        List<Route> newRouteList = new ArrayList<>();
        long startTime = System.currentTimeMillis();
        routeList.forEach(route -> {
            RouteBuilder routeBuilder = new RouteBuilder(route);
            routeBuilder.setPrimary(new PrimaryBuilder(route.getPrimary()).setCrossConnections(
                    getRealCrossConnection(route.getPrimary().getCrossConnections())).build());
            if (route.getSecondary() != null) {
                routeBuilder.setSecondary(
                        new SecondaryBuilder(route.getSecondary()).setCrossConnections(
                                        getRealCrossConnection(route.getSecondary().getCrossConnections()))
                                .build());
            }
            if (route.getThird() != null) {
                List<Third> thirds = reconstructThird(route.getThird());
                routeBuilder.setThird(thirds);
            }
            newRouteList.add(routeBuilder.build());
        });
        long cost = System.currentTimeMillis() - startTime;
        log.info("get real route cost:{} ms", cost);
        return newRouteList;
    }

    private List<Third> reconstructThird(List<Third> thirds) {
        log.debug("reconstruct third route");
        List<Third> reConstructThirds = new ArrayList<>();
        for (Third third : thirds) {
            ThirdBuilder thirdBuilder = new ThirdBuilder(third).setCrossConnections(
                    getRealCrossConnection(third.getCrossConnections()));
            reConstructThirds.add(thirdBuilder.build());
        }
        return reConstructThirds;
    }

    private List<CrossConnections> getRealCrossConnection(List<CrossConnections> crossConnections) {
        log.debug("get real cross connection on ne");
        List<CrossConnections> realCrossConnection = new ArrayList<>();
        List<CrossConnections> externalCrossConnection = new ArrayList<>();
        List<String> internalCrossConnectionIds = new ArrayList<>();
        List<CrossConnections> internalCrossConnection = new ArrayList<>();
        List<String> apsXcIds = new ArrayList<>();
        for (CrossConnections crossConnection : crossConnections) {
            if (CrossConnectionUtils.isInternalCrossConnection(crossConnection)
                    && !(crossConnection.getDescription().equals(LOGIC_DESCRIPTION_XC))) {
                String xcId = crossConnection.getCrossConnectionId().getValue();
                if (crossConnection.getAps() == null) {
                    internalCrossConnectionIds.add(xcId);
                } else {
                    apsXcIds.add(xcId);
                }
            } else {
                externalCrossConnection.add(crossConnection);
            }
        }
        //real aps xcId
        List<String> realApsXcIds = buildRealApsXcIdsOnNe(apsXcIds);
        internalCrossConnectionIds.addAll(realApsXcIds);
        long t1 = System.currentTimeMillis();
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> realXcs = crossConnectionsDao.listAllRealXcByXcIds(
                internalCrossConnectionIds);
        long t2 = System.currentTimeMillis();
        internalCrossConnection = realXcs.stream()
                .map(xc -> new CrossConnectionsBuilder(xc).build()).collect(
                        Collectors.toList());
//        List<String> realXcIds = realXcs.stream().map(xc -> xc.getCrossConnectionId().getValue())
//                .collect(
//                        Collectors.toList());
//        List<String> missingXcIds = new ArrayList<>(xcIds);
//        missingXcIds.removeAll(realXcIds);
//        if (!missingXcIds.isEmpty()) {
//            for (String xcId : xcIds) {
//                List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> regexXcs = crossConnectionsDao.getXCByXcIdRegexLike(
//                        xcId);
//                internalCrossConnection.add(new CrossConnectionsBuilder(regexXcs.get(0)).build());
//            }
//        }
        long t3 = System.currentTimeMillis();
        log.info("[ROUTE-TIMING] getRealCrossConnection: db={}ms, builder={}ms, xcCount={}",
                t2 - t1, t3 - t2, internalCrossConnectionIds.size());
//        internalCrossConnection = internalCrossConnection.stream().map(xc -> {
//            String crossConnectionId = xc.getCrossConnectionId().getValue();
//            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> realXcs = crossConnectionsDao.getXCByXcIdRegexLike(
//                    crossConnectionId);
//            return new CrossConnectionsBuilder(realXcs.get(0)).build();
//        }).collect(Collectors.toList());
        realCrossConnection.addAll(internalCrossConnection);
        realCrossConnection.addAll(externalCrossConnection);
        return realCrossConnection;
    }

    /**
     * build the real aps xc ids on ne
     *
     * @param apsXcIds
     * @return
     */
    private List<String> buildRealApsXcIdsOnNe(List<String> apsXcIds) {
        log.debug("build the real aps xc ids on ne the aps xcIds :{}", apsXcIds);
        List<String> realApsIds = new ArrayList<>();
        for (String apsXcId : apsXcIds) {
            if (!StringUtils.hasText(apsXcId)) {
                continue;
            }
            Matcher matcher = MPO_SUFFIX_PATTERN.matcher(apsXcId);
            if (matcher.matches()) {
                String mainPrefix = matcher.group(1);
                String mpoBaseTemplate = matcher.group(2);

                StringBuilder sb = new StringBuilder(mainPrefix);
                for (int i = 1; i <= 8; i++) {
                    sb.append(mpoBaseTemplate).append(i).append("-");
                }
                sb.setLength(sb.length() - 1);
                realApsIds.add(sb.toString());
            } else {
                log.warn("xc id format not match, skip: {}", apsXcId);
                realApsIds.add(apsXcId);
            }
        }

        return realApsIds;
    }
}
