package net.flex.dci.otn.controller.nms.nms.component.retrieve.connections.sitelink;

import static net.flex.dci.otn.controller.nms.utils.Constants.OCH_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_VIEW_TOPO_KEY;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/3/11 14:52
 */
@Slf4j
@Component
public class LinkRefSiteLinkRetrieveHandler extends AbstractSiteLinkRetrieveHandler {

//    private String topologyRef;
//
//    private String linkId;

    public LinkRefSiteLinkRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
//        this.topologyRef = topology;
//        this.linkId = linkId;
    }

    @Override
    public PageResult<Link> retrieveAllSiteLinkPaged(Integer pageNum, Integer pageSize) {
        return PageResult.<Link>builder().build();
    }

    @Override
    public PageResult<Link> retrieveAllSiteLinkPaged(RetrieveTopologyDto retrieveTopologyDto) {
        String topologyRef = retrieveTopologyDto.getTopologyRef();
        String linkId = retrieveTopologyDto.getLinkRef();
        log.info(
                "retrieve all site link by condition ,topology :{},link id :{}",
                topologyRef, linkId);

        if (topologyRef.equals(PHY_TOPO_KEY)) {
            return retrievePhyLinkRefSiteLinkPaged(linkId, retrieveTopologyDto);
        } else if (topologyRef.equals(OCH_TOPO_KEY)) {
            return retrieveOchLinkRefSiteLinkPaged(linkId, retrieveTopologyDto);
        } else if (topologyRef.equals(SITE_TOPO_KEY)) {
            Link siteLink = netconfTopology.getSiteLink(linkId);
            if (siteLink == null) {
                throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                        "can not found the site link :" + linkId);
            }
            return PageResult.<Link>builder().list(Collections.singletonList(siteLink)).total(1l)
                    .pageSize(retrieveTopologyDto.getPageSize())
                    .pageNum(retrieveTopologyDto.getPageNum()).build();
        } else if (topologyRef.equals(SITE_VIEW_TOPO_KEY)) {
            return retrieveSiteViewLinkRefSiteLinkPaged(linkId, retrieveTopologyDto);
        } else {
            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                    "can not support the topology :" + topologyRef + " site link retrieve");
        }
    }

    private PageResult<Link> retrieveSiteViewLinkRefSiteLinkPaged(String linkId,
            RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("retrieve with site view link");
        Link siteViewLink = netconfTopology.getViewLink(linkId);
        if (siteViewLink == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can not find the site view link " + linkId);
        }
        List<String> siteLinkIds = siteViewLink.getSupportingLink().stream()
                .map(supportingLink -> supportingLink.getLinkRef().getValue()).collect(
                        Collectors.toList());
        return retrieveAllSiteLinkByIdsPaged(siteLinkIds, retrieveTopologyDto);
    }

    private PageResult<Link> retrieveOchLinkRefSiteLinkPaged(String linkId,
            RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("retrieve with och link");
        Link ochLink = netconfTopology.getOchLink(linkId);
        if (ochLink == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can not find the och link :" + linkId);
        }
        List<String> siteLinkIds = getOchRefSiteLinkIds(Collections.singletonList(ochLink));
        return retrieveAllSiteLinkByIdsPaged(siteLinkIds, retrieveTopologyDto);
    }

    private PageResult<Link> retrievePhyLinkRefSiteLinkPaged(String linkId,
            RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("retrieve with phy link");
        Link phyLink = netconfTopology.getPhyLink(linkId);
        if (null == phyLink) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can not find the phy link :" + linkId);
        }
        Link1 physical = phyLink.getAugmentation(Link1.class);
        List<String> siteLinkIds = physical.getPhysical().getSupportedLink().stream()
                .filter(supportedLink -> supportedLink.getTopologyRef().getValue()
                        .equals(SITE_TOPO_KEY))
                .map(supportedLink -> supportedLink.getLinkRef().getValue()).collect(
                        Collectors.toList());
        return retrieveAllSiteLinkByIdsPaged(siteLinkIds, retrieveTopologyDto);
    }

    private PageResult<Link> retrieveSiteViewLinkRefSiteLinkPaged(String linkId, Integer pageNum,
            Integer pageSize) {
        log.debug("retrieve with site view link");
        Link siteViewLink = netconfTopology.getViewLink(linkId);
        if (siteViewLink == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can not find the site view link " + linkId);
        }
        List<String> siteLinkIds = siteViewLink.getSupportingLink().stream()
                .map(supportingLink -> supportingLink.getLinkRef().getValue()).collect(
                        Collectors.toList());
        return netconfTopology.retrieveAllSiteLinkByIdsPaged(siteLinkIds, pageNum, pageSize);
    }

    private PageResult<Link> retrieveOchLinkRefSiteLinkPaged(String linkId, Integer pageNum,
            Integer pageSize) {
        log.debug("retrieve with och link");
        Link ochLink = netconfTopology.getOchLink(linkId);
        if (ochLink == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can not find the och link :" + linkId);
        }
        List<String> siteLinkIds = getOchRefSiteLinkIds(Collections.singletonList(ochLink));
        return netconfTopology.retrieveAllSiteLinkByIdsPaged(siteLinkIds, pageNum, pageSize);
    }

    private PageResult<Link> retrievePhyLinkRefSiteLinkPaged(String linkId, Integer pageNum,
            Integer pageSize) {
        log.debug("retrieve with phy link");
        Link phyLink = netconfTopology.getPhyLink(linkId);
        if (null == phyLink) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can not find the phy link :" + linkId);
        }
        Link1 physical = phyLink.getAugmentation(Link1.class);
        if (CollectionUtils.isEmpty(physical.getPhysical().getSupportedLink())) {
            return PageResult.<Link>builder().build();
        }
        List<String> siteLinkIds = physical.getPhysical().getSupportedLink().stream()
                .filter(supportedLink -> supportedLink.getTopologyRef().getValue()
                        .equals(SITE_TOPO_KEY))
                .map(supportedLink -> supportedLink.getLinkRef().getValue()).collect(
                        Collectors.toList());
        return netconfTopology.retrieveAllSiteLinkByIdsPaged(siteLinkIds, pageNum, pageSize);
    }


}
