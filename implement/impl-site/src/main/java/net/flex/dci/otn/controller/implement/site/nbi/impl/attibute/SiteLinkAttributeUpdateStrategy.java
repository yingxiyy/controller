package net.flex.dci.otn.controller.implement.site.nbi.impl.attibute;

import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkInput;

/**
 * 2026/2/27
 *
 * @author musa
 * @version 1.0
 **/
public interface SiteLinkAttributeUpdateStrategy {

    boolean supports(UpdateLinkInput input);

    void execute(UpdateLinkInput input,
            TaskInfoMessage taskInfoMessage);

    ActionType taskActionType();
}
