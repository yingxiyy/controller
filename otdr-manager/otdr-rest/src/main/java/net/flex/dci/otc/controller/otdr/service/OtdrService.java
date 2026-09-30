package net.flex.dci.otc.controller.otdr.service;

import com.alibaba.fastjson.JSONObject;
import net.flex.dci.otc.common.exception.CommonException;

/**
 * @version 1.0
 * @date 2022/8/30 11:09
 */
public interface OtdrService {

    String getOtdrResult(String input);

    JSONObject getOtdrMonitorStatus(String input);

    Object startOtdrOutput(String input, String user);

    void setOtdrBaseBenchmark(String requestBody) throws CommonException;
}
