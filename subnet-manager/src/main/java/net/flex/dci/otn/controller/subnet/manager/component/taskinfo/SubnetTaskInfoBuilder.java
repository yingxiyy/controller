package net.flex.dci.otn.controller.subnet.manager.component.taskinfo;

import java.util.List;
import java.util.Map;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import net.flex.dci.otn.controller.subnet.manager.dto.MigrationSiteLinkInfo.MigrationSiteLink;
import net.flex.dci.otn.controller.subnet.manager.dto.MigrationTunnelInfo.MigrationTunnel;
import net.flex.dci.otn.controller.subnet.manager.dto.output.DeleteResult;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;

/**
 * 2026/2/28
 *
 * @author musa
 * @version 1.0
 **/

public interface SubnetTaskInfoBuilder {

    TaskInfoMessage buildCreateSubNetTaskInfo(String author, String subNetName,
            String parentSubnetId);

    TaskInfoMessage buildModifySubnetNameTaskInfo(String author, String newSubnetName,
            String subnetId);

    TaskInfoMessage buildSuccessSubNetTaskInfo(TaskInfoMessage taskInfoMessage,
            SubNetTreeNode newNode);

    TaskInfoMessage buildFailedCreateSubNetTaskInfo(TaskInfoMessage taskInfoMessage,
            String errorMsg);

    TaskInfoMessage buildSuccessCreateSubNetTaskInfo(TaskInfoMessage taskInfoMessage,
            SubNetTreeNode newNode);

    void buildSuccessModifySubNetTaskInfo(TaskInfoMessage taskInfoMessage, SubNetTreeNode newNode);

    void buildFailedModifySubNetTaskInfo(TaskInfoMessage taskInfoMessage, String errorMsg);

    TaskInfoMessage buildDeleteSubnetTaskInfoMessage(String author, String subNetName,
            String subnetId);

    void buildSuccessRemoveSubnetTaskInfo(TaskInfoMessage taskInfoMessage,
            DeleteResult deleteResult);

    void buildFailedRemoveSubnetTaskInfo(TaskInfoMessage taskInfoMessage, String message);

    TaskInfoMessage buildSubnetMigrationTask(SubNetTreeNode subnet, List<Link> siteLinks,
            List<Tunnel> tunnels,
            String author);

    TaskInfoMessage buildSubnetMigrationTask(List<MigrationSiteLink> migrationSiteLinks,
            List<MigrationTunnel> migrationTunnels, Map<String, Link> siteLinkMap,
            Map<String, Tunnel> tunnelMap,
            Map<String, SubNetTreeNode> subNetTreeNodes,
            String author);

    void buildSuccessMigrationTask(TaskInfoMessage taskInfoMessage);

    void buildFailedMigrationTask(TaskInfoMessage taskInfoMessage, String message);
}
