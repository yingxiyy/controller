/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.node.site;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.ViewNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RemoveSitesInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RemoveSitesOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RemoveSitesOutputBuilder;


/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
public class SiteNodeRemover {

    protected ChangedObject changedObject;
//  private TaskInfoKafkaService kafka;
    protected MultipleTransaction mongoTransaction;
    private TaskInfoMessage taskInfoMessage;

    private static SiteNodeDao siteNodeDao = SpringBeanFinder.getBean(SiteNodeDao.class);
    private static ViewNodeDao viewNodeDao = SpringBeanFinder.getBean(ViewNodeDao.class);

    public SiteNodeRemover() {
        init();
    }

    private void init() {
        mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);

        changedObject = new ChangedObject();
    }

    public RemoveSitesOutput doIt(RemoveSitesInput input) throws CommonException {
        List<Node> want2RemovedNodeList = check(input);
        RemoveSitesOutputBuilder outputBuilder = new RemoveSitesOutputBuilder();

        ZkResourceLock allocateLocker = new ZkResourceLock();
        try {
            allocateLocker.addResource(TopoNameConstants.Site_Topo_Key);
            allocateLocker.getLock();

            for (Node siteNode : want2RemovedNodeList) {
                //由于添加了planeViewNode 的功能，这些点也需要同时删除
                List<Node> viewNodes = viewNodeDao.listViewNodesBySiteId(siteNode.getNodeId().getValue());
                viewNodes.forEach(x->changedObject.addRemovedViewNode(x.getNodeId().getValue()));

                changedObject.addRemovedSiteNode(siteNode);
            }

            mongoTransaction.save(changedObject);
            outputBuilder.setReturnCode(RpcResultType.Success);
            logMessage(want2RemovedNodeList, null);
            log.debug("remove siteNode done");
        } catch (CommonException ce) {
            logMessage(null, ce.getMessage());
            log.error("remove siteNode file", ce);
            throw ce;
        } catch (Exception e) {
            logMessage(null, e.toString());
            log.error("remove siteNode file", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.toString(), e);
        } finally {
            allocateLocker.unlock();
        }

        return outputBuilder.build();
    }

    private void logMessage(List<Node> removedNodeList, String errorMessage) {
        String msg = String.format("remove siteNodes");

        String extMsg = "fail";
        boolean isOk = false;
        if (errorMessage == null) {
            //成功
            isOk = true;
            extMsg = "successfully.";
        }

        if (taskInfoMessage != null) {
            taskInfoMessage.setEndTime(System.currentTimeMillis());
            if (isOk) {
                if (removedNodeList.size() > 1) {//send a summarize
                    taskInfoMessage.setResourceId(msg);
                    taskInfoMessage.setResourceName("siteNodes");
                    taskInfoMessage.setSuccessfully(isOk);
                    taskInfoMessage.setErrorReason(null);

                    TaskInfoMessager.sendMessage(taskInfoMessage);
                    taskInfoMessage.setDetail(null);  //存储单个siteNode的时候detail不需要了
                }

                for (Node node : removedNodeList) {
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

    private List<Node> check(RemoveSitesInput input) throws CommonException {
        if (input.getNodeId() == null || input.getNodeId().isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "pls check input, nodeId is mandatory.");
        }

        List<Node> want2RemovedNodeList = new ArrayList<>();
        for (String nodeId : input.getNodeId()) {
            want2RemovedNodeList.add(checkSiteDeleteOp(nodeId));
        }
        return want2RemovedNodeList;
    }

    private Node checkSiteDeleteOp(String nodeId) throws CommonException {
        Node siteNode = siteNodeDao.getSiteNodeById(nodeId);
        if (siteNode == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("Site %s does not exist.", nodeId));
        }
        if (siteNode != null) {// should be site node.
            List<SupportingNode> list = siteNode.getSupportingNode();
            if (list != null && list.size() > 0) {
                // if no ne is contained in this site, then it can be removed.
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        String.format("Site %s has sub nodes and cannot be deleted.",
                                siteNode.getAugmentation(Node1.class).getSite().getFriendlyName()));
            }
        }
        return siteNode;
    }

    public SiteNodeRemover setTaskInfo(TaskInfoMessage taskInfoMessage) {
        this.taskInfoMessage = taskInfoMessage;
//    this.kafka = SpringBeanFinder.getBean(TaskInfoKafkaService.class);

        return this;
    }
}
