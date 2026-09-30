/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.app.monitor.controller;

import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otn.controller.app.monitor.service.IAppMonitorService;
import net.flex.dci.otn.controller.app.monitor.service.LogDownloadService;
import net.flex.dci.otn.controller.app.monitor.service.impl.AppRebootService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class PMCController {


    @Autowired
    private IAppMonitorService appMonitorService;
    @Autowired
    private AppRebootService appRebootService;
    @Autowired
    private LogDownloadService logDownloadService;

    @PostMapping(value = "/restconf/operations/pmc:get-version", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getVersion() throws CommonException {
        log.info("get module version info");
        return appMonitorService.getModuleVersion();
    }


    @PostMapping(value = "/restconf/operations/pmc:get-system-info", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getSystemInfo() throws CommonException {
        log.info("get system info from app monitor");
        return appMonitorService.getSystemInfo();
    }

    @GetMapping(value = "/reboot/{appName}", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String rebootApp(@PathVariable String appName) throws CommonException {
        log.info("reboot app: {}", appName);
        return appRebootService.reboot(appName);
    }


    @GetMapping(value = "/restconf/log/download/{service}")
    @ResponseBody
    public void proxyDownload(@PathVariable String service, HttpServletResponse response)
            throws CommonException {
        log.info("down load the service log:{}", service);
        logDownloadService.forward(service, response);
    }
}
