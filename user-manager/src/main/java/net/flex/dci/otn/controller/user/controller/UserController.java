package net.flex.dci.otn.controller.user.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.user.domain.PageQueryParamDto;
import net.flex.dci.otn.controller.user.domain.rest.CreateUserDto;
import net.flex.dci.otn.controller.user.domain.rest.UserDto;
import net.flex.dci.otn.controller.user.domain.rest.modify.ModifyPasswordDto;
import net.flex.dci.otn.controller.user.domain.rest.modify.ModifyRoleDto;
import net.flex.dci.otn.controller.user.domain.rest.paged.UserPagedDto;
import net.flex.dci.otn.controller.user.enums.OrderElement;
import net.flex.dci.otn.controller.user.service.UserService;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 2022/4/19 13:34
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(value = "/users")
public class UserController {

    private final UserService userService;

   
    @GetMapping
    public ResponseEntity<?> getAllUserPaged(
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "limit", defaultValue = "20", required = false) int limit,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "sort", defaultValue = "desc", required = false) String sort,
            @RequestParam(value = "order", defaultValue = "id", required = false) OrderElement order) {
        log.info("list all user paged");
        page = page < 0 ? 0 : page;
        limit = limit < 0 ? 20 : limit;
        Direction direction =
                sort == null || !sort.equalsIgnoreCase("asc") ? Direction.DESC
                        : Direction.ASC;
        order = order == null ? OrderElement.id : order;
        PageQueryParamDto pageQueryParamDto = PageQueryParamDto.builder()
                .page(page)
                .limit(limit)
                .direction(direction)
                .keywords(keywords)
                .orderElement(order)
                .build();

        UserPagedDto userPagedDto = userService.listAllAliveUserPaged(pageQueryParamDto);
        return new ResponseEntity<>(Result.ok(userPagedDto), HttpStatus.OK);
    }


    @GetMapping(value = "/deleted")
    public ResponseEntity<?> getAllDeletedUserPaged(
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "limit", defaultValue = "20", required = false) int limit,
            @RequestParam(value = "keywords", required = false) String keywords,
            @RequestParam(value = "sort", defaultValue = "desc", required = false) String sort,
            @RequestParam(value = "order", defaultValue = "id", required = false) OrderElement order) {
        log.info("list all user paged");
        page = page < 0 ? 0 : page;
        limit = limit < 0 ? 20 : limit;
        Direction direction =
                sort == null || !sort.equalsIgnoreCase("asc") ? Direction.DESC
                        : Direction.ASC;
        order = order == null ? OrderElement.id : order;
        PageQueryParamDto pageQueryParamDto = PageQueryParamDto.builder()
                .page(page)
                .limit(limit)
                .direction(direction)
                .keywords(keywords)
                .orderElement(order)
                .build();

        UserPagedDto userPagedDto = userService.listAllDeletedUserPaged(pageQueryParamDto);
        return new ResponseEntity<>(Result.ok(userPagedDto), HttpStatus.OK);
    }


    /**
     * create a user
     *
     * @param createUserDto
     * @return
     */
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> createUser(@RequestBody CreateUserDto createUserDto) {
        log.info("create user ");
        userService.createUser(createUserDto);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }

    @PutMapping(value = "/email")
    public ResponseEntity<?> modifyUserEmail() {
        Long id = null;
        log.info("start to modify the user role,the user id is:{}", id);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }

    /**
     * modify user password
     *
     * @return
     */
    @PutMapping(value = "/password")
    public ResponseEntity<?> modifyUserPassword(@RequestBody ModifyPasswordDto modifyPasswordDto) {
        log.info("modify the user password");
        userService.modifyUserPassword(modifyPasswordDto);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }


    /**
     * modify user role
     *
     * @return
     */
    @PutMapping(value = "/role")
    public ResponseEntity<?> modifyUserRole(@RequestBody ModifyRoleDto modifyRoleDto) {

        log.info("start to modify the user role,");
        userService.modifyUserRole(modifyRoleDto);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        log.info("start to remove the user,user id is:{}", id);
        UserDto userDto = userService.getUserDetailInfoById(id);
        return new ResponseEntity<>(Result.ok(userDto), HttpStatus.OK);
    }

    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        log.info("start to remove the user,user id is:{}", id);
        userService.deleteUserById(id);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }
}
