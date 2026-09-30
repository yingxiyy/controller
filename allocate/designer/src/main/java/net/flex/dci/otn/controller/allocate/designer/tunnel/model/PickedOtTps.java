/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Builder
@Data
public class PickedOtTps {
    @NonNull
    private String ctp; // the  picked C port TP
    @NonNull
    private String ltp;// the  picked L port TP

    @NonNull
    private String lPortSlot;//e.g. "/odu4x2=1/odu4=1"
}
