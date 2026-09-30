/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.rpc;


import net.flex.dci.otc.common.exception.CommonException;

/**
 * @date: 2021/3/30
 */
public interface IRpc {

    //get
    //getPaged
    String executeRequest(String cmd, String requestBody) throws CommonException;
}
