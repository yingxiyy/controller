/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.properties.nms;

import java.io.Serializable;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author: xinyzhao
 * @date: 2021/4/7
 */
@Data
@Slf4j
public class NMSOperationsHandlersMap implements Serializable {

    private String desc;

    private List<NMSOperation> nms;

}
