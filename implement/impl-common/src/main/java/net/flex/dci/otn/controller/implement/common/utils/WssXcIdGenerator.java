package net.flex.dci.otn.controller.implement.common.utils;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class WssXcIdGenerator {
    private final static OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);

    public static Uri getXcId(ChangedObject changedObject, String src, String dst, long centerFreq) {

        List<String> tpIds = new ArrayList<>();
        tpIds.add(String.format("%s/%d", src, centerFreq));
        tpIds.add(String.format("%s/%d", dst, centerFreq));

        String xcId = tpIds.stream().sorted().collect(Collectors.joining("-", PhysicalXcIdNamingRule.ASE_XC_PREFIX, ""));

        xcId = fetchXcId(changedObject, xcId);
        return new Uri(xcId);
    }

    /**
     * 很重要的一个改动，加入假波后， 某些情况下，已经删除了的假波的中心频率，将和刚刚加进来的波段的中心频率是同一个，
     * 先调用删除，changeObject 中的node 就失去了这个xc
     * 而后面的新建业务波将没有这个波，出现严重问题
     * 为此创建不同的xcID
     *
     * Adding business OCH:   [191400000,191475000]
     *  remove dummy och (2), [[191350000,191450000], [191450000,191550000]]
     *  add dummy och (2),    [[191350000,191400000], [191475000,191525000]]
     *
     * 这种情况下可以看出removed och 的中心频率 和 将要添加的假波的中心频率一样都是  191500000
     *
     * @param changedObject
     * @param newXcId
     */
    private static String fetchXcId(ChangedObject changedObject, String newXcId) {
        String nodeId = PhysicalXcIdNamingRule.getNodeId(newXcId);
        Node node = changedObject.getChangedPhyNode(nodeId);
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

        CrossConnections dbXc = nodeAttr.getCrossConnections().stream()
                .filter(x -> x.getCrossConnectionId().getValue().equals(newXcId))
                .findAny().orElse(null);

        if (dbXc == null) {
            return newXcId;
        } else {
            log.warn("find duplicate xc, {}", newXcId );
            if (isXCUsedInDBOCHLink(nodeId, newXcId)) {
                log.debug("the xcId {} is used in ochLink on node {}, when checking in DB", newXcId, nodeId);

                if (isXCUsedInCacheOchLink(changedObject, nodeId, newXcId)) {
                    log.debug("the xcId {} is used in ochLink on node {}, when checking in cache", newXcId, nodeId);
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "duplicate xc id " + newXcId + " on node " + nodeId);
                } else {
                    log.debug("the xcId not used in DB and cached ochLink, so return it");
                    return newXcId;
                }
            } else {
                log.debug("the xcId existed in db, but no ochLink take it, remove the old one");
                //remove this crossConnection, because the ID is same but maybe lower/upper isn't match
                removeDeprecatedXc(changedObject, newXcId);
                return newXcId;
            }
        }
    }

    private static boolean isXCUsedInCacheOchLink(ChangedObject changedObject, String nodeId, String newXcId) {
        // Dummy OCH creation can run per siteLink in parallel. Use a value
        // snapshot so the duplicate check does not observe key/value mismatch.
        List<Link> matchedOchLinkList = new ArrayList<>(changedObject.getChangedOchLinkList().values()).stream()
                .filter(ochLink -> ochLink != null && ochLink.getLinkId().getValue().contains(nodeId))
                .collect(Collectors.toList());
        return usedInOchLink(matchedOchLinkList, newXcId);
    }

    private static boolean isXCUsedInDBOCHLink(String nodeId, String xcId) {
        List<Link> ochLinks = ochLinkDao.queryWithNode(nodeId);
        return usedInOchLink(ochLinks, xcId);
    }

    private static boolean usedInOchLink(List<Link> ochLinks, String xcId) {
        for (Link ochLink : ochLinks) {
            Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
            Route route = ochLinkAttr.getExplictRoute().getRoute().get(0);

            boolean included;
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> allXcs = new ArrayList<>();
            allXcs.addAll(route.getPrimary().getCrossConnections());
            if (route.getSecondary() != null) {
                allXcs.addAll(route.getSecondary().getCrossConnections());
            }
            if (route.getThird() != null) {
                for (Third t : route.getThird()) {
                    allXcs.addAll(t.getCrossConnections());
                }
            }

            included = existedInRoute(xcId, allXcs);

            if (included) {
                log.debug("the XC is used in ochLink {}", ochLink.getLinkId().getValue());
                return true;
            }
        }
        return false;
    }


    private static boolean existedInRoute(String xcId, List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> xcsInRoute) {
        return xcsInRoute.stream()
                .anyMatch(xc -> xc.getCrossConnectionId().getValue().equals(xcId));
    }

    private static void removeDeprecatedXc(ChangedObject changedObject, String deprecatedXcId) {
        String nodeId = PhysicalXcIdNamingRule.getNodeId(deprecatedXcId);
        Node node = changedObject.getChangedPhyNode(nodeId);
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

        List<CrossConnections> xcList = new ArrayList<>(nodeAttr.getCrossConnections());
        xcList.removeIf(xc->xc.getCrossConnectionId().getValue().equals(deprecatedXcId));

        Node newNode = new NodeBuilder(node)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(nodeAttr)
                                .setCrossConnections(xcList)
                                .build())
                        .build())
                .build();

        changedObject.addChangedPhyNode(newNode);
    }

}
