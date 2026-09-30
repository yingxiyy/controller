/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.common.service;

//import net.flex.dci.otc.common.exception.CommonException;
//import net.flex.dci.otc.common.exception.CommonExceptionType;
//import net.flex.dci.otn.controller.batchlock.core.BatchLockTransaction;
//import net.flex.dci.otn.controller.batchlock.lib.ZooKeeperToolset;
import org.springframework.stereotype.Service;

/**
 * @author YYX
 * @version 1.0
 */
@Service
public class AllocateLocker {

//  private BatchLockTransaction locker = null;
//
//  public void lockResource(String id) throws CommonException {
//    if (locker == null) {
//      locker = ZooKeeperToolset.instance().newTransaction();
//    }
//    locker.lock(id);
//  }
//
//  public void lock() throws CommonException {
//    locker.require();
//    if (locker.require_once() == false) {
//      throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
//              "resource has been used by other thread");
//    }
//  }
//
//  public void unlock() throws CommonException {
//    if (locker != null) {
//      locker.dismiss();
//    }
//  }
}
