package net.flex.dci.otn.controller.user.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.user.domain.rest.RoleDto;
import net.flex.dci.otn.controller.user.service.RoleService;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 2022/4/19 13:45
 */
@RestController
@Slf4j
@RequestMapping(value = "/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<?> listAllRoles() {
        log.info("list all  role");
        List<RoleDto> roles = roleService.listAllRoles();
        return new ResponseEntity<>(Result.ok(roles), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> addRole(@RequestBody RoleDto roleDto) {
        log.info("create a new role");
        roleService.createRole(roleDto);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }


    @DeleteMapping(value = "/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable("id") Long id) {
        log.info("create a new role");
        roleService.deleteRole(id);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }
}
