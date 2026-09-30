package net.flex.dci.otn.controller.subnet.manager.component.taskinfo;

import static net.flex.dci.otc.common.constants.BroadCastConstant.CREATE_SUBNET;
import static net.flex.dci.otc.common.constants.BroadCastConstant.MODIFY_SUBNET;
import static net.flex.dci.otc.common.constants.BroadCastConstant.SUBNET_MIGRATION;
import static net.flex.dci.otn.controller.subnet.manager.utils.Constants.CREATE_SUBNET_PARENT_ID;
import static net.flex.dci.otn.controller.subnet.manager.utils.Constants.FAILED;
import static net.flex.dci.otn.controller.subnet.manager.utils.Constants.MIGRATE_SUBNET;
import static net.flex.dci.otn.controller.subnet.manager.utils.Constants.MODIFY_SUBNET_NAME;
import static net.flex.dci.otn.controller.subnet.manager.utils.Constants.REMOVE_SUBNET;

import com.alibaba.fastjson.JSON;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.model.TaskInfoMessage.ResourceType;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.subnet.manager.dto.MigrationSiteLinkInfo.MigrationSiteLink;
import net.flex.dci.otn.controller.subnet.manager.dto.MigrationTunnelInfo.MigrationTunnel;
import net.flex.dci.otn.controller.subnet.manager.dto.output.DeleteResult;
import net.flex.dci.otn.controller.subnet.manager.dto.task.SubNetTaskOperationDetail;
import net.flex.dci.otn.controller.subnet.manager.dto.task.SubnetCreateMessage;
import net.flex.dci.otn.controller.subnet.manager.dto.task.SubnetDeleteMessage;
import net.flex.dci.otn.controller.subnet.manager.dto.task.SubnetMigrateMessage;
import net.flex.dci.otn.controller.subnet.manager.dto.task.SubnetMigrateSiteLinkInfo;
import net.flex.dci.otn.controller.subnet.manager.dto.task.SubnetMigrateTunnelInfo;
import net.flex.dci.otn.controller.subnet.manager.dto.task.SubnetModifyMessage;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;

