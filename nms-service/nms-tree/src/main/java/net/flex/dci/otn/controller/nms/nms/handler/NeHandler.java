/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;

/**
 * @date: 2021/4/9
 */
@Slf4j
public class NeHandler extends AbstractBaseHandler {

    public NeHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

//    @Override
//    public List<String> getNeUnitList(GetNeUnitListInput input) {
//        log.debug("getNeUnitList input {}", input);
//        return null;
//    }
}
