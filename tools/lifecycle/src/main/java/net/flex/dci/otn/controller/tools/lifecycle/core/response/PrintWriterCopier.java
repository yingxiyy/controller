/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.lifecycle.core.response;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/30 14:23
 */
public class PrintWriterCopier {

    private CharArrayWriter charArrayWriter;
    private PrintWriter writer;

    public PrintWriterCopier() {
        this.charArrayWriter = new CharArrayWriter();
        this.writer = new PrintWriter(charArrayWriter);
    }

    public char[] getCopy() {
        return charArrayWriter.toCharArray();
    }

    public byte[] getBytes() {
        return charArrayWriter.toString().getBytes(StandardCharsets.UTF_8);
    }


    public PrintWriter getWriter() {
        return writer;
    }

}