/**
 * 2026/2/28
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class SubnetTaskInfoBuilderImpl implements SubnetTaskInfoBuilder {

    private final SubnetTaskNotification subnetTaskNotification;

    private final SubNetTreeNodeDao subNetTreeNodeDao;

    @Override
    public TaskInfoMessage buildCreateSubNetTaskInfo(String author,
            String subNetName, String parentSubnetId) {
        log.info(
                "build create subnet task info,the author is:{} createSubnet name:{} parentSubnetId:{}",
                author, subNetName, parentSubnetId);

        SubnetCreateMessage subnetCreateMessage = SubnetCreateMessage.builder()
                .author(author)
                .subnetName(subNetName)
                .subnetParentId(parentSubnetId)
                .executeTimestamp(System.currentTimeMillis())
                .build();
        SubNetTaskOperationDetail subNetTaskOperationDetail = SubNetTaskOperationDetail.builder()
                .request(subnetCreateMessage).build();
        String operationDetail = JSON.toJSONString(subNetTaskOperationDetail);
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(author, ResourceType.subnet,
                ActionType.subnetCreate, operationDetail);
        String resouceId = CREATE_SUBNET_PARENT_ID + parentSubnetId;
        String resourceName = String.format("CREATE_SUBNET_%s", subNetName);
        taskInfoMessage.setResourceId(resouceId);
        taskInfoMessage.setResourceName(resourceName);
        subnetTaskNotification.sendStartNotification(CREATE_SUBNET,
                taskInfoMessage);
        return taskInfoMessage;
    }

    @Override
    public TaskInfoMessage buildModifySubnetNameTaskInfo(String author, String newSubnetName,
            String subnetId) {
        log.info("build modify subnet new name :{} subnetId:{}", newSubnetName, subnetId);
        SubNetTreeNode subNetTreeNode = subNetTreeNodeDao.findBySubNetId(subnetId)
                .orElseThrow(() -> new CommonException(
                        CommonExceptionType.INVALID_PARAMETER,
                        "current subnet node  " + subnetId + " is not found"));
        String oldSubnetName = subNetTreeNode.getName();
        SubnetModifyMessage subnetModifyMessage = SubnetModifyMessage.builder()
                .newSubnetName(newSubnetName)
                .author(author)
                .executeTimestamp(System.currentTimeMillis())
                .subnetId(subnetId)
                .oldSubnetName(oldSubnetName)
                .build();
        SubNetTaskOperationDetail subNetTaskOperationDetail = SubNetTaskOperationDetail.builder()
                .request(subnetModifyMessage).build();
        String operationDetail = JSON.toJSONString(subNetTaskOperationDetail);
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(author, ResourceType.subnet,
                ActionType.subnetModify, operationDetail);
        String resouceId = MODIFY_SUBNET_NAME + subnetId;
        String resourceName = String.format("SubNet %s update to %s", oldSubnetName, newSubnetName);
        taskInfoMessage.setResourceId(resouceId);
        taskInfoMessage.setResourceName(resourceName);
        subnetTaskNotification.sendStartNotification(MODIFY_SUBNET,
                taskInfoMessage);
        return taskInfoMessage;
    }

    @Override
    public TaskInfoMessage buildDeleteSubnetTaskInfoMessage(String author, String subNetName,
            String subnetId) {
        log.info("build delete subnet task info message subnet name:{} subnet id:{}", subNetName,
                subnetId);
        SubnetDeleteMessage subnetDeleteMessage = SubnetDeleteMessage.builder()
                .executeTimestamp(System.currentTimeMillis())
                .subnetName(subNetName).subnetId(subnetId).author(author).build();
        SubNetTaskOperationDetail subNetTaskOperationDetail = SubNetTaskOperationDetail.builder()
                .request(subnetDeleteMessage).build();
        String operationDetail = JSON.toJSONString(subNetTaskOperationDetail);
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(author, ResourceType.subnet,
                ActionType.subnetDelete, operationDetail);
        String resouceId = REMOVE_SUBNET + subnetId;
        String resourceName = String.format("SubNet %s delete", subNetName);
        taskInfoMessage.setResourceId(resouceId);
        taskInfoMessage.setResourceName(resourceName);
        subnetTaskNotification.sendStartNotification(BroadCastConstant.REMOVE_SUBNET,
                taskInfoMessage);
        return taskInfoMessage;
    }

    @Override
    public void buildSuccessRemoveSubnetTaskInfo(TaskInfoMessage taskInfoMessage,
            DeleteResult deleteResult) {
        log.debug("build success remove subnet task info");
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        subnetTaskNotification.sendSuccessNotification(BroadCastConstant.REMOVE_SUBNET,
                taskInfoMessage);
    }

    @Override
    public void buildFailedRemoveSubnetTaskInfo(TaskInfoMessage taskInfoMessage, String errorMsg) {
        log.info("build failed remove subnet taskInfo");
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        taskInfoMessage.setErrorReason(FAILED);
        sendFailedNotification(BroadCastConstant.REMOVE_SUBNET, errorMsg, taskInfoMessage);

    }

    @Override
    public TaskInfoMessage buildSubnetMigrationTask(SubNetTreeNode subnet, List<Link> siteLinks,
            List<Tunnel> tunnels,
            String author) {
        log.info("build migrate subnet task info resource migration to subnet name:{} subnet id:{}",
                subnet.getName(),
                subnet.getSubNetId());
        List<SubnetMigrateSiteLinkInfo> migrateLinks = buildMigrateLink(siteLinks);
        List<SubnetMigrateTunnelInfo> migrateTunnels = buildMigrateTunnel(tunnels);
        SubnetMigrateMessage migrateMessage = SubnetMigrateMessage.builder()
                .author(author)
                .executeTimestamp(System.currentTimeMillis())
                .migrateSubnetId(subnet.getSubNetId())
                .migrateSubnetName(subnet.getName())
                .migrateSiteLinkInfos(migrateLinks)
                .migrateTunnelInfos(migrateTunnels)
                .build();
        SubNetTaskOperationDetail subNetTaskOperationDetail = SubNetTaskOperationDetail.builder()
                .request(migrateMessage)
                .build();
        String operationDetail = JSON.toJSONString(subNetTaskOperationDetail);
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(author, ResourceType.subnet,
                ActionType.subnetMigrate, operationDetail);
        String resouceId = MIGRATE_SUBNET + subnet.getSubNetId();
        String resourceName = String.format("Migrate Reuse Segment to Subnet - %s",
                subnet.getName());
        taskInfoMessage.setResourceId(resouceId);
        taskInfoMessage.setResourceName(resourceName);
        subnetTaskNotification.sendStartNotification(BroadCastConstant.SUBNET_MIGRATION,
                taskInfoMessage);
        return taskInfoMessage;
    }

    @Override
    public TaskInfoMessage buildSubnetMigrationTask(List<MigrationSiteLink> migrationSiteLinks,
            List<MigrationTunnel> migrationTunnels, Map<String, Link> siteLinkMap,
            Map<String, Tunnel> tunnelMap, Map<String, SubNetTreeNode> subNetTreeNodes,
            String author) {
        log.info("build migrate subnet task info resource migration {} sieLink(s) and {} tunnel(s)",
                migrationSiteLinks.size(), migrationTunnels.size());
        List<SubnetMigrateSiteLinkInfo> migrateLinks = buildMigrateLink(migrationSiteLinks,
                siteLinkMap, subNetTreeNodes);
        List<SubnetMigrateTunnelInfo> migrateTunnels = buildMigrateTunnel(migrationTunnels,
                tunnelMap, subNetTreeNodes);
        SubnetMigrateMessage migrateMessage = SubnetMigrateMessage.builder()
                .author(author)
                .executeTimestamp(System.currentTimeMillis())
                .migrateSiteLinkInfos(migrateLinks)
                .migrateTunnelInfos(migrateTunnels)
                .build();
        SubNetTaskOperationDetail subNetTaskOperationDetail = SubNetTaskOperationDetail.builder()
                .request(migrateMessage)
                .build();
        String operationDetail = JSON.toJSONString(subNetTaskOperationDetail);
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(author, ResourceType.subnet,
                ActionType.subnetMigrate, operationDetail);
        String resourceId = String.format("SUBNET_MIGRATION_%d", System.currentTimeMillis());

        String resourceName = buildTaskName(migrationSiteLinks.size(), migrationTunnels.size());
        taskInfoMessage.setResourceId(resourceId);
        taskInfoMessage.setResourceName(resourceName);
        subnetTaskNotification.sendStartNotification(BroadCastConstant.SUBNET_MIGRATION,
                taskInfoMessage);
        return taskInfoMessage;
    }

    private String buildTaskName(int siteLinkCount, int tunnelCount) {
        StringBuilder sb = new StringBuilder("Subnet Migration Task - ");

        if (siteLinkCount > 0 && tunnelCount > 0) {
            sb.append(String.format("Migrate %d site links and %d tunnels", siteLinkCount,
                    tunnelCount));
        } else if (siteLinkCount > 0) {
            sb.append(String.format("Migrate %d site link%s",
                    siteLinkCount, siteLinkCount > 1 ? "s" : ""));
        } else if (tunnelCount > 0) {
            sb.append(String.format("Migrate %d tunnel%s",
                    tunnelCount, tunnelCount > 1 ? "s" : ""));
        } else {
            sb.append("Empty Migration Task");
        }

        return sb.toString();
    }

    private List<SubnetMigrateTunnelInfo> buildMigrateTunnel(List<MigrationTunnel> migrationTunnels,
            Map<String, Tunnel> tunnelMap, Map<String, SubNetTreeNode> subNetTreeNodes) {
        List<SubnetMigrateTunnelInfo> subnetMigrateTunnelInfos = migrationTunnels.stream()
                .map(tunnel -> {
                    String tunnelId = tunnel.getTunnelId();
                    Tunnel refTunnel = tunnelMap.get(tunnelId);
                    String subnetId = refTunnel.getPlaneId();
                    String subnetName = refTunnel.getPlaneName();
                    String friendlyName = refTunnel.getFriendlyName();
                    String targetSubnetId = tunnel.getTargetSubnet();
                    String targetSubnetName = subNetTreeNodes.get(targetSubnetId).getName();
                    return SubnetMigrateTunnelInfo.builder().tunnelName(friendlyName)
                            .tunnelId(tunnelId)
                            .oldSubnetId(subnetId).oldSubnetName(subnetName)
                            .targetSubnetId(targetSubnetName)
                            .targetSubnetName(targetSubnetName)
                            .build();
                })
                .collect(
                        Collectors.toList());
        return subnetMigrateTunnelInfos;
    }

    private List<SubnetMigrateSiteLinkInfo> buildMigrateLink(
            List<MigrationSiteLink> migrationSiteLinks, Map<String, Link> siteLinkMap,
            Map<String, SubNetTreeNode> subNetTreeNodes) {
        List<SubnetMigrateSiteLinkInfo> subnetMigrateSiteLinkInfos = migrationSiteLinks.stream()
                .map(siteLink -> {
                    Link refSiteLink = siteLinkMap.get(siteLink.getSiteLinkId());
                    Site site = refSiteLink.getAugmentation(Link1.class).getSite();
                    String siteLinkId = siteLink.getSiteLinkId();
                    String subnetId = site.getPlaneId();
                    String subnetName = site.getPlaneName();
                    String friendlyName = site.getFriendlyName();
                    String targetSubnetId = siteLink.getTargetSubnet();
                    String targetSubnetName = subNetTreeNodes.get(targetSubnetId).getName();
                    return SubnetMigrateSiteLinkInfo.builder().siteLinkName(friendlyName)
                            .siteLinkId(siteLinkId)
                            .oldSubnetId(subnetId).oldSubnetName(subnetName)
                            .targetSubnetId(targetSubnetId)
                            .targetSubnetName(targetSubnetName)
                            .build();
                })
                .collect(
                        Collectors.toList());
        return subnetMigrateSiteLinkInfos;
    }


    private List<SubnetMigrateTunnelInfo> buildMigrateTunnel(List<Tunnel> tunnels) {
        List<SubnetMigrateTunnelInfo> subnetMigrateTunnelInfos = tunnels.stream()
                .map(tunnel -> {
                    String tunnelId = tunnel.getTunnelId().getValue();
                    String subnetId = tunnel.getPlaneId();
                    String subnetName = tunnel.getPlaneName();
                    String friendlyName = tunnel.getFriendlyName();
                    return SubnetMigrateTunnelInfo.builder().tunnelName(friendlyName)
                            .tunnelId(tunnelId)
                            .oldSubnetId(subnetId).oldSubnetName(subnetName).build();
                })
                .collect(
                        Collectors.toList());
        return subnetMigrateTunnelInfos;
    }

    private List<SubnetMigrateSiteLinkInfo> buildMigrateLink(List<Link> siteLinks) {
        List<SubnetMigrateSiteLinkInfo> subnetMigrateSiteLinkInfos = siteLinks.stream()
                .map(siteLink -> {
                    Site site = siteLink.getAugmentation(Link1.class).getSite();
                    String siteLinkId = siteLink.getLinkId().getValue();
                    String subnetId = site.getPlaneId();
                    String subnetName = site.getPlaneName();
                    String friendlyName = site.getFriendlyName();
                    return SubnetMigrateSiteLinkInfo.builder().siteLinkName(friendlyName)
                            .siteLinkId(siteLinkId)
                            .oldSubnetId(subnetId).oldSubnetName(subnetName).build();
                })
                .collect(
                        Collectors.toList());
        return subnetMigrateSiteLinkInfos;
    }

    @Override
    public void buildSuccessMigrationTask(TaskInfoMessage taskInfoMessage) {
        log.info("build success migrate subnet task info");
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        subnetTaskNotification.sendSuccessNotification(BroadCastConstant.SUBNET_MIGRATION,
                taskInfoMessage);

    }

    @Override
    public void buildFailedMigrationTask(TaskInfoMessage taskInfoMessage, String errorMsg) {
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        taskInfoMessage.setErrorReason(FAILED);
        sendFailedNotification(SUBNET_MIGRATION, errorMsg, taskInfoMessage);

    }

    @Override
    public TaskInfoMessage buildSuccessSubNetTaskInfo(TaskInfoMessage taskInfoMessage,
            SubNetTreeNode subnetNode) {
        log.info("build success subnet task info");
        return null;
    }

    @Override
    public TaskInfoMessage buildFailedCreateSubNetTaskInfo(TaskInfoMessage taskInfoMessage,
            String errorMsg) {
        log.info("build failed subnet taskInfo");
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        taskInfoMessage.setErrorReason(FAILED);
        sendFailedNotification(CREATE_SUBNET, errorMsg, taskInfoMessage);
        return taskInfoMessage;
    }

    private void sendFailedNotification(String broadCastTitle, String errorMsg,
            TaskInfoMessage taskInfoMessage) {
        subnetTaskNotification.sendFailedNotification(broadCastTitle, errorMsg, taskInfoMessage);
    }

    @Override
    public TaskInfoMessage buildSuccessCreateSubNetTaskInfo(TaskInfoMessage taskInfoMessage,
            SubNetTreeNode newNode) {
        log.info("build success create subnet task info");
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        subnetTaskNotification.sendSuccessNotification(CREATE_SUBNET, taskInfoMessage);
        return taskInfoMessage;
    }

    @Override
    public void buildSuccessModifySubNetTaskInfo(TaskInfoMessage taskInfoMessage,
            SubNetTreeNode newNode) {
        log.info("build and send success modify subnet task info");
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        subnetTaskNotification.sendSuccessNotification(MODIFY_SUBNET, taskInfoMessage);
    }

    @Override
    public void buildFailedModifySubNetTaskInfo(TaskInfoMessage taskInfoMessage, String errorMsg) {
        log.info("build and send failed modify subnet task info");
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        taskInfoMessage.setErrorReason(FAILED);
        sendFailedNotification(MODIFY_SUBNET, errorMsg, taskInfoMessage);
    }


}
