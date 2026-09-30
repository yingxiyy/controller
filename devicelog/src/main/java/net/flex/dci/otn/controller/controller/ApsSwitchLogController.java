package net.flex.dci.otn.controller.controller;

import static net.flex.dci.otn.controller.utils.Constants.COLON;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otn.controller.apsswitchlog.dto.ApsRelatedData;
import net.flex.dci.otn.controller.apsswitchlog.dto.ApsRelatedTunnelOrSiteLink;
import net.flex.dci.otn.controller.apsswitchlog.dto.PageApsSwitchLogDto;
import net.flex.dci.otn.controller.apsswitchlog.dto.PageApsSwitchLogQueryParamDto;
import net.flex.dci.otn.controller.apsswitchlog.dto.SortCondition;
import net.flex.dci.otn.controller.apsswitchlog.enums.OrderElement;
import net.flex.dci.otn.controller.apsswitchlog.enums.ResourceType;
import net.flex.dci.otn.controller.apsswitchlog.service.impl.ApsSwitchLogServiceImpl;
import net.flex.dci.otn.controller.module.ApsRequest;
import net.flex.dci.otn.controller.utils.ApsSwitchLogUtils;
import net.flex.dci.otn.controller.webapp.Result;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 2022/1/26 14:14
 */
@RestController
@Slf4j
public class ApsSwitchLogController {

    @Autowired
    private ApsSwitchLogServiceImpl apsSwitchLogService;

    @Autowired
    private PhyLinkDao phyLinkDao;

    @Autowired
    private OchLinkDao ochLinkDao;

    @Autowired
    private TunnelDao tunnelDao;

    @Autowired
    private SiteLinkDao siteLinklDao;


    @Autowired
    private PhyNodeDao phyNodeDao;


    private static String genComparePath(String neID, String apsModuleName) {
        // APS-1-1-2
        int index = apsModuleName.indexOf("-");
        //-1-1
        String linkStr = "LINECARD" + apsModuleName.substring(index, index + 4);
        //-1-1-2
        String portStr = "PORT" + apsModuleName.substring(index, index + 6);
        return neID + "#" + linkStr + "#" + portStr;
    }

