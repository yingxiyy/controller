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

/**
 * 2026/2/26
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
@Component
public class TunnelAttributeDemandSourceUpdateStrategy extends
        AbstractTunnelAttributeUpdateStrategy {


    @Override
    public boolean supports(UpdateTunnelInput updateTunnelInput) {
        return StringUtils.hasText(updateTunnelInput.getOrderId());
    }

    @Override
    public void execute(String tunnelId, UpdateTunnelInput updateTunnelInput,
            TaskInfoMessage taskInfoMessage) {
        log.debug("execute update tunnel attribute demand source tunnel:{}", tunnelId);
        log.info("update tunnel:{} demand source", tunnelId);
        Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
        if (null == tunnel) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("cannot find required tunnel %s.", tunnelId));
        }
        String demandSource = updateTunnelInput.getOrderId();
        String oldDemandSource = tunnel.getOrderId().get(0);
        updateDemandSource(tunnelId, tunnel.getFriendlyName(), oldDemandSource, demandSource,
                taskInfoMessage);
    }

    @Override
    public ActionType taskActionType() {
        return ActionType.updateDemandSource;
    }

    private void updateDemandSource(String tunnelId, String tunnelName, String oldDemandSource,
            String demandSource,
            TaskInfoMessage taskInfoMessage) {

        log.debug("update demand source for tunnel tunnelId {} to demandSource:{}",
                tunnelId, demandSource);
        tunnelDao.updateTunnelDemandSource(tunnelId, demandSource);
        String resourceName = String.format("Tunnel %s: Demand Source updated (%s → %s)",
                tunnelName, oldDemandSource, demandSource);
        logMessage(BroadCastConstant.UPDATE_TUNNEL_DEMAND_SOURCE, resourceName, BLANK,
                taskInfoMessage);
    }
}
