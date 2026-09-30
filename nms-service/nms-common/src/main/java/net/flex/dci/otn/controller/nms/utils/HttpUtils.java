/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;

/**
 * filter for http request
 *
 * @author: xinyzhao
 * @date: 2021/3/29
 */
@Slf4j
public class HttpUtils {

    /**
     * get request body from the http servlet request
     *
     * @param request
     * @return
     * @throws IOException
     */
    public static String getRequestBody(HttpServletRequest request) {
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(
                    new InputStreamReader(request.getInputStream()));
            String str;
            while ((str = reader.readLine()) != null) {
                sb.append(str);
            }
            reader.close();
        } catch (IOException exception) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to read the request body");
        } finally {
            if (null != reader) {
                try {
                    reader.close();

                } catch (IOException ex) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "failed to read the request body");
                }
            }
        }
        return sb.toString();
    }

    /**
     * extract url
     *
     * @param request
     * @return
     */
    public static String extractURL(HttpServletRequest request)
            throws UnsupportedEncodingException {
        String url = request.getRequestURI();
        url = URLDecoder.decode(url, "UTF-8");
        return url;
    }
}
