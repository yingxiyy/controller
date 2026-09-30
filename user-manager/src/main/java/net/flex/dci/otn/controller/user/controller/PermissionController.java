package net.flex.dci.otn.controller.user.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.user.domain.PermissionDto;
import net.flex.dci.otn.controller.user.domain.RolePermissionDto;
import net.flex.dci.otn.controller.user.domain.rest.AssignPermissionDto;
import net.flex.dci.otn.controller.user.domain.rest.CreateOrUpdatePermissionDto;
import net.flex.dci.otn.controller.user.domain.rest.PermissionInfoDto;
import net.flex.dci.otn.controller.user.domain.rest.RemovePermissionDto;
import net.flex.dci.otn.controller.user.service.PermissionService;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 2022/4/22 16:04
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(value = "/permission")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<?> listAllPermission() {
        List<PermissionDto> permissionDtos = permissionService.listAllPermission();
        return new ResponseEntity<>(Result.ok(permissionDtos), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<?> addPermission(
            @RequestBody CreateOrUpdatePermissionDto createOrUpdatePermissionDto) {
        log.info("add permission for the path");
        Long permissionId = permissionService.createPermission(createOrUpdatePermissionDto);
        return new ResponseEntity<>(
                Result.ok(PermissionInfoDto.builder().permissionId(permissionId).build()),
                HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<?> deletePermission(
            @RequestBody CreateOrUpdatePermissionDto createOrUpdatePermissionDto) {
        log.info("add permission for the path");
        permissionService.createPermission(createOrUpdatePermissionDto);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }


    @PatchMapping
    public ResponseEntity<?> updatePermission(
            @RequestBody CreateOrUpdatePermissionDto createOrUpdatePermissionDto) {
        log.info("add permission for the path");
        permissionService.updatePermission(createOrUpdatePermissionDto);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }

    @GetMapping(value = "/role/{id}")
    public ResponseEntity<?> listRefRolePermission(@PathVariable("id") Long id) {
        RolePermissionDto rolePermission = permissionService.listAllPermissionByRole(id);
        return new ResponseEntity<>(Result.ok(rolePermission), HttpStatus.OK);
    }


    @PostMapping(value = "/role")
    public ResponseEntity<?> assignRolePermission(
            @RequestBody AssignPermissionDto assignPermissionDto) {
        permissionService.assignPermission(assignPermissionDto);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }

    @PostMapping(value = "/remove")
    public ResponseEntity<?> removeRolePermission(
            @RequestBody RemovePermissionDto removePermissionDto) {
        permissionService.removePermission(removePermissionDto);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }

    @PutMapping(value = "/role")
    public ResponseEntity<?> addRolePermission(
            @RequestBody AssignPermissionDto assignPermissionDto) {
        permissionService.addRolePermission(assignPermissionDto);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }


}
