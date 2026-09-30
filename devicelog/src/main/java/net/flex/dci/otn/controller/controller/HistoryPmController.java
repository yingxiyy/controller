package net.flex.dci.otn.controller.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otn.controller.module.HistoryPmUploadRequest;
import net.flex.dci.otn.controller.pm.UploadPmImpl;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class HistoryPmController {

    private final UploadPmImpl uploadImpl;

    @RequestMapping(value = "/historyPm/upload", method = RequestMethod.POST)
    public ResponseEntity<?> upload(@RequestBody HistoryPmUploadRequest uploadRequest)
            throws CommonException {
        uploadImpl.upload(uploadRequest.getNeIds());

//	HistoryPmUploadResponse response = new HistoryPmUploadResponse();
//        response.setReturnMessage("Upload started");
//        response.setReturnCode("success");
//
//       return ResponseEntity.accepted().body(response);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }
}
