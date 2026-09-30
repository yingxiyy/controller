package net.flex.dci.otn.controller.nms.utils;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.dto.FilterItemDto;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.dto.SortItemDto;
import net.flex.dci.otn.controller.nms.nms.enums.RetrieveType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.common.type.rev220821.PagedQueryParams;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.common.type.rev220821.SortQueryParams;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetOchLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetOchLinkPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyLinkPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyNodeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyNodePagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteLinkPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteNodeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteNodePagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelBetweenSitePagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTunnelPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.NmsQueryAllParams;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 2022/3/8 10:55
 */
@Slf4j
public class RetrieveElementExtractor {


    /**
     * EXTRACT PARAM FOR RETRIEVE CONNECTION
     *
     * @param rpcInput
     * @return
     */
    public static RetrieveTopologyDto connectionExtract(DataObject rpcInput) {
        RetrieveType retrieveType = getLinkRetrieveType(rpcInput);
        NmsQueryAllParams input = (NmsQueryAllParams) rpcInput;
        String topologyRef =
                input.getTopologyRef() == null ? null : input.getTopologyRef().getValue();
        String nodeRef = input.getNodeRef() == null ? null : input.getNodeRef().getValue();
        String rackRef = input.getRackRef() == null ? null : input.getRackRef();
        String equipRef = input.getEquipmentRef() == null ? null : input.getEquipmentRef();
        String tpRef = input.getTpRef() == null ? null : input.getTpRef().getValue();
        String linkRef = input.getLinkRef() == null ? null : input.getLinkRef().getValue();
        String tunnelRef = input.getTunnelRef() == null ? null : input.getTunnelRef();
        String planeId = input.getPlaneId() == null ? null : input.getPlaneId();
//        if (!StringUtils.hasText(planeId)) {
//            planeId = GLOBAL_ROOT_NODE_ID;
//        }
        RetrieveTopologyDto retrieveTopologyDto = RetrieveTopologyDto.builder()
                .retrieveType(retrieveType)
                .equipRef(equipRef)
                .linkRef(linkRef)
                .rackRef(rackRef)
                .nodeRef(nodeRef)
                .planeId(planeId)
                .topologyRef(topologyRef)
                .tunnelRef(tunnelRef)
                .tpRef(tpRef)
                .build();
        if (rpcInput instanceof PagedQueryParams) {
            PagedQueryParams params = (PagedQueryParams) rpcInput;
            int pageSize = params.getHowMany() == null ? 20 : params.getHowMany();
            int pageNum =
                    params.getStartPos() == null ? 1 : (params.getStartPos() + 1) / pageSize + 1;
            retrieveTopologyDto.setPageNum(pageNum);
            retrieveTopologyDto.setPageSize(pageSize);
            String filter = getFilterString(rpcInput);
            List<FilterItemDto> filterItems = FilterItemsUtils.extractFilterItems(filter);
            retrieveTopologyDto.setFilterItem(filterItems);
        }
        if (rpcInput instanceof SortQueryParams) {
            SortQueryParams sortQueryParams = (SortQueryParams) rpcInput;
            if (sortQueryParams.getSortInfos() != null) {
                List<SortItemDto> sortItems = sortQueryParams.getSortInfos().stream()
                        .map(sortInfo -> SortItemDto.builder().sortName(sortInfo.getSortName())
                                .ascending(sortInfo.isAscending()).build()).collect(
                                Collectors.toList());
                retrieveTopologyDto.setSortItem(sortItems);
            }
        }
        return retrieveTopologyDto;
    }


