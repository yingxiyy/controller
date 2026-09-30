/*
 *
 *  * Copyright (c) 2021-2020 Network Flex Any Comp. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package net.flex.dci.otn.controller.allocate.network;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.allocate.link.common.CreateSiteLinkParam;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateLink2Input;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateLink2InputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateNetworkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateNetworkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateNetworkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreationParams;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.LinkRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.network.param.CreateLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.create.link._2.input.LinkComputeResultBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.compute.result.MainBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.compute.result.SlaveBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.compute.result.ThirdBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.network.sitelinks.SiteLinks;


/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
public class NetworkCreator {
//  private TaskInfoKafkaService kafka;

    protected TaskInfoMessage taskInfoMessage;
    //following attributes are created/updated. should save to DB.
    //============
    protected ChangedObject changedObject;
    private Map<String, CreateSiteLinkParam> paramMap;
    private String networkName;
    //============

    public NetworkCreator() {
        init();
    }

    private void init() {
//    this.kafka = SpringBeanFinder.getBean(TaskInfoKafkaService.class);

        paramMap = new HashMap<>();
        changedObject = new ChangedObject();
    }

    /**
     * 创建长传系统，有若干siteLink 和 WSSLink 构成 网络
     *
     * @param input
     * @return
     * @throws CommonException
     */
    public CreateNetworkOutput doIt(CreateNetworkInput input) throws CommonException {
        log.debug("create network start...");
        //所有的必要参数在checkInput中都提取，且放在类变量中
        parse(input);
        checking(input);  //检查新加的subnetId(planeId),

        //由于耗时, 把这个同步命令改为异步
        //new version, 不能用同步处理，一个network的创建过程中，涉及若干siteLink的创建
        //如果同时创建siteLink， 这些siteLink直接的互斥也需要考虑，如siteNode, viewLink的更新必须互斥完成
        lazy(input);

        return new CreateNetworkOutputBuilder()
                .setReturnCode(RpcResultType.AcceptAndStartAsync)
                .setReturnMessage("lazy creation start.")
                .build();
    }

    private void checking(CreateNetworkInput input) {
        String planeId = input.getCreateLinks().get(0).getPlaneId();
        if (planeId != null) {
            //检查planeId是否存在
            SubNetTreeNodeDao subNetTreeNodeDao = SpringBeanFinder.getBean(SubNetTreeNodeDao.class);
            if (! subNetTreeNodeDao.existsBySubNetId(planeId)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "planeId " + planeId + " does not exist");
            }
        }
    }

    private void parse(CreateNetworkInput input) throws CommonException {
        for (CreateLinks siteLinkCreationParam : input.getCreateLinks()) {
            CreateSiteLinkParam param = new CreateSiteLinkParam(changedObject);
            paramMap.put(siteLinkCreationParam.getFriendlyName(), param);
            param.parser(siteLinkCreationParam);
        }
    }

    /**
     * network 中的siteLink不能并行创建，因为，其中的NE的是孤立的。
     *
     * @param createNetworkInput
     * @throws CommonException
     */
    public void lazy(CreateNetworkInput createNetworkInput) {
        //开始锁资源，一直到createNetwork写数据库结束。否则提取释放锁会导致生成的ID重复
        ZkResourceLock locker = lockResource();

        StepToe toe = new StepToe();
        try {
            Long groupId = updateTaskInfo(createNetworkInput.getTaskInfoId(),createNetworkInput.getUiInfo());

            for (CreateLinks siteLinkCreationParam : createNetworkInput.getCreateLinks()) {
                CreateLink2Input createLinkInput = convertLinkCreationInput(siteLinkCreationParam,
                    createNetworkInput.getSiteLinks().stream()
                        .filter(t -> t.getFriendlyName()
                            .equals(siteLinkCreationParam.getFriendlyName()))
                        .findFirst());

                toe.addToe(new SiteLinkCreator(changedObject, createLinkInput, true));
            }
            String planeName = createNetworkInput.getCreateLinks().get(0).getPlaneName();
            int grid = createNetworkInput.getCreateLinks().get(0).getFrequencyGrid();
            taskInfoMessage.setResourceName(String.format("ROADM 网络 %s--%s (%s)", planeName,
                grid == 0 ? "Flex" : grid + "GHz", System.currentTimeMillis()));
            taskInfoMessage.setResourceId(taskInfoMessage.getResourceName());
            taskInfoMessage.setGroupId(groupId);
            taskInfoMessage.setRoot(false);

        } catch (Exception e) {
            log.error("error happen when create siteLink", e);
            locker.unlock();
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "error happen when create siteLink");
        }

        List<String> siteLinkFriendlyNameList = createNetworkInput.getCreateLinks()
                .stream().map(t -> t.getFriendlyName())
                .collect(Collectors.toList());

        Step<CreationResult> step = new Step(toe);
        step.start(new SummaryNetwork(locker, changedObject,
            taskInfoMessage, siteLinkFriendlyNameList,
            createNetworkInput.getWssLinks(),
            createNetworkInput.getRoadms()));

        log.debug("create network aSync start, \n wssLinks {}, \n oadms {}, \nsiteLinks {}",
            createNetworkInput.getWssLinks(), createNetworkInput.getRoadms(), siteLinkFriendlyNameList);
    }


    private long updateTaskInfo(BigInteger taskInfoId, String uiInfo) {
        if (taskInfoId == null) {
            return 0;
        }
        log.info("update taskIno data {}", taskInfoId);

        long groupId = System.currentTimeMillis();
        TaskInfoMessage msg = new TaskInfoMessage(taskInfoMessage);
        msg.setId(taskInfoId.longValue());
        msg.setErrorReason("confirmed");
        msg.setSuccessfully(true);
        msg.setDetail(uiInfo);
        msg.setGroupId(groupId);
        msg.setRoot(true);
        msg.setActionType(null);
        msg.setEndTime(System.currentTimeMillis());

        TaskInfoMessager.sendMessage(msg);

        return groupId;
    }


    /**
     * The siteLink related creation param(creationParam), and computer function result(site_link)
     * @param siteLinkCreationParam
     * @param computeResultOp
     * @return
     * @throws CommonException
     */
    private CreateLink2Input convertLinkCreationInput(CreationParams siteLinkCreationParam,
            Optional<SiteLinks> computeResultOp) throws CommonException {
        if (computeResultOp.isPresent()) {
            LinkRoute mainRroute = computeResultOp.get().getMain();
            LinkRoute spareRroute = computeResultOp.get().getSlave();
            LinkRoute thirdRroute = computeResultOp.get().getThird();
            CreateLink2Input input = new CreateLink2InputBuilder(siteLinkCreationParam)
                    .setLinkComputeResult(new LinkComputeResultBuilder()
                            .setMain(new MainBuilder(mainRroute).build())
                            .setSlave(spareRroute == null ? null : new SlaveBuilder(spareRroute).build())
                            .setThird(thirdRroute == null ? null : new ThirdBuilder(thirdRroute).build())
                            .build())
                    .build();

            return input;
        }

        throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "cannot find out creation param for " + siteLinkCreationParam.getFriendlyName());
    }

    private void logMessage(String errorMessage) {
        String msg = String.format("create network at  %s ", taskInfoMessage.getResourceName());

        String extMsg = "fail";
        boolean isOk = false;
        if (errorMessage == null) {
            //创建成功
            isOk = true;
            extMsg = "successfully.";
        }

        taskInfoMessage.setSuccessfully(isOk);
        if (taskInfoMessage != null) {
            if (isOk) {
                //do nothing
            } else {
                taskInfoMessage.setErrorReason(errorMessage);
            }
            TaskInfoMessager.sendMessage(taskInfoMessage);
        }

        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title("create network")
                        .message(msg + extMsg)
                        .error(!isOk)
                        .build());
    }

    /**
     * 只锁siteNode
     */
    private ZkResourceLock lockResource() {
        ZkResourceLock locker = new ZkResourceLock();
        locker.addResource(TopoNameConstants.Network_Topo_Key);  //防止利旧的时候出错
        locker.getLock();

        return locker;
    }

    public NetworkCreator setTaskInfo(TaskInfoMessage taskInfoMessage) {
        this.taskInfoMessage = taskInfoMessage;

        return this;
    }
}
