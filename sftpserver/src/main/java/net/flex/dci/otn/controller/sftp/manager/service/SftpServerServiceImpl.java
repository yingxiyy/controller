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
import net.flex.dci.otc.serialization.util.SerializeUtil;
import net.flex.dci.otn.controller.sftp.manager.manager.SftpServerManager;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class SftpServerServiceImpl implements SftpServerService {


    private final SftpServerManager sftpServerManager;


    @Override
    public String createServer(String request) throws CommonException {
        log.info("start to create sftp server ");
        CreateFtpServerInput inputObj = SerializeUtil.parseRpcInput(request,
                CreateFtpServerInput.class);

        if (inputObj == null || StringUtils.isEmpty(inputObj.getName())) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "input params is null or key attribute name is null");
        }
        sftpServerManager.createSftpServer(inputObj);
        CreateFtpServerOutputBuilder outputBuilder = new CreateFtpServerOutputBuilder();
        outputBuilder.setReturnCode(RpcResultType.Success);

        return SerializeUtil.serializeRpcOutput2Json(outputBuilder.build());
    }

    @Override
    public String updateServer(String request) throws CommonException {
        log.info("start to update sftp server");
        UpdateFtpServerInput inputObj = SerializeUtil.parseRpcInput(request,
                UpdateFtpServerInput.class);
        log.info("update the ftp server :{} total info", request);
        if (inputObj == null || StringUtils.isEmpty(inputObj.getId())) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "input params is null or key attribute id is null");
        }
        sftpServerManager.updateSftpServer(inputObj);
        UpdateFtpServerOutputBuilder outputBuilder = new UpdateFtpServerOutputBuilder();
        outputBuilder.setReturnCode(RpcResultType.Success);
        return SerializeUtil.serializeRpcOutput2Json(outputBuilder.build());
    }

    @Override
    public String deleteServer(String request) throws CommonException {
        log.info("start to delete the sftp server");
        log.debug("start to delete the sftp server,the request body is:{}", request);
        DeleteFtpServerInput inputObj = SerializeUtil.parseRpcInput(request,
                DeleteFtpServerInput.class);
        if (inputObj == null || StringUtils.isEmpty(inputObj.getId())) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "input params is null or key attribute id is null");
        }
        sftpServerManager.deleteSftpServer(inputObj);
        DeleteFtpServerOutputBuilder outputBuilder = new DeleteFtpServerOutputBuilder();
        outputBuilder.setReturnCode(RpcResultType.Success);
        return SerializeUtil.serializeRpcOutput2Json(outputBuilder.build());
    }

    @Override
    public String uploadFileToFtpServer(String request) throws CommonException {
        log.debug("upload file to ftp server,the input is:{}", request);
        PutInput putInput = SerializeUtil.parseRpcInput(request, PutInput.class);
        PutOutput putOutput = sftpServerManager.uploadFile(putInput);
        return SerializeUtil.serializeRpcOutput2Json(putOutput);
    }

    @Override
    public String downloadFileFromFtpServer(String request) throws CommonException {
        log.debug("download file from ftp server,the input is:{}", request);
        GetInput getInput = SerializeUtil.parseRpcInput(request, GetInput.class);
        GetOutput getOutput = sftpServerManager.downloadFile(getInput);
        return SerializeUtil.serializeRpcOutput2Json(getOutput);
    }

    @Override
    public String retrieveFileFromFtpServerDirectory(String request) throws CommonException {
        log.debug("retrieve file from ftp server directory,input is:{}", request);
        ListInput listInput = SerializeUtil.parseRpcInput(request, ListInput.class);
        ListOutput listOutput = sftpServerManager.retrieveServerDirectory(listInput);
        return SerializeUtil.serializeRpcOutput2Json(listOutput);
    }

    @Override
    public String mkdirForFtpServer(String request) throws CommonException {
        log.debug("make directory for ftp server,input is:{}", request);
        MkdirInput mkdirInput = SerializeUtil.parseRpcInput(request, MkdirInput.class);
        MkdirOutput mkdirOutput = sftpServerManager.mkdir(mkdirInput);
        return SerializeUtil.serializeRpcOutput2Json(mkdirOutput);
    }

    @Override
    public String rmFtpFile(String request) throws CommonException {
        log.debug("remove file from ftp server,input is:{}", request);
        RmInput rmInput = SerializeUtil.parseRpcInput(request, RmInput.class);
        RmOutput rmOutput = sftpServerManager.rmFile(rmInput);
        return SerializeUtil.serializeRpcOutput2Json(rmOutput);
    }

    @Override
    public String rmFtpFolder(String request) throws CommonException {
        log.debug("remove folder from ftp server,input is:{}", request);
        RmFolderInput rmInput = SerializeUtil.parseRpcInput(request, RmInput.class);
        RmFolderOutput rmOutput = sftpServerManager.rmFolder(rmInput);
        return SerializeUtil.serializeRpcOutput2Json(rmOutput);
    }


}
