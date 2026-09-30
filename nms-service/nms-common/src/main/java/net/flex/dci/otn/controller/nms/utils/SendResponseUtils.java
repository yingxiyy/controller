/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.utils;

import com.alibaba.fastjson.JSON;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.GZIPOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.nms.model.NMSResult;
import net.flex.dci.otn.controller.nms.model.Output;

/**
 * UTILS FOR RETURN  MESSAGE
 *
 * @author:
 * @date: 2021/3/29
 */
@Slf4j
public class SendResponseUtils {

    public static void sendResponse(HttpServletRequest request, HttpServletResponse response,
            Object obj) {
        sendJsonResponse(request, response, obj);
    }


    private static void sendJsonResponse(HttpServletRequest request, HttpServletResponse response,
            Object obj) {

//            String result = JSON.toJSONString(JSONObject.parseObject(String.valueOf(obj)));
        String result = toJson(obj);
        boolean gzipSupported = Optional.ofNullable(request.getHeader("Accept-Encoding"))
                .map(h -> h.contains("gzip"))
                .orElse(false);

        response.setCharacterEncoding(Constants.CHARSET_UTF8);
        response.setContentType(Constants.JSON_CONTENT_TYPE);
        try {
            if (gzipSupported && result.length() > 1024) {
                response.setHeader("Content-Encoding", "gzip");
                try (GZIPOutputStream gzip = new GZIPOutputStream(response.getOutputStream())) {
                    gzip.write(result.getBytes(StandardCharsets.UTF_8));
                }
            } else {
                response.getWriter().write(result);
            }
        } catch (IOException ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    private static String toJson(Object obj) {
        return (obj instanceof String) ? String.valueOf(obj) : JSON.toJSONString(obj);
    }

    public static String successNMSResult() {
        NMSResult result = new NMSResult();
        Output output = new Output();
        output.setReturnCode("success");
        result.setOutput(output);
        return JSON.toJSONString(result);
    }
}
