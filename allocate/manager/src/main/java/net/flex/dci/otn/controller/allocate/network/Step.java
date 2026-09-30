/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.flex.dci.otn.controller.allocate.network;

import com.google.common.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
public class Step<V> {
    private final static ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());

    private StepToe stepToe;

    public Step(StepToe toe) {
        this.stepToe = toe;
    }

    public void start(FutureCallback<List<V>> endingWork) {
        List<ListenableFuture<V>> listenableFutures = new LinkedList<>();
        for (Callable doIt : stepToe.getToeList()) {
            listenableFutures.add(service.submit(doIt));
        }

        ListenableFuture<List<V>> futures = Futures.allAsList(listenableFutures);
        Futures.addCallback(futures, endingWork, service);

    }

}



