/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.lifecycle.properties.dto;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/29 13:23
 */
@Data
public class LifecycleOperationsDetailMap implements Serializable {


    private String desc;

    @JSONField(name = "modules")
    private List<ModuleOperations> map;

}
