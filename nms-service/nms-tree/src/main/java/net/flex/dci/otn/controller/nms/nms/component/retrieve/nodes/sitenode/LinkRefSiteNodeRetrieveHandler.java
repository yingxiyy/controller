package net.flex.dci.otn.controller.nms.nms.component.retrieve.nodes.sitenode;

import static net.flex.dci.otn.controller.nms.utils.Constants.OCH_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.PHY_TOPO_KEY;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otn.controller.nms.nms.dto.RetrieveTopologyDto;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/13 15:34
 */
@Slf4j
@Component
public class LinkRefSiteNodeRetrieveHandler extends AbstractSiteNodeRetrieveHandler {


    public LinkRefSiteNodeRetrieveHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);

    }

    @Override
    public PageResult<Node> retrieveAllNodePaged(Integer pageNum, Integer pageSize) {
//        log.info("retrieve all site node from link,link id :{},topology :{},pageNum:{},pageSize:{}",
//                linkId, topologyRef, pageNum, pageSize);
//        PageResult<Node> pageResult = new PageResult<>();
//        if (topologyRef.equals(PHY_TOPO_KEY)) {
//            pageResult = retrievePhyLinkRefSiteNode(linkId, pageNum, pageSize);
//        } else if (topologyRef.equals(OCH_TOPO_KEY)) {
//            pageResult = retrieveOchLinkRefSiteNode(linkId, pageNum, pageSize);
//        } else if (topologyRef.equals(SITE_TOPO_KEY)) {
//            pageResult = retrieveSiteLinkRefSiteNode(linkId, pageNum, pageSize);
//        }
        return PageResult.<Node>builder().build();
    }


    @Override
    public PageResult<Node> retrieveAllNodePaged(RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("start to retrieve all node paged ,retrieve topology domain is :{}",
                retrieveTopologyDto);
        String topologyRef = retrieveTopologyDto.getTopologyRef();
        String linkId = retrieveTopologyDto.getLinkRef();
        PageResult<Node> pageResult = new PageResult<>();
        if (topologyRef.equals(PHY_TOPO_KEY)) {
            pageResult = retrievePhyLinkRefSiteNode(linkId, retrieveTopologyDto);
        } else if (topologyRef.equals(OCH_TOPO_KEY)) {
            pageResult = retrieveOchLinkRefSiteNode(linkId, retrieveTopologyDto);
        } else if (topologyRef.equals(SITE_TOPO_KEY)) {
            pageResult = retrieveSiteLinkRefSiteNode(linkId, retrieveTopologyDto);
        }
        return pageResult;
    }

    private PageResult<Node> retrieveSiteLinkRefSiteNode(String linkId,
            RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("retrieve from site link");
        Link siteLink = netconfTopology.getSiteLink(linkId);
        if (siteLink == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find the site link :" + linkId);
        }
        Link1 siteLinkPhysical = siteLink.getAugmentation(Link1.class);
        List<String> refSiteNodeIds = getRouteRefSiteId(
                siteLinkPhysical.getSite().getExplictRoute());
        return retrieveRefSiteNode(refSiteNodeIds, retrieveTopologyDto);
    }

    private PageResult<Node> retrieveOchLinkRefSiteNode(String linkId,
            RetrieveTopologyDto retrieveTopologyDto) {
        log.debug("retrieve from site link");
        Link ochLink = netconfTopology.getOchLink(linkId);
        if (ochLink == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find the och link :" + linkId);
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1 ochLinkPhysical = ochLink.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class);
        List<String> refSiteNodeIds = getRouteRefSiteId(
                ochLinkPhysical.getOch().getExplictRoute());
        return retrieveRefSiteNode(refSiteNodeIds, retrieveTopologyDto);
    }


    private PageResult<Node> retrievePhyLinkRefSiteNode(String linkId,
            RetrieveTopologyDto retrieveTopologyDto) {
        Link phyLink = netconfTopology.getPhyLink(linkId);
        if (phyLink == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find the phy link :" + linkId);
        }
        List<String> phyNodeIds = getPhyLinkRefNode(Collections.singletonList(phyLink));
        Set<String> refSiteNodeIds = phyNodeIds.stream().map(PhysicalNodeIdNamingRule::getSiteId)
                .collect(Collectors.toSet());
        return retrieveRefSiteNode(new ArrayList<>(refSiteNodeIds), retrieveTopologyDto);
    }

    private PageResult<Node> retrieveSiteLinkRefSiteNode(String linkId, Integer pageNum,
            Integer pageSize) {
        log.debug("retrieve from site link");
        Link siteLink = netconfTopology.getSiteLink(linkId);
        if (siteLink == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find the site link :" + linkId);
        }
        Link1 siteLinkPhysical = siteLink.getAugmentation(Link1.class);
        List<String> refSiteNodeIds = getRouteRefSiteId(
                siteLinkPhysical.getSite().getExplictRoute());
        return netconfTopology.retrieveAllSiteNodePagedByIds(new ArrayList<>(refSiteNodeIds),
                pageNum, pageSize);
    }

    private PageResult<Node> retrieveOchLinkRefSiteNode(String linkId, Integer pageNum,
            Integer pageSize) {
        log.debug("retrieve from site link");
        Link ochLink = netconfTopology.getOchLink(linkId);
        if (ochLink == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find the och link :" + linkId);
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1 ochLinkPhysical = ochLink.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class);
        List<String> refSiteNodeIds = getRouteRefSiteId(
                ochLinkPhysical.getOch().getExplictRoute());
        return netconfTopology.retrieveAllPhyNodePagedByIds(refSiteNodeIds, pageNum, pageSize);
    }

    private PageResult<Node> retrievePhyLinkRefSiteNode(String linkId, Integer pageNum,
            Integer pageSize) {
        log.debug("retrieve from site link");
        Link phyLink = netconfTopology.getPhyLink(linkId);
        if (phyLink == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "can't find the phy link :" + linkId);
        }
        List<String> phyNodeIds = getPhyLinkRefNode(Collections.singletonList(phyLink));
        Set<String> refSiteNodeIds = phyNodeIds.stream().map(PhysicalNodeIdNamingRule::getSiteId)
                .collect(Collectors.toSet());
        return netconfTopology.retrieveAllSiteNodePagedByIds(new ArrayList<>(refSiteNodeIds),
                pageNum, pageSize);
    }


}
