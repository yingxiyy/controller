package net.flex.dci.otc.controller.otdr.components.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.controller.otdr.components.OTDRQueryParser;
import net.flex.dci.otc.controller.otdr.components.ScanPortHelper;
import net.flex.dci.otc.controller.otdr.domain.OtsLinkTerminationPointInfo;
import net.flex.dci.otc.controller.otdr.dto.OTDRLinkQueryDto;
import net.flex.dci.otc.controller.otdr.utils.SortFilterItemUtils;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otn.db.jpa.service.dao.dto.otdr.FilterItemDto;
import net.flex.dci.otn.db.jpa.service.dao.dto.otdr.OTDRPagedQueryDto;
import net.flex.dci.otn.db.jpa.service.dao.dto.otdr.SortItemDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultsInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.provider.info.Provider;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2023/1/9 11:32
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OTDRQueryParserImpl implements OTDRQueryParser {

    private final PhyLinkDao phyLinkDao;

    private final ScanPortHelper scanPortHelper;

    @Override
    public OTDRLinkQueryDto parsePagedDto(GetOtdrResultsInput input) {
        String phyLinkId = input.getLinkId().getValue();
        log.debug("start to get otdr result paged by linkId:{}", phyLinkId);
        LinkInfo linkInfo = getRefLinkTpId(phyLinkId);
        List<SortItemDto> sortItemDtos = SortFilterItemUtils.parseSortItem(
                input.getSortInfos());
        List<FilterItemDto> filterItemDtos = SortFilterItemUtils.parseFilterItem(
                input.getFilterParam());
        Integer pageSize = input.getHowMany() == null ? 20 : input.getHowMany();
        Integer pageNum = input.getStartPos() == null ? 0 : input.getStartPos() / pageSize;
        OTDRPagedQueryDto pageQueryDto = OTDRPagedQueryDto.builder().linkId(phyLinkId)
                .sortItemDtos(sortItemDtos)
                .filterItemDtos(filterItemDtos).pageNum(pageNum).pageSize(pageSize)
                .tpIds(linkInfo.getTpIds())
                .build();
        return OTDRLinkQueryDto.builder().otdrPagedQueryDto(pageQueryDto)
                .phyLinkId(phyLinkId)
                .nodeMap(linkInfo.nodeMap)
                .provider(linkInfo.provider).build();
    }


    private LinkInfo getRefLinkTpId(String phyLinkId) {
        Link phyLink = phyLinkDao.getPhyLinkById(phyLinkId);
        if (phyLink == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the phy link is not existed");
        }
        OtsLinkTerminationPointInfo tpInfo =
                scanPortHelper.resolveOtsLinkEndpoint(phyLinkId);
        List<String> tpIds = new ArrayList<>();
        tpIds.add(tpInfo.getEdfaSourceTp());
        tpIds.add(tpInfo.getEdfaDestTp());
        Provider provider = phyLink.getAugmentation(Link1.class).getPhysical().getProvider();
        return new LinkInfo(provider, tpIds, tpInfo.getNodeMap());
    }

    @Data
    @AllArgsConstructor
    private static class LinkInfo implements Serializable {

        private Provider provider;
        private List<String> tpIds;
        private Map<String, Node> nodeMap;
    }
}
