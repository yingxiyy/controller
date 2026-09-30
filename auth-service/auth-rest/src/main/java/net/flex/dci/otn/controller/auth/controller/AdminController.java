package net.flex.dci.otn.controller.auth.controller;

import java.io.UnsupportedEncodingException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 2022/4/18 14:04
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @GetMapping("/hello")
    public String hello() throws UnsupportedEncodingException {
        
        String sessionInfo = "this is admin hello:";
        return sessionInfo;
    }
}
