/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.ne.manager.controller;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.controller.ne.manager.service.AdapterManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class AdapterManagerController {

    private final AdapterManager adapterManager;

//    @PostMapping(value = {
//            "/restconf/operations/eml-manager:create-adapter"}, produces = "application/json;charset=UTF-8")
//    public @ResponseBody
//    String createAdapter(@RequestBody String input) throws CommonException {
//        return adapterManager.createAdapter(input);
//    }
//
//    @PostMapping(value = {
//            "/restconf/operations/eml-manager:delete-adapter"}, produces = "application/json;charset=UTF-8")
//    public @ResponseBody
//    String deleteAdapter(@RequestBody String input) throws CommonException {
//
//        return adapterManager.deleteAdapter(input);
//    }

    @GetMapping(value = {
            "/restconf/operational/eml-manager:adapter-manager/**"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String geAdapterData(HttpServletRequest request) throws CommonException {
        log.debug("start to get adapter data ");
        return adapterManager.getAdapterByCondition(request);
    }
}
