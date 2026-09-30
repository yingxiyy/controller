package net.flex.dci.otn.controller.subnet.manager.controller;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.subnet.manager.dto.SubNetMigrationReq;
import net.flex.dci.otn.controller.subnet.manager.service.SubNetTreeReassignmentService;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2026/2/8
 *
 * @author musa
 * @version 1.0
 **/
@RestController
@RequestMapping(value = "/restconf/subnet")
@RequiredArgsConstructor
@Slf4j
public class SubNetMigrationController {

    private final SubNetTreeReassignmentService subNetTreeReassignmentService;

    @RequestMapping(value = "/migration", method = RequestMethod.POST)
    public ResponseEntity<?> subnetMigration(@RequestBody SubNetMigrationReq subNetMigrationReq,
            HttpServletRequest request) {
        String taskId = subNetTreeReassignmentService.migration(subNetMigrationReq, request);
        return new ResponseEntity<>(Result.ok(taskId), HttpStatus.OK);
    }

}
