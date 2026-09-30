/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.lifecycle.core;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.ServletComponentScan;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/30 10:07
 */
@Slf4j
@ServletComponentScan
@WebFilter(filterName = "ParamsRequestFilter", urlPatterns = {"/*"})
public class RequestFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
            FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
        HttpResponseWrapper responseCopier = new HttpResponseWrapper(
                httpServletResponse);
        try {

            filterChain.doFilter(new HttpRequestWrapper((HttpServletRequest) servletRequest),
                    responseCopier);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        writeResponse(responseCopier, servletResponse);
    }

    private void writeResponse(HttpResponseWrapper copier, ServletResponse response)
            throws IOException {
        if (copier.isUseWriter()) {
            PrintWriter out = response.getWriter();
            out.write(copier.getWriterCopy());
            out.flush();
            out.close();
        } else {
            OutputStream out = response.getOutputStream();
            out.write(copier.getStreamCopy());
            out.flush();
            out.close();
        }
    }
}