    /**
     * EXTRACT RETRIEVE NODE PARAM
     *
     * @param rpcInput
     * @param <T>
     * @return
     */
    public static <T> RetrieveTopologyDto nodeExtract(DataObject rpcInput) {
        RetrieveType retrieveType = getNodeRetrieveType(rpcInput);
        NmsQueryAllParams input = (NmsQueryAllParams) rpcInput;

        String topologyRef =
                input.getTopologyRef() == null ? null : input.getTopologyRef().getValue();
        String nodeRef = input.getNodeRef() == null ? null : input.getNodeRef().getValue();
        String rackRef = StringUtils.hasLength(input.getRackRef()) ? input.getRackRef() : null;
        String equipRef =
                StringUtils.hasLength(input.getEquipmentRef()) ? input.getEquipmentRef() : null;
        String tpRef = input.getTpRef() == null ? null : input.getTpRef().getValue();
        String linkRef = input.getLinkRef() == null ? null : input.getLinkRef().getValue();
        String tunnelRef =
                StringUtils.hasLength(input.getTunnelRef()) ? input.getTunnelRef() : null;
        List<String> nodeRefs = CollectionUtils.isEmpty(input.getNodeRefs()) ? null
                : input.getNodeRefs().stream().map(Uri::getValue).collect(
                        Collectors.toList());
        String planeId = StringUtils.hasLength(input.getPlaneId()) ? input.getPlaneId() : null;
        RetrieveTopologyDto retrieveTopologyDto = RetrieveTopologyDto.builder()
                .retrieveType(retrieveType)
                .equipRef(equipRef)
                .linkRef(linkRef)
                .rackRef(rackRef)
                .nodeRef(nodeRef)
                .topologyRef(topologyRef)
                .tunnelRef(tunnelRef)
                .nodeRefs(nodeRefs)
                .planeId(planeId)
                .tpRef(tpRef)
                .build();
        if (rpcInput instanceof PagedQueryParams) {
            PagedQueryParams params = (PagedQueryParams) rpcInput;
            int pageSize = params.getHowMany() == null ? 20 : params.getHowMany();
            int pageNum =
                    params.getStartPos() == null ? 1 : (params.getStartPos() + 1) / pageSize + 1;
            retrieveTopologyDto.setPageNum(pageNum);
            retrieveTopologyDto.setPageSize(pageSize);
            String filter = getFilterString(rpcInput);
            List<FilterItemDto> filterItems = FilterItemsUtils.extractFilterItems(filter);
            retrieveTopologyDto.setFilterItem(filterItems);
        }
        //sort item add
        if (rpcInput instanceof SortQueryParams) {
            SortQueryParams sortQueryParams = (SortQueryParams) rpcInput;
            if (sortQueryParams.getSortInfos() != null) {
                List<SortItemDto> sortItems = sortQueryParams.getSortInfos().stream()
                        .map(sortInfo -> SortItemDto.builder().sortName(sortInfo.getSortName())
                                .ascending(sortInfo.isAscending()).build()).collect(
                                Collectors.toList());
                retrieveTopologyDto.setSortItem(sortItems);
            }
        }
        return retrieveTopologyDto;
    }


    private static RetrieveType getLinkRetrieveType(DataObject rpcInput) {
        RetrieveType retrieveType = RetrieveType.PHY_LINK;
        if (rpcInput instanceof GetOchLinkInput || rpcInput instanceof GetOchLinkPagedInput) {
            retrieveType = RetrieveType.OCH_LINK;
        } else if (rpcInput instanceof GetSiteLinkInput
                || rpcInput instanceof GetSiteLinkPagedInput) {
            retrieveType = RetrieveType.SITE_LINK;
        } else if (rpcInput instanceof GetPhyLinkInput
                || rpcInput instanceof GetPhyLinkPagedInput) {
            retrieveType = RetrieveType.PHY_LINK;
        } else if (rpcInput instanceof GetTunnelInput || rpcInput instanceof GetTunnelPagedInput
                || rpcInput instanceof GetTunnelBetweenSitePagedInput) {
            retrieveType = RetrieveType.TUNNEL;
        }

        return retrieveType;
    }

    private static RetrieveType getNodeRetrieveType(DataObject rpcInput) {
        RetrieveType retrieveType = RetrieveType.PHY_NODE;
        if (rpcInput instanceof GetPhyNodeInput || rpcInput instanceof GetPhyNodePagedInput) {
            retrieveType = RetrieveType.PHY_NODE;
        } else if (rpcInput instanceof GetSiteNodeInput
                || rpcInput instanceof GetSiteNodePagedInput) {
            retrieveType = RetrieveType.SITE_NODE;
        }

        return retrieveType;
    }

    private static String getFilterString(DataObject rpcInput) {
        String filter = "";
        if (rpcInput instanceof GetOchLinkPagedInput) {
            filter = ((GetOchLinkPagedInput) rpcInput).getFilter();
        } else if (rpcInput instanceof GetSiteLinkPagedInput) {
            filter = ((GetSiteLinkPagedInput) rpcInput).getFilter();
        } else if (rpcInput instanceof GetPhyLinkPagedInput) {
            filter = ((GetPhyLinkPagedInput) rpcInput).getFilter();
        } else if (rpcInput instanceof GetTunnelPagedInput) {
            filter = ((GetTunnelPagedInput) rpcInput).getFilter();
        } else if (rpcInput instanceof GetPhyNodePagedInput) {
            filter = ((GetPhyNodePagedInput) rpcInput).getFilter();
        } else if (rpcInput instanceof GetSiteNodePagedInput) {
            filter = ((GetSiteNodePagedInput) rpcInput).getFilter();
        }
        return filter;
    }

}
