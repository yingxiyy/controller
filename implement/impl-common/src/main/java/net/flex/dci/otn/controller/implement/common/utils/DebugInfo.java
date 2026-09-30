package net.flex.dci.otn.controller.implement.common.utils;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;

@Slf4j
public class DebugInfo {

    public static void print(ChangedObject cache, RouteInfo rInfo) {
        //check first, rInfo required should stored in cache
        //在deimplement 的时候有些资源会被删除，如复用段 中一些垃圾假波数据
        try {
            rInfo.getNodeIdList().forEach(nodeId -> {
                if (!cache.getChangedPhyNodeList().containsKey(nodeId)) {
                    log.warn("debug info, node {} not in changed list", nodeId);
                    cache.getChangedPhyNode(nodeId);
                }
            });
            rInfo.getLogicServerLinkIdList().forEach(linkId -> {
                if (OchLinkIdNamingRule.isOchLink(linkId)) {
                    if (!cache.getChangedOchLinkList().containsKey(linkId)) {
                        log.warn("debug info, ochLink {} not in changed list", linkId);
                        cache.getChangedOchLink(linkId);
                    }
                } else if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                    if (!cache.getChangedSiteLinkList().containsKey(linkId)) {
                        log.warn("debug info, siteLink {} not in changed list", linkId);
                        cache.getChangedSiteLink(linkId);
                    }
                } else {
                    log.warn("debug info, logicServerLink {} is neither ochLink nor siteLink", linkId);
                }
            });
            rInfo.getPhyLinkIdList().forEach(linkId -> {
                if (!cache.getChangedPhyLinkList().containsKey(linkId)) {
                    log.warn("debug info, phyLink {} not in changed list", linkId);
                    cache.getChangedPhyLink(linkId);
                }
            });

            List<String> logInfo = new ArrayList<>();

            logInfo.add("\t   Tunnel status,      tunnelFriendlyName");
            if (!cache.getChangedTunnelList().isEmpty()) {
                cache.getChangedTunnelList().forEach((tunnelId, tunnel) -> {
                    logInfo.add(String.format("\t%s, %s", tunnel.getImplementState(),
                            tunnel.getFriendlyName()));
                });
            }
            logInfo.add("\t   ochLink status,      ochLinkID");
            if (!cache.getChangedOchLinkList().isEmpty()) {
                cache.getChangedOchLinkList().forEach((ochLinkId, ochLink) -> {
                    Och ochAttr = ochLink.getAugmentation(Link1.class).getOch();
                    logInfo.add(String.format("\t%s, %s", ochAttr.getImplementState(), ochLinkId));
                });
            }
            logInfo.add("\t  ochLink REMOVED,      ochLinkID");
            if (!cache.getRemovedOchLinkIdList().isEmpty()) {
                cache.getRemovedOchLinkIdList().forEach((ochLinkId) -> {
                    logInfo.add(String.format("\t%s", ochLinkId));
                });
            }
            logInfo.add("\t   SiteLink status,      siteLinkFriendlyName");
            if (!cache.getChangedSiteLinkList().isEmpty()) {
                cache.getChangedSiteLinkList().forEach((siteLinkId, siteLink) -> {
                    Site siteAttr = siteLink.getAugmentation(
                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                            .getSite();
                    logInfo.add(String.format("\t%s, %s", siteAttr.getImplementState(),
                            siteAttr.getFriendlyName()));
                });
            }
            logInfo.add("\t   PhyLink status,      phyLink FriendlyName");
            if (!cache.getChangedPhyLinkList().isEmpty()) {
                cache.getChangedPhyLinkList().forEach((phyLinkId, phyLink) -> {
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical phyAttr = phyLink.getAugmentation(
                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                            .getPhysical();
                    logInfo.add(String.format("\t%s, %s", phyAttr.getImplementState(),
                            phyAttr.getFriendlyName()));
                });
            }
            logInfo.add("\t   PhyNode status,      phyNode FriendlyName");
            if (!cache.getChangedPhyNodeList().isEmpty()) {
                cache.getChangedPhyNodeList().forEach((phyNodeId, phyNode) -> {
                    Physical phyAttr = phyNode.getAugmentation(Node1.class).getPhysical();
                    logInfo.add(String.format("\t%s, %s(%s)", phyAttr.getImplementState(),
                            phyAttr.getFriendlyName(), phyAttr.getIp()));
                });
            }

            //check tpList in rInfo
            logInfo.add("\t  TP status,      TP FriendlyName");
            rInfo.getTpIdList().forEach(tpId -> {
                String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
                Node node = cache.getChangedPhyNode(nodeId);
                Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

                TerminationPoint tp = node.getTerminationPoint().stream()
                        .filter(x -> x.getTpId().getValue().equals(tpId))
                        .findAny().orElse(null);

                if (tp == null) {
                    logInfo.add(String.format("\t%s, %s(%s)--%s", "REMOVED", nodeAttr.getFriendlyName(),
                            nodeAttr.getIp(), tpId));
                    return;
                }

                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr = tp.getAugmentation(
                        TerminationPoint1.class).getPhysical();
                logInfo.add(String.format("\t%s(%s, %s), %s(%s)--%s", tpAttr.getImplementState(),
                        tpAttr.getAdminState(), tpAttr.getConnectionStatus(),
                        nodeAttr.getFriendlyName(), nodeAttr.getIp(), tpAttr.getFriendlyName()));
            });

            //check eqList in rInfo
            logInfo.add("\t   EQ status,      EQ FriendlyName");
            rInfo.getEqIdList().forEach(eqId -> {
                String nodeId = PhysicalEqpIdNamingRule.getNodeId(eqId);
                Node node = cache.getChangedPhyNode(nodeId);
                Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

                Equipments eq = nodeAttr.getEquipments().stream()
                        .filter(x -> x.getEquipmentId().equals(eqId))
                        .findAny().orElse(null);

                if (eq == null) {
                    logInfo.add(String.format("\t%s, %s(%s)--%s", "REMOVED", nodeAttr.getFriendlyName(),
                            nodeAttr.getIp(), eqId));
                    return;
                }

                logInfo.add(String.format("\t%s(%s), %s(%s)--%s", eq.getImplementState(),
                        eq.getAdminState(), nodeAttr.getFriendlyName(), nodeAttr.getIp(),
                        eq.getFriendlyName()));
            });

            //check xcList in rInfo
            logInfo.add("\t   XC status,      XC FriendlyName");
            rInfo.getXcIdList().forEach(xcId -> {
                String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
                Node node = cache.getChangedPhyNode(nodeId);
                Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

                CrossConnections xc = nodeAttr.getCrossConnections().stream()
                        .filter(x -> x.getCrossConnectionId().getValue().equals(xcId))
                        .findAny().orElse(null);

                if (xc == null) {
                    logInfo.add(String.format("\t%s, %s(%s)--%s", "REMOVED", nodeAttr.getFriendlyName(),
                            nodeAttr.getIp(), xcId));
                    return;
                }

                logInfo.add(String.format("\t%s(%s), %s(%s)--%s", xc.getImplementState(),
                        xc.getAdminState(), nodeAttr.getFriendlyName(), nodeAttr.getIp(),
                        xc.getDescription()));
            });

            log.debug("debug info for tunnel, \n{}", String.join("\n", logInfo));
        } catch (Exception e) {
            log.error("when print debug info ", e);
        }
    }

    public static void printTunnelTable(ChangedObject cache, RouteInfo rInfo) {
        if (!log.isDebugEnabled()) {
            return;
        }
        try {
            ensureRouteObjectsLoaded(cache, rInfo);

            StringBuilder sb = new StringBuilder(4096);
            sb.append("debug info for tunnel\n");
            appendSummaryTable(sb, cache);
            appendTpTable(sb, cache, rInfo);
            appendEqTable(sb, cache, rInfo);
            appendXcTable(sb, cache, rInfo);

            log.debug("{}", sb);
        } catch (Exception e) {
            log.error("when print debug info ", e);
        }
    }

    private static void ensureRouteObjectsLoaded(ChangedObject cache, RouteInfo rInfo) {
        rInfo.getNodeIdList().forEach(nodeId -> {
            if (!cache.getChangedPhyNodeList().containsKey(nodeId)) {
                log.warn("debug info, node {} not in changed list", nodeId);
                cache.getChangedPhyNode(nodeId);
            }
        });
        rInfo.getLogicServerLinkIdList().forEach(linkId -> {
            if (OchLinkIdNamingRule.isOchLink(linkId)) {
                if (!cache.getChangedOchLinkList().containsKey(linkId)) {
                    log.warn("debug info, ochLink {} not in changed list", linkId);
                    cache.getChangedOchLink(linkId);
                }
            } else if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                if (!cache.getChangedSiteLinkList().containsKey(linkId)) {
                    log.warn("debug info, siteLink {} not in changed list", linkId);
                    cache.getChangedSiteLink(linkId);
                }
            } else {
                log.warn("debug info, logicServerLink {} is neither ochLink nor siteLink", linkId);
            }
        });
        rInfo.getPhyLinkIdList().forEach(linkId -> {
            if (!cache.getChangedPhyLinkList().containsKey(linkId)) {
                log.warn("debug info, phyLink {} not in changed list", linkId);
                cache.getChangedPhyLink(linkId);
            }
        });
    }

