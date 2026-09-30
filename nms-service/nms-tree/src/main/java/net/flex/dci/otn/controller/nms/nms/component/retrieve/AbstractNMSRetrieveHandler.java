package net.flex.dci.otn.controller.nms.nms.component.retrieve;

import static net.flex.dci.otn.controller.nms.utils.Constants.MPO;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_LINK_PREFIX;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.FilterQuerySelector.FilterLogicalOp;
import net.flex.dci.otc.common.enums.FilterQuerySelector.FilterOperation;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.dto.SortItem;
import net.flex.dci.otn.controller.nms.nms.component.retrieve.filter.FilterItemHelper;
import net.flex.dci.otn.controller.nms.nms.dto.FilterDto;
import net.flex.dci.otn.controller.nms.nms.dto.FilterItemDto;
import net.flex.dci.otn.controller.nms.nms.dto.MpoTpInfo;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.dto.SortItemDto;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import net.flex.dci.otn.controller.nms.properties.equip.EquipmentTypeConfiguration;
import net.flex.dci.otn.controller.nms.utils.Constants.Plane;
import net.flex.dci.otn.controller.nms.utils.NMSUtils;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import net.flex.dci.otn.controller.nms.utils.TerminationPointUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort.Direction;

/**
 * @version 1.0
 * @date 2022/3/8 10:39
 */
@Slf4j
public abstract class AbstractNMSRetrieveHandler implements INMSRetrieveOperations {

    protected final NetconfTopology netconfTopology;
    @Autowired
    protected EquipmentTypeConfiguration equipmentTypeConfiguration;
    @Autowired
    protected FilterItemHelper filterItemHelper;

    public AbstractNMSRetrieveHandler(NetconfTopology netconfTopology) {
        this.netconfTopology = netconfTopology;
    }


    protected List<Link> getTpRefPhyLinks(String nodeId, String tpId) {
        List<Link> phyLinks = netconfTopology.getTpRefPhyLinks(tpId);

        if (tpId.contains(MPO)) {
            List<Link> mpoRelativePhyLinks = getMuxPanelPortRefLinks(nodeId, phyLinks);
            phyLinks.addAll(mpoRelativePhyLinks);
        } else {
            TerminationPoint refTp = netconfTopology.getTerminationPoint(tpId);
            PortType portType = refTp.getAugmentation(TerminationPoint1.class).getPhysical()
                    .getPortType();
            if (portType.equals(PortType.OTUClient)) {
                String lineTpId = getRefLineTpId(nodeId, tpId);
                if (null != lineTpId) {
                    phyLinks.addAll(netconfTopology.getTpRefPhyLinks(lineTpId));
                }
            }


        }
        return phyLinks;
    }

    private List<Link> getMuxPanelPortRefLinks(String nodeId, List<Link> phyLinks) {
        log.debug("get mux panel port relative phy links,node :{}", nodeId);
        List<MpoTpInfo> mpoTpInfos = new ArrayList<>();
        phyLinks.forEach(link -> {
            String sourceTp = link.getSource().getSourceTp().getValue();
            String destTp = link.getDestination().getDestTp().getValue();
            Equipments sourceEq = getEquipment(nodeId, sourceTp);
            Equipments destEq = getEquipment(nodeId, destTp);
            if (equipmentTypeConfiguration.isMuxMpoSupported(sourceEq.getEquipType())) {
                mpoTpInfos.add(MpoTpInfo.builder().tpId(sourceTp).equipType(sourceEq.getEquipType())
                        .build());
            }
            if (equipmentTypeConfiguration.isMuxMpoSupported(destEq.getEquipType())) {
                mpoTpInfos.add(
                        MpoTpInfo.builder().tpId(destTp).equipType(destEq.getEquipType()).build());
            }
        });
        List<String> refTpIds = TerminationPointUtils.getMpoTpRefTps(mpoTpInfos);
        return netconfTopology.getTpsRefPhyLinks(refTpIds);
    }

