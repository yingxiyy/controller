/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.dto;

import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2021/11/29 14:23
 */
@Data
@Builder
public class PlaneViewInfoDto {

    private String plane;

    private ViewTopoDto planeTopo;
}
