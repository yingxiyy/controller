package net.flex.dci.otn.controller.sftp.manager.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.sftp.manager.service.SftpServerService;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @version 1.0
 * @date 9/23/2025 1:44 PM
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class FtpServerOperationController {

    private final SftpServerService sftpServerService;

    @RequestMapping(value = "/restconf/operations/ftp-server:put", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String uploadFile(@RequestBody String input) {
        log.debug("upload file to the ftp server the input:{}", input);
        String output = sftpServerService.uploadFileToFtpServer(input);
        return output;
    }


    @RequestMapping(value = "/restconf/operations/ftp-server:get", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String downloadFile(@RequestBody String input) {
        log.debug("download file from ftp server the input:{}", input);
        String output = sftpServerService.downloadFileFromFtpServer(input);
        return output;
    }

    @RequestMapping(value = "/restconf/operations/ftp-server:list", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String retrieveFile(@RequestBody String input) {
        log.debug("retrieve files from ftp server the input:{}", input);
        String output = sftpServerService.retrieveFileFromFtpServerDirectory(input);
        return output;
    }


    @RequestMapping(value = "/restconf/operations/ftp-server:mkdir", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String mkdir(@RequestBody String input) {
        log.debug("make directory for ftp server the input:{}", input);
        String output = sftpServerService.mkdirForFtpServer(input);
        return output;
    }

    @RequestMapping(value = "/restconf/operations/ftp-server:rm", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String rm(@RequestBody String input) {
        log.debug("remove file based on input:{}", input);
        String output = sftpServerService.rmFtpFile(input);
        return output;
    }

    @RequestMapping(value = "/restconf/operations/ftp-server:rm-folder", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    public String rmFolder(@RequestBody String input) {
        log.debug("remove folder based on input:{}", input);
        String output = sftpServerService.rmFtpFolder(input);
        return output;
    }


}
