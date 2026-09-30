/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.network.template;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
@Builder
public class SiteLinkRelationSheetData {

    @ExcelProperty("复用段A")
    private String linkA;
    @ExcelProperty("复用段A设备")
    private String aNeId;
    @ExcelProperty("复用段A设备IP")
    private String aNeIP;
    @ExcelProperty("复用段A端口")
    private String aTpName;

    @ExcelProperty("复用段Z")
    private String linkZ;
    @ExcelProperty("复用段Z设备")
    private String zNeId;
    @ExcelProperty("复用段Z设备IP")
    private String zNeIP;
    @ExcelProperty("复用段Z端口")
    private String zTpName;
}