    //activePath;triggerType;apsMode;
    @RequestMapping(value = "/apsswitchlog", method = RequestMethod.GET)
    public ResponseEntity<?> listAllApsSwitchLogByCondition(
            @RequestParam(value = "page", defaultValue = "0", required = false) int offset,
            @RequestParam(value = "limit", defaultValue = "20", required = false) int limit,
            @RequestParam(value = "order", required = false) List<String> sorts,
            @RequestParam(value = "neName", required = false) String neName,
            @RequestParam(value = "neId", required = false) String neId,
            @RequestParam(value = "activePath", required = false) String activePath,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "aps_mode", required = false) String apsMode,
            @RequestParam(value = "triggerType", required = false) String triggerType,
            @RequestParam(value = "startTime", required = false) Long startTime,
            @RequestParam(value = "apsModuleName", required = false) String apsModuleName,
            @RequestParam(value = "subnet-id", required = false) String subnetId,
            @RequestParam(value = "endTime", required = false) Long endTime)
            throws CommonException {
        log.info("list all task info by condition");
        offset = offset < 0 ? 0 : offset;
        limit = limit < 0 ? 20 : limit;
        List<SortCondition> sortConditions = new ArrayList<>();
        if (sorts != null) {
            for (String sort : sorts) {
                String[] parts = sort.split(COLON);
                if (parts.length == 2) {
                    OrderElement orderElement = OrderElement.fromElementName(parts[0]);
                    String sortType = parts[1];
                    sortConditions.add(
                            new SortCondition(orderElement, sortType));
                    if (orderElement == OrderElement.activePath) {
                        sortConditions.add(new SortCondition(OrderElement.activeIndex, sortType));
                    }
                }
            }
        }
        if (sortConditions.isEmpty()) {
            sortConditions.add(new SortCondition(OrderElement.id, "desc"));
        }

        Sort sort = ApsSwitchLogUtils.getSortElement(sortConditions);
        PageApsSwitchLogQueryParamDto pageQueryParamDto = PageApsSwitchLogQueryParamDto.builder()
                .page(offset)
                .limit(limit)
                .sort(sort)
                .keywords(keywords)
                .neName(neName)
                .neId(neId)
                .activePath(activePath)
                .apsModuleName(apsModuleName)
                .triggerType(triggerType)
                .apsMode(apsMode)
                .startTime(startTime)
                .endTime(endTime)
                .subnetId(subnetId)
                .build();
        PageApsSwitchLogDto pageIdcData = apsSwitchLogService.listAllApsSwitchLogByCondition(
                pageQueryParamDto);
        return new ResponseEntity<>(Result.ok(pageIdcData), HttpStatus.OK);
    }


    @RequestMapping(value = "/apsswitchlog/resource/{resourceType}/id/{resourceId}", method = RequestMethod.GET)
    public ResponseEntity<?> listAllResourceApsSwitchLogByCondition(
            @PathVariable(value = "resourceType") ResourceType resourceType,
            @PathVariable(value = "resourceId") String resourceId,
            @RequestParam(value = "neName", required = false) String neName,
            @RequestParam(value = "page", defaultValue = "0", required = false) int offset,
            @RequestParam(value = "limit", defaultValue = "20", required = false) int limit,
            @RequestParam(value = "order", required = false) List<String> sorts,
            @RequestParam(value = "neId", required = false) String neId,
            @RequestParam(value = "activePath", required = false) String activePath,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "aps_mode", required = false) String apsMode,
            @RequestParam(value = "triggerType", required = false) String triggerType,
            @RequestParam(value = "startTime", required = false) Long startTime,
            @RequestParam(value = "apsModuleName", required = false) String apsModuleName,
            @RequestParam(value = "endTime", required = false) Long endTime)
            throws CommonException {
        log.info("list all task info by condition");
        offset = offset < 0 ? 0 : offset;
        limit = limit < 0 ? 20 : limit;
        List<SortCondition> sortConditions = new ArrayList<>();
        if (sorts != null) {
            for (String sort : sorts) {
                String[] parts = sort.split(COLON);
                if (parts.length == 2) {
                    OrderElement orderElement = OrderElement.fromElementName(parts[0]);
                    String sortType = parts[1];
                    sortConditions.add(
                            new SortCondition(orderElement, sortType));
                    if (orderElement == OrderElement.activePath) {
                        sortConditions.add(new SortCondition(OrderElement.activeIndex, sortType));
                    }
                }
            }
        }
        // 如果没有提供排序参数，使用默认
        if (sortConditions.isEmpty()) {
            sortConditions.add(new SortCondition(OrderElement.id, "desc"));
        }

        Sort sort = ApsSwitchLogUtils.getSortElement(sortConditions);
        PageApsSwitchLogQueryParamDto pageQueryParamDto = PageApsSwitchLogQueryParamDto.builder()
                .resourceId(resourceId)
                .resourceType(resourceType)
                .page(offset)
                .limit(limit)
                .neName(neName)
                .sort(sort)
                .keywords(keywords)
                .neId(neId)
                .activePath(activePath)
                .apsModuleName(apsModuleName)
                .triggerType(triggerType)
                .apsMode(apsMode)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        PageApsSwitchLogDto pageIdcData = apsSwitchLogService.retrieveResourceAllApsSwitchLogByCondition(
                pageQueryParamDto);
        return new ResponseEntity<>(Result.ok(pageIdcData), HttpStatus.OK);
    }

    @RequestMapping(value = "/apsswitchlog/deleteApsSwitchLog/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteApsSwitchLog(@PathVariable("id") Long id)
            throws CommonException {
        log.info("delete ApsSwitchLog {}", id);
        apsSwitchLogService.deleteApsSwitchLog(id);

        return new ResponseEntity<>(Result.ok("OK"), HttpStatus.OK);
    }

    @RequestMapping(value = "/apsswitchlog/deleteApsSwitchLog1", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteApsSwitchLog1()
            throws CommonException {
        //log.info("delete ApsSwitchLog {}", id);
        apsSwitchLogService.deleteApsSwitchLog(3L);

        return new ResponseEntity<>(Result.ok("gao ok"), HttpStatus.OK);
    }

    @RequestMapping(value = "/apsswitchlog/getRelated", method = RequestMethod.POST)
    public ResponseEntity<?> getRelated(@RequestBody ApsRequest apsRequest)
            throws CommonException {

        String neId = apsRequest.getNeId();
        String apsName = apsRequest.getApsName();
        Node node = phyNodeDao.getConfigPhyNodeById(neId);
        if (node == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Node not found for id: " + neId);
        }
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections apsXc = nodeAttr.getCrossConnections()
                .stream().filter(x -> x.getDescription().equalsIgnoreCase(apsName))
                .findAny()
                .orElseThrow(() -> new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "APS Module not found for name: " + apsName));

        String apsTp = apsXc.getDestinationTp().get(0).getTpRef().getValue();
        ApsRelatedData data = ApsRelatedData.builder().apsTpId(apsTp).build();

//        List<ApsRelatedTunnelOrSiteLink> omspList = findApsOmsLink(neId, apsName);
//        if (!omspList.isEmpty())
//        {
//            ApsRelatedData data = ApsRelatedData.builder().type(ApsRelatedData.SITELINK).data(omspList)
//                    .build();
//            return new ResponseEntity<>(Result.ok(data), HttpStatus.OK);
//        }
//        List<ApsRelatedTunnelOrSiteLink> list = findApsOchLink(neId, apsName);
//
//        ApsRelatedData data = ApsRelatedData.builder().type(ApsRelatedData.TUNNEL).data(list)
//                .build();
        return new ResponseEntity<>(Result.ok(data), HttpStatus.OK);
    }

    @RequestMapping(value = "/apsswitchlog/test", method = RequestMethod.GET)
    public ResponseEntity<?> test()
            throws CommonException {

        //List<Link> list = phyLinkDao.getOtsLinkByNodeId("Site-1968801140351045632#Ne-1970034146264879104");
        List<ApsRelatedTunnelOrSiteLink> list = findApsOchLink(
                "Site-1968801140351045632#Ne-1972195297450921984", "APS-1-1-2");

        //return new ResponseEntity<>(Result.ok("test" + list.isEmpty()), HttpStatus.OK);
        ApsRelatedData data = ApsRelatedData.builder().type(ApsRelatedData.TUNNEL).data(list)
                .build();
        return new ResponseEntity<>(Result.ok(data), HttpStatus.OK);
    }

    private CrossConnections findCrossConnections(List<CrossConnections> crossList, long index) {
        for (CrossConnections cross : crossList) {
            if (cross.getSequence() == index) {
                return cross;
            }
        }
        return null;
    }

    private List<ApsRelatedTunnelOrSiteLink> findApsOmsLink(String neID, String apsModuleName) {
        List<ApsRelatedTunnelOrSiteLink> retValue = new ArrayList<>();
        List<Link> omspLink = siteLinklDao.queryWithNode(neID);
        for (Link link : omspLink) {
            Site site = siteLinklDao.getSiteLinkAttributeSite(link.getLinkId().getValue());
            if (NodeType.TD.equals(phyNodeDao.getPhysicalByNode(neID).getNodeType())) {
                continue;
            }
            retValue.add(
                    ApsRelatedTunnelOrSiteLink.builder().id(link.getLinkId().getValue())
                            .name(site.getFriendlyName()).build());
        }
        return retValue;
    }

    private List<ApsRelatedTunnelOrSiteLink> findApsOchLink(String neID, String apsModuleName) {
        List<ApsRelatedTunnelOrSiteLink> retValue = new ArrayList<>();
        List<Link> ochLinkList = ochLinkDao.queryWithNode(neID);
        String str = genComparePath(neID, apsModuleName);
        for (Link link : ochLinkList) {
            String linkID = link.getLinkId().getValue();
//            Link ochLink = ochLinkDao.getOchLinkByLinkId(linkID);
            //String linkName ochLink.;
            Och och = ochLinkDao.getLinkAttributeOch(linkID);
            ExplictRoute routes = och.getExplictRoute();
            List<Route> routeList = routes.getRoute();
            //Route route = routeList.get(0);
            for (Route route : routeList) {
                //need change
                List<CrossConnections> crossList = route.getPrimary().getCrossConnections();
                //CrossConnections crossConnections = findCrossConnections(crossList, 1L);
                retValue.addAll(
                        addTunelList(findCrossConnections(crossList, 1L), och, str, linkID));
                retValue.addAll(
                        addTunelList(findCrossConnections(crossList, crossList.size()), och, str,
                                linkID));

                /*
                if (crossConnections == null) {
                    continue;
                }
                String crossID = crossConnections.getCrossConnectionId().getValue();
                if (crossID.indexOf(str) > -1 && och.getImplementState()
                        .equals(ImplementState.Implement)) {
                    List<Tunnel> tunnelList = tunnelDao.queryWithOchLinkId(linkID);
                    retValue.addAll(genApsRelatedTunnels(tunnelList));
                }*/
            }
        }
        return retValue;
    }

    private List<ApsRelatedTunnelOrSiteLink> addTunelList(CrossConnections crossConnections,
            Och och, String str, String linkID) {
        List<ApsRelatedTunnelOrSiteLink> retValue = new ArrayList<>();
        if (crossConnections == null) {
            return retValue;
        }
        String crossID = crossConnections.getCrossConnectionId().getValue();
        if (crossID.indexOf(str) > -1 && och.getImplementState()
                .equals(ImplementState.Implement)) {
            List<Tunnel> tunnelList = tunnelDao.queryWithOchLinkId(linkID);
            retValue.addAll(genApsRelatedTunnels(tunnelList));
        }
        return retValue;
    }

    private List<ApsRelatedTunnelOrSiteLink> genApsRelatedTunnels(List<Tunnel> tunnelList) {
        List<ApsRelatedTunnelOrSiteLink> retValue = new ArrayList<>();
        for (Tunnel tunnel : tunnelList) {
            if (tunnel.getImplementState().equals(ImplementState.Implement)) {
                retValue.add(
                        ApsRelatedTunnelOrSiteLink.builder().id(tunnel.getTunnelId().getValue())
                                .name(tunnel.getFriendlyName()).build());
            }

        }
        return retValue;
    }

}
