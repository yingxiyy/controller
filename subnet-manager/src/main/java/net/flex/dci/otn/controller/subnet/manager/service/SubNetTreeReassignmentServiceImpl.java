package net.flex.dci.otn.controller.subnet.manager.service;

import static net.flex.dci.otn.controller.subnet.manager.utils.Constants.SUCCESS;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.subnet.manager.component.migration.SubNetMigration;
import net.flex.dci.otn.controller.subnet.manager.component.validator.SubnetRequestValidator;
import net.flex.dci.otn.controller.subnet.manager.dto.SubNetMigrationReq;
import net.flex.dci.otn.controller.subnet.manager.utils.AsynchronousExecutor;
import net.flex.dci.otn.controller.subnet.manager.utils.SubnetUtils;
import org.springframework.stereotype.Service;

/**
 * 2026/2/8
 *
 * @author musa
 * @version 1.0
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class SubNetTreeReassignmentServiceImpl implements SubNetTreeReassignmentService {

    private final SubnetRequestValidator subnetRequestValidator;

    private final SubNetMigration subNetMigration;

    @Override
    public String migration(SubNetMigrationReq subNetMigrationReq,
            HttpServletRequest servletRequest) {
        log.info("start migration subnetMigration request:{}", subNetMigrationReq);
        String author = SubnetUtils.getOperator(servletRequest);
        subnetRequestValidator.validatorMigration(subNetMigrationReq);
        AsynchronousExecutor.execute(() ->
                subNetMigration.reassignmentSubnet(subNetMigrationReq.getMigrationSiteLink(),
                        subNetMigrationReq.getMigrationTunnel(),
                        author)
        );
        return SUCCESS;
    }
}
