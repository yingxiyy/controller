/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.network;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

public class StepToe {
    List<Callable<CreationResult>> toeList;

    public StepToe() {
        toeList = new LinkedList<>();
    }

    public void addToe(Callable<CreationResult> toe) {
        toeList.add(toe);
    }

    public List<Callable<CreationResult>> getToeList() {
        return toeList;
    }
}
