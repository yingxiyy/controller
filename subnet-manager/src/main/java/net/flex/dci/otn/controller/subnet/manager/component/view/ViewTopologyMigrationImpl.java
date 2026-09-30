package net.flex.dci.otn.controller.subnet.manager.component.view;

import static net.flex.dci.otn.controller.subnet.manager.utils.Constants.ROOT_NODE_ID;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.view.ViewTopoAlarmRecalcMsg;
import net.flex.dci.otc.common.util.CommonUtil;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.ViewNodeNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dao.ViewLinkDao;
import net.flex.dci.otc.mongo.dao.ViewNodeDao;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.subnet.manager.dto.view.ViewLinkMigrationDto;
import net.flex.dci.otn.controller.subnet.manager.dto.view.ViewNodeMigrationDto;
import net.flex.dci.otn.controller.subnet.manager.dto.view.ViewTopologyMigrationDto;
import net.flex.dci.otn.controller.subnet.manager.enums.SubnetMigrationType;
import net.flex.dci.otn.controller.subnet.manager.utils.SubnetUtils;
import net.flex.dci.otn.controller.tools.kafka.service.ViewTopoAlarmRecalcSender;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Link1;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Link1Builder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1Builder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.View;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.ViewBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * 2026/2/11
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class ViewTopologyMigrationImpl implements ViewTopologyMigration {

    private final ViewNodeDao viewNodeDao;

    private final ViewLinkDao viewLinkDao;

    private final SubNetTreeNodeDao subNetTreeNodeDao;

    private final PhyNodeDao phyNodeDao;

    @Override
    public void migrate(List<ViewTopologyMigrationDto> viewTopologyMigrationDtoList) {
        log.info("migrate view topology total migration dto list size:{}",
                viewTopologyMigrationDtoList.size());
        if (CollectionUtils.isEmpty(viewTopologyMigrationDtoList)) {
            log.warn("migrate view topology:migration dto list is empty,skip");
            return;
        }
        log.info("migrate view topology total migration dto list size:{}",
                viewTopologyMigrationDtoList.size());
        handleAllMigrateOut(viewTopologyMigrationDtoList);
        handleAllMigrateIn(viewTopologyMigrationDtoList);
//        cleanEmptyOutSubnet(viewTopologyMigrationDtoList);
        triggerInSubnetAlarmConvergence(viewTopologyMigrationDtoList);
        log.info("migrate view topology finished,total processed dto list size:{}",
                viewTopologyMigrationDtoList.size());
    }

    @Override
    public void migrate(Map<String, String> nodeOldSubnetMap, String subnetId) {
        Set<String> viewNodeIds = new HashSet<>();
        if (nodeOldSubnetMap != null) {
            for (Map.Entry<String, String> entry : nodeOldSubnetMap.entrySet()) {
                String siteId = PhysicalNodeIdNamingRule.getSiteId(entry.getKey());
                viewNodeIds.add(ViewNodeNamingRule.generatedIdWithPlaneId(siteId, subnetId));
                if (StringUtils.hasText(entry.getValue())) {
                    viewNodeIds.add(
                            ViewNodeNamingRule.generatedIdWithPlaneId(siteId, entry.getValue()));
                }
            }
        }
        ViewTopoAlarmRecalcMsg viewTopoAlarmRecalcMsg = ViewTopoAlarmRecalcMsg.builder()
                .viewNodeIds(new ArrayList<>(viewNodeIds)).build();
        ViewTopoAlarmRecalcSender.sendAlarmRecalcMsg(viewTopoAlarmRecalcMsg);
    }

    /**
     * trigger in subnet alarm convergence
     *
     * @param viewTopologyMigrationDtoList
     */
    private void triggerInSubnetAlarmConvergence(
            List<ViewTopologyMigrationDto> viewTopologyMigrationDtoList) {
        log.info("trigger in subnet alarm convergence,migration dto size:{}",
                viewTopologyMigrationDtoList.size());
        List<ViewNodeMigrationDto> viewNodeMigrationDtos = new ArrayList<>();
        List<ViewLinkMigrationDto> viewLinkMigrationDtos = new ArrayList<>();
        for (ViewTopologyMigrationDto viewTopologyMigrationDto : viewTopologyMigrationDtoList) {
            viewLinkMigrationDtos.addAll(
                    viewTopologyMigrationDto.getViewLinkMigrationDtoList() == null
                            ? new ArrayList<>()
                            : viewTopologyMigrationDto.getViewLinkMigrationDtoList());
            viewNodeMigrationDtos.addAll(
                    viewTopologyMigrationDto.getViewNodeMigrationDtoList() == null
                            ? new ArrayList<>()
                            : viewTopologyMigrationDto.getViewNodeMigrationDtoList());
        }
        List<String> viewNodeIds = viewNodeMigrationDtos.stream()
                .map(ViewNodeMigrationDto::getViewNodeId).collect(
                        Collectors.toList());
        List<String> viewLinkIds = viewLinkMigrationDtos.stream()
                .map(ViewLinkMigrationDto::getViewLinkId).collect(
                        Collectors.toList());
        ViewTopoAlarmRecalcMsg viewTopoAlarmRecalcMsg = ViewTopoAlarmRecalcMsg.builder()
                .viewLinkIds(viewLinkIds).viewNodeIds(viewNodeIds).build();
        ViewTopoAlarmRecalcSender.sendAlarmRecalcMsg(viewTopoAlarmRecalcMsg);
    }

    private void handleAllMigrateIn(List<ViewTopologyMigrationDto> viewTopologyMigrationDtoList) {
        log.info("start handle MIGRATE_IN operation,total dto size:{}",
                viewTopologyMigrationDtoList.size());
        for (ViewTopologyMigrationDto viewTopologyMigrationDto : viewTopologyMigrationDtoList) {
            handleNodeMigration(viewTopologyMigrationDto.getViewNodeMigrationDtoList(),
                    SubnetMigrationType.MIGRATION_IN);

            handleLinkMigration(viewTopologyMigrationDto.getViewLinkMigrationDtoList(),
                    SubnetMigrationType.MIGRATION_IN);
        }
        log.info("finish handle MIGRATION_IN operation");
    }

    private void handleAllMigrateOut(List<ViewTopologyMigrationDto> viewTopologyMigrationDtoList) {
        log.info("start handle MIGRATE_OUT operation,total dto size:{}",
                viewTopologyMigrationDtoList.size());
        for (ViewTopologyMigrationDto viewTopologyMigrationDto : viewTopologyMigrationDtoList) {
            handleNodeMigration(viewTopologyMigrationDto.getViewNodeMigrationDtoList(),
                    SubnetMigrationType.MIGRATION_OUT);

            handleLinkMigration(viewTopologyMigrationDto.getViewLinkMigrationDtoList(),
                    SubnetMigrationType.MIGRATION_OUT);
        }
        log.info("finish handle MIGRATION_OUT operation");
    }

    private void handleLinkMigration(List<ViewLinkMigrationDto> viewLinkMigrationDtoList,
            SubnetMigrationType migrationType) {
        if (CollectionUtils.isEmpty(viewLinkMigrationDtoList)) {
            log.debug("no migration link dto list is empty,skip type:{}", migrationType);
            return;
        }
        List<ViewLinkMigrationDto> migrationDtos = viewLinkMigrationDtoList.stream()
                .filter(viewLinkMigrationDto -> viewLinkMigrationDto.getMigrationType()
                        .equals(migrationType)).collect(
                        Collectors.toList());
        if (CollectionUtils.isEmpty(migrationDtos)) {
            log.debug("Migration view link is empty,skip");
            return;
        }
        for (ViewLinkMigrationDto viewLinkMigrationDto : migrationDtos) {
            String subnetId = viewLinkMigrationDto.getSubnetId();
            String viewLinkId = viewLinkMigrationDto.getViewLinkId();
            List<String> refLinkIds = viewLinkMigrationDto.getConnectionIds();
            Link viewLink = viewLinkDao.getViewLinkById(viewLinkId);
            if (SubnetMigrationType.MIGRATION_OUT.equals(migrationType)) {
                if (viewLink == null) {
                    log.warn("relative view link is not existed,skip");
                    continue;
                }
                migrationOutViewLink(viewLink, refLinkIds);
            } else {
                if (viewLink == null) {
                    createViewLink(viewLinkMigrationDto);
                    log.debug("viewLink MIGRATION_IN create success:viewLinkId={} subnetId={}",
                            viewLinkId,
                            subnetId);
                } else {
                    migrationInViewLink(viewLink, refLinkIds);
                    log.debug(
                            "node MIGRATION_IN update success viewLinkId={} subnetId={} add phyLinkIds={}",
                            viewLinkId, subnetId, refLinkIds);
                }
            }
        }


    }

    private void migrationInViewLink(Link viewLink, List<String> refLinkIds) {
        log.debug("migrationInViewLink viewLink id:{} migrationInLink:{}", viewLink.getLinkId(),
                refLinkIds);
        List<SupportingLink> supportingLinks = viewLink.getSupportingLink();
        List<SupportingLink> migrationInLinks = buildSupportingLink(new HashSet<>(refLinkIds));
        supportingLinks.addAll(migrationInLinks);
        int size = supportingLinks.size();
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.View viewLinkPhysical = viewLink.getAugmentation(
                Link1.class).getView();
        LinkBuilder linkBuilder = new LinkBuilder(viewLink);
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.ViewBuilder viewBuilder = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.ViewBuilder(
                viewLinkPhysical);
        viewBuilder.setBundleNumber(size);
        Link1Builder link1Builder = new Link1Builder();
        link1Builder.setView(viewBuilder.build());
        linkBuilder.setSupportingLink(supportingLinks);
        linkBuilder.addAugmentation(Link1.class, link1Builder.build());
        viewLinkDao.saveViewLink(linkBuilder.build());
    }

    private void createViewLink(ViewLinkMigrationDto viewLinkMigrationDto) {
        log.debug("create view link:{} ref linkId:{}", viewLinkMigrationDto.getViewLinkId(),
                viewLinkMigrationDto.getConnectionIds());
        Set<String> connectionIds = new HashSet<>(viewLinkMigrationDto.getConnectionIds());
        String subnetId = viewLinkMigrationDto.getSubnetId();
        String subnetName = viewLinkMigrationDto.getSubnetName();
        ViewLinkType viewLinkType = viewLinkMigrationDto.getViewLinkType();
        String sourceSite = ViewNodeNamingRule.generatedIdWithPlaneId(
                viewLinkMigrationDto.getSourceSite(), subnetId);
        String destSite = ViewNodeNamingRule.generatedIdWithPlaneId(
                viewLinkMigrationDto.getDestSite(), subnetId);
        String viewLinkId = viewLinkMigrationDto.getViewLinkId();
        int size = connectionIds.size();
        List<SupportingLink> supportingLinks = buildSupportingLink(connectionIds);
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.ViewBuilder viewBuilder = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.ViewBuilder();
        viewBuilder.setAlarmState(AlarmSeverity.Cleared);
        viewBuilder.setLevel(viewLinkType);
        viewBuilder.setBundleNumber(size);
        viewBuilder.setSubnetId(subnetId);
        viewBuilder.setSubnetName(subnetName);
        Link1Builder link1Builder = new Link1Builder();
        link1Builder.setView(viewBuilder.build());
        LinkBuilder linkBuilder = new LinkBuilder();
        linkBuilder.setSupportingLink(supportingLinks);
        linkBuilder.addAugmentation(Link1.class, link1Builder.build());
        linkBuilder.setSource(new SourceBuilder().setSourceNode(
                NodeId.getDefaultInstance(sourceSite)).build());
        linkBuilder.setDestination(new DestinationBuilder().setDestNode(
                NodeId.getDefaultInstance(destSite)).build());
        linkBuilder.setLinkId(LinkId.getDefaultInstance(viewLinkId));
        viewLinkDao.saveViewLink(linkBuilder.build());
    }

    private void migrationOutViewLink(Link viewLink, List<String> refLinkIds) {
        log.debug("migration out view link:{} phyLinkIds:{}", viewLink.getLinkId().getValue(),
                refLinkIds);
//        String subnetId = SubnetUtils.getViewLinkSubnetId(viewLink);
        Set<String> spLinkIds = viewLink.getSupportingLink().stream()
                .map(SupportingLink::getLinkRef).map(Uri::getValue).collect(Collectors.toSet());
        Set<String> outLinkIds = new HashSet<>(refLinkIds);
        Set<String> notOutLinkIds = CommonUtil.getDifferenceSetByGuava(spLinkIds, outLinkIds);
        if (notOutLinkIds.isEmpty()) {
            log.debug(
                    "migration out view link ref phyLinks and out phyLinks are completely consistent,clear view link:{}",
                    viewLink.getLinkId().getValue());
            viewLinkDao.deleteViewLink(viewLink.getLinkId().getValue());
        } else {
            List<SupportingLink> newSupportingLink = buildSupportingLink(notOutLinkIds);
            updateViewLink(viewLink, newSupportingLink);
        }

    }

    private List<SupportingLink> buildSupportingLink(Set<String> linkIds) {
        return linkIds.stream()
                .map(linkId -> new SupportingLinkBuilder().setLinkRef(
                        LinkId.getDefaultInstance(linkId)).build()).collect(
                        Collectors.toList());
    }

    private void updateViewLink(Link viewLink, List<SupportingLink> newSupportingLink) {
        log.debug("update viewLink :{} with supporting link:{}", viewLink.getLinkId(),
                newSupportingLink);
        LinkBuilder linkBuilder = new LinkBuilder(viewLink);
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.View viewLinkPhysical = viewLink.getAugmentation(
                Link1.class).getView();
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.ViewBuilder viewBuilder = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.ViewBuilder(
                viewLinkPhysical);
        int size = newSupportingLink.size();
        viewBuilder.setBundleNumber(size);
        viewBuilder.setAlarmState(AlarmSeverity.Cleared);
        Link1Builder link1Builder = new Link1Builder();
        link1Builder.setView(viewBuilder.build());
        linkBuilder.setSupportingLink(newSupportingLink);
        linkBuilder.addAugmentation(Link1.class, link1Builder.build());
        viewLinkDao.rewriteViewLink(linkBuilder.build());
    }

    private void handleNodeMigration(List<ViewNodeMigrationDto> viewNodeMigrationDtoList,
            SubnetMigrationType migrationType) {
        if (CollectionUtils.isEmpty(viewNodeMigrationDtoList)) {
            log.debug("no migration node dto list is empty,skip type:{}", migrationType);
            return;
        }
        List<ViewNodeMigrationDto> migrationViewNodes = viewNodeMigrationDtoList.stream()
                .filter(viewNodeMigrationDto -> viewNodeMigrationDto.getMigrationType()
                        .equals(migrationType)).collect(
                        Collectors.toList());
        if (migrationViewNodes.isEmpty()) {
            log.debug("Migration  view node is empty,skip");
            return;
        }
        for (ViewNodeMigrationDto viewNodeMigrationDto : migrationViewNodes) {
            String subnetId = viewNodeMigrationDto.getSubnetId();
            String viewNodeId = viewNodeMigrationDto.getViewNodeId();
            List<String> phyNodeIds = viewNodeMigrationDto.getPhyNodeId();
            Node viewNode = viewNodeDao.getViewNodeById(viewNodeId);
            if (SubnetMigrationType.MIGRATION_OUT.equals(migrationType)) {
                if (viewNode == null) {
                    log.warn("relative view node has not existed,skip");
                    continue;
                }
                migrationOutViewNode(viewNode, phyNodeIds);
                log.debug("node MIGRATION_OUT success:viewNodeId={} subnetId={} phyNodeIds={}",
                        viewNodeId, subnetId, phyNodeIds);
            } else {
                if (viewNode == null) {
                    //todo create view node
                    createViewNode(viewNodeMigrationDto);
                    log.debug("node MIGRATION_IN create success:viewNodeId={} subnetId={}",
                            viewNodeId,
                            subnetId);
                } else {
                    createParentNonLeafViewNode(viewNodeMigrationDto);
                    log.debug("node MIGRATION_IN update success viewNodeId={} subnetId={}",
                            viewNodeId, subnetId);

                }
            }
        }
    }


    private void migrationOutViewNode(Node viewNode, List<String> phyNodeIds) {
        log.debug("migration out view node:{} phyNodeIds:{}", viewNode.getNodeId().getValue(),
                phyNodeIds);
        String subnetId = SubnetUtils.getViewNodeSubnetId(viewNode);
        String refSiteId = ViewNodeNamingRule.extractSiteId(viewNode.getNodeId().getValue());
        List<String> siteSubRefPhyNodeId = phyNodeDao.listConfigOpticalPhyNodeIdsBySiteIdAndPlaneId(
                refSiteId, subnetId);
        boolean isListEqual = isListEqual(phyNodeIds, siteSubRefPhyNodeId);
        if (isListEqual) {
            log.debug(
                    "migration out view node ref phyNodeIds and site sub refPhyNodeId are completely consistent,clear view node:{}",
                    viewNode.getNodeId().getValue());
            viewNodeDao.deleteViewNode(viewNode.getNodeId().getValue());
        } else {
            log.debug(
                    "phyNodeIds and siteSubRefPhyNodeId are inconsistent, only remove specified phyNode association");
        }
    }

    private static boolean isListEqual(List<String> phyNodeIds, List<String> siteSubRefPhyNodeId) {
        boolean isListEqual = false;
        if (CollectionUtils.isEmpty(phyNodeIds) && CollectionUtils.isEmpty(siteSubRefPhyNodeId)) {
            isListEqual = true;
        } else {
            Set<String> outPhyNodeIds = new HashSet<>(phyNodeIds);
            Set<String> siteRefPhyNodeIds = new HashSet<>(siteSubRefPhyNodeId);
            Set<String> notInOutPhyNodeIds = CommonUtil.getDifferenceSetByGuava(siteRefPhyNodeIds,
                    outPhyNodeIds);
            isListEqual = notInOutPhyNodeIds.isEmpty();
        }
        return isListEqual;
    }


    private void createViewNode(ViewNodeMigrationDto viewNodeMigrationDto) {
        log.debug("create view node id:{}", viewNodeMigrationDto.getViewNodeId());
        createParentNonLeafViewNode(viewNodeMigrationDto);
        String subnetId = viewNodeMigrationDto.getSubnetId();
        String subnetName = viewNodeMigrationDto.getSubnetName();
        Integer subnetLevel = viewNodeMigrationDto.getSubnetLevel();
        String viewNodeId = viewNodeMigrationDto.getViewNodeId();
        String refSiteId = ViewNodeNamingRule.extractSiteId(viewNodeId);
        Node viewNode = viewNodeDao.getViewNodeById(refSiteId);
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(NodeId.getDefaultInstance(viewNodeId));
        View view = viewNode.getAugmentation(Node1.class).getView();
        Node1Builder node1Builder = new Node1Builder();
        ViewBuilder viewBuilder = new ViewBuilder(view);
        viewBuilder.setSubnetId(subnetId);
        viewBuilder.setSubnetName(subnetName);
        viewBuilder.setSubnetLevel(subnetLevel);
        viewBuilder.setAlarmState(AlarmSeverity.Cleared);
        node1Builder.setView(viewBuilder.build());
        nodeBuilder.addAugmentation(Node1.class, node1Builder.build());
        viewNodeDao.saveViewNode(nodeBuilder.build());
    }

    private void createParentNonLeafViewNode(ViewNodeMigrationDto viewNodeMigrationDto) {
        String subnetId = viewNodeMigrationDto.getSubnetId();
        SubNetTreeNode subnet = subNetTreeNodeDao.findBySubNetId(subnetId)
                .orElseThrow(() -> new CommonException(
                        CommonExceptionType.NOT_SUPPORT_ERROR,
                        "subnet is not found,subnet id:" + subnetId));
        String parentSubnetId = subnet.getParentId();
        if (parentSubnetId.equals(ROOT_NODE_ID)) {
            log.debug("current subnet:{} parent id is root id,skip", subnetId);
            return;
        }
        SubNetTreeNode parentSubnet = subNetTreeNodeDao.findBySubNetId(parentSubnetId)
                .orElseThrow(() -> new CommonException(
                        CommonExceptionType.NOT_SUPPORT_ERROR,
                        "subnet is not found,subnet id:" + parentSubnetId));
        String viewNodeId = viewNodeMigrationDto.getViewNodeId();
        String refSiteId = ViewNodeNamingRule.extractSiteId(viewNodeId);

        String parentViewNodeId = ViewNodeNamingRule.generatedIdWithPlaneId(refSiteId,
                parentSubnetId);
        Node parentViewNode = viewNodeDao.getViewNodeById(parentViewNodeId);
        if (parentViewNode != null) {
            log.debug("current subnet parent SubnetName:{} for site:{} existed,skip",
                    parentSubnet.getName(), refSiteId);
            return;
        }
        Node viewNode = viewNodeDao.getViewNodeById(refSiteId);
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(NodeId.getDefaultInstance(parentViewNodeId));
        View view = viewNode.getAugmentation(Node1.class).getView();

        Node1Builder node1Builder = new Node1Builder();
        ViewBuilder viewBuilder = new ViewBuilder(view);
        viewBuilder.setAlarmState(AlarmSeverity.Cleared);
        viewBuilder.setSubnetId(parentSubnet.getSubNetId());
        viewBuilder.setSubnetName(parentSubnet.getName());
        viewBuilder.setSubnetLevel(parentSubnet.getLevel());
        node1Builder.setView(viewBuilder.build());
        nodeBuilder.addAugmentation(Node1.class, node1Builder.build());
        viewNodeDao.saveViewNode(nodeBuilder.build());

    }
}
