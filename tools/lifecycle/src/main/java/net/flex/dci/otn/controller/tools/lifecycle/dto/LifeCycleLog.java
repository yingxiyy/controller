/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.lifecycle.dto;

import java.io.Serializable;
import lombok.Data;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/16 14:47
 */
@Data
public class LifeCycleLog implements Serializable {

    private Long sessionId;

    private String oper_url;

    private String oper_req_param;

    private String clientIp;

    private String method;

    private String oper_resp_param;

    private Long createTimestamp;

    private String operModel;

    private String operation;

    private String operationName;

    private String operDesc;

    private String operIp;

    private String exp_msg;

    private String operStatus;

    private String objectType;

    private String objectId;
}
