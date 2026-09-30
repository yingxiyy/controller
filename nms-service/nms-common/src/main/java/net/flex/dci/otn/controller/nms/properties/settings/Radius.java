/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.properties.settings;

import java.io.Serializable;
import lombok.Data;

/**
 * @author: xinyzhao
 * @date: 2021/4/12
 */
@Data
public class Radius implements Serializable {

    private String ip;

    private Integer authPort;

    private String secretKey;

    private Integer retryTimes = 3;

    private Integer timeout = 60;

}
