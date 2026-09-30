package net.flex.dci.otn.controller.subnet.manager.controller;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.subnet.manager.dto.output.DeleteResult;
import net.flex.dci.otn.controller.subnet.manager.service.SubTreeNodeService;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2026/1/13
 *
 * @author musa
 * @version 1.0
 **/
@RestController
@RequestMapping(value = "/restconf/subnet")
@RequiredArgsConstructor
@Slf4j
public class SubNetTreeDeleteController {

    private final SubTreeNodeService subTreeNodeService;

    @DeleteMapping(value = "/nodes/{subnetId}", produces = "application/json;charset=utf-8")
    public ResponseEntity<?> deleteNode(@PathVariable(name = "subnetId") String subNetId,
            @RequestParam(defaultValue = "false") boolean force, HttpServletRequest request) {
        DeleteResult deleteResult = subTreeNodeService.deleteSubNetNode(subNetId, force, request);
        return new ResponseEntity<>(Result.ok(deleteResult), HttpStatus.OK);
    }

}
