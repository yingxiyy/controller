/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.lifecycle.core.response;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/30 14:23
 */
public class ServletOutputStreamCopier extends ServletOutputStream {

    private static final int INT_BUFFER_SIZE = 1024;

    private ByteArrayOutputStream copy;

    public ServletOutputStreamCopier() {
        this.copy = new ByteArrayOutputStream(INT_BUFFER_SIZE);
    }

    @Override
    public void write(int b) throws IOException {
        copy.write(b);
    }

    @Override
    public boolean isReady() {
        return false;
    }

    public byte[] getCopy() {
        return copy.toByteArray();
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {

    }

    @Override
    public void flush() throws IOException {
        copy.flush();
    }
}
