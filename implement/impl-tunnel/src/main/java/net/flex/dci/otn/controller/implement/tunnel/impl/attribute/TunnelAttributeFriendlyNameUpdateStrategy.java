/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.tunnel.impl.attribute;


import static net.flex.dci.otn.controller.implement.common.utils.CommonUtils.logMessage;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class TunnelAttributeFriendlyNameUpdateStrategy extends
        AbstractTunnelAttributeUpdateStrategy {

//    private static TunnelAttributeFriendlyNameUpdateStrategy inst = null;
//
//    private final TunnelDao tunnelDao;
//
//    private TunnelAttributeFriendlyNameUpdateStrategy() {
//        tunnelDao = SpringBeanFinder.getBean(TunnelDao.class);
//    }
//
//    public static TunnelAttributeFriendlyNameUpdateStrategy instance() {
//        if (inst == null) {
//            inst = new TunnelAttributeFriendlyNameUpdateStrategy();
//        }
//        return inst;
//    }

    @Override
    public boolean supports(UpdateTunnelInput updateTunnelInput) {
        return StringUtils.hasText(updateTunnelInput.getFriendlyName());
    }

    @Override
    public void execute(String tunnelId, UpdateTunnelInput updateTunnelInput,
            TaskInfoMessage taskInfoMessage) {
        log.info("update tunnel:{} friendly name", tunnelId);
        Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
        if (null == tunnel) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("cannot find required tunnel %s.", tunnelId));
        }
        String newFriendlyName = updateTunnelInput.getFriendlyName();
        String oldFriendlyName = tunnel.getFriendlyName();
        updateFriendlyName(tunnelId, oldFriendlyName, newFriendlyName, taskInfoMessage);
    }

    @Override
    public ActionType taskActionType() {
        return ActionType.updateFriendlyName;
    }

    /**
     * @param tunnelId
     * @param oldFriendlyName
     * @param newFriendlyName
     * @param taskInfoMessage
     * @throws CommonException
     */
    public void updateFriendlyName(String tunnelId, String oldFriendlyName, String newFriendlyName,
            TaskInfoMessage taskInfoMessage) throws CommonException {
        Boolean existed = tunnelDao.existsFriendlyName(newFriendlyName);
        if (existed) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Duplicate friendlyName");
        }
        log.debug("updateFriendlyName from oldFriendlyName {} to newFriendlyName:{}",
                oldFriendlyName, newFriendlyName);
        String resourceName = String.format("Tunnel %s: friendly name update to %s",
                oldFriendlyName, newFriendlyName);
        tunnelDao.updateTunnelFriendlyName(tunnelId, newFriendlyName);
        logMessage(BroadCastConstant.UPDATE_TUNNEL_FRIENDLY_NAME, resourceName, BLANK,
                taskInfoMessage);
    }


}