    private static void appendSummaryTable(StringBuilder sb, ChangedObject cache) {
        appendSectionHeader(sb, "SUMMARY");
        appendRow(sb, "TYPE", "STATE", "NAME", "ID");
        appendDivider(sb);

        cache.getChangedTunnelList().forEach((tunnelId, tunnel) ->
            appendRow(sb, "Tunnel", String.valueOf(tunnel.getImplementState()),
                tunnel.getFriendlyName(), tunnelId));

        cache.getChangedOchLinkList().forEach((ochLinkId, ochLink) -> {
            Och ochAttr = ochLink.getAugmentation(Link1.class).getOch();
            appendRow(sb, "OchLink", String.valueOf(ochAttr.getImplementState()), "", ochLinkId);
        });

        cache.getRemovedOchLinkIdList().forEach(ochLinkId ->
            appendRow(sb, "OchLinkRemoved", "REMOVED", "", ochLinkId));

        cache.getChangedSiteLinkList().forEach((siteLinkId, siteLink) -> {
            Site siteAttr = siteLink.getAugmentation(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                .getSite();
            appendRow(sb, "SiteLink", String.valueOf(siteAttr.getImplementState()),
                siteAttr.getFriendlyName(), siteLinkId);
        });

        cache.getChangedPhyLinkList().forEach((phyLinkId, phyLink) -> {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical phyAttr = phyLink.getAugmentation(
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                .getPhysical();
            appendRow(sb, "PhyLink", String.valueOf(phyAttr.getImplementState()),
                phyAttr.getFriendlyName(), phyLinkId);
        });

        cache.getChangedPhyNodeList().forEach((phyNodeId, phyNode) -> {
            Physical phyAttr = phyNode.getAugmentation(Node1.class).getPhysical();
            appendRow(sb, "PhyNode", String.valueOf(phyAttr.getImplementState()),
                String.format("%s(%s)", phyAttr.getFriendlyName(), phyAttr.getIp()), phyNodeId);
        });
        sb.append('\n');
    }

    private static void appendTpTable(StringBuilder sb, ChangedObject cache, RouteInfo rInfo) {
        appendSectionHeader(sb, "TP");
        appendRow(sb, "STATE", "ADMIN", "CONN", "NODE", "TP");
        appendDivider(sb);
        rInfo.getTpIdList().forEach(tpId -> {
            String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
            Node node = cache.getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

            TerminationPoint tp = node.getTerminationPoint().stream()
                .filter(x -> x.getTpId().getValue().equals(tpId))
                .findAny().orElse(null);
            if (tp == null) {
                appendRow(sb, "REMOVED", "", "", nodeName(nodeAttr), tpId);
                return;
            }

            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr = tp.getAugmentation(
                TerminationPoint1.class).getPhysical();
            appendRow(sb, String.valueOf(tpAttr.getImplementState()), String.valueOf(tpAttr.getAdminState()),
                String.valueOf(tpAttr.getConnectionStatus()), nodeName(nodeAttr), tpAttr.getFriendlyName());
        });
        sb.append('\n');
    }

    private static void appendEqTable(StringBuilder sb, ChangedObject cache, RouteInfo rInfo) {
        appendSectionHeader(sb, "EQ");
        appendRow(sb, "STATE", "ADMIN", "NODE", "EQ", "ID");
        appendDivider(sb);
        rInfo.getEqIdList().forEach(eqId -> {
            String nodeId = PhysicalEqpIdNamingRule.getNodeId(eqId);
            Node node = cache.getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

            Equipments eq = nodeAttr.getEquipments().stream()
                .filter(x -> x.getEquipmentId().equals(eqId))
                .findAny().orElse(null);
            if (eq == null) {
                appendRow(sb, "REMOVED", "", nodeName(nodeAttr), "", eqId);
                return;
            }

            appendRow(sb, String.valueOf(eq.getImplementState()), String.valueOf(eq.getAdminState()),
                nodeName(nodeAttr), eq.getFriendlyName(), eqId);
        });
        sb.append('\n');
    }

    private static void appendXcTable(StringBuilder sb, ChangedObject cache, RouteInfo rInfo) {
        appendSectionHeader(sb, "XC");
        appendRow(sb, "STATE", "ADMIN", "NODE", "DESCRIPTION", "ID");
        appendDivider(sb);
        rInfo.getXcIdList().forEach(xcId -> {
            String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
            Node node = cache.getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

            CrossConnections xc = nodeAttr.getCrossConnections().stream()
                .filter(x -> x.getCrossConnectionId().getValue().equals(xcId))
                .findAny().orElse(null);
            if (xc == null) {
                appendRow(sb, "REMOVED", "", nodeName(nodeAttr), "", xcId);
                return;
            }

            appendRow(sb, String.valueOf(xc.getImplementState()), String.valueOf(xc.getAdminState()),
                nodeName(nodeAttr), xc.getDescription(), xcId);
        });
    }

    private static String nodeName(Physical nodeAttr) {
        return String.format("%s(%s)", nodeAttr.getFriendlyName(), nodeAttr.getIp());
    }

    private static void appendSectionHeader(StringBuilder sb, String title) {
        sb.append('\n').append("== ").append(title).append(" ==\n");
    }

    private static void appendDivider(StringBuilder sb) {
        sb.append("--------------------------------------------------------------------------------\n");
    }

    private static void appendRow(StringBuilder sb, Object... cols) {
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(safe(cols[i]));
        }
        sb.append('\n');
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
