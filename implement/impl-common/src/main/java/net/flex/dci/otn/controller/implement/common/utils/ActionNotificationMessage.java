package net.flex.dci.otn.controller.implement.common.utils;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otn.controller.implement.common.enums.BusinessLinkType;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;

/**
 *
 * 2025/11/20
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public class ActionNotificationMessage {

    private static final String SITE_LINK_MESSAGE_TEMPLATE = "SITE LINK %s %s start";

    private static final String TUNNEL_MESSAGE_TEMPLATE = "TUNNEL %s %s start";

    private static final String SITE_LINK_MESSAGE_TEMPLATE_DONE = "SITE LINK %s %s done";

    private static final String TUNNEL_MESSAGE_TEMPLATE_DONE = "TUNNEL %s %s done";

    public static void sendLinkImplementMethodDoneNotification(BusinessLinkType businessLinkType,
                                                                String linkName, ActionType actionType) {
        log.debug("send link type:{} and link name:{} action type:{} DONE", businessLinkType, linkName,
            actionType);
        String template = "";
        String title = "";
        switch (businessLinkType) {
            case TUNNEL:
                template = TUNNEL_MESSAGE_TEMPLATE_DONE;
                title = BroadCastConstant.UPDATE_TUNNEL_IMPL;
                break;
            case SITE_LINK:
                template = SITE_LINK_MESSAGE_TEMPLATE_DONE;
                title = BroadCastConstant.UPDATE_SITE_LINK_IMPL;
                break;
            default:
                throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                    "not support business link type:" + businessLinkType);
        }
        String message = String.format(template, linkName, actionType.name());
        BroadcastMessage broadcastMessage = BroadcastMessage.builder()
            .title(title)
            .message(message)
            .error(false)
            .build();
        BroadcastMessager.publishKafkaMessage(broadcastMessage);
    }

    public static void sendLinkImplementMethodStartNotification(BusinessLinkType businessLinkType,
            String linkName, ActionType actionType) {
        log.debug("send link type:{} and link name:{} action type:{}", businessLinkType, linkName,
                actionType);
        String template = "";
        String title = "";
        switch (businessLinkType) {
            case TUNNEL:
                template = TUNNEL_MESSAGE_TEMPLATE;
                title = BroadCastConstant.UPDATE_TUNNEL_IMPL;
                break;
            case SITE_LINK:
                template = SITE_LINK_MESSAGE_TEMPLATE;
                title = BroadCastConstant.UPDATE_SITE_LINK_IMPL;
                break;
            default:
                throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                        "not support business link type:" + businessLinkType);
        }
        String message = String.format(template, linkName, actionType.name());
        BroadcastMessage broadcastMessage = BroadcastMessage.builder()
                .title(title)
                .message(message)
                .error(false)
                .build();
        BroadcastMessager.publishKafkaMessage(broadcastMessage);
    }
}
