package net.flex.dci.otn.controller.auth.controller.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 2022/4/16 10:39
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

    
    @RequestMapping(value = "/current", method = RequestMethod.GET)
    public ResponseEntity<?> currentUser() {
        /**
         * todo: get current user properties
         */

        return new ResponseEntity<>("success", HttpStatus.OK);
    }

}
