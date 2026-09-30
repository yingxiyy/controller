package net.flex.dci.otn.controller.subnet.manager.component.migration;

import static net.flex.dci.otn.controller.subnet.manager.utils.Constants.LINE_FIX;
import static net.flex.dci.otn.controller.subnet.manager.utils.Constants.OS_LINK_PREFIX;
import static net.flex.dci.otn.controller.subnet.manager.utils.Constants.UNASSIGN;
import static net.flex.dci.otn.controller.subnet.manager.utils.SubnetUtils.getLinkIds;
import static net.flex.dci.otn.controller.subnet.manager.utils.SubnetUtils.getLinkRefSiteByViewLinkType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.ViewLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.ViewNodeNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.dto.AssignSubnetTunnelInfo;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import net.flex.dci.otc.mongo.dto.NeSubnetInfo;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.subnet.manager.component.taskinfo.SubnetTaskInfoBuilder;
import net.flex.dci.otn.controller.subnet.manager.component.view.ViewTopologyMigration;
import net.flex.dci.otn.controller.subnet.manager.dto.MigrationSiteLinkInfo;
import net.flex.dci.otn.controller.subnet.manager.dto.MigrationSiteLinkInfo.MigrationSiteLink;
import net.flex.dci.otn.controller.subnet.manager.dto.MigrationTunnelInfo;
import net.flex.dci.otn.controller.subnet.manager.dto.MigrationTunnelInfo.MigrationTunnel;
import net.flex.dci.otn.controller.subnet.manager.dto.view.ViewLinkMigrationDto;
import net.flex.dci.otn.controller.subnet.manager.dto.view.ViewLinkRefConnectionDto;
import net.flex.dci.otn.controller.subnet.manager.dto.view.ViewNodeMigrationDto;
import net.flex.dci.otn.controller.subnet.manager.dto.view.ViewTopologyMigrationDto;
import net.flex.dci.otn.controller.subnet.manager.enums.SubnetMigrationType;
import net.flex.dci.otn.controller.subnet.manager.utils.SubnetUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 2026/2/9
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class SubNetMigrationImpl implements SubNetMigration {

    private final SubNetTreeNodeDao subNetTreeNodeDao;

    private final SiteLinkDao siteLinkDao;

    private final PhyNodeDao phyNodeDao;

    private final TunnelDao tunnelDao;

    private final PhyLinkDao phyLinkDao;

    private final OchLinkDao ochLinkDao;

    //view node update
    private final ViewTopologyMigration viewTopologyMigration;

    private final SubnetTaskInfoBuilder subnetTaskInfoBuilder;

    @Override
    public void reassignmentSubnet(String subnetId, List<String> siteLinkIds,
            List<String> tunnelIds, String author) {
        log.info(" author :{} reassign siteLink:{} and tunnel:{} to subnet:{}", author,
                siteLinkIds.size(), tunnelIds.size(), subnetId);
        SubNetTreeNode subnet = subNetTreeNodeDao.findBySubNetId(
                        subnetId)
                .orElseThrow(() -> new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "current subnet node  " + subnetId + " is not found"));
        List<Link> siteLinks = siteLinkDao.listAllSiteLinkByIds(siteLinkIds);
        List<Tunnel> tunnels = tunnelDao.listAllTunnelByIds(tunnelIds);
        TaskInfoMessage taskInfoMessage = subnetTaskInfoBuilder.buildSubnetMigrationTask(subnet,
                siteLinks, tunnels, author);
        assignSubnet(siteLinks, tunnels, subnet, taskInfoMessage);
    }

    @Override
    public void reassignmentSubnet(MigrationSiteLinkInfo migrationSiteLink,
            MigrationTunnelInfo migrationTunnel, String author) {
        List<MigrationSiteLink> migrationSiteLinks =
                migrationSiteLink == null ? new ArrayList<>() : migrationSiteLink.getMigration();
        List<MigrationTunnel> migrationTunnels =
                migrationTunnel == null ? new ArrayList<>() : migrationTunnel.getMigration();
        log.info("author:{} reassign siteLink:{} and tunnel:{} to target", author,
                migrationSiteLinks.size(), migrationTunnels.size());
        List<String> migrationSiteLinkIds = migrationSiteLinks.stream()
                .map(MigrationSiteLink::getSiteLinkId).collect(
                        Collectors.toList());
        List<String> migrationTunnelIds = migrationTunnels.stream()
                .map(MigrationTunnel::getTunnelId).collect(
                        Collectors.toList());
        List<String> allTargetSubnet = getAllTargetSubnet(migrationSiteLinks, migrationTunnels);
        List<SubNetTreeNode> subNetTreeNodes = subNetTreeNodeDao.getSubNetBySubnetIds(
                allTargetSubnet);
        List<Link> siteLinks = siteLinkDao.listAllSiteLinkByIds(migrationSiteLinkIds);
        List<Tunnel> tunnels = CollectionUtils.isEmpty(migrationTunnels) ? new ArrayList<>()
                : tunnelDao.listAllTunnelByIds(migrationTunnelIds);
        Map<String, Link> siteLinkMap = siteLinks.stream()
                .collect(Collectors.toMap(
                        link -> link.getLinkId().getValue(),
                        Function.identity(),
                        (oldValue, newValue) -> oldValue
                ));
        Map<String, Tunnel> tunnelMap =
                CollectionUtils.isEmpty(migrationTunnels) ? new HashMap<>() : tunnels.stream()
                        .collect(Collectors.toMap(tunnel -> tunnel.getTunnelId().getValue(),
                                Function.identity(),
                                (oldValue, newValue) -> oldValue));
        Map<String, SubNetTreeNode> subNetTreeNodeMap = subNetTreeNodes.stream()
                .collect(Collectors.toMap(SubNetTreeNode::getSubNetId,
                        Function.identity(),
                        (oldValue, newValue) -> oldValue));
        TaskInfoMessage taskInfoMessage = subnetTaskInfoBuilder.buildSubnetMigrationTask(
                migrationSiteLinks, migrationTunnels, siteLinkMap, tunnelMap, subNetTreeNodeMap,
                author);
        assignSubnet(migrationSiteLinks, migrationTunnels, subNetTreeNodeMap, taskInfoMessage);
    }

    @Transactional(value = "mongoTransactionManager", rollbackFor = Exception.class)
    private void assignSubnet(List<MigrationSiteLink> migrationSiteLinks,
            List<MigrationTunnel> migrationTunnels, Map<String, SubNetTreeNode> subNetTreeNodeMap,
            TaskInfoMessage taskInfoMessage) {
        try {
            log.debug("start to migrate {} siteLink(s) and {} tunnel(s)", migrationSiteLinks.size(),
                    migrationTunnels.size());
            //migrate siteLink
            if (!CollectionUtils.isEmpty(migrationSiteLinks)) {
                Map<String, List<String>> migrationSiteLinkMap = migrationSiteLinks.stream()
                        .filter(link -> link.getTargetSubnet() != null)
                        .collect(Collectors.groupingBy(
                                MigrationSiteLink::getTargetSubnet,
                                Collectors.mapping(MigrationSiteLink::getSiteLinkId,
                                        Collectors.toList())
                        ));
                List<ViewTopologyMigrationDto> viewTopologyMigrationDtoList = assignSiteLinkSubnet(
                        migrationSiteLinkMap, subNetTreeNodeMap);
                viewTopologyMigration.migrate(viewTopologyMigrationDtoList);
            }

            //migrate tunnel
            if (!CollectionUtils.isEmpty(migrationTunnels)) {
                log.info("start to migration tunnels size:{}", migrationTunnels.size());
                migrationTunnel(migrationTunnels, subNetTreeNodeMap);
            }

            subnetTaskInfoBuilder.buildSuccessMigrationTask(taskInfoMessage);
        } catch (Exception ex) {
            log.error("viewTopology migration failed:{}", ex.getMessage(), ex);
            subnetTaskInfoBuilder.buildFailedMigrationTask(taskInfoMessage, ex.getMessage());
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "subnet migration failed,the reason is:" + ex.getMessage(), ex);
        }

    }

    private List<ViewTopologyMigrationDto> assignSiteLinkSubnet(
            Map<String, List<String>> migrationSiteLinkMap,
            Map<String, SubNetTreeNode> subNetTreeNodeMap) {
        log.debug("assign site link to target Subnet size:{}", migrationSiteLinkMap.size());
        List<ViewTopologyMigrationDto> viewTopologyMigrations = new ArrayList<>();
        for (Map.Entry<String, List<String>> migrationSiteLinkEntry : migrationSiteLinkMap.entrySet()) {
            String targetSubnetId = migrationSiteLinkEntry.getKey();
            List<String> migrateSiteLinkIds = migrationSiteLinkEntry.getValue();
            List<Link> migrateSiteLinks = siteLinkDao.listAllSiteLinkByIds(migrateSiteLinkIds);
            SubNetTreeNode targetSubnet = subNetTreeNodeMap.get(targetSubnetId);
            ViewTopologyMigrationDto viewTopoMigrationDto = assignSubnetAndUpdateViewTopo(
                    targetSubnet, migrateSiteLinks);
            viewTopologyMigrations.add(viewTopoMigrationDto);
        }

        return viewTopologyMigrations;
    }


    private void migrationTunnel(List<MigrationTunnel> migrationTunnels,
            Map<String, SubNetTreeNode> subNetTreeNodeMap) {
        log.debug("migration tunnel size:{}", migrationTunnels.size());
        List<AssignSubnetTunnelInfo> assignSubnetTunnelInfos = migrationTunnels.stream()
                .map(migrationTunnel -> {
                    String tunnelId = migrationTunnel.getTunnelId();
                    String subnetId = migrationTunnel.getTargetSubnet();
                    String subnetName = subNetTreeNodeMap.get(subnetId).getName();
                    return AssignSubnetTunnelInfo.builder().subnetId(subnetId).tunnelId(tunnelId)
                            .subnetName(subnetName).build();
                }).collect(Collectors.toList());
        tunnelDao.batchAssignSubnetToTunnel(assignSubnetTunnelInfos);
        Map<String, List<MigrationTunnel>> subnetTunnelMap = migrationTunnels.stream()
                .collect(Collectors.groupingBy(MigrationTunnel::getTargetSubnet));
        for (Map.Entry<String, List<MigrationTunnel>> entry : subnetTunnelMap.entrySet()) {
            String subnetId = entry.getKey();
            SubNetTreeNode subnet = subNetTreeNodeMap.get(subnetId);
            List<String> tunnelIds = entry.getValue().stream()
                    .map(MigrationTunnel::getTunnelId)
                    .collect(Collectors.toList());
            EPCInfo epcInfo = retrieveTdNodeIdsByTunnelIds(tunnelIds);

            phyNodeDao.assignSubnetToPhyNodes(subnetId, subnet.getName(), epcInfo.nodeIds);
            viewTopologyMigration.migrate(epcInfo.nodeOldSubnetMap, subnetId);
            phyLinkDao.assignSubnetToPhyLinks(subnetId, subnet.getName(), epcInfo.osLinkIds);
            log.info("migrated {} td node(s) to subnet:{}", epcInfo.nodeIds.size(), subnetId);
        }
    }

    private EPCInfo retrieveTdNodeIdsByTunnelIds(List<String> tunnelIds) {
        List<String> refOchLinkIds = tunnelDao.retrieveAllOchLinkIdsByTunnelIds(tunnelIds);
        List<LinkStateDto> ochLinkStates = ochLinkDao.getOchLinkLinksStateDto(
                refOchLinkIds);
        List<String> osLinkIds = ochLinkStates.stream()
                .flatMap(linkStateDto -> linkStateDto.getSupportingLink().stream())
                .filter(linkId -> linkId.startsWith(OS_LINK_PREFIX)).collect(Collectors.toList());
        List<String> tdNeIds = new ArrayList<>();
        for (String osLinkId : osLinkIds) {
            String srcTp = PhysicalLinkIdNamingRule.getTpAId(osLinkId);
            String destTp = PhysicalLinkIdNamingRule.getTpZId(osLinkId);
            String srcNeId = PhysicalLinkIdNamingRule.getNodeAId(osLinkId);
            String destNeId = PhysicalLinkIdNamingRule.getNodeZId(osLinkId);
            if (destTp.contains(LINE_FIX)) {
                tdNeIds.add(destNeId);
            }
            if (srcTp.contains(LINE_FIX)) {
                tdNeIds.add(srcNeId);
            }
        }
        List<String> distinctNeIds = tdNeIds.stream().distinct().collect(Collectors.toList());
        Map<String, String> nodeOldSubnetMap = queryTdNodeOldSubnet(distinctNeIds);
        return EPCInfo.builder().nodeIds(tdNeIds).osLinkIds(osLinkIds)
                .nodeOldSubnetMap(nodeOldSubnetMap).build();
    }

    /**
     * query td node old subnet
     *
     * @param tdNeIds
     * @return
     */
    private Map<String, String> queryTdNodeOldSubnet(List<String> tdNeIds) {
        if (CollectionUtils.isEmpty(tdNeIds)) {
            return new HashMap<>();
        }
        Map<String, String> nodeOldSubnetMap = new HashMap<>();
        int batchSize = 100;
        for (int i = 0; i < tdNeIds.size(); i += batchSize) {
            List<String> batch = tdNeIds.subList(i, Math.min(i + batchSize, tdNeIds.size()));
            for (NeSubnetInfo info : phyNodeDao.listPhyNodeSubnetInfoByIds(batch)) {
                if (info.getNeId() != null && info.getSubnetId() != null) {
                    nodeOldSubnetMap.put(info.getNeId(), info.getSubnetId());
                }
            }
        }
        return nodeOldSubnetMap;
    }

    private List<String> getAllTargetSubnet(List<MigrationSiteLink> migrationSiteLinks,
            List<MigrationTunnel> migrationTunnels) {
        Set<String> subnetIds = new HashSet<>();
        Set<String> migrationSiteLinkTargetSubnets = migrationSiteLinks.stream()
                .map(MigrationSiteLink::getTargetSubnet).collect(
                        Collectors.toSet());
        Set<String> migrationTunnelTargetSubnets = migrationTunnels.stream()
                .map(MigrationTunnel::getTargetSubnet).collect(
                        Collectors.toSet());
        subnetIds.addAll(migrationTunnelTargetSubnets);
        subnetIds.addAll(migrationSiteLinkTargetSubnets);
        return new ArrayList<>(subnetIds);
    }

    @Transactional(value = "mongoTransactionManager", rollbackFor = Exception.class)
    public void assignSubnet(List<Link> siteLinks, List<Tunnel> tunnels, SubNetTreeNode subnet,
            TaskInfoMessage taskInfoMessage) {
        try {
            log.debug("start ot assign {} siteLinks to subnet :{}", siteLinks.size(),
                    subnet.getName());
            Map<String, List<Link>> subnetSiteLinkMap = siteLinks.stream().collect(
                    Collectors.groupingBy(
                            siteLink -> {
                                String planeId = siteLink.getAugmentation(Link1.class).getSite()
                                        .getPlaneId();
                                return StringUtils.hasText(planeId) ? planeId : UNASSIGN;
                            }));
            List<ViewTopologyMigrationDto> viewTopologyMigrationDtoList = assignSubnet(subnet,
                    subnetSiteLinkMap);
            viewTopologyMigration.migrate(viewTopologyMigrationDtoList);
            subnetTaskInfoBuilder.buildSuccessMigrationTask(taskInfoMessage);
        } catch (Exception ex) {
            log.error("viewTopology migration failed:{}", ex.getMessage(), ex);
            subnetTaskInfoBuilder.buildFailedMigrationTask(taskInfoMessage, ex.getMessage());
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "subnet migration failed,the reason is:" + ex.getMessage(), ex);

        }
    }

    private List<ViewTopologyMigrationDto> assignSubnet(SubNetTreeNode subnet,
            Map<String, List<Link>> subnetSiteLinkMap) {
        log.info("assign siteLink to subnet start,subnet:{}", subnet.getName());
        List<ViewTopologyMigrationDto> viewTopologyMigrations = new ArrayList<>();
        for (Map.Entry<String, List<Link>> entry : subnetSiteLinkMap.entrySet()) {
            String subnetId = entry.getKey();
            List<Link> groupSiteLinks = entry.getValue();
            log.debug("handle subnetId:{},siteLink count:{}", subnetId, groupSiteLinks.size());
            if (UNASSIGN.equals(subnetId)) {
                ViewTopologyMigrationDto viewTopologyMigrationDto = assignBasicSubnet(subnet,
                        groupSiteLinks);
                ViewTopologyMigrationDto viewTopologyMigrationOutDto = migrationOutUnAssign(
                        groupSiteLinks);
                viewTopologyMigrations.add(viewTopologyMigrationOutDto);
                viewTopologyMigrations.add(viewTopologyMigrationDto);
                log.info("UNASSIGN subnet:handle {} siteLinks,only migrate ,skip view topo update",
                        groupSiteLinks.size());
            } else {
                ViewTopologyMigrationDto viewTopoMigrationDto = assignSubnetAndUpdateViewTopo(
                        subnet, groupSiteLinks);
                viewTopologyMigrations.add(viewTopoMigrationDto);
                log.info(
                        "normal assign  subnet:{} handle  {} siteLinkIds,migrate+update view topo ",
                        subnetId, groupSiteLinks.size());
            }

        }
        return viewTopologyMigrations;
    }

    /**
     * @param groupSiteLinks
     * @return
     */
    private ViewTopologyMigrationDto migrationOutUnAssign(List<Link> groupSiteLinks) {
        List<String> siteLinkIds = groupSiteLinks.stream().map(LinkAttributes::getLinkId)
                .map(Uri::getValue).collect(
                        Collectors.toList());
        log.debug("migrationOut unassign viewLink refSiteLink siteIds:{}", siteLinkIds);
        List<Link> refOchLinks = ochLinkDao.getAllOchLinksUnderSiteLinkIds(siteLinkIds);
        List<String> refPhyLinkIds = SubnetUtils.getPhyLinkIdsFromLinkSupportingLinks(
                groupSiteLinks);
        List<String> ochLinkIds = refOchLinks.stream().map(LinkAttributes::getLinkId)
                .map(Uri::getValue).collect(
                        Collectors.toList());
        if (!CollectionUtils.isEmpty(refOchLinks)) {
            List<String> ochLinkRefPhyLinkId = SubnetUtils.getPhyLinkIdsFromLinkSupportingLinks(
                    refOchLinks);
            refPhyLinkIds.addAll(ochLinkRefPhyLinkId);
        }
        //change the connection subnet
        List<ViewLinkMigrationDto> viewLinkMigrationDtos = new ArrayList<>();
        List<ViewLinkMigrationDto> viewLinkPhyLinkMigrationOut = buildUnAssignSubnetViewLinkPhyLinkMigrationOut(
                refPhyLinkIds);
        List<ViewLinkMigrationDto> viewSiteLinkMigrationOut = buildUnAssignSubnetViewLinkOut(
                siteLinkIds, ViewLinkType.SiteLink);
        List<ViewLinkMigrationDto> viewOchLinkMigrationOut = buildUnAssignSubnetViewLinkOut(
                ochLinkIds, ViewLinkType.OchLink);
        viewLinkMigrationDtos.addAll(viewOchLinkMigrationOut);
        viewLinkMigrationDtos.addAll(viewLinkPhyLinkMigrationOut);
        viewLinkMigrationDtos.addAll(viewSiteLinkMigrationOut);
        return ViewTopologyMigrationDto.builder()
                .viewLinkMigrationDtoList(viewLinkMigrationDtos)
                .build();
    }


    private List<ViewLinkMigrationDto> buildUnAssignSubnetViewLinkPhyLinkMigrationOut(
            List<String> refPhyLinkIds) {
        List<String> otsLinkIds = refPhyLinkIds.stream()
                .filter(PhysicalLinkIdNamingRule::isOtsLink).collect(
                        Collectors.toList());
        List<String> osLinkIds = refPhyLinkIds.stream()
                .filter(PhysicalLinkIdNamingRule::isOsLink)
                .collect(Collectors.toList());
        List<ViewLinkMigrationDto> otsViewLinkMigrationDtoList = buildUnAssignSubnetViewLinkOut(
                otsLinkIds, ViewLinkType.OtsLink);
        List<ViewLinkMigrationDto> osViewLinkMigrationDtoList = buildUnAssignSubnetViewLinkOut(
                osLinkIds, ViewLinkType.OsLink);
        return Stream.concat(otsViewLinkMigrationDtoList.stream(),
                osViewLinkMigrationDtoList.stream()).collect(
                Collectors.toList());
    }

    private List<ViewLinkMigrationDto> buildUnAssignSubnetViewLinkOut(List<String> linkIds,
            ViewLinkType viewLinkType) {
        log.debug(
                "get migration out for unassign link:{} viewLinkType is:{}",
                linkIds, viewLinkType);
        List<ViewLinkMigrationDto> viewLinkMigrationDtos = new ArrayList<>();
        Map<String, ViewLinkRefConnectionDto> viewLinkMap = new HashMap<>();
        for (String linkId : linkIds) {
            String srcSiteId = getLinkRefSiteByViewLinkType(linkId, viewLinkType, true);
            String destSiteId = getLinkRefSiteByViewLinkType(linkId, viewLinkType, false);
            String viewLinkId = ViewLinkIdNamingRule.generateId(srcSiteId, destSiteId,
                    viewLinkType);
            viewLinkMap.computeIfAbsent(viewLinkId, k ->
                    ViewLinkRefConnectionDto.builder()
                            .viewLinkId(viewLinkId)
                            .sourceSite(srcSiteId)
                            .destSite(destSiteId)
                            .refConnectionIds(new ArrayList<>())
                            .build()
            ).getRefConnectionIds().add(linkId);
        }
        for (Map.Entry<String, ViewLinkRefConnectionDto> entry : viewLinkMap.entrySet()) {
            String viewLinkId = entry.getKey();
            ViewLinkRefConnectionDto refConnectionDto = entry.getValue();
            viewLinkMigrationDtos.add(ViewLinkMigrationDto.builder().migrationType(
                            SubnetMigrationType.MIGRATION_OUT)
                    .sourceSite(refConnectionDto.getSourceSite())
                    .destSite(refConnectionDto.getDestSite())
                    .connectionIds(refConnectionDto.getRefConnectionIds())
                    .viewLinkId(viewLinkId).viewLinkType(viewLinkType).build());
        }

        return viewLinkMigrationDtos;
    }


    /**
     * assign targetSubnet and update view Topo
     *
     * @param targetSubnet
     * @param groupSiteLinks
     */
    private ViewTopologyMigrationDto assignSubnetAndUpdateViewTopo(
            SubNetTreeNode targetSubnet,
            List<Link> groupSiteLinks) {
        log.debug(
                "assign  to targetSubnet:{}and update view topo total siteLinks:{}",
                targetSubnet.getSubNetId(),
                groupSiteLinks.size());
        ViewTopologyMigrationDto migrationOutViewTopologyDto = getMigrationOutViewTopology(
                groupSiteLinks);
        ViewTopologyMigrationDto assignViewTopologyMigrationDto = assignBasicSubnet(targetSubnet,
                groupSiteLinks);
        ViewTopologyMigrationDto viewTopologyMigrationDto = totalViewTopologyMigration(
                migrationOutViewTopologyDto, assignViewTopologyMigrationDto);
        return viewTopologyMigrationDto;
    }

    private ViewTopologyMigrationDto totalViewTopologyMigration(
            ViewTopologyMigrationDto migrationOutViewTopologyDto,
            ViewTopologyMigrationDto assignViewTopologyMigrationDto) {
        log.debug("start to combine total View Topology migration ");
        List<ViewLinkMigrationDto> links = new ArrayList<>(
                migrationOutViewTopologyDto.getViewLinkMigrationDtoList());
        List<ViewNodeMigrationDto> nodes = new ArrayList<>(
                migrationOutViewTopologyDto.getViewNodeMigrationDtoList());

        links.addAll(assignViewTopologyMigrationDto.getViewLinkMigrationDtoList());
        nodes.addAll(assignViewTopologyMigrationDto.getViewNodeMigrationDtoList());

        return ViewTopologyMigrationDto.builder().viewNodeMigrationDtoList(nodes)
                .viewLinkMigrationDtoList(links).build();
    }

    private ViewTopologyMigrationDto getMigrationOutViewTopology(List<Link> groupSiteLinks) {
        log.info("build migration out view topology the ref total siteLink :{}",
                groupSiteLinks.size());
        List<ViewLinkMigrationDto> links = new ArrayList<>();
        List<ViewNodeMigrationDto> nodes = new ArrayList<>();

        groupSiteLinks.stream()
                .map(this::getSiteLinkViewMigrationOutDto)
                .forEach(dto -> {
                    links.addAll(dto.getViewLinkMigrationDtoList());
                    nodes.addAll(dto.getViewNodeMigrationDtoList());
                });

        return ViewTopologyMigrationDto.builder()
                .viewLinkMigrationDtoList(links)
                .viewNodeMigrationDtoList(nodes)
                .build();
    }

    private ViewTopologyMigrationDto getSiteLinkViewMigrationOutDto(Link siteLink) {
        log.debug("get siteLink:{} ref view migration out view topology",
                siteLink.getLinkId().getValue());
        String currentSubnetId = SubnetUtils.getSiteLinkRefSubnet(siteLink);
        SubNetTreeNode subnet = subNetTreeNodeDao.findBySubNetId(currentSubnetId)
                .orElseThrow(() -> new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                        String.format("current subnet %s is not existed", currentSubnetId)));

        List<String> refPhyLinkIds = SubnetUtils.getPhyLinkIdsFromLinkSupportingLinks(
                Collections.singletonList(siteLink));

        List<String> refPhyNodeIds = getLinkRefPhyNode(refPhyLinkIds);
        List<ViewLinkMigrationDto> viewLinkMigrationDtos = changeConnectionSubnet(refPhyLinkIds,
                Collections.singletonList(siteLink), new ArrayList<>(),
                subnet, SubnetMigrationType.MIGRATION_OUT);
        List<ViewNodeMigrationDto> viewNodeMigrationDtos = changeRefPhyNodeSubnet(refPhyNodeIds,
                subnet, SubnetMigrationType.MIGRATION_OUT);
        return ViewTopologyMigrationDto.builder()
                .viewLinkMigrationDtoList(viewLinkMigrationDtos)
                .viewNodeMigrationDtoList(viewNodeMigrationDtos).build();
    }

    private ViewTopologyMigrationDto assignBasicSubnet(SubNetTreeNode subnet,
            List<Link> groupSiteLinks) {
        log.debug("assign basic subnet to subnet:{}({})", subnet.getName(), subnet.getSubNetId());
        List<String> siteLinkIds = groupSiteLinks.stream().map(LinkAttributes::getLinkId)
                .map(Uri::getValue).collect(
                        Collectors.toList());
        List<Link> refOchLinks = ochLinkDao.getAllBusinessOchLinksUnderSiteLinkIds(siteLinkIds);
        List<String> refPhyLinkIds = SubnetUtils.getPhyLinkIdsFromLinkSupportingLinks(
                groupSiteLinks);
        //wssLink
        List<String> refWssLinkIds = phyLinkDao.retrieveAllWssLinkIdBySupportingSiteLinks(
                siteLinkIds);
        if (!CollectionUtils.isEmpty(refWssLinkIds)) {
            refPhyLinkIds.addAll(refWssLinkIds);
        }
        List<String> refPhyNodeIds = getLinkRefPhyNode(refPhyLinkIds);
        //change the connection subnet
        List<ViewLinkMigrationDto> viewLinkMigrationDtos = changeConnectionSubnet(refPhyLinkIds,
                groupSiteLinks, refOchLinks,
                subnet, SubnetMigrationType.MIGRATION_IN);
        List<ViewNodeMigrationDto> viewNodeMigrationDtos = changeRefPhyNodeSubnet(refPhyNodeIds,
                subnet, SubnetMigrationType.MIGRATION_IN);
        return ViewTopologyMigrationDto.builder()
                .viewLinkMigrationDtoList(viewLinkMigrationDtos)
                .viewNodeMigrationDtoList(viewNodeMigrationDtos).build();
    }

    private List<String> getLinkRefPhyNode(List<String> phyLinkIds) {
        Set<String> refPhyNodeIds = new HashSet<>();
        for (String phyLinkId : phyLinkIds) {
            String sourceNeId = PhysicalLinkIdNamingRule.getNodeAId(phyLinkId);
            String destNeId = PhysicalLinkIdNamingRule.getNodeZId(phyLinkId);
            refPhyNodeIds.add(sourceNeId);
            refPhyNodeIds.add(destNeId);
        }
        return new ArrayList<>(refPhyNodeIds);
    }

    private List<ViewNodeMigrationDto> changeRefPhyNodeSubnet(List<String> refPhyNodeIds,
            SubNetTreeNode subnet, SubnetMigrationType migrationType) {
        log.debug("assign phyNode :{} to subnet:{}", refPhyNodeIds, subnet);
        String subnetId = subnet.getSubNetId();
        String subnetName = subnet.getName();
        Integer subnetLevel = subnet.getLevel();

        if (migrationType.equals(SubnetMigrationType.MIGRATION_IN)) {
            phyNodeDao.assignSubnetToPhyNodes(subnetId, subnetName, refPhyNodeIds);
        }

        Map<String, List<String>> siteNodeMap = refPhyNodeIds.stream()
                .collect(Collectors.groupingBy(PhysicalNodeIdNamingRule::getSiteId));

        List<ViewNodeMigrationDto> viewNodeMigrationDtoList = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : siteNodeMap.entrySet()) {
            String siteId = entry.getKey();
            String viewNodeId = ViewNodeNamingRule.generatedIdWithPlaneId(siteId, subnetId);
            List<String> phyNodeIds = entry.getValue();
            viewNodeMigrationDtoList.add(ViewNodeMigrationDto.builder().migrationType(
                            migrationType).subnetName(subnetName).subnetId(subnetId)
                    .subnetLevel(subnetLevel)
                    .phyNodeId(phyNodeIds).viewNodeId(viewNodeId).build());
        }
        return viewNodeMigrationDtoList;
    }

    private List<ViewLinkMigrationDto> changeConnectionSubnet(List<String> phyLinkIds,
            List<Link> siteLinks, List<Link> ochLinks, SubNetTreeNode subnet,
            SubnetMigrationType migrationType) {
        log.info(
                "assign subnet:{}({}) to connection totalPhyLinks:{} totalSiteLink:{} totalOchLink:{} ",
                subnet.getName(), subnet.getSubNetId(),
                phyLinkIds.size(), siteLinks.size(), ochLinks.size());
        List<String> siteLinkIds = getLinkIds(siteLinks);

        if (migrationType.equals(SubnetMigrationType.MIGRATION_IN)) {
            phyLinkDao.assignSubnetToPhyLinks(subnet.getSubNetId(), subnet.getName(), phyLinkIds);
            siteLinkDao.assignSubnetToSiteLinks(subnet.getSubNetId(), subnet.getName(),
                    siteLinkIds);
            if (!CollectionUtils.isEmpty(ochLinks)) {
                List<String> ochLinkIds = getLinkIds(ochLinks);
                ochLinkDao.assignSubnetToOchLinks(subnet.getSubNetId(), subnet.getName(),
                        ochLinkIds);
            }
        }

        List<ViewLinkMigrationDto> viewLinkMigrationDtos = new ArrayList<>();
        List<ViewLinkMigrationDto> phyLinkViewLinkMigrationDtos = getPhyLinkViewMigration(
                phyLinkIds, subnet.getSubNetId(), subnet.getName(),
                migrationType);
        List<ViewLinkMigrationDto> siteLinkViewLinkMigrationDtos = getViewLinkMigration(
                siteLinkIds, subnet.getSubNetId(), subnet.getName(),
                migrationType, ViewLinkType.SiteLink);
        viewLinkMigrationDtos.addAll(phyLinkViewLinkMigrationDtos);
        viewLinkMigrationDtos.addAll(siteLinkViewLinkMigrationDtos);

        return viewLinkMigrationDtos;

    }


    private List<ViewLinkMigrationDto> getViewLinkMigration(List<String> linkIds,
            String subnetId, String subnetName, SubnetMigrationType migrationType,
            ViewLinkType viewLinkType) {
        List<ViewLinkMigrationDto> viewLinkMigrationDtos = new ArrayList<>();
        Map<String, ViewLinkRefConnectionDto> linkMap = new HashMap<>();
        for (String linkId : linkIds) {
            String sourceSiteId = SubnetUtils.getLinkRefSiteByViewLinkType(linkId, viewLinkType,
                    true);
            String destSiteId = SubnetUtils.getLinkRefSiteByViewLinkType(linkId, viewLinkType,
                    false);
            String viewSiteLinkId = ViewLinkIdNamingRule.generatedIdWithPlaneId(sourceSiteId,
                    destSiteId,
                    viewLinkType, subnetId);
            linkMap.computeIfAbsent(viewSiteLinkId, k ->
                    ViewLinkRefConnectionDto.builder()
                            .viewLinkId(viewSiteLinkId)
                            .sourceSite(sourceSiteId)
                            .destSite(destSiteId)
                            .refConnectionIds(new ArrayList<>())
                            .build()
            ).getRefConnectionIds().add(linkId);
        }
        for (Map.Entry<String, ViewLinkRefConnectionDto> entry : linkMap.entrySet()) {
            String viewLinkId = entry.getKey();
            ViewLinkRefConnectionDto refConnectionDto = entry.getValue();
            viewLinkMigrationDtos.add(ViewLinkMigrationDto.builder().migrationType(
                            migrationType).subnetName(subnetName).subnetId(subnetId)
                    .sourceSite(refConnectionDto.getSourceSite())
                    .destSite(refConnectionDto.getDestSite())
                    .connectionIds(refConnectionDto.getRefConnectionIds())
                    .viewLinkId(viewLinkId).viewLinkType(viewLinkType).build());
        }
        return viewLinkMigrationDtos;
    }

    private List<ViewLinkMigrationDto> getPhyLinkViewMigration(List<String> phyLinkIds,
            String subnetId, String subnetName, SubnetMigrationType migrationType) {
        List<String> otsLinkIds = phyLinkIds.stream()
                .filter(PhysicalLinkIdNamingRule::isOtsLink).collect(
                        Collectors.toList());
        List<String> osLinkIds = phyLinkIds.stream().filter(PhysicalLinkIdNamingRule::isOsLink)
                .collect(
                        Collectors.toList());
        List<ViewLinkMigrationDto> otsLinkViewLink = getViewLinkMigration(otsLinkIds, subnetId,
                subnetName, migrationType,
                ViewLinkType.OtsLink);
        List<ViewLinkMigrationDto> osLinkViewLink = getViewLinkMigration(osLinkIds, subnetId,
                subnetName, migrationType,
                ViewLinkType.OsLink);
        return Stream.concat(otsLinkViewLink.stream(), osLinkViewLink.stream())
                .collect(Collectors.toList());
    }


    @Data
    @Builder
    private static class EPCInfo implements Serializable {

        private List<String> osLinkIds;

        private List<String> nodeIds;

        private Map<String, String> nodeOldSubnetMap;

    }


}
