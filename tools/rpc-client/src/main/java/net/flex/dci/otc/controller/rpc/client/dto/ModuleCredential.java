/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/8/15 21:00
 */
@Data
public class ModuleCredential implements Serializable {

    private String ip;

    private Integer port;

    private String username;

    private String password;

}
