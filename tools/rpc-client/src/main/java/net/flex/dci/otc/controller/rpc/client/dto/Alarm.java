/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;

/**
 * @version 1.0
 * @date 2021/8/31 10:18
 */
@Data
@Builder
public class Alarm implements Serializable {

    private Long index;


    private String alarmId;

    private String resourceRef;

    private String componentRef;


    private AlarmSeverity severity;

    private String alarmTypeId;

    private String alarmText;

    private String alarmGroup;

    private Long creationTime;

    private Long nmlReceivedTime;

    private String neId;

    private String nmlKey;

    private Boolean sa;

    //alarm source ip
    private String ip;

    private String nmlKeyName;
}
