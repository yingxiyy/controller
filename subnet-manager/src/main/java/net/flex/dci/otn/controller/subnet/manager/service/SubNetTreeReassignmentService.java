package net.flex.dci.otn.controller.subnet.manager.service;

import javax.servlet.http.HttpServletRequest;
import net.flex.dci.otn.controller.subnet.manager.dto.SubNetMigrationReq;

/**
 * 2026/2/8
 *
 * @author musa
 * @version 1.0
 **/
public interface SubNetTreeReassignmentService {

    String migration(SubNetMigrationReq subNetMigrationReq, HttpServletRequest request);
}
