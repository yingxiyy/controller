/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.gateway.common.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Flux;

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
    public static String getRequestBody(ServerHttpRequest request) {
//        AtomicReference<String> bodyRef = new AtomicReference<>();
        StringBuilder sb = new StringBuilder();
        Flux<DataBuffer> body = request.getBody();
        body.subscribe(buffer -> {
            CharBuffer charBuffer = StandardCharsets.UTF_8.decode(buffer.asByteBuffer());
            DataBufferUtils.release(buffer);
            sb.append(charBuffer.toString());
//            bodyRef.set(charBuffer.toString());
        });
        return sb.toString();
//        BufferedReader reader = new BufferedReader(new InputStreamReader(request.getBody()));
//        StringBuilder sb = new StringBuilder();
//        try {
//            String str;
//            while ((str = reader.readLine()) != null) {
//                sb.append(str);
//            }
//            reader.close();
//        } finally {
//            if (null != reader) {
//                try {
//                    reader.close();
//
//                } catch (IOException ex) {
//                    log.error("failed to close reader,{}", ex);
//                }
//            }
//        }
//        return sb.toString();
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
