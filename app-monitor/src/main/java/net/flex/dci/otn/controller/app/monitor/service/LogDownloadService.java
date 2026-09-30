package net.flex.dci.otn.controller.app.monitor.service;

import net.flex.dci.otc.common.exception.CommonException;

import javax.servlet.http.HttpServletResponse;

public interface LogDownloadService {

    void forward(String service, HttpServletResponse response) throws CommonException;
}
