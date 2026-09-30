/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.sftp.manager.controller;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otn.controller.sftp.manager.service.SftpServerServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
public class SftpServerController {


    @Autowired
    private SftpServerServiceImpl sftpServerService;

    @PostMapping(value = {
            "/restconf/operations/ftp-server:create-ftp-server"}, produces = "application/json;charset=UTF-8")
    public String createFtpServer(@RequestBody String input) throws CommonException {
        log.debug("createFtpServer input:{}", input);
        return sftpServerService.createServer(input);
    }

    @PostMapping(value = {
            "/restconf/operations/ftp-server:update-ftp-server"}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String updateFtpServer(@RequestBody String input) throws CommonException {
        log.debug("updateFtpServer input:{}", input);
        return sftpServerService.updateServer(input);
    }

    @PostMapping(value = {
            "/restconf/operations/ftp-server:delete-ftp-server"}, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String deleteFtpServer(@RequestBody String input) throws CommonException {
        log.debug("deleteFtpServer input:{}", input);
        return sftpServerService.deleteServer(input);
    }

//    @RequestMapping(value = {
//            "/restconf/config/ftp-server:ftp-servers"}, produces = "application/json;charset=UTF-8", method = RequestMethod.GET)
//    @ResponseBody
//    public String getAllFtpServer(HttpServletRequest request) throws Exception {
////        String uri = request.getRequestURI();
////        uri = URLDecoder.decode(uri, "UTF-8");
////        String identifier = uri.substring("/restconf/config/".length());
////        final InstanceIdentifier<FtpServer> iid = InstanceIdentifier
////                .create(FtpServers.class).child(
////                        FtpServer.class);
////        List<FtpServer> serverList = mongoDao.readDataList(iid, DataStoreType.CONFIG);
////        FtpServersBuilder builder = new FtpServersBuilder();
////        builder.setFtpServer(serverList);
////        return jsonUtil.fromDataObjectToJson(identifier, builder.build());
//        return "ok";
//
//    }
}
