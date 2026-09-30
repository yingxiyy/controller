package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.tunnel;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.ProtocolRate;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.dto.FilterItem;
import net.flex.dci.otc.mongo.dto.SortItem;
import net.flex.dci.otc.mongo.dto.TunnelRateInfo;
import net.flex.dci.otn.controller.nms.nms.dto.FilterDto;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.nms.dto.tunnel.TunnelBetweenSitePageResult;
import net.flex.dci.otn.controller.nms.nms.enums.NMSConvertType;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/9 16:26
 */
@Slf4j
@Component
public class TunnelRetrieveHandler extends AbstractTunnelRetrieveHandler {

    public TunnelRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public PageResult<Tunnel> retrieveAllTunnelPaged(Integer pageNum, Integer pageSize) {
        log.info("retrieve all topology tunnel ,pageNum is {},pageSize is :{}", pageNum, pageSize);
        return netconfTopology.retrieveAllTunnelPaged(pageNum, pageSize);
    }


    @Override
    public PageResult<Tunnel> retrieveAllTunnelPaged(RetrieveTopologyDto retrieveTopologyDto) {
        log.info("retrieve all topology tunnel retrieveTopology domain is:{}", retrieveTopologyDto);
        PageResult<Tunnel> pageResult = new PageResult<>();
//        FilterDto filterDto = getFilterItems(retrieveTopologyDto);
        List<FilterItem> filterItems = buildFilterItemsByPlaneId(retrieveTopologyDto,
                NMSConvertType.TUNNEL);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        int pageNum = retrieveTopologyDto.getPageNum();
        int pageSize = retrieveTopologyDto.getPageSize();
        pageResult = netconfTopology.retrieveAllTunnelPaged(pageNum, pageSize, filterItems,
                sortItems);
        return pageResult;
    }


    @Override
    public List<Tunnel> retrieveAllTunnel(RetrieveTopologyDto retrieveTopologyDto) {
        log.info("retrieve all topology tunnel retrieveTopology domain is:{}", retrieveTopologyDto);
        List<Tunnel> tunnels = new ArrayList<>();
        FilterDto filterDto = getFilterItems(retrieveTopologyDto);
        List<FilterItem> filterItems = filterItemHelper.getFilterItems(filterDto,
                NMSConvertType.TUNNEL);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);

        tunnels = netconfTopology.retrieveAllTunnel(filterItems,
                sortItems);
        return tunnels;
    }


    @Override
    public TunnelBetweenSitePageResult retrieveAllTunnelBetweenTwoSitePaged(String sourceSiteId,
            String destSiteId, RetrieveTopologyDto retrieveTopologyDto) {
        log.info("retrieve all tunnel from site :{} to site:{} retrieve item is:{}", sourceSiteId,
                destSiteId, retrieveTopologyDto);
        String subnetId = retrieveTopologyDto.getPlaneId();
        List<String> occupiedOchLinkIds = netconfTopology.getOccupiedOchLinkIdsBetweenTwoSite(
                sourceSiteId, destSiteId, subnetId);
        List<TunnelRateInfo> tunnelRateInfos = netconfTopology.retrieveAllTunnelRateInfoByOchLinkIds(
                occupiedOchLinkIds);
        BigDecimal totalBandwidth = calculateTunnelTotalBandwidth(tunnelRateInfos);
        PageResult<Tunnel> pageResult = new PageResult<>();
        List<FilterItem> filterItems = buildFilterItemsByPlaneId(retrieveTopologyDto,
                NMSConvertType.TUNNEL);
        List<SortItem> sortItems = getSortItems(retrieveTopologyDto);
        int pageNum = retrieveTopologyDto.getPageNum();
        int pageSize = retrieveTopologyDto.getPageSize();
        pageResult = netconfTopology.retrieveAllTunnelBetweenSitePaged(sourceSiteId, destSiteId,
                pageNum, pageSize, filterItems,
                sortItems);

        return TunnelBetweenSitePageResult.builder().tunnelPageResult(pageResult)
                .occupiedChannelCount((long) occupiedOchLinkIds.size())
                .totalBandwidth(totalBandwidth).build();
    }

    private BigDecimal calculateTunnelTotalBandwidth(List<TunnelRateInfo> tunnelRateInfos) {
        log.info("calculate tunnel total bandwidth the size is:{}", tunnelRateInfos.size());
        List<ProtocolRate> protocolRates = tunnelRateInfos.stream()
                .map(tunnelRateInfo -> ProtocolRate.fromProtocolString(
                        tunnelRateInfo.getSignalRate()))
                .collect(
                        Collectors.toList());
        Long totalBandwidth = protocolRates.stream().mapToLong(ProtocolRate::getRateGbps).sum();
        return new BigDecimal(totalBandwidth);
    }
}
