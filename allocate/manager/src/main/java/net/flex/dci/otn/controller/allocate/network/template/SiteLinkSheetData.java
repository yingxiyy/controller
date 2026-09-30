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
public class SiteLinkSheetData {

    @ExcelProperty("主备")
    private String primarySecondary;
    @ExcelProperty("A点")
    private String aNeId;
    @ExcelProperty("A点IP")
    private String aTpName;
    @ExcelProperty("Z点")
    private String zNeId;
    @ExcelProperty("z点IP")
    private String zTpName;
}