    private String getRefLineTpId(String nodeId, String terminationPointId) {
        log.debug("get line port from client port:{} nodeId:{}", terminationPointId, nodeId);
        Node ne = netconfTopology.getPhyNode(nodeId);
        List<CrossConnections> crossConnections = ne.getAugmentation(Node1.class).getPhysical()
                .getCrossConnections();
        CrossConnections cLCrossConnection = crossConnections.stream().filter(crossConnection -> {
            List<String> sourceTpIds = crossConnection.getSourceTp().stream().map(
                    CrossConnectionTp::getTpRef).map(Uri::getValue).collect(
                    Collectors.toList());
            List<String> destTpIds = crossConnection.getDestinationTp().stream()
                    .map(CrossConnectionTp::getTpRef).map(Uri::getValue).collect(
                            Collectors.toList());
            return sourceTpIds.contains(terminationPointId) || destTpIds.contains(
                    terminationPointId);
        }).findAny().orElse(null);
        if (cLCrossConnection == null) {
            log.error("data is invalid ,client tp id :{} cannot found the ref line port ",
                    terminationPointId);
            return null;
        }
        String sourceTp = cLCrossConnection.getSourceTp().get(0).getTpRef().getValue();
        String destinationTp = cLCrossConnection.getDestinationTp().get(0).getTpRef().getValue();
        return sourceTp.equals(terminationPointId) ? destinationTp : sourceTp;
    }

    private String getMuxPanelPort(String nodeId, List<Link> phyLinks) {
        log.debug("start to find ref mpo ");
        AtomicReference<String> mpoPort = new AtomicReference<>();
        phyLinks.stream().forEach(link -> {
            String sourceTp = link.getSource().getSourceTp().getValue();
            String destTp = link.getDestination().getDestTp().getValue();
            Equipments sourceEq = getEquipment(nodeId, sourceTp);
            Equipments destEq = getEquipment(nodeId, destTp);
            if (sourceEq.getEquipType().equals(EquipType.MUXPANEL)) {
                mpoPort.set(sourceTp);
            }
            if (destEq.getEquipType().equals(EquipType.MUXPANEL)) {
                mpoPort.set(destTp);
            }
        });
        return mpoPort.get();
    }

    protected List<String> getSiteLinkRefPhyLinkIds(Link siteLink) {
        List<String> refPhyLinkIds = siteLink.getSupportingLink().stream()
                .map(supportingLink -> supportingLink.getLinkRef().getValue())
                .collect(Collectors.toList());
        return refPhyLinkIds;
    }


    private Equipments getEquipment(String nodeId, String tpId) {
        String eqId = NMSUtils.getEquipIdFromTpId(tpId);
        return netconfTopology.getEquipment(nodeId, eqId);
    }

    protected List<String> getTunnelRefPhyLinkIds(Tunnel tunnel) {
        Set<String> ochLinkIds = tunnel.getSupportingLink().stream()
                .map(supportingLink -> supportingLink.getLinkRef().getValue())
                .collect(Collectors.toSet());
        log.debug("ref och link is :{}", ochLinkIds);
        List<Link> ochLinks = netconfTopology.getOchLinksByIds(ochLinkIds);
        List<String> phyLinksId = new ArrayList<>();
        for (Link ochLink : ochLinks) {
            List<SupportingLink> supportLinks = ochLink.getSupportingLink();
            for (SupportingLink link : supportLinks) {
                if (link.getLinkRef().getValue().startsWith(SITE_LINK_PREFIX)) {
                    Link siteLink = netconfTopology.getSiteLink(link.getLinkRef().getValue());
                    phyLinksId.addAll(siteLink.getSupportingLink().stream()
                            .map(supportingLink -> supportingLink.getLinkRef().getValue())
                            .collect(Collectors.toList()));
                } else {
                    phyLinksId.add(link.getLinkRef().getValue());
                }
            }
        }
        return phyLinksId;
    }

    protected List<SortItem> getSortItems(RetrieveTopologyDto retrieveTopologyDto) {
//        log.debug("get sort items for sort item");
        List<SortItemDto> sortItems = retrieveTopologyDto.getSortItem();
        if (sortItems == null) {
            return new ArrayList<>();
        }
        List<SortItem> dbSortItem = sortItems.stream().map(sortItemDto -> SortItem.builder()
                .direction(sortItemDto.isAscending() ? Direction.ASC : Direction.DESC)
                .sortName(sortItemDto.getSortName())
                .build()).collect(Collectors.toList());
        return dbSortItem;
    }

//    protected List<FilterItem> getFilterItems(RetrieveTopologyDto retrieveTopologyDto) {
//        log.debug("get sort items for sort item");
//
//        List<FilterItemDto> filterItemDtos = retrieveTopologyDto.getFilterItem();
//        if (filterItemDtos == null || filterItemDtos.isEmpty()) {
//            return new ArrayList<>();
//        }
//        List<FilterItem> dbSortItem = filterItemDtos.stream()
//                .map(filterItemDto -> FilterItem.builder()
//                        .filter(filterItemDto.getFilter())
//                        .filterItem(filterItemDto.getFilterItem())
//                        .filterOp(filterItemDto.getFilterOp())
//                        .logicalOp(filterItemDto.getLogicalOp())
//                        .build()).collect(Collectors.toList());
//        return dbSortItem;
//    }


