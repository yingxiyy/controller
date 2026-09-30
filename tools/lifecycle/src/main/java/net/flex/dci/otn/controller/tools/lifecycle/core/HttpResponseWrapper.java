/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.lifecycle.core;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import net.flex.dci.otn.controller.tools.lifecycle.core.response.PrintWriterCopier;
import net.flex.dci.otn.controller.tools.lifecycle.core.response.ServletOutputStreamCopier;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/30 13:40
 */
public class HttpResponseWrapper extends HttpServletResponseWrapper {


    private ServletOutputStreamCopier streamCopier;

    private PrintWriterCopier writerCopier;

    private boolean useWriter;

    public HttpResponseWrapper(HttpServletResponse response) throws IOException {
        super(response);
        useWriter = true;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writerCopier != null) {
            throw new IllegalStateException(
                    "getWriter() has already been called on this response.");
        }

        if (streamCopier == null) {
            useWriter = false;
            streamCopier = new ServletOutputStreamCopier();
        }

        return streamCopier;
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (streamCopier != null) {
            throw new IllegalStateException(
                    "getOutputStream() has already been called on this response.");
        }

        if (writerCopier == null) {
            useWriter = true;
            writerCopier = new PrintWriterCopier();
        }

        return writerCopier.getWriter();
    }

    public byte[] getBytes() {
        if (streamCopier == null && writerCopier == null) {
            return new byte[0];
        }
        return useWriter ? writerCopier.getBytes() : streamCopier.getCopy();
    }


    public byte[] getStreamCopy() {
        if (useWriter) {
            throw new IllegalStateException("already use writer, please call getWriterCopy()");
        }
        return streamCopier == null ? new byte[0] : streamCopier.getCopy();
    }

    public char[] getWriterCopy() {
        if (!useWriter) {
            throw new IllegalStateException(
                    "already use outputStream, please call getStreamCopy()");
        }

        return writerCopier == null ? new char[0] : writerCopier.getCopy();
    }

    public boolean isUseWriter() {
        return useWriter;
    }
}
