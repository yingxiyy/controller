package net.flex.dci.otn.controller.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otn.controller.module.DeviceLogDownloadRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import net.flex.dci.otn.controller.devicelog.DeviceLogServiceImpl;

import javax.servlet.http.HttpServletResponse;

@Slf4j
@RestController
@RequiredArgsConstructor
public class DeviceLogController {
    private final DeviceLogServiceImpl deviceLogService;

    @RequestMapping(value = "/deviceLog/download", method = RequestMethod.POST)
    public void download(@RequestBody DeviceLogDownloadRequest downloadRequest, HttpServletResponse response) throws CommonException {
        deviceLogService.download(downloadRequest.getNeId(), response);
    }
}
