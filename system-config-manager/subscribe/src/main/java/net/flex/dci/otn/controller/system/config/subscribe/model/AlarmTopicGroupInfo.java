/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.subscribe.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

/**
 * @version 1.0
 * @date 2021/12/8 15:59
 */
@Data
@Builder
@AllArgsConstructor
public class AlarmTopicGroupInfo implements Serializable {

    @Tolerate
    public AlarmTopicGroupInfo() {

    }

    private Long id;

    private String name;

    private String description;

    private String neGroup;

}
