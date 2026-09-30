/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.node.site;

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.allocate.common.namingrule.SiteNodeFriendlyName;
import net.flex.dci.otn.controller.allocate.node.view.ViewNode;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateSiteInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateSiteOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateSiteOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder;
import org.springframework.stereotype.Component;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpdateSite {

    //  private TaskInfoKafkaService kafka;
    private final MultipleTransaction mongoTransaction;
    private final SiteNodeDao siteNodeDao;
    private final SiteNodeFriendlyName siteNodeFriendlyName;
    private TaskInfoMessage taskInfoMessage;

    public UpdateSiteOutput updateSiteByInput(UpdateSiteInput input) throws CommonException {
        log.info("update site input is:{}", input);
        validateInput(input);
        String siteNodeId = input.getNodeId().getValue();
        Node siteNode = siteNodeDao.getSiteNodeById(siteNodeId);
        log.debug("update site id is:{}", siteNodeId);
        ZkResourceLock allocateLocker = new ZkResourceLock();
        try {
            allocateLocker.addResource(siteNodeId);
            allocateLocker.getLock();
            String friendlyName = input.getSite().getFriendlyName();
            ChangedObject changedObject = new ChangedObject();
            changedObject.addChangedSiteNode(updateSiteFriendlyName(siteNode, friendlyName));
            Node viewNode = changedObject.getChangedViewNode(siteNodeId);
            if (viewNode != null) {
                changedObject.addChangedViewNode(
                        new ViewNode(input.getNodeId()).updateFriendlyName(viewNode, friendlyName));
            }
            mongoTransaction.save(changedObject);
            logMessage(changedObject.getChangedSiteNodeList().values(), null);
            UpdateSiteOutputBuilder outputBuilder = new UpdateSiteOutputBuilder();
            outputBuilder.setReturnCode(RpcResultType.Success);
            log.debug("update site node friendlyName done ");
            return outputBuilder.build();
        } catch (CommonException e) {
            logMessage(null, e.getMessage());
            log.error("update site node friendlyName fail ", e);
            throw e;
        } catch (Exception e) {
            logMessage(null, e.toString());
            log.error("update site node friendlyName fail ", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.toString(), e);
        } finally {
            allocateLocker.unlock();
        }
    }

    private void logMessage(Collection<Node> updatedSiteNodeList, String errorMessage) {
        String msg = "create siteNodes";

        String extMsg = "fail";
        boolean isOk = false;
        if (errorMessage == null) {
            //成功
            isOk = true;
            extMsg = "successfully.";
        }

        if (taskInfoMessage != null) {
            if (isOk) {
                if (updatedSiteNodeList.size() > 1) {//send a summarize
                    taskInfoMessage.setResourceId(msg);
                    taskInfoMessage.setResourceName("siteNodes");
                    taskInfoMessage.setSuccessfully(isOk);
                    taskInfoMessage.setErrorReason(null);

                    TaskInfoMessager.sendMessage(taskInfoMessage);
                    taskInfoMessage.setDetail(null);  //存储单个siteNode的时候detail不需要了
                }

                for (Node node : updatedSiteNodeList) {
                    taskInfoMessage.setResourceId(node.getNodeId().getValue());
                    taskInfoMessage.setResourceName(
                            node.getAugmentation(Node1.class).getSite().getFriendlyName());
                    taskInfoMessage.setSuccessfully(isOk);

                    TaskInfoMessager.sendMessage(taskInfoMessage);
                }
            } else {
                taskInfoMessage.setResourceId(msg);
                taskInfoMessage.setResourceName("siteNodes");
                taskInfoMessage.setSuccessfully(isOk);
                taskInfoMessage.setErrorReason(errorMessage);

                TaskInfoMessager.sendMessage(taskInfoMessage);
            }
        }
    }

    private void validateInput(UpdateSiteInput input) throws CommonException {
        if (input.getNodeId() == null || input.getNodeId().getValue() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Failed to update site because of the nodeId is null.");
        }
        String siteNodeId = input.getNodeId().getValue();
        if (input.getSite() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Input data is not include site config.");
        }
        Node siteNode = isSiteExist(siteNodeId);
        if (input.getSite().getFriendlyName() != null) {
            siteNodeFriendlyName.checkFridendlyName(input.getSite().getFriendlyName());
        }
    }

    private Node isSiteExist(String nodeId) throws CommonException {
        Node node = siteNodeDao.getSiteNodeById(nodeId);
        if (node == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("Failed to update site %s because it does not exist", nodeId));
        }
        return node;
    }

    private Node updateSiteFriendlyName(Node dbNode, String friendlyName) {
        return new NodeBuilder(dbNode)
                .addAugmentation(Node1.class, new Node1Builder()
                        .setSite(new SiteBuilder(dbNode.getAugmentation(Node1.class).getSite())
                                .setFriendlyName(friendlyName)
                                .build())
                        .build())
                .build();
    }

    public void setTaskInfo(TaskInfoMessage taskInfoMessage) {
        log.debug("handle the taskInfo message");
        this.taskInfoMessage = taskInfoMessage;
    }


}
