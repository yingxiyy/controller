/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.component;

import javax.servlet.http.HttpServletRequest;
import net.flex.dci.otc.common.exception.CommonException;

/**
 * @author: xinyzhao
 * @date: 2021/3/24
 */
public interface ITopologyTree {

    String getNetWorkTopology(String identifier) throws CommonException;

    String mergeNetworkTopology(HttpServletRequest request,
            String identifier)
            throws CommonException;
}
