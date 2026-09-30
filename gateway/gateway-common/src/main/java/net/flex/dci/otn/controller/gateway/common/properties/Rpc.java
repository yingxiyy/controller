/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.gateway.common.properties;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * @author: xinyzhao
 * @date: 2021/3/24
 */
@Data
public class Rpc implements Serializable {

    private String module;

    private List<String> paths;
}
