package net.flex.dci.otn.controller.nms.nms.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.INMSRetrieveOperations;
import net.flex.dci.otn.controller.nms.nms.enums.RetrieveType;

/**
 * @version 1.0
 * @date 2022/3/6 20:23
 */
@Data
@Builder
@AllArgsConstructor
@Slf4j
public class RetrieveTopologyDto {

    private String topologyRef;

    private String nodeRef;

    private String linkRef;

    private String equipRef;

    private String rackRef;

    private String tpRef;

    private String tunnelRef;

    private String planeId;

    private RetrieveType retrieveType;

    private Integer pageNum;

    private Integer pageSize;

    private List<SortItemDto> sortItem;

    private List<FilterItemDto> filterItem;

    private List<String> nodeRefs;

//    private static final NetconfTopology netconfTopology;
//
//
//    static {
//        netconfTopology = SpringBeanFinder.getBean(NetconfTopology.class);
//    }

//    public INMSRetrieveOperations nmsRetrieveOperations() {
//        log.info(
//                "retrieve topology on conditional, topology:{}, node:{}, rack:{}, equip:{}, tp:{}, link:{}, tunnel:{}",
//                this.getTopologyRef() == null ? "" : this.getTopologyRef(),
//                this.getNodeRef() == null ? " " : this.getNodeRef(),
//                this.getRackRef() == null ? " " : this.getRackRef(),
//                this.getEquipRef() == null ? " " : this.getEquipRef(),
//                this.getTpRef() == null ? " " : this.getTpRef(),
//                this.getLinkRef() == null ? " " : this.getLinkRef(),
//                this.getTunnelRef() == null ? " " : this.getTunnelRef());
//        if (topologyRef == null) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "topology-ref is mandatory.");
//        }
//        INMSRetrieveOperations operations = new DefaultRetrieveOperations();
//        if (retrieveType.equals(RetrieveType.SITE_NODE) || retrieveType.equals(
//                RetrieveType.PHY_NODE)) {
//            operations = nmsNodeRetrieveOperations();
//        } else {
//            operations = nmsConnectionOperations();
//        }
//        return operations;
//    }
//
//    /**
//     * ochlink phylink sitelink viewlink siteTunnel
//     *
//     * @return
//     */
//    private INMSRetrieveOperations nmsConnectionOperations() {
//        INMSRetrieveOperations operations = null;
//        if (retrieveType.equals(RetrieveType.PHY_LINK)) {
//            operations = nmsPhyLinkRetrieveOperation();
//        } else if (retrieveType.equals(RetrieveType.TUNNEL)) {
//            operations = nmsTunnelRetrieveOperation();
//        } else if (retrieveType.equals(RetrieveType.SITE_LINK)) {
//            operations = nmsSiteLinkRetrieveOperation();
//        } else if (retrieveType.equals(RetrieveType.OCH_LINK)) {
//            operations = nmsOchLinkRetrieveOperation();
//        } else {
//            operations = new DefaultRetrieveOperations();
//        }
//
//        return operations;
//    }
//
//    private INMSRetrieveOperations nmsOchLinkRetrieveOperation() {
//        log.debug("retrieve och link ");
//        INMSRetrieveOperations operations = new DefaultRetrieveOperations();
//        if (topologyRef.equals(OCH_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            operations = new OchLinkRetrieveHandler(netconfTopology);
//        }
//        return operations;
//    }
//
//    /**
//     * site link retrieve method selector
//     *
//     * @return
//     */
//    private INMSRetrieveOperations nmsSiteLinkRetrieveOperation() {
//        log.debug("retrieve site link ");
//        INMSRetrieveOperations operations = new DefaultRetrieveOperations();
//        if (topologyRef.equals(SITE_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            operations = new SiteLinkRetrieveHandler(netconfTopology);
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //node-ref tunnel
//            operations = new NodeRefSiteLinkRetrieveHandler(netconfTopology, topologyRef, nodeRef);
//
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(SITE_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isNotBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //rack ref tunnel
//            operations = new RackRefSiteLinkRetrieveHandler(netconfTopology, nodeRef, rackRef);
//
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(PHY_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isNotBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            // equipment ref tunnel
//            operations = new EquipRefSiteLinkRetrieveHandler(netconfTopology, nodeRef, equipRef);
//
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isNotBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //tp ref tunnel retrieve
//            operations = new TpRefSiteLinkRetrieveHandler(netconfTopology, topologyRef, nodeRef,
//                    tpRef);
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && StringUtils.isNotBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //link ref tunnel
//            operations = new LinkRefSiteLinkRetrieveHandler(netconfTopology, topologyRef, linkRef);
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(SITE_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isNotBlank(tunnelRef)) {
//            // site tunnel ref phy link retrieve
////            operations = new TunnelRefPhyLinkRetrieveHandler(netconfTopology, tunnelRef);
//            operations = new TunnelRefSiteLinkRetrieveHandler(netconfTopology, tunnelRef);
//        }
//
//        return operations;
//    }
//
//    /**
//     * tunnel retrieve operations selector
//     *
//     * @return
//     */
//    private INMSRetrieveOperations nmsTunnelRetrieveOperation() {
//        log.debug("retrieve tunnel ");
//        INMSRetrieveOperations operations = new DefaultRetrieveOperations();
//        if (topologyRef.equals(SITE_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            operations = new TunnelRetrieveHandler(netconfTopology);
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //node-ref tunnel
//            operations = new NodeRefTunnelRetrieveHandler(netconfTopology, topologyRef, nodeRef);
//
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(SITE_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isNotBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //rack ref tunnel
//            operations = new RackRefTunnelRetrieveHandler(netconfTopology, nodeRef, rackRef);
//
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(PHY_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isNotBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            // equipment ref tunnel
//            operations = new EquipRefTunnelRetrieveHandler(netconfTopology, nodeRef, equipRef);
//
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isNotBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //tp ref tunnel retrieve
//            operations = new TpRefTunnelRetrieveHandler(netconfTopology, topologyRef, nodeRef,
//                    tpRef);
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && StringUtils.isNotBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //link ref tunnel
//            operations = new LinkRefTunnelRetrieveHandler(netconfTopology, topologyRef, linkRef);
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(SITE_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isNotBlank(tunnelRef)) {
//            // site tunnel ref phy link retrieve
////            operations = new TunnelRefPhyLinkRetrieveHandler(netconfTopology, tunnelRef);
//            operations = new DefaultRetrieveOperations();
//        }
//
//        return operations;
//    }
//
//    private INMSRetrieveOperations nmsPhyLinkRetrieveOperation() {
//        log.debug("retrieve phy link");
//        INMSRetrieveOperations operations = new DefaultRetrieveOperations();
//        //phy-link
//        if (this.getTopologyRef().equals(PHY_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            operations = new PhyLinkRetrieveHandler(netconfTopology);
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //node-ref phy link
//            operations = new NodeRefPhyLinkRetrieveHandler(netconfTopology, topologyRef, nodeRef);
//
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(SITE_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isNotBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //rack ref phy link
//            operations = new RackRefPhyLinkRetrieveHandler(netconfTopology, nodeRef, rackRef);
//
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(PHY_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isNotBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            // equipment ref phy link retrieve
//            operations = new EquipRefPhyLinkRetrieveHandler(netconfTopology, nodeRef, equipRef);
//
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isNotBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //tp ref phy link retrieve
//            operations = new TpRefPhyLinkRetrieveHandler(netconfTopology, topologyRef, nodeRef,
//                    tpRef);
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(SITE_TOPO_KEY)
//                && StringUtils.isNotBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //site link ref phy link retrieve
//            operations = new SiteLinkRefPhyLinkRetrieveHandler(netconfTopology, linkRef);
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(SITE_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isNotBlank(tunnelRef)) {
//            // site tunnel ref phy link retrieve
//            operations = new TunnelRefPhyLinkRetrieveHandler(netconfTopology, tunnelRef);
//        }
//
//        return operations;
//    }
//
//    public INMSRetrieveOperations nmsNodeRetrieveOperations() {
//        log.debug("start to retrieve node");
//        INMSRetrieveOperations operations = null;
//        if (retrieveType.equals(RetrieveType.PHY_NODE)) {
//            operations = nmsPhyNodeRetrieveOperations();
//        } else if (retrieveType.equals(RetrieveType.SITE_NODE)) {
//            operations = nmsSiteNodeRetrieveOperations();
//        }
//
//        return operations;
//    }
//
//    private INMSRetrieveOperations nmsSiteNodeRetrieveOperations() {
//        log.debug("retrieve site node");
//        INMSRetrieveOperations operations = new DefaultRetrieveOperations();
//        if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(SITE_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            operations = new SiteNodeRetrieveHandler(netconfTopology);
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(SITE_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isNotBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //rack ref phy link
//            operations = new RackRefNodeRetrieveHandler(netconfTopology, nodeRef, rackRef);
//
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(PHY_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isNotBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            // equipment ref phy link retrieve
//            operations = new EquipRefNodeRetrieveHandler(netconfTopology, nodeRef, equipRef);
//
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isNotBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //tp ref phy link retrieve
//            operations = new TpRefNodeRetrieveHandler(netconfTopology, topologyRef, nodeRef,
//                    tpRef);
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && StringUtils.isNotBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //site link ref phy link retrieve
//            operations = new LinkRefSiteNodeRetrieveHandler(netconfTopology, topologyRef, linkRef);
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(SITE_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isNotBlank(tunnelRef)) {
//            // site tunnel ref phy link retrieve
//            operations = new TunnelRefSiteNodeRetrieveHandler(netconfTopology, tunnelRef);
//        }
//        return operations;
//    }
//
//
//    /**
//     * phy node retrieve handler phyNode phyTp siteNode siterack sitetp phyEquipment
//     *
//     * @return
//     */
//    public INMSRetrieveOperations nmsPhyNodeRetrieveOperations() {
//        log.debug("retrieve phy node");
//        INMSRetrieveOperations operations = new DefaultRetrieveOperations();
//        //retrieve node paged
//        if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(PHY_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            operations = new PhyNodeRetrieveHandler(netconfTopology);
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(SITE_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //node-ref phy link
//            operations = new SiteNodeRefNodeRetrieveHandler(netconfTopology, nodeRef);
//
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(SITE_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isNotBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //rack ref phy link
//            operations = new RackRefNodeRetrieveHandler(netconfTopology, nodeRef, rackRef);
//
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(PHY_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isNotBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            // equipment ref phy link retrieve
//            operations = new EquipRefNodeRetrieveHandler(netconfTopology, nodeRef, equipRef);
//
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isNotBlank(nodeRef)
//                && StringUtils.isNotBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //tp ref phy link retrieve
//            operations = new TpRefNodeRetrieveHandler(netconfTopology, topologyRef, nodeRef,
//                    tpRef);
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && StringUtils.isNotBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isBlank(tunnelRef)) {
//            //site link ref phy link retrieve
//            operations = new LinkRefNodeRetrieveHandler(netconfTopology, topologyRef, linkRef);
//        } else if (StringUtils.isNotBlank(topologyRef)
//                && topologyRef.equals(SITE_TOPO_KEY)
//                && StringUtils.isBlank(linkRef)
//                && StringUtils.isBlank(equipRef)
//                && StringUtils.isBlank(rackRef)
//                && StringUtils.isBlank(nodeRef)
//                && StringUtils.isBlank(tpRef)
//                && StringUtils.isNotBlank(tunnelRef)) {
//            // site tunnel ref phy link retrieve
//            operations = new TunnelRefNodeRetrieveHandler(netconfTopology, tunnelRef);
//        }
//
//        return operations;
//    }

    private INMSRetrieveOperations linkRefNodeOperationSelect(String topologyRef) {
//        INMSOperations operations = null;
//        if (topologyRef.equals(Site_Topo_Key)) {
//
//        } else if (topologyRef.equals(PHY_TOPO_KEY)) {
//
//        } else if (topologyRef.equals(OCH_TOPO_KEY)) {
//
//        } else {
//            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
//                    "not supported parameter to retrieve");
//        }
////                ntNodes = new SiteLink(netconfTopology).getSiteNodes(topologyRef, linkRef);
////            } else if (topologyRef.getValue().contains(TopoNameConstants.Phy_Topo_Key)) {
////                ntNodes = new PhyLink(netconfTopology).getSiteNodes(topologyRef, linkRef);
////            } else if (topologyRef.getValue().contains(TopoNameConstants.Och_Topo_Key)) {
////                ntNodes = new OchLink(netconfTopology).getSiteNodes(topologyRef, linkRef);
////            } else {
////                throw new Exception(
////                        "not supported parameter compose.");
////            }
//        return operations;
        return null;
    }

    private INMSRetrieveOperations tpRefNodeOperationsSelect(String topologyRef) {
//        INMSRetrieveOperations operations = new DefaultRetrieveOperations();
//        if (topologyRef.equals(SITE_TOPO_KEY)) {
//            operations = new SiteTp();
//        } else if (topologyRef.equals(PHY_TOPO_KEY)) {
//            operations = new PhyTp();
//        }
//        return operations;
        return null;
    }

}
