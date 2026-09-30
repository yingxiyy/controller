package net.flex.dci.otn.controller.implement.tunnel.impl.attribute;

import net.flex.dci.otc.common.model.TaskInfoMessage;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelInput;

/**
 * 2026/2/26
 *
 * @author musa
 * @version 1.0
 **/
public interface TunnelAttributeUpdateStrategy {

    boolean supports(UpdateTunnelInput updateTunnelInput);

    void execute(String tunnelId, UpdateTunnelInput updateTunnelInput,
            TaskInfoMessage taskInfoMessage);

    TaskInfoMessage.ActionType taskActionType();
}
