package net.flex.dci.otn.controller.allocate.link.site.removenode;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import net.flex.dci.otn.controller.allocate.link.site.SiteLinkNodeOperationState;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * insert-node-in-site-link 的逆操作。
 *
 * <p>输入 node-id 是要从 siteLink 中移除的 ILA/DGE 光层设备。该操作不会删除业务 OCH，
 * 而是把当前 siteLink 内的 A--N、N--B 两条 OTS 合并回 A--B，同时删除 N 自身和相关 rack 关系。</p>
 */
@Slf4j
@Service
public class RemoveNodeInSiteLinkImpl {
    @Autowired
    private MultipleTransaction mongoTransaction;

    @Autowired
    private OchLinkDao ochLinkDao;

    public void doIt(TaskInfoMessage taskInfo, String siteLinkId, String nodeId) {
        ZkResourceLock locker = new ZkResourceLock();
        try {
            checkingRequiredInput(siteLinkId, nodeId);

            String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(nodeId);
            locker.addResource(siteLinkId);
            locker.addResource(nodeId);
            locker.addResource(siteNodeId);
            locker.getLock();

            ChangedObject changedObject = prepareChanges(siteLinkId, nodeId);
            taskInfo.setResourceId(siteLinkId);
            taskInfo.setResourceName(changedObject.getChangedSiteLink(siteLinkId)
                    .getAugmentation(Link1.class).getSite().getFriendlyName());
            changedObject.siteLinkRouteChange(siteLinkId);
            mongoTransaction.save(changedObject);
        } catch (CommonException ce) {
            taskInfo.setSuccessfully(false);
            taskInfo.setErrorReason(ce.getMessage());
            TaskInfoMessager.sendMessage(taskInfo);
            throw ce;
        } catch (Exception e) {
            taskInfo.setSuccessfully(false);
            taskInfo.setErrorReason(e.getMessage());
            TaskInfoMessager.sendMessage(taskInfo);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ExceptionUtils.getRootCauseMessage(e), e);
        } finally {
            locker.unlock();
        }
    }

    private void checkingRequiredInput(String siteLinkId, String nodeId) {
        if (siteLinkId == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "site-link-id is required");
        }
        if (nodeId == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "node-id is required");
        }
    }

    /**
     * Build DB changes for removing an inserted ILA/DGE from a siteLink, without saving them.
     * Implement app uses this to first remove/create A/B internalLinks on devices; only after the
     * device action succeeds should these prepared DB changes be persisted.
     */
    public ChangedObject prepareChanges(String siteLinkId, String nodeId) {
        checkingRequiredInput(siteLinkId, nodeId);

        String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(nodeId);
        // 这是 Spring 单例 Service，请求级对象必须保持在局部变量里，避免并发 remove 互相覆盖。
        ChangedObject changedObject = new ChangedObject();
        RemoveNodeInSiteLinkContext context = checking(changedObject, siteLinkId, nodeId, siteNodeId);
        new RemoveNodeInSiteLinkOperation(context, changedObject).execute();
        Link changedSiteLink = changedObject.getChangedSiteLink(siteLinkId);
        changedObject.addChangedSiteLink(SiteLinkNodeOperationState.remove(changedSiteLink));
        return changedObject;
    }

    private RemoveNodeInSiteLinkContext checking(ChangedObject changedObject, String siteLinkId, String nodeId,
            String siteNodeId) {
        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);
        if (siteLink == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "cannot find required siteLink");
        }
        String operationState = SiteLinkNodeOperationState.get(siteLink);
        if (operationState != null
                && !SiteLinkNodeOperationState.WAITING_INSERT_APPLY.equals(operationState)
                && !SiteLinkNodeOperationState.INSERT_APPLY_FAILED_BEFORE_DEVICE.equals(
                        operationState)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "siteLink is processing a node operation: " + operationState);
        }
        if (SiteLinkNodeOperationState.WAITING_INSERT_APPLY.equals(operationState)
                || SiteLinkNodeOperationState.INSERT_APPLY_FAILED_BEFORE_DEVICE.equals(
                        operationState)) {
            String operationNodeId = SiteLinkNodeOperationState.getNodeId(siteLink);
            if (!nodeId.equals(operationNodeId)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "siteLink node operation belongs to " + operationNodeId
                                + ", cannot remove node " + nodeId);
            }
        }
        Node removedPhyNode = changedObject.getChangedPhyNode(nodeId);
        if (removedPhyNode == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "cannot find required phy node");
        }
        if (siteLink.getSource() != null && nodeId.equals(siteLink.getSource().getSourceNode().getValue())
                || siteLink.getDestination() != null && nodeId.equals(siteLink.getDestination().getDestNode().getValue())) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "node-id can not be the A/Z site node of siteLink");
        }
        if (changedObject.getChangedSiteNode(siteNodeId) == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "cannot find site node of removed node");
        }

        List<String> siteLinkIds = new ArrayList<>();
        siteLinkIds.add(siteLinkId);
        List<Link> ochLinks = ochLinkDao.getAllOchLinksUnderSiteLinkIds(siteLinkIds);

        String planeId = siteLink.getAugmentation(Link1.class).getSite().getPlaneId();
        return new RemoveNodeInSiteLinkContext(siteLinkId, nodeId, siteNodeId, planeId, siteLink, removedPhyNode,
                ochLinks);
    }
}
