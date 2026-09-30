package net.flex.dci.otn.controller.allocate.link.common;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.InternalLinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.PhyEquipAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.OchLinksSnapshot;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.ReusedNodesSnapshot;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.SiteLinksSnapshot;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.och.links.snapshot.OchLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.site.links.snapshot.SiteLinks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * this used for create-tunnel-3
 */
@Slf4j
public class ReuseResourceChecker {
    ChangedObject changedObject;

    public ReuseResourceChecker(ChangedObject changedObject) {
        this.changedObject = changedObject;
    }

    /**
     * snapshot是config树的,
     *   主要是tunnel涉及的电层网元，需要检测板卡，交叉，link
     *   需要完全一样的才可以匹配
     *
     * @param reusedNodesSnapshotList
     * @throws CommonException
     */
    public void checkInitialNodeEnv(List<ReusedNodesSnapshot> reusedNodesSnapshotList) throws CommonException {
        if (reusedNodesSnapshotList == null || reusedNodesSnapshotList.isEmpty())
            return;

        for (ReusedNodesSnapshot reuseNode : reusedNodesSnapshotList) {
            Node dbNode = changedObject.getChangedPhyNode(reuseNode.getNodeId().getValue());
            if (dbNode == null) {
                throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE,
                        String.format("利旧设备 %s 已经被删除", reuseNode.getPhysical().getFriendlyName()));
            }
            checkNodeResource(reuseNode, dbNode);
        }
    }

    private void checkNodeResource(ReusedNodesSnapshot reuseNode, Node dbNode) throws CommonException {
        Physical dbPhyAttr = dbNode.getAugmentation(Node1.class).getPhysical();
        Physical reusedPhyAttr = reuseNode.getPhysical();

        checkEquipment(dbPhyAttr.getFriendlyName(), dbPhyAttr.getEquipments(), reusedPhyAttr.getEquipments());
        checkCrossConnection(dbPhyAttr.getFriendlyName(), dbPhyAttr.getCrossConnections(), reusedPhyAttr.getCrossConnections());
        checkInternalLink(dbPhyAttr.getFriendlyName(), dbPhyAttr.getInternalLinks(), reusedPhyAttr.getInternalLinks());
        checkTp(dbPhyAttr.getFriendlyName(), dbNode.getTerminationPoint(), reuseNode.getTerminationPoint());
    }


    /**
     * assumption: TPC XC is C---L
     *   the latest else if means TPC XC is L---C,
     * @param nodeFriendlyName
     * @param dbCrossConnections
     * @param reusedcrossConnections
     * @throws CommonException
     */
    private void checkCrossConnection(String nodeFriendlyName, List<CrossConnections> dbCrossConnections, List<CrossConnections> reusedcrossConnections) throws CommonException {
        if (dbCrossConnections.size() != reusedcrossConnections.size() ) {
            throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE,
                    String.format("reuse device（%s）related XC info isn't match", nodeFriendlyName));
        }

        List<String> dbXCIds = dbCrossConnections.stream().map(x -> x.getCrossConnectionId().getValue()).collect(Collectors.toList());
        List<String> reuseXCIds = reusedcrossConnections.stream().map(x -> x.getCrossConnectionId().getValue()).collect(Collectors.toList());

        boolean same = new HashSet<>(dbXCIds).equals(new HashSet<>(reuseXCIds));
        if (!same) {
            throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE,
                    String.format("reuse device（%s）related XC info isn't match", nodeFriendlyName));
        }
    }

    private void checkTp(String nodeFriendlyName, List<TerminationPoint> dbTps, List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.phy.ne.full.info.TerminationPoint> reUsedTps) throws CommonException {
        if (dbTps.size() != reUsedTps.size() ) {
            throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE,
                    String.format("reuse device（%s）related TP info isn't match", nodeFriendlyName));
        }

        List<String> dbTPIds = dbTps.stream().map(x -> x.getTpId().getValue() + ":" + x.getAugmentation(TerminationPoint1.class).getPhysical().getConnectionStatus().toString()).collect(Collectors.toList());
        List<String> reuseTPIds = reUsedTps.stream().map(x -> x.getTpId().getValue() + ":" + x.getPhysical().getConnectionStatus().toString()).collect(Collectors.toList());

        boolean same = new HashSet<>(dbTPIds).equals(new HashSet<>(reuseTPIds));
        if (!same) {
            throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE,
                    String.format("reuse device（%s）related TP info isn't match", nodeFriendlyName));
        }
    }

    private void checkInternalLink(String nodeFriendlyName, List<InternalLinks> dbInternalLinks, List<InternalLinks> reusedInternalLinks) throws CommonException {
        if (dbInternalLinks.size() != reusedInternalLinks.size() ) {
            throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE,
                    String.format("reuse device（%s）related internal link info isn't match", nodeFriendlyName));
        }
        List<String> dbILIds = dbInternalLinks.stream().map(InternalLinkAttributes::getLinkRef).collect(Collectors.toList());
        List<String> reuseILIds = reusedInternalLinks.stream().map(InternalLinkAttributes::getLinkRef).collect(Collectors.toList());

        boolean same = new HashSet<>(dbILIds).equals(new HashSet<>(reuseILIds));
        if (!same) {
            throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE,
                    String.format("reuse device（%s）related internal link info isn't match", nodeFriendlyName));
        }
    }

    private void checkEquipment(String nodeFriendlyName, List<Equipments> dbEquipments, List<Equipments> reuseEquipments) throws CommonException {
        if (dbEquipments.size() != reuseEquipments.size() ) {
            throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE,
                    String.format("reuse device（%s）related EQ info isn't match", nodeFriendlyName));
        }
        List<String> dbEqIds = dbEquipments.stream().map(PhyEquipAttributes::getEquipmentId).collect(Collectors.toList());
        List<String> reuseEqIds = reuseEquipments.stream().map(PhyEquipAttributes::getEquipmentId).collect(Collectors.toList());

        boolean same = new HashSet<>(dbEqIds).equals(new HashSet<>(reuseEqIds));
        if (!same) {
            throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE,
                    String.format("reuse device（%s）related EQ info isn't match", nodeFriendlyName));
        }
    }

    public void checkInitialOchLinkEnv(OchLinksSnapshot ochLinksSnapshot) throws CommonException {
        if (ochLinksSnapshot == null || ochLinksSnapshot.getOchLinks() == null || ochLinksSnapshot.getOchLinks().isEmpty())
            return;

        for (OchLinks reusedOchLink : ochLinksSnapshot.getOchLinks()) {
            Link dbOchLink = changedObject.getChangedOchLink(reusedOchLink.getLinkId());
            checkOchLinkResource(reusedOchLink, dbOchLink);
        }
    }

    private void checkOchLinkResource(OchLinks reusedOchLink, Link dbOchLink) throws CommonException {
        Och dbOchAttr = dbOchLink.getAugmentation(Link1.class).getOch();
        Och reusedOchAttr = reusedOchLink.getOch();

        if (dbOchAttr.getAvailable().size() == reusedOchAttr.getAvailable().size()) {
            for (Available dbAva : dbOchAttr.getAvailable()) {
                boolean found = false;
                for (Available reusedAva : reusedOchAttr.getAvailable()) {
                    if (dbAva.getSupportedOduj().equals(reusedAva.getSupportedOduj())) {
                        if (dbAva.getAvailableOdujSlot().equals(reusedAva.getAvailableOdujSlot())) {
                            found = true;
                            break;
                        }
                    }
                }
                if (!found) {
                    throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE,
                            String.format("利旧OchLink %s 已经被使用", dbOchAttr.getFriendlyName(), dbOchAttr.getLowerFrequency().getValue().longValue()));
                }
            }
        } else {
            log.error("利旧OchLink {} 已经被使用, 数据库中的available size {}, reused is {}", dbOchAttr.getFriendlyName(), dbOchAttr.getAvailable().size(), reusedOchAttr.getAvailable().size());
            throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE,
                    String.format("利旧OchLink %s 已经被使用", dbOchAttr.getFriendlyName(), dbOchAttr.getAvailable().size()));
        }
    }

    public void checkInitialSiteLinkEnv(SiteLinksSnapshot siteLinksSnapshot) throws CommonException {
        if (siteLinksSnapshot == null || siteLinksSnapshot.getSiteLinks() == null || siteLinksSnapshot.getSiteLinks().isEmpty())
            return;

        for (SiteLinks reusedSiteLink : siteLinksSnapshot.getSiteLinks()) {
            Link dbSiteLink = changedObject.getChangedSiteLink(reusedSiteLink.getLinkId());
            checkSiteLinkResource(reusedSiteLink, dbSiteLink);
        }
    }

    private void checkSiteLinkResource(SiteLinks reusedSiteLink, Link dbSiteLink) throws CommonException {
        Site dbSiteAttr = dbSiteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
        Site reusedSiteAttr = reusedSiteLink.getSite();

        if (dbSiteAttr.getAvailable().size() == reusedSiteAttr.getAvailable().size()) {
            for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available dbAva : dbSiteAttr.getAvailable()) {
                boolean found = false;
                for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available reusedAva : reusedSiteAttr.getAvailable()) {
                    if (dbAva.getLowerFrequency().getValue().longValue() == reusedAva.getLowerFrequency().getValue().longValue() &&
                            dbAva.getUpperFrequency().getValue().longValue() == reusedAva.getUpperFrequency().getValue().longValue()) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE,
                            String.format("利旧siteLink %s  与原始值不一样", dbSiteAttr.getFriendlyName()));
                }
            }
        } else {
            log.error("利旧OchLink {} 已经被使用, 数据库中的available size {}, reused is {}", dbSiteAttr.getFriendlyName(), dbSiteAttr.getAvailable().size(), reusedSiteAttr.getAvailable().size());
            throw new CommonException(CommonExceptionType.NO_ENOUGH_RESOURCE,
                    String.format("利旧siteLink %s 与原始值不一样", dbSiteAttr.getFriendlyName()));
        }
    }
}

