/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.sftp.manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dao.FtpServerDao;
import net.flex.dci.otc.serialization.util.SerializeUtil;
import net.flex.dci.otn.controller.sftp.manager.components.FtpPathMatcher;
import net.flex.dci.otn.controller.sftp.manager.enums.FtpPathVariable;
import net.flex.dci.otn.controller.sftp.manager.model.AntPathInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.FtpServersBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.ftp.servers.FtpServer;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import static net.flex.dci.otc.common.constants.Constants.CONFIG_URL_PREFIX;

/**
 * @version 1.0
 * @date 2021/9/8 14:40
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SftpServerTreeService {


    private final FtpServerDao ftpServerDao;

    private final FtpPathMatcher ftpPathMatcher;

    /**
     * execute request
     *
     * @param request
     * @param response
     * @return
     */
    public String executeRequest(HttpServletRequest request, HttpServletResponse response) {
        log.info("start to execute the base operation for the ftp server tree");
        try {
            String uri = request.getRequestURI();
            uri = URLDecoder.decode(uri, "UTF-8");
            AntPathInfo antPathInfo = ftpPathMatcher.getAntPathInfo(uri);

            DataObject dataObject = null;
            if (antPathInfo.getPathVariableValue() == null) {
                List<FtpServer> serverList = new ArrayList<>();
                serverList = ftpServerDao.listFtpServers();
                FtpServersBuilder builder = new FtpServersBuilder();
                builder.setFtpServer(serverList);
                dataObject = builder.build();
            } else {
                FtpServer ftpServer = getFtpServerByCondition(antPathInfo);
                dataObject = ftpServer;
                if (dataObject == null) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "the request ftp server is not existed");
                }
            }

            String identifier = uri.substring(CONFIG_URL_PREFIX.length());
            return SerializeUtil.serializeDataObject2Json(identifier, dataObject);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "failed to get data from the sft tree,the reason is " + ex.getMessage());
        }
    }

    private FtpServer getFtpServerByCondition(AntPathInfo antPathInfo) {
        log.debug("get ftp server by condition");
        FtpPathVariable ftpPathVariable = FtpPathVariable.getByName(antPathInfo.getPathVariable());
        String pathVariableValue = antPathInfo.getPathVariableValue();
        assert ftpPathVariable != null;
        FtpServer ftpServer = null;
        if (ftpPathVariable.equals(FtpPathVariable.Name)) {
            ftpServer = ftpServerDao.getFtpServerByName(pathVariableValue);
        }
        return ftpServer;
    }
}
