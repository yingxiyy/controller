/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.component;

import java.util.concurrent.ExecutionException;
import net.flex.dci.otn.controller.nms.exceptions.RpcException;

/**
 * @author: xinyzhao
 * @date: 2021/3/25
 */
public interface IRpcExecutor {

    String executeRpc(String requestBody, String url)
            throws RpcException, ExecutionException, InterruptedException;
}
