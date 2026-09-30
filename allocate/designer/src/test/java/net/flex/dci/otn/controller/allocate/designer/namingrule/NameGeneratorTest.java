/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.namingrule;

import static org.junit.jupiter.api.Assertions.*;

import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import org.junit.jupiter.api.Test;

class NameGeneratorTest {

    @Test
    public void getTransceiverNameByTpName() throws NeDesignerException {
        assertEquals("TRANSCEIVER-1-1-SIGOSC",NameGenerator.getTransceiverNameByTpName("TRANSCEIVER?OSC","OA-1-1-SIG"));
        assertEquals("TRANSCEIVER-1-1-L1",NameGenerator.getTransceiverNameByTpName("TRANSCEIVER?","200G-C4L2-1-1-L1"));
    }

}