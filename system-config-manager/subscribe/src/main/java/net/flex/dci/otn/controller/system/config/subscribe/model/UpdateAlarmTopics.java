/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.subscribe.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * @version 1.0
 * @date 2021/12/10 10:14
 */
@Data
public class UpdateAlarmTopics implements Serializable {

    @JsonProperty("alarm-topics")
    List<AlarmTopic> alarmTopics;

}
