package net.flex.dci.otc.controller.rpc.client.rpcs.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.controller.rpc.client.constants.RpcCommand.FTPRpcCmd;
import net.flex.dci.otc.controller.rpc.client.rpcs.FtpRpc;
import org.asynchttpclient.Response;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ftp.server.rev190802.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 *
 * 2025/10/4
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class FtpRpcImpl extends BasicRpc implements FtpRpc {

    public FtpRpcImpl() {
        this.NAMESPACE = "ftp-server";
        this.MODULE_NAME = "ftpServer";
    }

    @Override
    public PutOutput putFile(String serverName, String remoteFolder, String fileName) {
        log.debug("start to put file the server name is:{} remoteFolder is:{} fileName is:{}",
                serverName, remoteFolder, fileName);
        String requestOp = FTPRpcCmd.PUT;
        try {
            PutInput input = new PutInputBuilder().setFileName(fileName).setServerName(serverName)
                    .setRemoteFolder(remoteFolder).build();
            String requestBody = formRpcInput(requestOp, input);
            Response response = executeReq(requestOp, requestBody);
            String rspBody = response.getResponseBody();
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                PutOutput output = (PutOutput) formRpcOutPut(
                        requestOp, rspBody);
                return output;
            } else {
                String errorMsg = getErrorDetail(rspBody);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errorMsg);
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public RmOutput deleteFile(String serverName, String remoteFolder, String fileName) {
        log.debug("start to remove file from server name is:{} remoteFolder is:{} fileName is:{}",
                serverName, remoteFolder, fileName);
        String requestOp = FTPRpcCmd.RM;
        try {
            RmInput input = new RmInputBuilder().setFileName(fileName).setServerName(serverName)
                    .setRemoteFolder(remoteFolder).build();
            String requestBody = formRpcInput(requestOp, input);
            Response response = executeReq(requestOp, requestBody);
            String rspBody = response.getResponseBody();
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                RmOutput output = (RmOutput) formRpcOutPut(
                        requestOp, rspBody);
                return output;
            } else {
                String errorMsg = getErrorDetail(rspBody);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errorMsg);
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public RmFolderOutput deleteFolder(String serverName, String remoteFolder) {
        log.debug("start to remove folder from server name is:{} remoteFolder is:{}",
                serverName, remoteFolder);
        String requestOp = FTPRpcCmd.RM_FOLDER;
        try {
            RmFolderInput input = new RmFolderInputBuilder().setRemoteFolder(remoteFolder)
                    .setServerName(serverName).build();
            String requestBody = formRpcInput(requestOp, input);
            Response response = executeReq(requestOp, requestBody);
            String rspBody = response.getResponseBody();
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                return (RmFolderOutput) formRpcOutPut(requestOp, rspBody);
            } else {
                String errorMsg = getErrorDetail(rspBody);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errorMsg);
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public GetOutput downloadFile(String serverName, String remoteFolder, String fileName) {
        log.debug("start to download file from server:{} remoteFolder :{} filename:{}", serverName,
                remoteFolder, fileName);
        String requestOp = FTPRpcCmd.DOWNLOAD;
        try {
            GetInput input = new GetInputBuilder().setFileName(fileName).setServerName(serverName)
                    .setRemoteFolder(remoteFolder).build();
            String requestBody = formRpcInput(requestOp, input);
            Response response = executeReq(requestOp, requestBody);
            String rspBody = response.getResponseBody();
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                GetOutput output = (GetOutput) formRpcOutPut(requestOp, rspBody);
                return output;
            } else {
                String errorMsg = getErrorDetail(rspBody);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errorMsg);
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public MkdirOutput mkdir(String serverName, String remoteFolder) {
        log.debug("start to make directory for server:{} remoteFolder:{}", serverName,
                remoteFolder);
        String requestOp = FTPRpcCmd.MKDIR;
        try {
            MkdirInput input = new MkdirInputBuilder().setRemoteFolder(remoteFolder)
                    .setServerName(serverName).build();
            String requestBody = formRpcInput(requestOp, input);
            Response response = executeReq(requestOp, requestBody);
            String rspBody = response.getResponseBody();
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                MkdirOutput mkdirOutput = (MkdirOutput) formRpcOutPut(requestOp, rspBody);
                return mkdirOutput;
            } else {
                String errorMsg = getErrorDetail(rspBody);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errorMsg);
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public ListOutput retrieveServerList(String serverName, String remoteFolder) {
        log.debug("start to retrieve directory from server:{} remoteFolder:{}", serverName,
                remoteFolder);
        String requestOp = FTPRpcCmd.LIST;
        try {
            ListInput input = new ListInputBuilder().setFolderName(remoteFolder)
                    .setServerName(serverName).build();
            String requestBody = formRpcInput(requestOp, input);
            Response response = executeReq(requestOp, requestBody);
            String rspBody = response.getResponseBody();
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                ListOutput output = (ListOutput) formRpcOutPut(requestOp, rspBody);
                return output;
            } else {
                String errorMsg = getErrorDetail(rspBody);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errorMsg);
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }
}
