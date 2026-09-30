/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.ne.manager.controller;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.controller.ne.manager.dto.OperationResult;
import net.flex.dci.otc.controller.ne.manager.service.impl.NeManagerImpl;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/restconf/operations/")
public class NeManagerController {

    @Autowired
    private NeManagerImpl neManager;

    @PostMapping(value = {
            "eml-manager:registe-ne"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String registerNe(@RequestBody String input) {
        log.info("register ne the ne info is {}", input);
        return neManager.registerNe(input);
    }

    @PostMapping(value = {
            "eml-manager:unregiste-ne"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String unregisteredNe(@RequestBody String input) {
        log.info("start to unregistered the ne {}", input);
        String result = neManager.unregisteredNe(input);
        return result;
    }


    @PostMapping(value = {
            "eml-manager:config-ne"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String configNe(@RequestBody String input) {
        log.info("config ne property the requestBody is {}", input);
        return neManager.configNe(input);
    }

    @PostMapping(value = {
            "eml-manager:batch-config-ne"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String batchConfigNe(@RequestBody String input) {
        log.info("batch config ne property the requestBody is {}", input);
        return neManager.batchConfigNe(input);
    }


    /**
     * @param input
     * @return
     * @throws Exception
     */
    @PostMapping(value = {
            "eml-manager:upload-ne"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String uploadNe(@RequestBody String input) {
        log.info("start to upload ne ,input is {}", input);
        return neManager.uploadNe(input);
    }

    /**
     * @param input
     * @return
     * @throws Exception
     */
    @PostMapping(value = {
            "eml-manager:merge-data"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody ResponseEntity<?> mergeData(@RequestBody String input) {
        log.info("start to merge ne ,input is {}", input);
        OperationResult operationResult = neManager.mergeData(input);
        return new ResponseEntity<>(Result.ok(operationResult), HttpStatus.OK);
    }

    /**
     * @param input
     * @return
     * @throws Exception
     */
    @PostMapping(value = {
            "eml:get-ne-data"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String getNeData(@RequestBody String input) {
        log.info("start to upload ne ,input is {}", input);
        return neManager.getNeData(input);
    }


    @PostMapping(value = {
            "eml-manager:remove-resource"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String removeResource(@RequestBody String input) {
        log.info("start to remove resource {}", input);
        return neManager.removeResource(input);
    }


    @PostMapping(value = {
            "eml-manager:compare-ne"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String compareNe(@RequestBody String input) {
        log.info("start to compare ne {}", input);
        return neManager.compareNe(input);
    }

    @PostMapping(value = {
            "eml-manager:manage-ne"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String manageNe() throws CommonException {
        log.info("start to manage the ne");
        return neManager.manageNe();
    }

    @PostMapping(value = {
            "eml-manager:report-1524-telemetry-data"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String report1524TelemetryDate(@RequestBody String input) throws CommonException {
        log.info("start to report 1524 tele data");
        return neManager.report1524TelemetryData(input);
    }

    @PostMapping(value = "eml-manager:ne-software-operate", produces = "application/json;charset=UTF-8")
    public @ResponseBody String neSoftwareOperation(@RequestBody String input)
            throws CommonException {
        log.info("ne software operation start");
        log.debug("ne software operation start,cmd is:{}", input);
        return neManager.neSoftwareOperate(input);
    }

    @PostMapping(value = "eml-manager:ne-database-operate", produces = "application/json;charset=UTF-8")
    public @ResponseBody String neDatabaseOperate(@RequestBody String input)
            throws CommonException {
        log.info("ne database operation start");
        log.debug("ne database operation start,cmd is:{}", input);
        return neManager.neDatabaseOperate(input);
    }

    @PostMapping(value = "eml-manager:switch-cu-active-standby", produces = "application/json;charset=UTF-8")
    public @ResponseBody String switchCuActiveStandby(@RequestBody String input) {
        log.info("switch cu active standby");
        return neManager.switchCUActiveStandby(input);
    }

    @PostMapping(value = "eml-manager:upload-history-pm", produces = "application/json;charset=UTF-8")
    public @ResponseBody String uploadNeHistoryPm(@RequestBody String input) {
        log.info("upload history pm");
        return neManager.uploadHistoryPm(input);
    }

}
