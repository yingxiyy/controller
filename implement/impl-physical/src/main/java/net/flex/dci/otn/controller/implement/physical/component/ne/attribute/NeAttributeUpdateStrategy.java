package net.flex.dci.otn.controller.implement.physical.component.ne.attribute;

import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otn.controller.implement.common.dto.ContactNeResult;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.update.node.input.Nodes;

/**
 * 2026/4/14
 *
 * @author musa
 * @version 1.0
 **/
public interface NeAttributeUpdateStrategy {

    boolean supports(Nodes nodes);

    ContactNeResult execute(Nodes nodes,
            TaskInfoMessage taskInfoMessage);

    TaskInfoMessage.ActionType taskActionType();
}