    protected FilterDto getFilterItems(RetrieveTopologyDto retrieveTopologyDto) {
//        log.debug("get sort items for sort item");

        List<FilterItemDto> filterItemDtos = retrieveTopologyDto.getFilterItem();
        if (filterItemDtos == null || filterItemDtos.isEmpty()) {
            return FilterDto.builder().additionalFilterItems(new ArrayList<>())
                    .directFilterItems(new ArrayList<>()).build();
        }
        List<FilterItem> directFilters = filterItemDtos.stream()
                .filter(filterItemDto -> !filterItemDto.getIsAdditional())
                .map(filterItemDto -> FilterItem.builder()
                        .filter(filterItemDto.getFilter())
                        .filterItem(filterItemDto.getFilterItem())
                        .filterOp(filterItemDto.getFilterOp())
                        .logicalOp(filterItemDto.getLogicalOp())
                        .build()).collect(Collectors.toList());
        List<FilterItem> additionalFilters = filterItemDtos.stream()
                .filter(FilterItemDto::getIsAdditional)
                .map(filterItemDto -> FilterItem.builder()
                        .filter(filterItemDto.getFilter())
                        .filterItem(filterItemDto.getFilterItem())
                        .filterOp(filterItemDto.getFilterOp())
                        .logicalOp(filterItemDto.getLogicalOp())
                        .build()).collect(Collectors.toList());
        return FilterDto.builder().additionalFilterItems(additionalFilters)
                .directFilterItems(directFilters).build();
    }


    protected List<FilterItem> buildFilterItemsByPlaneId(RetrieveTopologyDto retrieveTopologyDto,
            NMSConvertType convertType) {
        FilterDto filterDto = getFilterItems(retrieveTopologyDto);
        List<FilterItem> filterItems = filterItemHelper.getFilterItems(filterDto, convertType);
        FilterItem planeFilterItems = getPlaneFilterItem(retrieveTopologyDto, convertType);
        if (planeFilterItems != null) {
            if (!filterItems.isEmpty()) {
                planeFilterItems.setLogicalOp(FilterLogicalOp.AND);
            }
            filterItems.add(planeFilterItems);
        }
        return filterItems;
    }

    protected FilterItem getPlaneFilterItem(RetrieveTopologyDto retrieveTopologyDto,
            NMSConvertType nmsConvertType) {
        if (retrieveTopologyDto.getPlaneId() == null) {
            log.debug("current filter item plane id is null,do nothing");
            return null;
        }
        String planeId = retrieveTopologyDto.getPlaneId();
        List<String> descendants = netconfTopology.getPlaneDescendants(planeId);
        String filterValues = String.join(",", descendants);

        String filterKey = getPlaneFilterKeyByNmsConvertType(nmsConvertType);

        FilterItem filterItem = FilterItem.builder().filter(filterKey)
                .filterItem(filterValues).filterOp(
                        FilterOperation.IN).build();
        return filterItem;
    }

    private String getPlaneFilterKeyByNmsConvertType(NMSConvertType nmsConvertType) {
        switch (nmsConvertType) {
            case PHY_NODE:
                return Plane.PHY_NODE_FILTER_PLANE_PATH;
            case PHY_LINK:
                return Plane.PHY_LINK_FILTER_PLANE_PATH;
            case TUNNEL:
                return Plane.TUNNEL_FILTER_PLANE_PATH;
            case SITE_LINK:
                return Plane.SITE_LINK_FILTER_PLANE_PATH;
            case OCH_LINK:
                return Plane.OCH_LINK_FILTER_PLANE_PATH;
            default:
                throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                        "get plane filter key is not support the convertType:" + nmsConvertType);
        }

    }


}
