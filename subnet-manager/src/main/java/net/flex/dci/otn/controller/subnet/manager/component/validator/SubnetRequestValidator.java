package net.flex.dci.otn.controller.subnet.manager.component.validator;

import net.flex.dci.otn.controller.subnet.manager.dto.SubNetMigrationReq;

/**
 * 2026/2/9
 *
 * @author musa
 * @version 1.0
 **/
public interface SubnetRequestValidator {

    void validatorMigration(SubNetMigrationReq req);
}
