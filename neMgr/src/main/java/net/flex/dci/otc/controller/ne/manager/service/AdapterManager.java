/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.ne.manager.service;

import javax.servlet.http.HttpServletRequest;
import net.flex.dci.otc.common.exception.CommonException;

/**
 * @version 1.0
 * @date 2021/12/16 13:17
 */
public interface AdapterManager {

//    String createAdapter(String input) throws CommonException;
//
//    String deleteAdapter(String input) throws CommonException;

    String getAdapterByCondition(HttpServletRequest request) throws CommonException;

//    public void createAdapter(CreateAdapterInput input) throws CommonException;

}
