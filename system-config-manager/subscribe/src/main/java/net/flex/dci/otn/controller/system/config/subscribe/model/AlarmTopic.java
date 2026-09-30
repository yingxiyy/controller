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

/**
 * @version 1.0
 * @date 2021/12/9 10:26
 */
@Data
@Builder
@AllArgsConstructor
public class AlarmTopic implements Serializable {


    private String severity;

    private String alarmGroup;

    private String alarmTypeId;

    private String alarmText;

    private Long id;

    private Boolean subscribe;

    private Long boardAlarmType;
}
