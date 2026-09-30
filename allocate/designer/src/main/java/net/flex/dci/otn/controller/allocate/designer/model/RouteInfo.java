/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Builder
@Data
public class RouteInfo {

    @NonNull
    Route main;
    Route slave;
    Route third;

}
