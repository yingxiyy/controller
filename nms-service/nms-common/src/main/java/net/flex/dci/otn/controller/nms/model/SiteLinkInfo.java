/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.model;

import java.io.Serializable;
import lombok.Data;

/**
 * @date: 2021/4/6
 */
@Data
public class SiteLinkInfo implements Serializable {

    public String id;

    public String plane;

    public String name;

    public SiteLinkInfo(String id, String plane, String name) {
        this.id = id;
        this.plane = plane;
        this.name = name;
    }

}
