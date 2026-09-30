/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.common.nbi.impl;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**
 * @author YYX
 * @version 1.0
 */
public class Step {
  private final static ListeningExecutorService service = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(20));

  private List<StepToe> stepToeList;
  private int pos;

  public Step() {
    this.stepToeList = new LinkedList<>();
    pos = 0;
  }

  public void addStepToe(StepToe toe) {
    stepToeList.add(toe);
  }

  public void addStep(Step step) {
    stepToeList.addAll(step.getStepToes());
  }

  public boolean hasNext() {
    if (pos == stepToeList.size())
      return false;
    else
      return true;
  }

  public List<ListenableFuture<StepResult>> startNext() {
    List<ListenableFuture<StepResult>> listenableFutures = new LinkedList<>();
    for (Callable doIt : stepToeList.get(pos++).getToeList()) {
      listenableFutures.add(service.submit(doIt));
    }
    return listenableFutures;
  }

  public int getPos() {
    return pos;
  }

  public List<StepToe> getStepToes() {
    return stepToeList;
  }

}


