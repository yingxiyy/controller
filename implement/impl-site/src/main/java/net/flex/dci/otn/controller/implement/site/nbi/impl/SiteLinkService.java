/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.site.nbi.impl;

import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.AuthConstant;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.serialization.util.SerializeUtil;
import net.flex.dci.otn.controller.implement.common.nbi.impl.BaseImpl;
import net.flex.dci.otn.controller.implement.common.utils.CommonUtils;
import net.flex.dci.otn.controller.implement.site.nbi.impl.attibute.SiteLinkAttributeUpdateStrategy;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkOutputBuilder;
import org.springframework.stereotype.Service;

/**
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SiteLinkService extends BaseImpl {

//    public static final String signal = "here";

    private final SiteLinkImplSyncService siteLinkImplSyncService;

    private final List<SiteLinkAttributeUpdateStrategy> siteLinkAttributeUpdateStrategies;


    /**
     * only support change friendlyName
     *
     * @param input
     * @return
     */
    @Override
    public UpdateLinkOutput updateLink(String input, HttpServletRequest request)
            throws CommonException {
        log.info("update link the input is:{}", input);
        UpdateLinkInput updateLinkInput = SerializeUtil.parseRpcInput(input, UpdateLinkInput.class);
        if (updateLinkInput.getLinkId() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "update siteLink ref site link id is null");
        }

        String author = request.getHeader(AuthConstant.USER_TOKEN_HEADER);
        String siteLinkId = updateLinkInput.getLinkId();
        TaskInfoMessage taskInfoMessage = buildSiteLinkTaskInfoMessage(author,
                ActionType.updateFriendlyName, input);
        Link link = siteLinkDao.getSiteLinkById(siteLinkId);
        if (link == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find required link " + siteLinkId);
        }
        for (SiteLinkAttributeUpdateStrategy siteLinkAttributeUpdateStrategy : siteLinkAttributeUpdateStrategies) {
            if (siteLinkAttributeUpdateStrategy.supports(updateLinkInput)) {
                try {
                    taskInfoMessage.setActionType(siteLinkAttributeUpdateStrategy.taskActionType());
                    siteLinkAttributeUpdateStrategy.execute(updateLinkInput, taskInfoMessage);
                } catch (Exception ex) {
                    log.error("error happen during siteLink attribute update", ex);
                    throw ex;
                }
            }
        }

//        UpdateLinkOutput updateLinkOutput = null;
//        if (StringUtils.hasText(updateLinkInput.getFriendlyName())) {
//            TaskInfoMessage taskInfoMessage = buildSiteLinkTaskInfoMessage(author,
//                    TaskInfoMessage.ActionType.updateFriendlyName, input);
//            updateLinkOutput = updateLinkPhysicalName(link, friendlyName, taskInfoMessage);
//        } else if (StringUtils.hasText(updateLinkInput.getOrderId())) {
//            TaskInfoMessage taskInfoMessage = buildSiteLinkTaskInfoMessage(author,
//                    ActionType.updateDemandSource, input);
//            updateLinkOutput = updateLinkDemandSource(link, demandName, taskInfoMessage);
//        } else if (adminStatus != null
//                && implementState != null) {
//            updateLinkOutput = updateSiteLinkImplState(updateLinkInput, request);
//        }
        return new UpdateLinkOutputBuilder()
                .setReturnCode(RpcResultType.Success)
                .build();
    }

    private UpdateLinkOutput updateLinkDemandSource(Link link, String demandName,
            TaskInfoMessage taskInfoMessage) {
        log.info("update link demand source,the site link id:{} and demand name:{}",
                link.getLinkId(),
                demandName);
        String siteLinkId = link.getLinkId().getValue();
        String friendlyName = link.getAugmentation(Link1.class).getSite().getFriendlyName();
        taskInfoMessage.setResourceId(link.getLinkId().getValue());
        try {
            siteLinkDao.updateSiteLinkDemandSource(siteLinkId, demandName);
            CommonUtils.logMessage(BroadCastConstant.UPDATE_SITE_LINK_DEMAND_SOURCE,
                    friendlyName, BLANK, taskInfoMessage);
            return new UpdateLinkOutputBuilder()
                    .setReturnCode(RpcResultType.Success)
                    .build();
        } catch (Exception e) {
            log.error("failed update site link demand source,the reason is:{}", e.getMessage(), e);
            CommonUtils.logMessage(BroadCastConstant.UPDATE_SITE_LINK_DEMAND_SOURCE,
                    friendlyName, e.getMessage(), taskInfoMessage);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Failed update site link demand source,the reason is:" + e.getMessage(), e);
        }
    }


    private UpdateLinkOutput updateSiteLinkImplState(UpdateLinkInput updateLinkInput,
            HttpServletRequest request) {
        log.info("update siteLink:{} implement ", updateLinkInput.getLinkId());
        String author = request.getHeader(AuthConstant.USER_TOKEN_HEADER);
        return siteLinkImplSyncService.start(updateLinkInput, author);
    }

    private UpdateLinkOutput updateLinkPhysicalName(Link link, String friendlyName,
            TaskInfoMessage taskInfoMessage) {
        log.info("start to update site link:{} friendly name:{}", link.getLinkId().getValue(),
                friendlyName);
        String oldFriendlyName = link.getAugmentation(Link1.class).getSite().getFriendlyName();
        try {
            if (friendlyName == null || friendlyName.isEmpty()) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "only support change friendlyName, and length of friendlyName must be large than 0");
            }
            String linkId = link.getLinkId().getValue();
//            checkDuplicate(friendlyName);
            siteLinkDao.updateLinkFriendlyName(linkId, friendlyName);
//            Link1 link1 = link.getAugmentation(Link1.class);
//            LinkBuilder linkBuilder = new LinkBuilder(link).addAugmentation(Link1.class,
//                    new Link1Builder(link1)
//                            .setSite(new SiteBuilder(link1.getSite())
//                                    .setFriendlyName(friendlyName)
//                                    .build())
//                            .build());
//
//            ChangedObject changedObject = new ChangedObject();
//            changedObject.addChangedSiteLink(linkBuilder.build());
//            multipleTransaction.save(changedObject);
            taskInfoMessage.setResourceId(link.getLinkId().getValue());
            CommonUtils.logMessage(BroadCastConstant.UPDATE_SITE_LINK_FRIEND_NAME,
                    oldFriendlyName, BLANK, taskInfoMessage);
            return new UpdateLinkOutputBuilder()
                    .setReturnCode(RpcResultType.Success)
                    .build();
        } catch (Exception e) {
            log.error("failed to update site link physical name,the reason is:{}", e.getMessage(),
                    e);
            CommonUtils.logMessage(BroadCastConstant.UPDATE_SITE_LINK_FRIEND_NAME,
                    oldFriendlyName, e.getMessage(), taskInfoMessage);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "update site link friendly name failed " + e.getMessage(), e);
        }

    }

//    private void checkDuplicate(String friendlyName) throws CommonException {
//        if (siteLinkDao.existsLinkFriendlyName(friendlyName)) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "the friendlyName is duplicated with existed one " + friendlyName);
//        }
//    }

    private TaskInfoMessage buildSiteLinkTaskInfoMessage(String author, ActionType actionType,
            String input) {
        return new TaskInfoMessage(author,
                TaskInfoMessage.ResourceType.siteLink,
                actionType,
                input);
    }
}
