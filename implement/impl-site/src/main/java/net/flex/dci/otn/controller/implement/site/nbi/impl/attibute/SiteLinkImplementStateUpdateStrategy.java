package net.flex.dci.otn.controller.implement.site.nbi.impl.attibute;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otn.controller.implement.site.nbi.impl.SiteLinkImplSyncService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkInput;
import org.springframework.stereotype.Component;

/**
 * 2026/2/27
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class SiteLinkImplementStateUpdateStrategy implements SiteLinkAttributeUpdateStrategy {

    private final SiteLinkImplSyncService siteLinkImplSyncService;

    @Override
    public boolean supports(UpdateLinkInput input) {
        ImplementState implementState = input.getImplementState();
        AdminStatus adminStatus = input.getAdminState();
        return adminStatus != null
                && implementState != null;
    }

    @Override
    public void execute(UpdateLinkInput input, TaskInfoMessage taskInfoMessage) {
        log.info("update site link implement state,the input is:{}", input);
        String author = taskInfoMessage.getWho();
        siteLinkImplSyncService.start(input, author);
    }

    @Override
    public ActionType taskActionType() {
        return ActionType.implement;
    }


}
