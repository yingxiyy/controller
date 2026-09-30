/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.pm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class UploadPmImplTest {

    @Test
    void usesMillisecondsForHistoryPmIntervals() {
        assertEquals(900000L, UploadPmImpl.historyPmIntervalMillis("15Min"));
        assertEquals(86400000L, UploadPmImpl.historyPmIntervalMillis("24Hour"));
    }
}
